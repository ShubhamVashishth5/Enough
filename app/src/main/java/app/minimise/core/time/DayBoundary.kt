package app.minimise.core.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Turns instants into "usage days". A usage day starts at the time the user chose (04:00 unless they
 * changed it), so with the default 01:30 on Tuesday still belongs to Monday. Budgets, tasks, the first
 * hour, emergency passes and the weekly report all count days this way.
 */
class DayBoundary @Inject constructor(
    private val timeSource: TimeSource,
    private val dayStart: DayStartSource = FixedDayStart(),
) {
    private val resetAt: LocalTime get() = dayStart.dayStart()

    fun dayOf(instant: Instant): LocalDate {
        val local = instant.atZone(timeSource.zone())
        val date = local.toLocalDate()
        return if (local.toLocalTime() < resetAt) date.minusDays(1) else date
    }

    fun today(): LocalDate = dayOf(timeSource.now())

    /** The day before [today]. */
    fun yesterday(): LocalDate = today().minusDays(1)

    /** The first instant of [day]. */
    fun startOf(day: LocalDate): Instant = day.atTime(resetAt).atZone(timeSource.zone()).toInstant()

    /** The Monday that starts [day]'s usage week. */
    fun weekStartOf(day: LocalDate): LocalDate = day.minusDays((day.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    /** The first instant after [day] (exclusive end). */
    fun endOf(day: LocalDate): Instant = startOf(day.plusDays(1))
}
