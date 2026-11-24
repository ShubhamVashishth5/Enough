package app.minimise.stats.domain

import app.minimise.core.time.DayBoundary
import app.minimise.core.time.FakeTimeSource
import app.minimise.core.time.minus
import app.minimise.core.time.plus
import app.minimise.intervention.domain.Trigger
import app.minimise.testing.FakeEventLog
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyReportTest {
    private val utc = ZoneId.of("UTC")

    // Monday 2026-01-12 04:00 to Monday 2026-01-19 04:00, and the week before.
    private val thisWeek = Period(Instant.parse("2026-01-12T04:00:00Z"), Instant.parse("2026-01-19T04:00:00Z"))
    private val lastWeek = Period(Instant.parse("2026-01-05T04:00:00Z"), thisWeek.start)
    private val now = Instant.parse("2026-01-18T20:00:00Z")

    private fun span(pkg: String, start: Instant, length: kotlin.time.Duration) =
        Event(EventType.APP_SPAN, startAt = start, endAt = start + length, packageName = pkg)

    private fun slip(at: Instant) = Event(EventType.BLOCK_SHOWN, startAt = at, packageName = "insta", data = """{"reason":"daily_budget"}""")

    private fun build(events: List<Event>, monthDays: List<Period> = emptyList()) = WeeklyReports.build(
        events = events,
        thisWeek = thisWeek,
        lastWeek = lastWeek,
        monthDays = monthDays,
        now = now,
        zone = utc,
        vaultPackages = setOf("insta", "yt", "web:reddit.com"),
        tasksDone = 4 to 2,
    )

    @Test
    fun `this week is compared with last`() {
        val report = build(
            listOf(
                span("insta", lastWeek.start + 1.days, 60.minutes),
                span("insta", thisWeek.start + 1.days, 30.minutes),
                span("maps", thisWeek.start + 1.days, 90.minutes),
                Event(EventType.SCREEN_UNLOCK, startAt = thisWeek.start + 2.hours),
                Event(EventType.SCREEN_UNLOCK, startAt = lastWeek.start + 2.hours),
                Event(EventType.SCREEN_UNLOCK, startAt = lastWeek.start + 3.hours),
                Event(EventType.FOCUS_SESSION, startAt = thisWeek.start + 3.hours, endAt = thisWeek.start + 3.hours + 25.minutes),
            ),
        )
        assertEquals(30.minutes, report.thisWeek.vaultTime)
        assertEquals(60.minutes, report.lastWeek.vaultTime)
        assertEquals(1, report.thisWeek.unlocks)
        assertEquals(2, report.lastWeek.unlocks)
        assertEquals(25.minutes, report.thisWeek.focusTime)
        assertEquals(4, report.thisWeek.tasksDone)
        assertEquals(2, report.lastWeek.tasksDone)
    }

    @Test
    fun `a span across the week boundary is split between the weeks`() {
        val report = build(listOf(span("yt", thisWeek.start - 10.minutes, 30.minutes)))
        assertEquals(20.minutes, report.thisWeek.vaultTime)
        assertEquals(10.minutes, report.lastWeek.vaultTime)
    }

    @Test
    fun `the top three time sinks are the vault apps with the most time`() {
        val day = thisWeek.start + 1.days
        val report = build(
            listOf(
                span("insta", day, 40.minutes),
                span("yt", day + 1.hours, 50.minutes),
                span("web:reddit.com", day + 2.hours, 10.minutes),
                span("insta", day + 3.hours, 20.minutes),
                span("maps", day + 4.hours, 300.minutes),
            ),
        )
        assertEquals(listOf("insta", "yt", "web:reddit.com"), report.topSinks.map { it.packageName })
        assertEquals(60.minutes, report.topSinks.first().time)
    }

    @Test
    fun `slips show the busiest two hours, wrapping midnight`() {
        val day = Instant.parse("2026-01-14T00:00:00Z")
        val report = build(listOf(slip(day + 22.hours + 5.minutes), slip(day + 23.hours + 30.minutes), slip(day + 1.days + 10.minutes), slip(day + 9.hours)))
        assertEquals(4, report.slips)
        assertEquals(22, report.busiestSlipHour)
        assertEquals(23, WeeklyReports.busiestWindow(listOf(23, 0, 0)))
        assertNull(WeeklyReports.busiestWindow(emptyList()))
    }

    @Test
    fun `the most common trigger is named`() {
        val at = thisWeek.start + 1.days
        val report = build(
            listOf(Trigger.BOREDOM, Trigger.STRESS, Trigger.BOREDOM).map {
                Event(EventType.TRIGGER, startAt = at, data = """{"trigger":"${it.name}"}""")
            },
        )
        assertEquals(Trigger.BOREDOM, report.topTrigger)
    }

    @Test
    fun `days under budget count the days that never ran out`() {
        val days = (0 until 5).map { Period(thisWeek.start + it.days, thisWeek.start + (it + 1).days) }
        val events = listOf(
            slip(days[1].start + 3.hours),
            Event(EventType.TIME_UP, startAt = days[3].start + 2.hours, data = """{"budgetUsed":true}"""),
            Event(EventType.TIME_UP, startAt = days[4].start + 2.hours, data = """{"budgetUsed":false}"""),
        )
        val report = build(events, monthDays = days)
        assertEquals(3, report.daysUnderBudget)
        assertEquals(5, report.daysSoFar)
        assertTrue(WeeklyReports.ranOutOfBudget(events, days[1]))
        assertFalse(WeeklyReports.ranOutOfBudget(events, days[4]))
    }

    @Test
    fun `the report notification comes on the next Sunday at 20 00`() {
        val zone = ZoneId.of("Europe/Berlin")
        val wednesday = ZonedDateTime.of(2026, 1, 14, 9, 0, 0, 0, zone)
        val sunday = ZonedDateTime.of(2026, 1, 18, 20, 0, 0, 0, zone)
        assertEquals(sunday, ReportTimes.nextAfter(wednesday, DayOfWeek.SUNDAY, LocalTime.of(20, 0)))
        assertEquals(sunday.plusWeeks(1), ReportTimes.nextAfter(sunday, DayOfWeek.SUNDAY, LocalTime.of(20, 0)))
        assertEquals(sunday, ReportTimes.nextAfter(sunday.minusMinutes(1), DayOfWeek.SUNDAY, LocalTime.of(20, 0)))
    }

    @Test
    fun `stats open once per usage day`() = runTest {
        val clock = FakeTimeSource()
        val access = StatsAccess(FakeEventLog(), DayBoundary(clock), clock)
        assertTrue(access.claim())
        assertFalse(access.claim())
        clock.advance(1.days)
        assertTrue(access.claim())
    }

    @Test
    fun `csv escapes commas, quotes and line breaks`() {
        assertEquals("plain", EventCsv.escape("plain"))
        assertEquals("\"a,b\"", EventCsv.escape("a,b"))
        assertEquals("\"{\"\"x\"\":1}\"", EventCsv.escape("{\"x\":1}"))
        val out = StringBuilder()
        EventCsv.write(listOf(Event(EventType.SCREEN_UNLOCK, startAt = Instant.parse("2026-01-12T08:00:00Z"), id = 7)), out)
        assertEquals("id,type,package,start,end,data\r\n7,SCREEN_UNLOCK,,2026-01-12T08:00:00Z,,\r\n", out.toString())
    }
}
