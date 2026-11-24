package app.minimise.calendar.domain

import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.time.plus
import app.minimise.focus.domain.FocusSessions
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FocusEvents {
    fun isFocus(event: CalendarEvent): Boolean = event.title.contains(Defaults.CALENDAR_FOCUS_WORD, ignoreCase = true)

    /** Identifies one occurrence, so a session ended early isn't started again for the same event. */
    fun keyOf(event: CalendarEvent): String = "${event.begin.toEpochMilli()}|${event.title}"

    /** The focus event going on at [now], if any. */
    fun current(events: List<CalendarEvent>, now: Instant): CalendarEvent? =
        events.filter { isFocus(it) && !it.begin.isAfter(now) && it.end.isAfter(now) }.minByOrNull { it.begin }

    /** The next focus event to start after [now]. */
    fun next(events: List<CalendarEvent>, now: Instant): CalendarEvent? =
        events.filter { isFocus(it) && it.begin.isAfter(now) }.minByOrNull { it.begin }
}

/** Arms an alarm for the start of the next focus event (or a day ahead, to look again). */
interface CalendarFocusScheduler {
    suspend fun scheduleNext()
}

/**
 * Calendar focus (off until switched on): an event with "focus" in its title starts a focus session
 * that lasts until the event ends. Checked on every screen unlock and by an alarm at each event's start.
 */
@Singleton
class CalendarFocus @Inject constructor(
    private val calendar: CalendarSource,
    private val focusSessions: FocusSessions,
    private val settingsStore: SettingsStore,
    private val timeSource: TimeSource,
) {
    private val mutex = Mutex()

    suspend fun isOn(): Boolean = settingsStore.current().calendarFocus && calendar.isAvailable()

    /** Starts a session for the focus event going on now. False when there's nothing to start. */
    suspend fun startIfDue(): Boolean = mutex.withLock {
        if (!isOn()) return@withLock false
        val now = timeSource.now()
        val event = FocusEvents.current(calendar.eventsBetween(now, now + 1.minutes), now) ?: return@withLock false
        val key = FocusEvents.keyOf(event)
        if (settingsStore.current().calendarFocusStartedKey == key || focusSessions.current() != null) return@withLock false
        val length = durationBetween(now, event.end)
        if (length <= Duration.ZERO) return@withLock false
        settingsStore.update { it.copy(calendarFocusStartedKey = key) }
        focusSessions.start(length, locked = false)
        true
    }

    /** When the next alarm should go off, or null when calendar focus is off. */
    suspend fun nextCheck(): Instant? {
        if (!isOn()) return null
        val now = timeSource.now()
        val horizon = now + Defaults.CALENDAR_FOCUS_LOOKAHEAD
        return FocusEvents.next(calendar.eventsBetween(now, horizon), now)?.begin ?: horizon
    }
}
