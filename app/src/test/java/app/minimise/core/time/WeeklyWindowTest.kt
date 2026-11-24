package app.minimise.core.time

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyWindowTest {
    // 2026-11-23 is a Monday.
    private fun at(day: Int, time: String) = LocalDateTime.of(2026, 11, 22 + day, 0, 0).with(LocalTime.parse(time))
    private val workHours = WeeklyWindow(setOf(MONDAY, TUESDAY), LocalTime.of(10, 0), LocalTime.of(18, 0))
    private val overnight = WeeklyWindow(setOf(FRIDAY), LocalTime.of(22, 0), LocalTime.of(7, 0))

    @Test
    fun `a same-day window covers start to end on its days only`() {
        assertTrue(workHours.isActive(at(1, "10:00")))
        assertTrue(workHours.isActive(at(2, "17:59")))
        assertFalse(workHours.isActive(at(1, "18:00")))
        assertFalse(workHours.isActive(at(3, "12:00")))
    }

    @Test
    fun `an overnight window runs into the next morning`() {
        assertTrue(overnight.isActive(at(5, "23:30"))) // Friday night
        assertTrue(overnight.isActive(at(6, "06:59"))) // Saturday morning
        assertFalse(overnight.isActive(at(6, "07:00")))
        assertFalse(overnight.isActive(at(5, "06:00"))) // Friday morning belongs to Thursday, which isn't on
    }

    @Test
    fun `covers compares minutes of the week`() {
        val longer = overnight.copy(end = LocalTime.of(8, 0))
        assertTrue(longer.covers(overnight))
        assertFalse(overnight.covers(longer))
        assertTrue(overnight.copy(days = setOf(FRIDAY, SATURDAY)).covers(overnight))
        assertFalse(workHours.covers(overnight))
    }

    @Test
    fun `days survive the round trip through storage`() {
        val stored = WeeklyWindow.fromStored(overnight.daysMask(), 22 * 60, 7 * 60)
        assertEquals(overnight, stored)
    }
}
