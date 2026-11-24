package app.minimise.schedules.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.TimeSource
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.time.durationBetween
import app.minimise.core.time.minuteOfDay
import app.minimise.core.ui.ticker
import app.minimise.schedules.domain.Schedule
import app.minimise.schedules.domain.ScheduleRepository
import app.minimise.settings.domain.RemoveSchedule
import app.minimise.settings.domain.SaveSchedule
import app.minimise.settings.domain.SetFirstHourRule
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SchedulesUiState(
    val rows: List<ScheduleRow> = emptyList(),
    val firstHour: Boolean = false,
    /** How long until a waiting switch-off of the first-hour rule applies. */
    val firstHourOffIn: Duration? = null,
    val editor: ScheduleDraft? = null,
    val loaded: Boolean = false,
)

/** @property pendingIn how long until a waiting edit or removal applies. */
data class ScheduleRow(val schedule: Schedule, val pendingIn: Duration?, val pendingRemoval: Boolean)

/** @property id 0 for a new schedule. */
data class ScheduleDraft(val id: Long, val name: String, val window: WeeklyWindow) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = name.isNotBlank() && window.days.isNotEmpty()
}

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    scheduleRepository: ScheduleRepository,
    settingsStore: SettingsStore,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val draft = MutableStateFlow<ScheduleDraft?>(null)

    val uiState: StateFlow<SchedulesUiState> = combine(
        scheduleRepository.schedules,
        settingsStore.settings,
        settingsGuard.pending,
        draft,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { schedules, settings, pending, draft, _ ->
        val now = timeSource.now()
        SchedulesUiState(
            rows = schedules.map { schedule ->
                val waiting = pending.firstOrNull { it.change.key == "schedule:${schedule.id}" }
                ScheduleRow(
                    schedule = schedule,
                    pendingIn = waiting?.let { durationBetween(now, it.applyAt) },
                    pendingRemoval = waiting?.change is RemoveSchedule,
                )
            },
            firstHour = settings.firstHourRule,
            firstHourOffIn = pending.firstOrNull { it.change is SetFirstHourRule }?.let { durationBetween(now, it.applyAt) },
            editor = draft,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SchedulesUiState())

    /** Switching on applies at once; switching off waits. Tapping again while off is pending cancels it. */
    fun onFirstHourToggled() {
        val state = uiState.value
        val enabled = if (state.firstHourOffIn != null) true else !state.firstHour
        viewModelScope.launch { settingsGuard.request(SetFirstHourRule(enabled)) }
    }

    fun onAdd() {
        draft.value = ScheduleDraft(id = 0, name = "", window = DEFAULT_WINDOW)
    }

    fun onEdit(row: ScheduleRow) {
        draft.value = ScheduleDraft(row.schedule.id, row.schedule.name, row.schedule.window)
    }

    fun onNameChange(name: String) = draft.update { it?.copy(name = name) }

    fun onWindowChange(window: WeeklyWindow) = draft.update { it?.copy(window = window) }

    fun onDismiss() {
        draft.value = null
    }

    fun onSave() {
        val current = draft.value?.takeIf { it.canSave } ?: return
        draft.value = null
        val window = current.window
        viewModelScope.launch {
            settingsGuard.request(
                SaveSchedule(current.id, current.name.trim(), window.daysMask(), window.start.minuteOfDay, window.end.minuteOfDay),
            )
        }
    }

    fun onDelete() {
        val current = draft.value ?: return
        draft.value = null
        if (!current.isNew) viewModelScope.launch { settingsGuard.request(RemoveSchedule(current.id)) }
    }

    private companion object {
        val REFRESH = 15.seconds
        val DEFAULT_WINDOW = WeeklyWindow(DayOfWeek.entries.toSet(), LocalTime.of(22, 0), LocalTime.of(7, 0))
    }
}
