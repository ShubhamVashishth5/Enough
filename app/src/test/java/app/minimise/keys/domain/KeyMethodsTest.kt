package app.minimise.keys.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyMethodsTest {
    private val sticker = byteArrayOf(0x04, 0x1A, 0x2B, 0x3C)
    private val code = KeySecrets.newQrSecret()
    private val keys = listOf(
        PhysicalKey(1, KeyType.NFC, "Sticker", KeySecrets.hashOf(sticker)),
        PhysicalKey(2, KeyType.QR, "Code", KeySecrets.hashOf(code)),
    )

    @Test
    fun `a paired sticker or code matches, anything else doesn't`() {
        assertTrue(matches(keys, KeyType.NFC, KeySecrets.hashOf(sticker)))
        assertTrue(matches(keys, KeyType.QR, KeySecrets.hashOf(code)))
        assertFalse(matches(keys, KeyType.NFC, KeySecrets.hashOf(byteArrayOf(1, 2, 3))))
        assertFalse(matches(keys, KeyType.QR, KeySecrets.hashOf("enough-key:guess")))
    }

    @Test
    fun `a code's hash only works for its own type`() {
        assertFalse(matches(keys, KeyType.NFC, KeySecrets.hashOf(code)))
    }

    @Test
    fun `each new code is different and hashes are stable`() {
        assertNotEquals(KeySecrets.newQrSecret(), KeySecrets.newQrSecret())
        assertEquals(KeySecrets.hashOf("abc"), KeySecrets.hashOf("abc"))
        assertEquals(64, KeySecrets.hashOf("abc").length)
    }
}
