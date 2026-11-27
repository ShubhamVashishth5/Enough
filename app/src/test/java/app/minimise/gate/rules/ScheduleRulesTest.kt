package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.time.plus
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.schedules.domain.Schedule
import app.minimise.settings.domain.Direction
import app.minimise.settings.domain.scheduleChangeDirection
import app.minimise.core.prefs.Settings
import app.minimise.testing.FakeSettingsStore
import app.minimise.testing.app
import app.minimise.usage.domain.TodayUsage
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRulesTest {
    private val instagram = app("Instagram", AppKind.VAULT)
    private val maps = app("Maps")
    private val bedtime = Schedule(1, "Bedtime", WeeklyWindow(DayOfWeek.entries.toSet(), LocalTime.of(22, 0), LocalTime.of(7, 0)))
    private val night = LocalDateTime.of(2026, 11, 24, 23, 0)
    private val noon = LocalDateTime.of(2026, 11, 24, 12, 0)

    private fun ctx(app: app.minimise.apps.domain.AppEntry, today: TodayUsage = TodayUsage(), now: Instant = Instant.EPOCH) =
        GateContext(app, today, now)

    @Test
    fun `a schedule blocks vault apps inside its window, with keys and a pass as ways past`() {
        assertEquals(Decision.Block(ScheduleRule.REASON, ScheduleRule.OVERRIDES), ScheduleRule.evaluate(ctx(instagram), listOf(bedtime), night))
        assertEquals(Decision.Allow, ScheduleRule.evaluate(ctx(instagram), listOf(bedtime), noon))
        assertEquals(Decision.Allow, ScheduleRule.evaluate(ctx(maps), listOf(bedtime), night))
    }

    @Test
    fun `the first hour after the day's first unlock blocks vault apps`() {
        val unlock = Instant.parse("2026-11-24T07:00:00Z")
        val today = TodayUsage(firstUnlockAt = unlock)
        assertTrue(FirstHourRule.evaluate(ctx(instagram, today, unlock + 59.minutes)) is Decision.Block)
        assertEquals(Decision.Allow, FirstHourRule.evaluate(ctx(instagram, today, unlock + 60.minutes)))
        assertEquals(Decision.Allow, FirstHourRule.evaluate(ctx(maps, today, unlock)))
        assertEquals(Decision.Allow, FirstHourRule.evaluate(ctx(instagram, TodayUsage(), unlock)))
    }

    @Test
    fun `the first-hour Rule the GateEngine calls gives the same answer, and nothing when off`() = runTest {
        val unlock = Instant.parse("2026-11-24T07:00:00Z")
        val inside = ctx(instagram, TodayUsage(firstUnlockAt = unlock), unlock + 10.minutes)
        assertTrue(FirstHourRule(FakeSettingsStore(Settings(firstHourRule = true))).evaluate(inside) is Decision.Block)
        assertEquals(Decision.Allow, FirstHourRule(FakeSettingsStore(Settings(firstHourRule = false))).evaluate(inside))
    }

    @Test
    fun `adding or growing a schedule tightens, shrinking it loosens`() {
        val window = bedtime.window
        assertEquals(Direction.TIGHTEN, scheduleChangeDirection(null, null, window, "Bedtime"))
        assertEquals(Direction.TIGHTEN, scheduleChangeDirection(window, "Bedtime", window.copy(end = LocalTime.of(8, 0)), "Bedtime"))
        assertEquals(Direction.LOOSEN, scheduleChangeDirection(window, "Bedtime", window.copy(start = LocalTime.of(23, 0)), "Bedtime"))
        assertEquals(Direction.TIGHTEN, scheduleChangeDirection(window, "Bedtime", window, "Sleep"))
        assertEquals(null, scheduleChangeDirection(window, "Bedtime", window, "Bedtime"))
    }
}
