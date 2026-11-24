package app.minimise.modes.domain

import app.minimise.core.Defaults
import app.minimise.core.time.WeeklyWindow
import app.minimise.settings.domain.Direction
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class StrictnessTest {
    private val base = ModeValues(45.minutes, Defaults.WAIT_STEPS, allowlist = null)
    private val work = ModeValues(20.minutes, listOf(20, 40, 60, 120, 180).map { it.seconds }, allowlist = null)
    private val weekdays = WeeklyWindow(DayOfWeek.entries.take(5).toSet(), LocalTime.of(10, 0), LocalTime.of(18, 0))

    @Test
    fun `smaller budgets, longer waits and narrower allowlists are stricter`() {
        assertEquals(Strictness.STRICTER, compare(base, work))
        assertEquals(Strictness.LOOSER, compare(work, base))
        assertEquals(Strictness.SAME, compare(base, base))
        assertEquals(Strictness.STRICTER, compare(base, base.copy(allowlist = setOf("maps"))))
        assertEquals(Strictness.STRICTER, compare(base.copy(allowlist = setOf("maps", "phone")), base.copy(allowlist = setOf("maps"))))
    }

    @Test
    fun `stricter one way and looser another is mixed, which waits`() {
        val mixed = work.copy(budget = 60.minutes)
        assertEquals(Strictness.MIXED, compare(base, mixed))
        assertEquals(Direction.LOOSEN, compare(base, mixed).toDirection())
    }

    @Test
    fun `switching to Work tightens and switching back to Home loosens`() {
        assertEquals(Direction.TIGHTEN, compare(base, work).toDirection())
        assertEquals(Direction.LOOSEN, compare(work, base).toDirection())
    }

    @Test
    fun `giving a stricter mode hours tightens, taking them away loosens`() {
        val workMode = Mode(1, "Work", work, schedule = null)
        assertEquals(Direction.TIGHTEN, modeEditDirection(base, workMode, work, weekdays))
        val scheduled = workMode.copy(schedule = weekdays)
        assertEquals(Direction.LOOSEN, modeEditDirection(base, scheduled, work, null))
        assertEquals(Direction.LOOSEN, modeEditDirection(base, scheduled, work.copy(budget = 30.minutes), weekdays))
        assertEquals(Direction.TIGHTEN, modeEditDirection(base, scheduled, work.copy(budget = 10.minutes), weekdays))
        assertEquals(null, modeEditDirection(base, scheduled, work, weekdays))
    }
}
