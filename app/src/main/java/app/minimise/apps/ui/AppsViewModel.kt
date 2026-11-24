package app.minimise.apps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetOpenLimit
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** @property editingLimit the app whose open limit is being set, if any. */
data class AppsUiState(val rows: List<AppRow> = emptyList(), val editingLimit: AppEntry? = null)

/**
 * @property pending a waiting loosen for this app: the kind it moves to and how long until it does.
 * @property pendingLimit a waiting raise or removal of its open limit.
 */
data class AppRow(val app: AppEntry, val pending: PendingKind?, val pendingLimit: PendingLimit? = null)

data class PendingKind(val kind: AppKind, val remaining: Duration)

/** @property limit null when the limit is being removed. */
data class PendingLimit(val limit: Int?, val remaining: Duration)

@HiltViewModel
class AppsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val editingLimit = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppsUiState> = combine(
        appRepository.apps,
        settingsGuard.pending,
        editingLimit,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { apps, pending, editing, _ ->
        val now = timeSource.now()
        val byPackage = pending.mapNotNull { p -> (p.change as? SetAppKind)?.let { it.packageName to (it to p.applyAt) } }.toMap()
        val limits = pending.mapNotNull { p -> (p.change as? SetOpenLimit)?.let { it.packageName to (it to p.applyAt) } }.toMap()
        AppsUiState(
            rows = apps.map { app ->
                val waiting = byPackage[app.packageName]
                AppRow(
                    app = app,
                    pending = waiting?.let { (change, applyAt) -> PendingKind(change.kind, durationBetween(now, applyAt)) },
                    pendingLimit = limits[app.packageName]?.let { (change, applyAt) -> PendingLimit(change.limit, durationBetween(now, applyAt)) },
                )
            },
            editingLimit = editing?.let { pkg -> apps.firstOrNull { it.packageName == pkg } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    /** Tightening applies at once; loosening shows as pending. Choosing the current kind cancels a pending move. */
    fun onKindChosen(app: AppEntry, kind: AppKind) {
        viewModelScope.launch { settingsGuard.request(SetAppKind(app.packageName, kind)) }
    }

    fun onEditLimit(app: AppEntry) {
        editingLimit.value = app.packageName
    }

    fun onLimitDismissed() {
        editingLimit.value = null
    }

    /** A new or lower limit applies at once; a higher one, or none, waits. */
    fun onLimitChosen(limit: Int?) {
        val packageName = editingLimit.value ?: return
        editingLimit.value = null
        viewModelScope.launch { settingsGuard.request(SetOpenLimit(packageName, limit?.takeIf { it > 0 })) }
    }

    private companion object {
        val REFRESH = 15.seconds
    }
}
