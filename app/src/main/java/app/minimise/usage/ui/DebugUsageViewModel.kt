package app.minimise.usage.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.BuildConfig
import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DebugUsageUiState(
    val now: LocalDateTime? = null,
    val day: LocalDate? = null,
    val clockOffset: Duration = Duration.ZERO,
    val unlocks: Int = 0,
    val spans: List<SpanRow> = emptyList(),
)

data class SpanRow(val id: Long, val label: String, val start: LocalTime, val end: LocalTime, val length: Duration)

/** Debug builds only: today's raw spans and unlocks, and the wall-clock offset for testing day resets. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DebugUsageViewModel @Inject constructor(
    eventLog: EventLog,
    appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val dayBoundary: DayBoundary,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val todayEvents = ticker(5.seconds)
        .map { dayBoundary.today() }
        .flatMapLatest { day ->
            eventLog.observeBetween(dayBoundary.startOf(day), dayBoundary.endOf(day)).map { day to it }
        }

    val uiState: StateFlow<DebugUsageUiState> = combine(
        todayEvents,
        appRepository.apps,
        settingsStore.settings,
    ) { (day, events), apps, settings ->
        val labels = apps.associate { it.packageName to it.label }
        val zone = timeSource.zone()
        DebugUsageUiState(
            now = timeSource.now().atZone(zone).toLocalDateTime(),
            day = day,
            clockOffset = settings.debugTimeOffset,
            unlocks = events.count { it.type == EventType.SCREEN_UNLOCK },
            spans = events.filter { it.type == EventType.APP_SPAN }.map { span ->
                val end = span.endAt ?: span.startAt
                SpanRow(
                    id = span.id,
                    label = labels[span.packageName] ?: span.packageName.orEmpty(),
                    start = span.startAt.atZone(zone).toLocalTime(),
                    end = end.atZone(zone).toLocalTime(),
                    length = durationBetween(span.startAt, end),
                )
            }.reversed(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebugUsageUiState())

    fun onShiftClock(hours: Int) = updateOffset { it + hours.hours }

    fun onResetClock() = updateOffset { Duration.ZERO }

    private fun updateOffset(transform: (Duration) -> Duration) {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch { settingsStore.update { it.copy(debugTimeOffset = transform(it.debugTimeOffset)) } }
    }
}
