package app.minimise.calendar.domain

import app.minimise.core.prefs.Settings
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.FakeTimeSource
import app.minimise.core.time.minus
import app.minimise.core.time.plus
import app.minimise.focus.domain.DoNotDisturb
import app.minimise.focus.domain.FocusRepository
import app.minimise.focus.domain.FocusSession
import app.minimise.focus.domain.FocusSessions
import app.minimise.testing.FakeAppRepository
import app.minimise.testing.FakeEventLog
import app.minimise.testing.FakeSettingsStore
import app.minimise.usage.domain.EarnIt
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarFocusTest {
    private class FakeFocusRepository : FocusRepository {
        val sessions = MutableStateFlow<List<FocusSession>>(emptyList())

        override val open: Flow<FocusSession?> = sessions.map { list -> list.firstOrNull { it.endedAt == null } }
        override val lastEnded: Flow<FocusSession?> = sessions.map { list -> list.lastOrNull { it.endedAt != null } }

        override suspend fun open(): FocusSession? = sessions.value.firstOrNull { it.endedAt == null }

        override suspend fun start(startAt: Instant, endAt: Instant, locked: Boolean, allowlist: Set<String>, previousInterruptionFilter: Int?) {
            sessions.value += FocusSession(sessions.value.size + 1L, startAt, endAt, null, locked, allowlist)
        }

        override suspend fun end(id: Long, at: Instant): Boolean {
            sessions.value = sessions.value.map { if (it.id == id) it.copy(endedAt = at) else it }
            return true
        }
    }

    private val clock = FakeTimeSource()
    private val settings = FakeSettingsStore(Settings(calendarFocus = true, focusAllowlist = emptySet()))
    private val events = FakeEventLog()
    private val repository = FakeFocusRepository()
    private var calendarEvents = listOf<CalendarEvent>()
    private val calendar = object : CalendarSource {
        override fun isAvailable() = true

        override suspend fun eventsBetween(from: Instant, until: Instant) =
            calendarEvents.filter { it.begin.isBefore(until) && it.end.isAfter(from) }
    }
    private val sessions = FocusSessions(
        repository,
        settings,
        FakeAppRepository(),
        object : DoNotDisturb {
            override fun isAvailable() = false

            override fun enable(): Int? = null

            override fun restore(previous: Int) = Unit
        },
        events,
        clock,
        EarnIt(settings, events, DayBoundary(clock), clock),
    )
    private val calendarFocus = CalendarFocus(calendar, sessions, settings, clock)

    @Test
    fun `only events with focus in the title count`() {
        val now = clock.now
        val list = listOf(
            CalendarEvent("Standup", now, now + 15.minutes),
            CalendarEvent("Focus: report", now - 5.minutes, now + 1.hours),
            CalendarEvent("deep FOCUS", now + 2.hours, now + 3.hours),
        )
        assertEquals("Focus: report", FocusEvents.current(list, now)?.title)
        assertEquals("deep FOCUS", FocusEvents.next(list, now)?.title)
    }

    @Test
    fun `a focus event going on starts a session until it ends`() = runTest {
        calendarEvents = listOf(CalendarEvent("Focus: report", clock.now - 5.minutes, clock.now + 50.minutes))
        assertTrue(calendarFocus.startIfDue())
        val session = sessions.current()!!
        assertEquals(clock.now + 50.minutes, session.endAt)
        assertFalse(session.locked)
    }

    @Test
    fun `a session ended early isn't started again for the same event`() = runTest {
        calendarEvents = listOf(CalendarEvent("Focus", clock.now, clock.now + 1.hours))
        calendarFocus.startIfDue()
        sessions.endEarly()
        clock.advance(5.minutes)
        assertFalse(calendarFocus.startIfDue())
        assertNull(sessions.current())
    }

    @Test
    fun `nothing starts while calendar focus is off`() = runTest {
        settings.state.value = Settings(calendarFocus = false)
        calendarEvents = listOf(CalendarEvent("Focus", clock.now, clock.now + 1.hours))
        assertFalse(calendarFocus.startIfDue())
        assertNull(calendarFocus.nextCheck())
    }

    @Test
    fun `the alarm is set for the next focus event, or a day ahead`() = runTest {
        assertEquals(clock.now + 24.hours, calendarFocus.nextCheck())
        calendarEvents = listOf(CalendarEvent("Focus block", clock.now + 3.hours, clock.now + 4.hours))
        assertEquals(clock.now + 3.hours, calendarFocus.nextCheck())
    }
}
