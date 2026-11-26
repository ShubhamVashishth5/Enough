package app.minimise.launcher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutCodecTest {
    @Test
    fun `icons read back the same, in order, with and without a chosen app`() {
        val shortcuts = listOf(
            Shortcut(ShortcutIcon.CAMERA),
            Shortcut(ShortcutIcon.CHAT, "org.thoughtcrime.securesms"),
            Shortcut(ShortcutIcon.PHONE),
        )
        assertEquals(shortcuts, ShortcutCodec.decode(ShortcutCodec.encode(shortcuts)))
    }

    @Test
    fun `no icons is an empty string and reads back as none`() {
        assertEquals("", ShortcutCodec.encode(emptyList()))
        assertEquals(emptyList<Shortcut>(), ShortcutCodec.decode(""))
    }

    @Test
    fun `an icon a newer version added is skipped, and the rest kept`() {
        assertEquals(
            listOf(Shortcut(ShortcutIcon.PHONE), Shortcut(ShortcutIcon.MUSIC, "com.spotify.music")),
            ShortcutCodec.decode("PHONE:,HOLOGRAM:com.example,MUSIC:com.spotify.music"),
        )
    }

    @Test
    fun `no more than the most icons home shows`() {
        val many = ShortcutIcon.entries.take(Shortcut.MAX + 2).map { Shortcut(it) }
        assertEquals(many.take(Shortcut.MAX), ShortcutCodec.decode(ShortcutCodec.encode(many)))
    }

    @Test
    fun `the defaults are phone, messages and camera, each on Automatic`() {
        assertEquals(listOf(ShortcutIcon.PHONE, ShortcutIcon.MESSAGES, ShortcutIcon.CAMERA), Shortcut.DEFAULTS.map { it.icon })
        assertEquals(listOf(null, null, null), Shortcut.DEFAULTS.map { it.packageName })
    }
}
