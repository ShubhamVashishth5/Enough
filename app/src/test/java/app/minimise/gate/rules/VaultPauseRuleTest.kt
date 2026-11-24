package app.minimise.gate.rules

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultPauseRuleTest {
    @Test
    fun `waits follow the steps and the last one repeats`() {
        val waits = (0..6).map { VaultPauseRule.waitFor(it) }
        assertEquals(listOf(10, 20, 40, 60, 120, 120, 120).map { it.seconds }, waits)
    }
}
