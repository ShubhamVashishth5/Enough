package app.minimise.keys.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.flow.Flow

/** A sticker to tap or a printed code to scan. Leave it in another room so getting past a block takes a walk. */
enum class KeyType { NFC, QR }

data class PhysicalKey(val id: Long, val type: KeyType, val label: String, val secretHash: String)

interface KeyRepository {
    val keys: Flow<List<PhysicalKey>>

    suspend fun all(): List<PhysicalKey>

    suspend fun get(id: Long): PhysicalKey?

    /** Writes directly. Only SettingsGuard should call this. */
    suspend fun add(type: KeyType, label: String, secretHash: String)

    /** Only SettingsGuard should call this. */
    suspend fun delete(id: Long)
}

object KeySecrets {
    private const val QR_PREFIX = "enough-key:"
    private val random = SecureRandom()

    /** The text to print as a QR code. Never stored; only its hash is. */
    fun newQrSecret(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return QR_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hashOf(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun hashOf(text: String): String = hashOf(text.toByteArray(Charsets.UTF_8))
}
