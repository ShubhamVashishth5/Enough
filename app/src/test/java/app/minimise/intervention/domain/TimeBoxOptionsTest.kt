package app.minimise.intervention.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeBoxOptionsTest {
    @Test
    fun `options are capped at the budget left`() {
        assertEquals(listOf(2.minutes, 5.minutes, 10.minutes), TimeBoxOptions.cappedAt(30.minutes))
        assertEquals(listOf(2.minutes, 4.minutes), TimeBoxOptions.cappedAt(4.minutes))
    }

    @Test
    fun `with no budget left (an override), the usual options`() {
        assertEquals(listOf(2.minutes, 5.minutes, 10.minutes), TimeBoxOptions.cappedAt(Duration.ZERO))
    }
}
