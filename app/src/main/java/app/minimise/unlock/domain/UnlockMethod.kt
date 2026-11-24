package app.minimise.unlock.domain

/** Every way past a gate. [WAIT] is the pause screen itself; the rest are overrides offered by a Block. */
enum class UnlockMethodId { WAIT, NFC_KEY, QR_KEY, FRIEND_CODE, TYPING, EMERGENCY_PASS }

/**
 * One way past a Block. Each method validates its own attempt (a tap, a scan, a wait) and is registered
 * by id with a Hilt @IntoMap binding in UnlockModule, next to a screen in the Blocked screen.
 */
interface UnlockMethod {
    val id: UnlockMethodId

    suspend fun isAvailable(): Boolean

    /** How many uses are left, for methods that are rationed. Null when unlimited. */
    suspend fun usesLeft(): Int? = null
}
