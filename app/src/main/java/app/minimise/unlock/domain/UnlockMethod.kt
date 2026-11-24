package app.minimise.unlock.domain

/** Every way past a gate. Phase 1 has only [WAIT] (the pause screen itself). */
enum class UnlockMethodId { WAIT, NFC_KEY, QR_KEY, FRIEND_CODE, TYPING, EMERGENCY_PASS }

interface UnlockMethod {
    val id: UnlockMethodId

    suspend fun isAvailable(): Boolean
}
