package app.minimise.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DayBoundaryTest {
    private val clock = FakeTimeSource(zone = ZoneId.of("Asia/Kolkata"))
    private val boundary = DayBoundary(clock)

    private fun local(text: String): Instant =
        java.time.LocalDateTime.parse(text).atZone(clock.zone).toInstant()

    @Test
    fun `just before 04-00 belongs to the previous day`() {
        assertEquals(LocalDate.of(2026, 3, 9), boundary.dayOf(local("2026-03-10T03:59:59")))
    }

    @Test
    fun `04-00 exactly starts the new day`() {
        assertEquals(LocalDate.of(2026, 3, 10), boundary.dayOf(local("2026-03-10T04:00:00")))
    }

    @Test
    fun `midnight is still yesterday`() {
        assertEquals(LocalDate.of(2026, 3, 9), boundary.dayOf(local("2026-03-10T00:00:00")))
    }

    @Test
    fun `late evening is today`() {
        assertEquals(LocalDate.of(2026, 3, 10), boundary.dayOf(local("2026-03-10T23:30:00")))
    }

    @Test
    fun `today follows the time source`() {
        clock.now = local("2026-03-10T02:00:00")
        assertEquals(LocalDate.of(2026, 3, 9), boundary.today())
    }

    @Test
    fun `start and end of a day are 04-00 local, 24 hours apart`() {
        val day = LocalDate.of(2026, 3, 10)
        assertEquals(local("2026-03-10T04:00:00"), boundary.startOf(day))
        assertEquals(local("2026-03-11T04:00:00"), boundary.endOf(day))
    }

    @Test
    fun `a day across a daylight saving change is 23 hours long`() {
        clock.zone = ZoneId.of("Europe/London") // clocks go forward at 01:00 on 29 March 2026
        val day = LocalDate.of(2026, 3, 28)
        val length = java.time.Duration.between(boundary.startOf(day), boundary.endOf(day))
        assertEquals(java.time.Duration.ofHours(23), length)
    }

    @Test
    fun `every instant inside a day maps back to that day`() {
        val day = LocalDate.of(2026, 7, 1)
        val start = boundary.startOf(day)
        val end = boundary.endOf(day)
        assertEquals(day, boundary.dayOf(start))
        assertEquals(day, boundary.dayOf(end.minusMillis(1)))
        assertEquals(day.plusDays(1), boundary.dayOf(end))
    }

    @Test
    fun `a day can start at midnight`() {
        val midnight = DayBoundary(clock, FixedDayStart(LocalTime.MIDNIGHT))
        assertEquals(LocalDate.of(2026, 3, 10), midnight.dayOf(local("2026-03-10T00:00:00")))
        assertEquals(LocalDate.of(2026, 3, 9), midnight.dayOf(local("2026-03-09T23:59:59")))
        assertEquals(local("2026-03-10T00:00:00"), midnight.startOf(LocalDate.of(2026, 3, 10)))
    }

    @Test
    fun `a later start keeps the small hours in the day before`() {
        val six = DayBoundary(clock, FixedDayStart(LocalTime.of(6, 0)))
        assertEquals(LocalDate.of(2026, 3, 9), six.dayOf(local("2026-03-10T05:30:00")))
        assertEquals(LocalDate.of(2026, 3, 10), six.dayOf(local("2026-03-10T06:00:00")))
        assertEquals(local("2026-03-11T06:00:00"), six.endOf(LocalDate.of(2026, 3, 10)))
    }

    @Test
    fun `a changed start applies to the next question asked`() {
        var start = LocalTime.of(4, 0)
        val moving = DayBoundary(clock) { start }
        clock.now = local("2026-03-10T02:00:00")
        assertEquals(LocalDate.of(2026, 3, 9), moving.today())
        start = LocalTime.of(1, 0)
        assertEquals(LocalDate.of(2026, 3, 10), moving.today())
        assertEquals(LocalDate.of(2026, 3, 9), moving.yesterday())
    }

    @Test
    fun `usage weeks start on Monday`() {
        val monday = java.time.LocalDate.of(2026, 11, 23)
        assertEquals(monday, boundary.weekStartOf(monday))
        assertEquals(monday, boundary.weekStartOf(monday.plusDays(6)))
        assertEquals(monday.plusDays(7), boundary.weekStartOf(monday.plusDays(7)))
    }
}
