package app.minimise.modes.domain

import app.minimise.core.Defaults
import app.minimise.core.prefs.Settings
import app.minimise.core.time.WeeklyWindow
import app.minimise.settings.domain.Direction
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModeTimelineTest {
    private val zone = ZoneId.of("UTC")
    private val base = ModeValues(45.minutes, Defaults.WAIT_STEPS, allowlist = null)
    private val workValues = ModeValues(20.minutes, listOf(20, 40, 60, 120, 180).map { it.seconds }, allowlist = null)
    private val sleepValues = ModeValues(0.minutes, listOf(60, 120, 300).map { it.seconds }, allowlist = null)
    private val work = Mode(1, "Work", workValues, schedule = null, presetKey = "work")
    private val home = Mode(3, "Home", base, schedule = null, presetKey = "home")
    private val sleep = Mode(4, "Sleep", sleepValues, schedule = null, presetKey = "sleep")
    private val modes = listOf(work, home, sleep)

    /** 2026-01-15 is a Thursday. */
    private fun at(time: String): Instant = Instant.parse("2026-01-15T$time:00Z")

    private fun direction(now: String, from: ManualPick?, to: ManualPick?, modes: List<Mode> = this.modes): Direction? =
        ModeTimeline.switchDirection(at(now), zone, modes, base, from, to)

    @Test
    fun `a pick is on until its end, then the schedules decide`() {
        val workDays = work.copy(schedule = WeeklyWindow(DayOfWeek.entries.toSet(), LocalTime.of(9, 0), LocalTime.of(17, 0)))
        val list = listOf(workDays, home, sleep)
        val sleepTillTen = ManualPick(sleep.id, until = at("10:00"))
        assertEquals(sleep.id, ModeTimeline.activeAt(at("09:59"), zone, list, sleepTillTen)?.id)
        assertEquals(workDays.id, ModeTimeline.activeAt(at("10:00"), zone, list, sleepTillTen)?.id)
        assertNull(ModeTimeline.activeAt(at("17:00"), zone, list, sleepTillTen))
    }

    @Test
    fun `a pick whose end has passed is no pick`() {
        val settings = Settings(manualModeId = work.id, manualModeUntil = at("10:00"))
        assertEquals(ManualPick(work.id, at("10:00")), settings.manualPickAt(at("09:00")))
        assertNull(settings.manualPickAt(at("10:00")))
        assertNull(direction("11:00", from = ManualPick(work.id, at("10:00")), to = null))
    }

    @Test
    fun `turning on a stricter mode for a while applies at once`() {
        assertEquals(Direction.TIGHTEN, direction("10:00", from = null, to = ManualPick(work.id, at("12:00"))))
        assertEquals(Direction.TIGHTEN, direction("10:00", from = null, to = ManualPick(sleep.id, at("23:00"))))
    }

    @Test
    fun `ending a mode early waits, letting it end by itself doesn't`() {
        val workTillSix = ManualPick(work.id, at("18:00"))
        assertEquals(Direction.LOOSEN, direction("10:00", from = workTillSix, to = null))
        assertNull(direction("18:00", from = workTillSix, to = null))
    }

    @Test
    fun `a later end tightens and a sooner one loosens`() {
        val workTillSix = ManualPick(work.id, at("18:00"))
        assertEquals(Direction.TIGHTEN, direction("10:00", from = workTillSix, to = ManualPick(work.id, at("20:00"))))
        assertEquals(Direction.LOOSEN, direction("10:00", from = workTillSix, to = ManualPick(work.id, at("16:00"))))
    }

    @Test
    fun `a stricter mode that ends sooner than the one on now still loosens`() {
        val workTillSix = ManualPick(work.id, at("18:00"))
        assertEquals(Direction.LOOSEN, direction("10:00", from = workTillSix, to = ManualPick(sleep.id, at("12:00"))))
        assertEquals(Direction.TIGHTEN, direction("10:00", from = workTillSix, to = ManualPick(sleep.id, at("19:00"))))
    }

    @Test
    fun `a looser mode waits`() {
        assertEquals(Direction.LOOSEN, direction("10:00", from = ManualPick(sleep.id, at("12:00")), to = ManualPick(work.id, at("12:00"))))
    }

    @Test
    fun `a mode no stricter than none can't cover a stricter scheduled one`() {
        val workAtNine = work.copy(schedule = WeeklyWindow(DayOfWeek.entries.toSet(), LocalTime.of(9, 0), LocalTime.of(17, 0)))
        val list = listOf(workAtNine, home, sleep)
        // At 08:00 Home is the same as no mode, but by 09:00 it would be standing in for Work.
        assertEquals(Direction.LOOSEN, direction("08:00", from = null, to = ManualPick(home.id, at("18:00")), modes = list))
        assertEquals(Direction.TIGHTEN, direction("08:00", from = null, to = ManualPick(home.id, at("09:00")), modes = list))
    }

    @Test
    fun `picking the mode whose schedule is on, until its hours end, just records it`() {
        val workAtNine = work.copy(schedule = WeeklyWindow(DayOfWeek.entries.toSet(), LocalTime.of(9, 0), LocalTime.of(17, 0)))
        assertEquals(Direction.TIGHTEN, direction("10:00", from = null, to = ManualPick(work.id, at("17:00")), modes = listOf(workAtNine, home)))
    }

    @Test
    fun `a pick from before picks had ends lasts until changed`() {
        val forever = ManualPick(work.id, until = null)
        assertEquals(work.id, ModeTimeline.activeAt(at("10:00").plusSeconds(30L * 24 * 3600), zone, modes, forever)?.id)
        assertEquals(Direction.LOOSEN, direction("10:00", from = forever, to = ManualPick(work.id, at("18:00"))))
        assertEquals(Direction.TIGHTEN, direction("10:00", from = ManualPick(work.id, at("18:00")), to = forever))
    }

    @Test
    fun `a pick of a mode that's gone is the same as following the schedules`() {
        assertEquals(Direction.TIGHTEN, direction("10:00", from = null, to = ManualPick(99, at("12:00"))))
        assertNull(ModeTimeline.activeAt(at("10:00"), zone, modes, ManualPick(99, at("12:00"))))
    }
}
