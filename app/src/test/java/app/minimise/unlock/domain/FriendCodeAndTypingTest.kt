package app.minimise.unlock.domain

import java.util.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendCodeAndTypingTest {
    private class FakeCodes : FriendCodeRepository {
        val hashes = mutableMapOf<String, Boolean>() // hash -> used

        override val unused: Flow<Int> = MutableStateFlow(0)

        override suspend fun unusedCount(): Int = hashes.count { !it.value }

        override suspend fun replaceAll(hashes: List<String>) {
            this.hashes.clear()
            hashes.forEach { this.hashes[it] = false }
        }

        override suspend fun spend(hash: String): Boolean {
            if (hashes[hash] != false) return false
            hashes[hash] = true
            return true
        }
    }

    @Test
    fun `codes are readable and distinct`() {
        val codes = FriendCodes.generate(10, Random(1))
        assertEquals(10, codes.toSet().size)
        codes.forEach { assertTrue(it.matches(Regex("[A-Z2-9]{4}-[A-Z2-9]{4}"))) }
        assertFalse(codes.any { c -> c.any { it in "01ILO" } })
    }

    @Test
    fun `a code works once, however it's typed`() = runTest {
        val repo = FakeCodes()
        val code = FriendCodes.generate(1, Random(2)).single()
        repo.replaceAll(listOf(FriendCodes.hashOf(code)))
        val method = FriendCodeMethod(repo)
        assertTrue(method.isAvailable())
        assertTrue(method.verify(" " + code.lowercase().replace("-", " ") + " "))
        assertFalse(method.verify(code))
        assertFalse(method.isAvailable())
    }

    @Test
    fun `a wrong or empty code is refused`() = runTest {
        val repo = FakeCodes()
        repo.replaceAll(listOf(FriendCodes.hashOf("AAAA-BBBB")))
        val method = FriendCodeMethod(repo)
        assertFalse(method.verify("AAAA-BBBC"))
        assertFalse(method.verify(" - "))
        assertEquals(1, method.usesLeft())
    }

    @Test
    fun `new codes replace old ones`() = runTest {
        val repo = FakeCodes()
        repo.replaceAll(listOf(FriendCodes.hashOf("OLDC-ODE1")))
        repo.replaceAll(listOf(FriendCodes.hashOf("NEWC-ODE2")))
        assertFalse(FriendCodeMethod(repo).verify("OLDC-ODE1"))
    }

    @Test
    fun `a passage is long enough and made of the words`() {
        val words = listOf("apple", "river", "window")
        val passage = TypingPenance.passage(words, length = 300, random = kotlin.random.Random(3))
        assertTrue(passage.length >= 300)
        assertTrue(passage.split(' ').all { it in words })
    }

    @Test
    fun `typing one key at a time is accepted, pasting is not`() {
        assertTrue(TypingPenance.accepts("app", "appl"))
        assertTrue(TypingPenance.accepts("apple", "ap"))
        assertFalse(TypingPenance.accepts("", "apple river"))
    }

    @Test
    fun `progress counts the right characters from the start`() {
        assertEquals(3, TypingPenance.correctSoFar("apple", "appx"))
        assertTrue(TypingPenance.isDone("apple", "apple"))
        assertFalse(TypingPenance.isDone("apple", "Apple"))
    }
}
