package app.minimise.modes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.time.plus
import app.minimise.core.ui.ticker
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.modes.domain.manualPickAt
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SetManualMode
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModesUiState(
    val modes: List<Mode> = emptyList(),
    /** The mode in effect now, by hand or by schedule. */
    val activeId: Long? = null,
    /** The hand-picked mode still on; null when following schedules. */
    val manualId: Long? = null,
    /** When the hand-picked mode ends. Null for one picked before picks had ends: it lasts until changed. */
    val manualUntil: LocalTime? = null,
    /** A waiting switch: the mode it goes to (null: follow schedules) and how long until it does. */
    val pendingSwitch: PendingSwitch? = null,
    val pendingEdits: Map<Long, Duration> = emptyMap(),
    /** The mode whose end is being chosen, to turn it on or to change when it ends. */
    val choosingEndFor: Mode? = null,
    /** The New mode dialog is open; [nameTaken] when the name last tried is already a mode's. */
    val addingMode: Boolean = false,
    val nameTaken: Boolean = false,
    /** Where the clock starts when picking an end: the next whole hour. */
    val nextHour: LocalTime = LocalTime.NOON,
)

/**
 * @property until when the mode it switches to would end.
 * @property endsFirst that end comes before the switch would: waiting it out changes nothing.
 */
data class PendingSwitch(val modeId: Long?, val remaining: Duration, val until: LocalTime? = null, val endsFirst: Boolean = false)

@HiltViewModel
class ModesViewModel @Inject constructor(
    private val modeRepository: ModeRepository,
    settingsStore: SettingsStore,
    private val activeMode: ActiveModeProvider,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
    private val dayBoundary: DayBoundary,
) : ViewModel() {
    private val dialogs = MutableStateFlow(Dialogs())
    private val opened = Channel<Long>(Channel.BUFFERED)

    /** A mode just added, to open for editing. */
    val openedModes: Flow<Long> = opened.receiveAsFlow()

    val uiState: StateFlow<ModesUiState> = combine(
        modeRepository.modes,
        settingsStore.settings,
        settingsGuard.pending,
        dialogs,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { modes, settings, pending, dialogs, _ ->
        val now = timeSource.now()
        val pick = settings.manualPickAt(now)
        ModesUiState(
            modes = modes,
            activeId = activeMode.active()?.id,
            manualId = pick?.modeId,
            manualUntil = pick?.until?.let(::localTime),
            pendingSwitch = pending.firstNotNullOfOrNull { p ->
                (p.change as? SetManualMode)?.let { change ->
                    val until = change.untilEpochMillis?.takeIf { change.modeId != null }?.let(Instant::ofEpochMilli)
                    PendingSwitch(
                        modeId = change.modeId,
                        remaining = durationBetween(now, p.applyAt),
                        until = until?.let(::localTime),
                        endsFirst = until != null && !until.isAfter(p.applyAt),
                    )
                }
            },
            pendingEdits = pending.mapNotNull { p ->
                p.change.key.removePrefix("mode:").toLongOrNull()?.takeIf { p.change.key.startsWith("mode:") }
                    ?.let { it to durationBetween(now, p.applyAt) }
            }.toMap(),
            choosingEndFor = dialogs.endFor?.let { id -> modes.firstOrNull { it.id == id } },
            addingMode = dialogs.adding,
            nameTaken = dialogs.nameTaken,
            nextHour = localTime(now).plusHours(1).withMinute(0).withSecond(0).withNano(0),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModesUiState())

    /** Asks when [modeId] should end; turning it on waits for that. */
    fun onUse(modeId: Long) = dialogs.update { it.copy(endFor = modeId) }

    fun onEndDismissed() = dialogs.update { it.copy(endFor = null) }

    fun onUseFor(hours: Int) = pick { now -> now + hours.hours }

    /** Until the usage day ends ([Defaults.DAY_RESET]). */
    fun onUseUntilReset() = pick { dayBoundary.endOf(dayBoundary.today()) }

    /** Until the next [time]: later today, or tomorrow if it has passed. */
    fun onUseUntil(time: LocalTime) = pick { now ->
        val zone = timeSource.zone()
        val today = now.atZone(zone).toLocalDate()
        today.atTime(time).atZone(zone).toInstant().takeIf { it.isAfter(now) }
            ?: today.plusDays(1).atTime(time).atZone(zone).toInstant()
    }

    /** Ends the hand-picked mode now. Stricter than it, or the same, applies at once; looser waits 24 hours. */
    fun onFollowSchedules() {
        viewModelScope.launch { settingsGuard.request(SetManualMode(null)) }
    }

    fun onAddMode() = dialogs.update { it.copy(adding = true, nameTaken = false) }

    fun onAddDismissed() = dialogs.update { it.copy(adding = false, nameTaken = false) }

    /** Adds a mode with [template]'s budget, waits and apps, and no schedule, then opens it. */
    fun onAddConfirmed(name: String, template: Mode) {
        val trimmed = name.trim().take(Mode.MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (modeRepository.all().any { it.name.equals(trimmed, ignoreCase = true) }) {
                dialogs.update { it.copy(nameTaken = true) }
                return@launch
            }
            dialogs.update { it.copy(adding = false, nameTaken = false) }
            val result = settingsGuard.request(
                SaveMode(
                    id = 0,
                    name = trimmed,
                    budgetMinutes = template.values.budget.inWholeMinutes.toInt(),
                    allowlist = template.values.allowlist?.sorted(),
                    scheduleDays = 0,
                    scheduleStartMinute = 0,
                    scheduleEndMinute = 0,
                    waitStepsSeconds = template.values.waitSteps.map { it.inWholeSeconds },
                ),
            )
            if (result != GuardResult.AppliedNow) return@launch
            modeRepository.all().filter { it.isCustom && it.name == trimmed }.maxByOrNull { it.id }?.let { opened.send(it.id) }
        }
    }

    /** A stricter mode, or a later end, applies at once; a looser one, or a sooner end, waits 24 hours. */
    private fun pick(until: (Instant) -> Instant) {
        val modeId = dialogs.value.endFor ?: return
        dialogs.update { it.copy(endFor = null) }
        val end = until(timeSource.now())
        viewModelScope.launch { settingsGuard.request(SetManualMode(modeId, end.toEpochMilli())) }
    }

    private fun localTime(at: Instant): LocalTime = at.atZone(timeSource.zone()).toLocalTime()

    private data class Dialogs(val endFor: Long? = null, val adding: Boolean = false, val nameTaken: Boolean = false)

    companion object {
        /** The "For N hours" choices when turning a mode on. */
        val PICK_HOURS: List<Int> = listOf(1, 2, 4)

        private val REFRESH = 15.seconds
    }
}
