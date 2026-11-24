package app.minimise.usage.domain

import app.minimise.core.prefs.Settings
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.FakeTimeSource
import app.minimise.core.time.plus
import app.minimise.settings.domain.LoosenDelay
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SettingChange
import app.minimise.settings.domain.SettingsGuard
import app.minimise.settings.domain.SettingsTarget
import app.minimise.settings.domain.budgetChangeDirection
import app.minimise.testing.FakeEventLog
import app.minimise.testing.FakePendingChangeStore
import app.minimise.testing.FakeSettingsStore
import app.minimise.testing.NoLockPeriod
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetPlansTest {
    // A Monday afternoon, so "this week" starts today at 04:00.
    private val clock = FakeTimeSource(now = Instant.parse("2026-01-12T15:00:00Z"))
    private val days = DayBoundary(clock)
    private val settings = FakeSettingsStore(Settings(onboardingDone = true))
    private val events = FakeEventLog()

    /** Writes budgets straight to the fake settings, as AppSettingsTarget does. */
    private val target = object : SettingsTarget {
        override suspend fun direction(change: SettingChange) =
            (change as? SetBudget)?.let { budgetChangeDirection(settings.state.value.dailyBudget.inWholeMinutes.toInt(), it.minutes) }

        override suspend fun apply(change: SettingChange) {
            (change as? SetBudget)?.let { c -> settings.update { it.copy(dailyBudget = c.minutes.minutes) } }
        }
    }
    private val guard = SettingsGuard(
        target,
        FakePendingChangeStore(),
        clock,
        object : LoosenDelay {
            override suspend fun current(): Duration = 24.hours
        },
        events,
        NoLockPeriod,
    )

    private val budget get() = settings.state.value.dailyBudget.inWholeMinutes.toInt()

    @Test
    fun `taper takes 5 percent a week and stops at the floor`() {
        assertEquals(43, BudgetMath.tapered(45, floor = 20))
        assertEquals(41, BudgetMath.tapered(43, floor = 20))
        assertEquals(20, BudgetMath.tapered(21, floor = 20))
        assertNull(BudgetMath.tapered(20, floor = 20))
        assertEquals(9, BudgetMath.tapered(10, floor = 0))
    }

    @Test
    fun `taper runs once a week, from the week after it was switched on`() = runTest {
        val taper = Taper(settings, guard, days)
        settings.update { it.copy(dailyBudget = 45.minutes, taperFloorMinutes = 40, taperWeek = days.weekStartOf(days.today())) }
        taper.applyIfDue()
        assertEquals(45, budget)

        clock.advance(7.days)
        taper.applyIfDue()
        taper.applyIfDue()
        assertEquals(43, budget)

        clock.advance(7.days)
        taper.applyIfDue()
        assertEquals(41, budget)

        clock.advance(7.days)
        taper.applyIfDue()
        assertEquals(40, budget)

        clock.advance(7.days)
        taper.applyIfDue()
        assertEquals(40, budget)
    }

    @Test
    fun `taper off does nothing`() = runTest {
        settings.update { it.copy(dailyBudget = 45.minutes) }
        clock.advance(7.days)
        Taper(settings, guard, days).applyIfDue()
        assertEquals(45, budget)
    }

    private fun overBudgetOn(daysAgo: Int) {
        val day = days.today().minusDays(daysAgo.toLong())
        events.events.value += Event(
            EventType.BLOCK_SHOWN,
            startAt = days.startOf(day) + 5.hours,
            packageName = "insta",
            data = """{"reason":"daily_budget"}""",
        )
    }

    @Test
    fun `three days over budget in a row offer a 10 percent smaller budget`() = runTest {
        val autoTighten = AutoTighten(events, days, settings, guard)
        settings.update { it.copy(dailyBudget = 40.minutes) }
        overBudgetOn(1)
        overBudgetOn(2)
        assertNull(autoTighten.offer())
        overBudgetOn(3)
        assertEquals(36, autoTighten.offer())
    }

    @Test
    fun `an answered offer waits for a fresh run of days`() = runTest {
        val autoTighten = AutoTighten(events, days, settings, guard)
        (1..3).forEach(::overBudgetOn)
        autoTighten.decline()
        assertNull(autoTighten.offer())
        clock.advance(1.days)
        overBudgetOn(1)
        assertNull(autoTighten.offer())
        clock.advance(3.days)
        (1..3).forEach(::overBudgetOn)
        assertEquals(41, autoTighten.offer())
    }

    @Test
    fun `accepting the offer lowers the budget at once`() = runTest {
        AutoTighten(events, days, settings, guard).accept(36)
        assertEquals(36, budget)
    }

    @Test
    fun `earn-it adds minutes only when on, once per task, up to the daily cap`() = runTest {
        val earnIt = EarnIt(settings, events, days, clock)
        fun bonus() = TodayUsage.from(events.events.value, days.startOf(days.today()), days.endOf(days.today()), clock.now, emptySet(), null).bonus

        earnIt.onTaskDone(1)
        assertEquals(Duration.ZERO, bonus())

        settings.update { it.copy(earnIt = true) }
        earnIt.onTaskDone(1)
        earnIt.onTaskDone(1)
        assertEquals(5.minutes, bonus())

        earnIt.onFocusCompleted(clock.now)
        earnIt.onFocusCompleted(clock.now + 1.hours)
        assertEquals(25.minutes, bonus())

        earnIt.onTaskDone(2)
        earnIt.onTaskDone(3)
        assertEquals(30.minutes, bonus())
    }
}
