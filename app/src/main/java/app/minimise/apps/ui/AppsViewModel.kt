package app.minimise.apps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.data.AppActions
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.apps.domain.WebTarget
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.protection.ProtectionStatus
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetBreatheWait
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
 * @property editingBreatheWait the Breathe app whose pause is being set, if any.
 * @property addingWebsite the "Add a website" dialog is open; [websiteInvalid] when the last entry wasn't a domain.
 */
data class AppsUiState(
    val rows: List<AppRow> = emptyList(),
    val websites: List<AppRow> = emptyList(),
    val editingLimit: AppEntry? = null,
    val editingBreatheWait: AppEntry? = null,
    val addingWebsite: Boolean = false,
    val websiteInvalid: Boolean = false,
    /** Websites are read from the address bar, which only Full protection can see. */
    val websitesCovered: Boolean = true,
    /** The app whose App info / Uninstall choice is open, after a long-press. */
    val actionsFor: AppEntry? = null,
)

/**
 * @property pending a waiting loosen for this app: the kind it moves to and how long until it does.
 * @property pendingLimit a waiting raise or removal of its open limit.
 * @property pendingBreatheWait a waiting shorter Breathe pause.
 */
data class AppRow(
    val app: AppEntry,
    val pending: PendingKind?,
    val pendingLimit: PendingLimit? = null,
    val pendingBreatheWait: PendingBreatheWait? = null,
)

data class PendingKind(val kind: AppKind, val remaining: Duration)

/** @property limit null when the limit is being removed. */
data class PendingLimit(val limit: Int?, val remaining: Duration)

/** @property seconds null when the app goes back to the usual pause. */
data class PendingBreatheWait(val seconds: Int?, val remaining: Duration)

@HiltViewModel
class AppsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
    private val protectionStatus: ProtectionStatus,
    private val appActions: AppActions,
) : ViewModel() {
    private val editingLimit = MutableStateFlow<String?>(null)
    private val editingBreatheWait = MutableStateFlow<String?>(null)
    private val websiteDialog = MutableStateFlow<WebsiteDialog?>(null)
    private val actionsFor = MutableStateFlow<AppEntry?>(null)

    private val everything = combine(appRepository.apps, appRepository.webTargets, actionsFor) { apps, websites, actions ->
        Everything(apps, websites, actions)
    }

    private val editing = combine(editingLimit, editingBreatheWait) { limit, breathe -> Editing(limit, breathe) }

    val uiState: StateFlow<AppsUiState> = combine(
        everything,
        settingsGuard.pending,
        editing,
        websiteDialog,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { (apps, websites, actions), pending, editing, dialog, _ ->
        val now = timeSource.now()
        val byPackage = pending.mapNotNull { p -> (p.change as? SetAppKind)?.let { it.packageName to (it to p.applyAt) } }.toMap()
        val limits = pending.mapNotNull { p -> (p.change as? SetOpenLimit)?.let { it.packageName to (it to p.applyAt) } }.toMap()
        val breatheWaits = pending.mapNotNull { p -> (p.change as? SetBreatheWait)?.let { it.packageName to (it to p.applyAt) } }.toMap()
        fun rowFor(app: AppEntry) = AppRow(
            app = app,
            pending = byPackage[app.packageName]?.let { (change, applyAt) -> PendingKind(change.kind, durationBetween(now, applyAt)) },
            pendingLimit = limits[app.packageName]?.let { (change, applyAt) -> PendingLimit(change.limit, durationBetween(now, applyAt)) },
            pendingBreatheWait = breatheWaits[app.packageName]
                ?.let { (change, applyAt) -> PendingBreatheWait(change.seconds, durationBetween(now, applyAt)) },
        )
        AppsUiState(
            rows = apps.map(::rowFor),
            websites = websites.map(::rowFor),
            editingLimit = editing.limit?.let { pkg -> apps.firstOrNull { it.packageName == pkg } },
            editingBreatheWait = editing.breatheWait?.let { pkg -> apps.firstOrNull { it.packageName == pkg } },
            addingWebsite = dialog != null,
            websiteInvalid = dialog?.invalid == true,
            websitesCovered = protectionStatus.current().readsScreens,
            actionsFor = actions,
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

    fun onEditBreatheWait(app: AppEntry) {
        editingBreatheWait.value = app.packageName
    }

    fun onBreatheWaitDismissed() {
        editingBreatheWait.value = null
    }

    /** A longer pause applies at once; a shorter one, or the usual one when that's shorter, waits. */
    fun onBreatheWaitChosen(wait: Duration?) {
        val packageName = editingBreatheWait.value ?: return
        editingBreatheWait.value = null
        val seconds = wait?.inWholeSeconds?.toInt()?.takeIf { wait in Defaults.BREATHE_WAITS }
        viewModelScope.launch { settingsGuard.request(SetBreatheWait(packageName, seconds)) }
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

    fun onAppLongPressed(app: AppEntry) {
        if (appActions.canManage(app.packageName)) actionsFor.value = app
    }

    fun onAppInfo() {
        actionsFor.value?.let { appActions.openAppInfo(it.packageName) }
        actionsFor.value = null
    }

    /** The system asks to confirm; once it's gone, the list drops it by itself. */
    fun onUninstall() {
        actionsFor.value?.let { appActions.uninstall(it.packageName) }
        actionsFor.value = null
    }

    fun onActionsDismissed() {
        actionsFor.value = null
    }

    private data class WebsiteDialog(val invalid: Boolean = false)

    private data class Everything(val apps: List<AppEntry>, val websites: List<AppEntry>, val actions: AppEntry?)

    /** Package names of the apps whose open limit or Breathe pause is being set. */
    private data class Editing(val limit: String?, val breatheWait: String?)

    private companion object {
        val REFRESH = 15.seconds
    }
}
