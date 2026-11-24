package app.minimise.usage.domain

import app.minimise.core.time.minus
import app.minimise.core.time.plus
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayUsageTest {
    private val dayStart = Instant.parse("2026-01-15T04:00:00Z")
    private val dayEnd = dayStart + 24.hours
    private val now = dayStart + 10.hours
    private val vault = setOf("insta", "yt")

    private fun span(id: Long, pkg: String, start: Instant, end: Instant) =
        Event(type = EventType.APP_SPAN, startAt = start, endAt = end, packageName = pkg, id = id)

    private fun usage(events: List<Event>, openSpanId: Long? = null) =
        TodayUsage.from(events, dayStart, dayEnd, now, vault, openSpanId)

    @Test
    fun `adds up vault spans across apps and ignores tool apps`() {
        val events = listOf(
            span(1, "insta", dayStart + 1.hours, dayStart + 1.hours + 10.minutes),
            span(2, "maps", dayStart + 2.hours, dayStart + 3.hours),
            span(3, "yt", dayStart + 4.hours, dayStart + 4.hours + 5.minutes),
        )
        assertEquals(15.minutes, usage(events).vaultTime)
    }

    @Test
    fun `a span that started before 04-00 only counts from the reset`() {
        val events = listOf(span(1, "insta", dayStart - 20.minutes, dayStart + 5.minutes))
        assertEquals(5.minutes, usage(events).vaultTime)
    }

    @Test
    fun `the open span counts up to now, not its last checkpoint`() {
        val events = listOf(span(7, "insta", now - 3.minutes, now - 2.minutes))
        assertEquals(1.minutes, usage(events).vaultTime)
        assertEquals(3.minutes, usage(events, openSpanId = 7).vaultTime)
    }

    @Test
    fun `counts proceeded opens and unlocks inside the day only`() {
        val events = listOf(
            Event(type = EventType.GATE_PROCEEDED, startAt = dayStart + 1.hours),
            Event(type = EventType.GATE_PROCEEDED, startAt = dayStart + 2.hours),
            Event(type = EventType.GATE_CANCELLED, startAt = dayStart + 2.hours),
            Event(type = EventType.SCREEN_UNLOCK, startAt = dayStart - 1.minutes),
            Event(type = EventType.SCREEN_UNLOCK, startAt = dayStart + 1.minutes),
        )
        val today = usage(events)
        assertEquals(2, today.proceededOpens)
        assertEquals(1, today.unlocks)
    }
}
