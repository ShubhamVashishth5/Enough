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
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppsUiState(val rows: List<AppRow> = emptyList())

/** @property pending a waiting loosen for this app: the kind it moves to and how long until it does. */
data class AppRow(val app: AppEntry, val pending: PendingKind?)

data class PendingKind(val kind: AppKind, val remaining: Duration)

@HiltViewModel
class AppsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    val uiState: StateFlow<AppsUiState> = combine(
        appRepository.apps,
        settingsGuard.pending,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { apps, pending, _ ->
        val now = timeSource.now()
        val byPackage = pending.mapNotNull { p -> (p.change as? SetAppKind)?.let { it.packageName to (it to p.applyAt) } }.toMap()
        AppsUiState(
            rows = apps.map { app ->
                val waiting = byPackage[app.packageName]
                AppRow(
                    app = app,
                    pending = waiting?.let { (change, applyAt) -> PendingKind(change.kind, durationBetween(now, applyAt)) },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    /** Tightening applies at once; loosening shows as pending. Choosing the current kind cancels a pending move. */
    fun onKindChosen(app: AppEntry, kind: AppKind) {
        viewModelScope.launch { settingsGuard.request(SetAppKind(app.packageName, kind)) }
    }

    private companion object {
        val REFRESH = 15.seconds
    }
}
