package app.minimise.swaps.domain

import kotlinx.coroutines.flow.Flow

/**
 * One entry of the Instead list: something to do in place of a Vault app.
 *
 * @property packageName an optional Tool app that "Do this instead" opens ("Read 5 pages" opens the e-book app).
 */
data class Swap(
    val id: Long = 0,
    val text: String,
    val packageName: String? = null,
)

interface SwapRepository {
    /** The Instead list, oldest first. */
    val swaps: Flow<List<Swap>>

    suspend fun all(): List<Swap>

    /** Adds the swap when its id is 0, otherwise updates it. Blank text is ignored. */
    suspend fun save(swap: Swap)

    suspend fun delete(id: Long)

    /** Fills the list with the built-in swaps the first time it's called, and never again. */
    suspend fun seedDefaultsOnce()
}

/** The swap to offer on the [turn]th pause: steps through the list in order, then starts over. */
fun List<Swap>.forTurn(turn: Int): Swap? = if (isEmpty()) null else this[Math.floorMod(turn, size)]
