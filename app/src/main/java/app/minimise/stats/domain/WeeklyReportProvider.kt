package app.minimise.stats.domain

import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.TimeSource
import app.minimise.tasks.domain.TaskRepository
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import app.minimise.usage.domain.UsageTracker
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Builds the weekly report from the event log. Weeks run Monday 04:00 to Monday 04:00. */
class WeeklyReportProvider @Inject constructor(
    private val eventLog: EventLog,
    private val dayBoundary: DayBoundary,
    private val timeSource: TimeSource,
    private val appRepository: AppRepository,
    private val taskRepository: TaskRepository,
    private val usageTracker: UsageTracker,
) {
    suspend fun build(): WeeklyReport {
        val today = dayBoundary.today()
        val weekStart = dayBoundary.weekStartOf(today)
        val thisWeek = Period(dayBoundary.startOf(weekStart), dayBoundary.startOf(weekStart.plusDays(7)))
        val lastWeek = Period(dayBoundary.startOf(weekStart.minusDays(7)), thisWeek.start)
        val monthStart = today.withDayOfMonth(1)
        val monthDays = generateSequence(monthStart) { it.plusDays(1) }
            .takeWhile { it.isBefore(today) }
            .map { Period(dayBoundary.startOf(it), dayBoundary.endOf(it)) }
            .toList()
        val from = minOf(lastWeek.start, dayBoundary.startOf(monthStart))
        return WeeklyReports.build(
            events = eventLog.between(from, thisWeek.end),
            thisWeek = thisWeek,
            lastWeek = lastWeek,
            monthDays = monthDays,
            now = timeSource.now(),
            zone = timeSource.zone(),
            vaultPackages = appRepository.kinds.first().filterValues { it == AppKind.VAULT }.keys,
            tasksDone = taskRepository.doneBetween(weekStart, weekStart.plusDays(7)) to
                taskRepository.doneBetween(weekStart.minusDays(7), weekStart),
            openSpanId = usageTracker.openSpan.value?.eventId,
        )
    }
}

/** The stats screen opens once per usage day, so checking it can't become a habit of its own. */
@Singleton
class StatsAccess @Inject constructor(
    private val eventLog: EventLog,
    private val dayBoundary: DayBoundary,
    private val timeSource: TimeSource,
) {
    private val mutex = Mutex()

    /** True the first time it's asked each usage day, and logs the visit. */
    suspend fun claim(): Boolean = mutex.withLock {
        val today = dayBoundary.today()
        val seen = eventLog.between(dayBoundary.startOf(today), dayBoundary.endOf(today), EventType.STATS_VIEWED)
        if (seen.isNotEmpty()) return@withLock false
        eventLog.log(Event(type = EventType.STATS_VIEWED, startAt = timeSource.now()))
        true
    }
}

/** Arms the Sunday-evening notification that links to the report. */
interface WeeklyReportScheduler {
    fun scheduleNext()
}

object ReportTimes {
    /** The next [day] at [time] strictly after [now]. */
    fun nextAfter(now: ZonedDateTime, day: DayOfWeek, time: LocalTime): ZonedDateTime {
        val daysAhead = ((day.value - now.dayOfWeek.value) + 7) % 7
        val candidate = now.toLocalDate().plusDays(daysAhead.toLong()).atTime(time).atZone(now.zone)
        return if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
    }
}
