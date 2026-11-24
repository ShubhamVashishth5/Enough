package app.minimise.digest.domain

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DigestTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val times = listOf(LocalTime.of(9, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
    private fun at(day: Int, time: String) = ZonedDateTime.of(2026, 11, day, 0, 0, 0, 0, zone).with(LocalTime.parse(time))

    @Test
    fun `the next digest is the next time today, or the first one tomorrow`() {
        assertEquals(at(24, "09:00"), DigestTimes.nextAfter(at(24, "07:30"), times))
        assertEquals(at(24, "14:00"), DigestTimes.nextAfter(at(24, "09:00"), times))
        assertEquals(at(24, "20:00"), DigestTimes.nextAfter(at(24, "19:59:30"), times))
        assertEquals(at(25, "09:00"), DigestTimes.nextAfter(at(24, "20:01"), times))
    }

    private fun action(
        packageName: String = "com.whatsapp",
        exempt: Set<String> = emptySet(),
        category: String? = "msg",
        ongoing: Boolean = false,
        clearable: Boolean = true,
        fullScreen: Boolean = false,
        groupSummary: Boolean = false,
    ) = DigestFilter.actionFor(packageName, "app.minimise", exempt, category, ongoing, clearable, fullScreen, groupSummary)

    @Test
    fun `ordinary notifications wait for the digest`() {
        assertEquals(DigestAction.HOLD, action())
        assertEquals(DigestAction.DROP, action(groupSummary = true))
    }

    @Test
    fun `calls, alarms, exempt apps and ongoing notifications come straight through`() {
        assertEquals(DigestAction.PASS, action(category = "call"))
        assertEquals(DigestAction.PASS, action(category = "alarm"))
        assertEquals(DigestAction.PASS, action(exempt = setOf("com.whatsapp")))
        assertEquals(DigestAction.PASS, action(ongoing = true))
        assertEquals(DigestAction.PASS, action(clearable = false))
        assertEquals(DigestAction.PASS, action(fullScreen = true))
        assertEquals(DigestAction.PASS, action(packageName = "app.minimise"))
    }
}
