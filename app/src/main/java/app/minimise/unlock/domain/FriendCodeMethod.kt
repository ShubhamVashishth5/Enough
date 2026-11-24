package app.minimise.unlock.domain

import app.minimise.keys.domain.KeySecrets
import java.security.SecureRandom
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** One-time codes a friend keeps. Only their hashes are stored. */
interface FriendCodeRepository {
    val unused: Flow<Int>

    suspend fun unusedCount(): Int

    /** Throws away every earlier code. */
    suspend fun replaceAll(hashes: List<String>)

    /** Spends the code with [hash]. False when no unused code matches. */
    suspend fun spend(hash: String): Boolean
}

object FriendCodes {
    /** No 0/O, 1/I/L: codes are read out loud or typed from a message. */
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 8

    /** "K7QM-4XPA". */
    fun generate(count: Int, random: java.util.Random = SecureRandom()): List<String> =
        List(count) {
            val chars = List(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
            chars.chunked(LENGTH / 2).joinToString("-")
        }

    /** Spaces, dashes and case don't matter when the code is typed back. */
    fun normalise(input: String): String = input.uppercase().filter { it.isLetterOrDigit() }

    fun hashOf(code: String): String = KeySecrets.hashOf(normalise(code))
}

/** A friend reads one of their codes back to you. Each code works once. */
class FriendCodeMethod @Inject constructor(
    private val codes: FriendCodeRepository,
) : UnlockMethod {
    override val id: UnlockMethodId = UnlockMethodId.FRIEND_CODE

    override suspend fun isAvailable(): Boolean = codes.unusedCount() > 0

    override suspend fun usesLeft(): Int = codes.unusedCount()

    /** Spends the code if it's good. */
    suspend fun verify(input: String): Boolean = FriendCodes.normalise(input).isNotEmpty() && codes.spend(FriendCodes.hashOf(input))
}
