package app.minimise.core.time

import app.minimise.core.Defaults
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Turns instants into "usage days". A usage day starts at [Defaults.DAY_RESET] (04:00) local time,
 * so 01:30 on Tuesday still belongs to Monday.
 */
class DayBoundary @Inject constructor(
    private val timeSource: TimeSource,
) {
    private val resetAt: LocalTime = Defaults.DAY_RESET

    fun dayOf(instant: Instant): LocalDate {
        val local = instant.atZone(timeSource.zone())
        val date = local.toLocalDate()
        return if (local.toLocalTime() < resetAt) date.minusDays(1) else date
    }

    fun today(): LocalDate = dayOf(timeSource.now())

    /** The first instant of [day]. */
    fun startOf(day: LocalDate): Instant = day.atTime(resetAt).atZone(timeSource.zone()).toInstant()

    /** The first instant after [day] (exclusive end). */
    fun endOf(day: LocalDate): Instant = startOf(day.plusDays(1))
}
