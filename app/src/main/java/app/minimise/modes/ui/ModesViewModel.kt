package app.minimise.modes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.settings.domain.SetManualMode
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

data class ModesUiState(
    val modes: List<Mode> = emptyList(),
    /** The mode in effect now, by hand or by schedule. */
    val activeId: Long? = null,
    /** The hand-picked mode; null when following schedules. */
    val manualId: Long? = null,
    /** A waiting switch: the mode it goes to (null: follow schedules) and how long until it does. */
    val pendingSwitch: PendingSwitch? = null,
    val pendingEdits: Map<Long, Duration> = emptyMap(),
)

data class PendingSwitch(val modeId: Long?, val remaining: Duration)

@HiltViewModel
class ModesViewModel @Inject constructor(
    modeRepository: ModeRepository,
    settingsStore: SettingsStore,
    private val activeMode: ActiveModeProvider,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    val uiState: StateFlow<ModesUiState> = combine(
        modeRepository.modes,
        settingsStore.settings,
        settingsGuard.pending,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { modes, settings, pending, _ ->
        val now = timeSource.now()
        ModesUiState(
            modes = modes,
            activeId = activeMode.active()?.id,
            manualId = settings.manualModeId,
            pendingSwitch = pending.firstNotNullOfOrNull { p ->
                (p.change as? SetManualMode)?.let { PendingSwitch(it.modeId, durationBetween(now, p.applyAt)) }
            },
            pendingEdits = pending.mapNotNull { p ->
                p.change.key.removePrefix("mode:").toLongOrNull()?.takeIf { p.change.key.startsWith("mode:") }
                    ?.let { it to durationBetween(now, p.applyAt) }
            }.toMap(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModesUiState())

    /** A stricter mode applies at once; a looser one waits 24 hours. */
    fun onUse(modeId: Long?) {
        viewModelScope.launch { settingsGuard.request(SetManualMode(modeId)) }
    }

    private companion object {
        val REFRESH = 15.seconds
    }
}
