package app.minimise.calendar.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextEventTest {
    private val now = Instant.parse("2026-11-24T10:30:00Z")
    private fun event(title: String, begin: String, end: String) = CalendarEvent(title, Instant.parse(begin), Instant.parse(end))

    @Test
    fun `an event under way comes before the next one`() {
        val standup = event("Standup", "2026-11-24T10:00:00Z", "2026-11-24T10:45:00Z")
        val review = event("Review", "2026-11-24T14:00:00Z", "2026-11-24T15:00:00Z")
        assertEquals(standup, NextEvent.pick(listOf(review, standup), now))
    }

    @Test
    fun `finished events are skipped`() {
        val done = event("Done", "2026-11-24T09:00:00Z", "2026-11-24T10:30:00Z")
        val review = event("Review", "2026-11-24T14:00:00Z", "2026-11-24T15:00:00Z")
        assertEquals(review, NextEvent.pick(listOf(done, review), now))
        assertNull(NextEvent.pick(listOf(done), now))
    }
}
