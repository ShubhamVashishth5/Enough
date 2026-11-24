package app.minimise.calendar.domain

import java.time.Instant

data class CalendarEvent(val title: String, val begin: Instant, val end: Instant)

/** Reads the phone's calendar, when the user has allowed it. */
interface CalendarSource {
    fun isAvailable(): Boolean

    /** Timed events overlapping [from, until), soonest first. All-day and declined events are left out. */
    suspend fun eventsBetween(from: Instant, until: Instant): List<CalendarEvent>
}

object NextEvent {
    /** The event going on now, else the next one to start. */
    fun pick(events: List<CalendarEvent>, now: Instant): CalendarEvent? =
        events.filter { it.end.isAfter(now) }.minByOrNull { it.begin }
}
