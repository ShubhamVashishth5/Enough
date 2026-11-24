package app.minimise.apps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.apps.domain.WebTarget
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

/**
 * @property websites Vault (and formerly Vault) websites, covered in the browsers Enough knows.
 * @property editingLimit the app whose open limit is being set, if any.
 * @property addingWebsite the "Add a website" dialog is open; [websiteInvalid] when the last entry wasn't a domain.
 */
data class AppsUiState(
    val rows: List<AppRow> = emptyList(),
    val websites: List<AppRow> = emptyList(),
    val editingLimit: AppEntry? = null,
    val addingWebsite: Boolean = false,
    val websiteInvalid: Boolean = false,
)

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
    private val websiteDialog = MutableStateFlow<WebsiteDialog?>(null)

    private val everything = combine(appRepository.apps, appRepository.webTargets) { apps, websites -> apps to websites }

    val uiState: StateFlow<AppsUiState> = combine(
        everything,
        settingsGuard.pending,
        editingLimit,
        websiteDialog,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { (apps, websites), pending, editing, dialog, _ ->
        val now = timeSource.now()
        val byPackage = pending.mapNotNull { p -> (p.change as? SetAppKind)?.let { it.packageName to (it to p.applyAt) } }.toMap()
        val limits = pending.mapNotNull { p -> (p.change as? SetOpenLimit)?.let { it.packageName to (it to p.applyAt) } }.toMap()
        fun rowFor(app: AppEntry) = AppRow(
            app = app,
            pending = byPackage[app.packageName]?.let { (change, applyAt) -> PendingKind(change.kind, durationBetween(now, applyAt)) },
            pendingLimit = limits[app.packageName]?.let { (change, applyAt) -> PendingLimit(change.limit, durationBetween(now, applyAt)) },
        )
        AppsUiState(
            rows = apps.map(::rowFor),
            websites = websites.map(::rowFor),
            editingLimit = editing?.let { pkg -> apps.firstOrNull { it.packageName == pkg } },
            addingWebsite = dialog != null,
            websiteInvalid = dialog?.invalid == true,
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

    fun onAddWebsite() {
        websiteDialog.value = WebsiteDialog()
    }

    fun onWebsiteDismissed() {
        websiteDialog.value = null
    }

    /** A new Vault website is tightening, so it applies at once. */
    fun onWebsiteEntered(text: String) {
        val domain = WebTarget.normalise(text)
        if (domain == null) {
            websiteDialog.value = WebsiteDialog(invalid = true)
            return
        }
        websiteDialog.value = null
        viewModelScope.launch { settingsGuard.request(SetAppKind(WebTarget.idFor(domain), AppKind.VAULT)) }
    }

    private data class WebsiteDialog(val invalid: Boolean = false)

    private companion object {
        val REFRESH = 15.seconds
    }
}
