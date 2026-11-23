package app.minimise.core.time

import java.time.Instant
import java.time.LocalDate
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
}
