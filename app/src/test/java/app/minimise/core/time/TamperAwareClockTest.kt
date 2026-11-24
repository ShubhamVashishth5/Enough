package app.minimise.core.time

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TamperAwareClockTest {
    private var wall = Instant.parse("2026-03-01T10:00:00Z")
    private var elapsed = 5.hours
    private var boot = 7
    private var network = false
    private val store = object : ClockAnchorStore {
        var saved: ClockAnchor? = null

        override fun load(): ClockAnchor? = saved

        override fun save(anchor: ClockAnchor) {
            saved = anchor
        }
    }

    private fun clock() = TamperAwareClock({ wall }, { elapsed }, { boot }, { network }, store)

    private fun pass(time: Duration) {
        wall += time
        elapsed += time
    }

    @Test
    fun `an untouched clock is trusted`() {
        val clock = clock()
        assertEquals(wall, clock.now())
        pass(3.hours)
        assertEquals(wall, clock.now())
        assertFalse(clock.tampered)
    }

    @Test
    fun `moving the clock forward is ignored`() {
        val clock = clock()
        val start = clock.now()
        wall += 3.hours // the user changes the date and time
        pass(10.minutes)
        assertEquals(start + 10.minutes, clock.now())
        assertTrue(clock.tampered)
    }

    @Test
    fun `moving the clock back is ignored too`() {
        val clock = clock()
        val start = clock.now()
        wall -= 2.hours
        assertEquals(start, clock.now())
    }

    @Test
    fun `small corrections are taken`() {
        val clock = clock()
        clock.now()
        wall += 2.minutes
        assertEquals(wall, clock.now())
    }

    @Test
    fun `network time is always taken`() {
        val clock = clock()
        clock.now()
        wall += 3.hours
        network = true
        assertEquals(wall, clock.now())
    }

    @Test
    fun `a tampered clock stays ignored after the app restarts`() {
        val start = clock().now()
        wall += 3.hours
        pass(1.minutes)
        assertEquals(start + 1.minutes, clock().now())
    }

    @Test
    fun `after a reboot the clock can't go back before the last trusted time`() {
        val start = clock().now()
        boot++
        elapsed = 30.minutes.also { wall = start - 1.hours }
        assertEquals(start + 30.minutes, clock().now())
    }

    @Test
    fun `after a reboot a later clock is trusted`() {
        val start = clock().now()
        boot++
        elapsed = 1.minutes
        wall = start + 8.hours // the phone was off overnight
        assertEquals(wall, clock().now())
    }
}
