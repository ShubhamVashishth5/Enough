package app.minimise.unlock.domain

import app.minimise.core.Defaults
import javax.inject.Inject
import kotlin.random.Random

/**
 * Typing penance: copy a long random passage exactly, key by key. No paste, no swipe typing, so
 * getting past the wait costs a few minutes of dull effort.
 */
class TypingMethod @Inject constructor() : UnlockMethod {
    override val id: UnlockMethodId = UnlockMethodId.TYPING

    override suspend fun isAvailable(): Boolean = true
}

object TypingPenance {
    /** Random words until the passage is at least [length] characters. */
    fun passage(words: List<String>, length: Int = Defaults.TYPING_PASSAGE_LENGTH, random: Random = Random.Default): String {
        require(words.isNotEmpty())
        val builder = StringBuilder()
        while (builder.length < length) {
            if (builder.isNotEmpty()) builder.append(' ')
            builder.append(words[random.nextInt(words.size)])
        }
        return builder.toString()
    }

    /**
     * Whether an edit from [old] to [new] could be typed by hand: at most [Defaults.TYPING_MAX_INSERT]
     * new characters at once. Pastes and whole-word swipes are rejected; deleting is always fine.
     */
    fun accepts(old: String, new: String): Boolean = new.length - old.length <= Defaults.TYPING_MAX_INSERT

    /** How many characters from the start are right. */
    fun correctSoFar(passage: String, typed: String): Int = passage.commonPrefixWith(typed).length

    fun isDone(passage: String, typed: String): Boolean = typed == passage
}
