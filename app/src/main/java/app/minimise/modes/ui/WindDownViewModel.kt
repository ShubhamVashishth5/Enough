package app.minimise.modes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.modes.domain.WindDownPlan
import app.minimise.modes.domain.WindDownPlans
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @property on wind-down is set up (the Sleep mode runs every night).
 * @property draft the plan being edited; starts as the saved one.
 * @property pendingIn a waiting change to the Sleep mode (wind-down off, a later bedtime) and how long it waits.
 */
data class WindDownUiState(
    val loaded: Boolean = false,
    val on: Boolean = false,
    val draft: WindDownPlan = WindDownPlans.DEFAULT,
    val ebookLabel: String? = null,
    val toolApps: List<AppEntry> = emptyList(),
    val pendingIn: Duration? = null,
    val result: GuardResult? = null,
)

/** Wind-down is the Sleep mode on a nightly schedule, so every change goes through SettingsGuard as a mode edit. */
@HiltViewModel
class WindDownViewModel @Inject constructor(
    modeRepository: ModeRepository,
    appRepository: AppRepository,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val draft = MutableStateFlow<WindDownPlan?>(null)
    private val result = MutableStateFlow<GuardResult?>(null)
    private var sleep: Mode? = null

    val uiState: StateFlow<WindDownUiState> = combine(
        modeRepository.modes,
        appRepository.apps,
        settingsGuard.pending,
        draft,
        result,
    ) { modes, apps, pending, draft, result ->
        val sleepMode = modes.firstOrNull { it.isSleep }.also { sleep = it }
        val saved = sleepMode?.let(WindDownPlans::of)
        val plan = draft ?: saved ?: WindDownPlans.DEFAULT
        WindDownUiState(
            loaded = sleepMode != null,
            on = saved != null,
            draft = plan,
            ebookLabel = plan.ebook?.let { pkg -> apps.firstOrNull { it.packageName == pkg }?.label },
            toolApps = apps.filter { it.kind == AppKind.TOOL },
            pendingIn = pending.firstOrNull { (it.change as? SaveMode)?.id == sleepMode?.id }?.let { durationBetween(timeSource.now(), it.applyAt) },
            result = result,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WindDownUiState())

    fun onBedtime(time: LocalTime) = edit { it.copy(bedtime = time) }

    fun onWake(time: LocalTime) = edit { it.copy(wake = time) }

    fun onEbook(packageName: String?) = edit { it.copy(ebook = packageName) }

    fun onSave() = request(uiState.value.draft)

    fun onTurnOff() = request(null)

    private fun edit(change: (WindDownPlan) -> WindDownPlan) {
        draft.value = change(uiState.value.draft)
        result.value = null
    }

    private fun request(plan: WindDownPlan?) {
        val mode = sleep ?: return
        viewModelScope.launch {
            result.value = settingsGuard.request(WindDownPlans.change(mode, plan))
            draft.value = null
        }
    }
}
