package app.minimise.modes.domain

import app.minimise.core.Defaults
import app.minimise.core.time.WeeklyWindow
import app.minimise.settings.domain.Direction
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WindDownTest {
    private val base = ModeValues(Defaults.DAILY_BUDGET, Defaults.WAIT_STEPS, allowlist = null)
    private val sleep = Mode(4, "Sleep", ModeValues(0.minutes, Defaults.WAIT_STEPS, allowlist = null), schedule = null, presetKey = Mode.SLEEP)
    private val plan = WindDownPlan(bedtime = LocalTime.of(23, 0), wake = LocalTime.of(7, 0), ebook = "com.books")

    private fun Mode.with(change: app.minimise.settings.domain.SaveMode) = copy(
        values = values.copy(budget = change.budgetMinutes.minutes, allowlist = change.allowlist?.toSet()),
        schedule = if (change.scheduleDays == 0) null else WeeklyWindow.fromStored(change.scheduleDays, change.scheduleStartMinute, change.scheduleEndMinute),
    )

    @Test
    fun `wind-down starts half an hour before bedtime every night and ends at wake`() {
        val window = WindDownPlans.window(plan)
        assertEquals(7, window.days.size)
        assertEquals(LocalTime.of(22, 30), window.start)
        assertEquals(LocalTime.of(7, 0), window.end)
    }

    @Test
    fun `the plan round-trips through the sleep mode`() {
        val change = WindDownPlans.change(sleep, plan)
        assertEquals(0, change.budgetMinutes)
        assertEquals(listOf("com.books"), change.allowlist)
        assertEquals(plan, WindDownPlans.of(sleep.with(change)))
    }

    @Test
    fun `a sleep mode not on every night isn't wind-down`() {
        assertNull(WindDownPlans.of(sleep))
    }

    @Test
    fun `starting wind-down tightens, switching it off loosens`() {
        val on = WindDownPlans.change(sleep, plan)
        val asleep = sleep.with(on)
        assertEquals(Direction.TIGHTEN, modeEditDirection(base, sleep, asleep.values, asleep.schedule))
        val off = sleep.with(WindDownPlans.change(asleep, null)).let { asleep.copy(schedule = it.schedule) }
        assertEquals(Direction.LOOSEN, modeEditDirection(base, asleep, off.values, off.schedule))
    }

    @Test
    fun `an earlier bedtime tightens, a later one loosens`() {
        val asleep = sleep.with(WindDownPlans.change(sleep, plan))
        val earlier = sleep.with(WindDownPlans.change(asleep, plan.copy(bedtime = LocalTime.of(22, 0))))
        val later = sleep.with(WindDownPlans.change(asleep, plan.copy(bedtime = LocalTime.of(23, 30))))
        assertEquals(Direction.TIGHTEN, modeEditDirection(base, asleep, earlier.values, earlier.schedule))
        assertEquals(Direction.LOOSEN, modeEditDirection(base, asleep, later.values, later.schedule))
    }
}
