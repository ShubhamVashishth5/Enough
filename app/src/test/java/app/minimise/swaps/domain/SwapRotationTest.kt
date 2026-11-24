package app.minimise.swaps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwapRotationTest {
    private val swaps = listOf(Swap(1, "Read 5 pages"), Swap(2, "10 push-ups"), Swap(3, "Go for a walk"))

    @Test
    fun `takes turns through the list and starts over`() {
        val picked = (0..6).map { swaps.forTurn(it)?.id }
        assertEquals(listOf(1L, 2L, 3L, 1L, 2L, 3L, 1L), picked)
    }

    @Test
    fun `an empty list offers nothing`() {
        assertNull(emptyList<Swap>().forTurn(4))
    }
}
