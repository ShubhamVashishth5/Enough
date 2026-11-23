package app.minimise.usage.domain

import app.minimise.core.time.FakeTimeSource
import app.minimise.core.time.plus
import app.minimise.monitor.domain.ForegroundEvent
import app.minimise.testing.FakeEventLog
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageTrackerTest {
    private val clock = FakeTimeSource()
    private val log = FakeEventLog()
    private val tracker = UsageTracker(log, clock)

    private suspend fun open(packageName: String?) = tracker.onForeground(ForegroundEvent(packageName, clock.now))

    private fun spans() = log.events.value.filter { it.type == EventType.APP_SPAN }

    @Test
    fun `one span per continuous stretch`() = runTest {
        val start = clock.now
        open("maps")
        clock.advance(2.minutes)
        open("insta")
        clock.advance(3.minutes)
        open(null)

        val spans = spans()
        assertEquals(listOf("maps", "insta"), spans.map { it.packageName })
        assertEquals(start, spans[0].startAt)
        assertEquals(start + 2.minutes, spans[0].endAt)
        assertEquals(start + 5.minutes, spans[1].endAt)
        assertNull(tracker.openSpan.value)
    }

    @Test
    fun `the same app twice does not split the span`() = runTest {
        open("insta")
        clock.advance(1.minutes)
        open("insta")
        assertEquals(1, spans().size)
    }

    @Test
    fun `checkpoints move the open span's end forward`() = runTest {
        val start = clock.now
        open("insta")
        clock.advance(30.seconds)
        tracker.checkpoint()
        assertEquals(start + 30.seconds, spans().single().endAt)
    }
}
