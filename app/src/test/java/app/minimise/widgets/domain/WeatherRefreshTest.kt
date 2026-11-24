package app.minimise.widgets.domain

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRefreshTest {
    private val now = 100.minutes

    @Test
    fun `the first time, always`() {
        assertTrue(WeatherRefresh.due(now, lastAttempt = null, lastSuccess = null, missing = true, forced = false))
    }

    @Test
    fun `a fresh reading is kept until it's half an hour old`() {
        assertFalse(WeatherRefresh.due(now, lastAttempt = now - 29.minutes, lastSuccess = now - 29.minutes, missing = false, forced = false))
        assertTrue(WeatherRefresh.due(now, lastAttempt = now - 30.minutes, lastSuccess = now - 30.minutes, missing = false, forced = false))
    }

    @Test
    fun `a tap or a newly switched-on line asks again`() {
        assertTrue(WeatherRefresh.due(now, lastAttempt = now - 5.minutes, lastSuccess = now - 5.minutes, missing = false, forced = true))
        assertTrue(WeatherRefresh.due(now, lastAttempt = now - 5.minutes, lastSuccess = now - 5.minutes, missing = true, forced = false))
    }

    @Test
    fun `never more than once a minute, however often home opens`() {
        assertFalse(WeatherRefresh.due(now, lastAttempt = now - 30.seconds, lastSuccess = null, missing = true, forced = true))
    }

    @Test
    fun `a failed refresh is tried again at the next visit a minute later`() {
        // The last good reading is old, and the attempt to replace it two minutes ago failed.
        assertTrue(WeatherRefresh.due(now, lastAttempt = now - 2.minutes, lastSuccess = now - 40.minutes, missing = false, forced = false))
    }
}
