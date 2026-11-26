package app.minimise.backup.domain

import app.minimise.apps.domain.AppKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val backup = Backup(
        exportedAtMillis = 1_764_000_000_000,
        apps = listOf(Backup.App("com.instagram.android", AppKind.VAULT, openLimit = 3), Backup.App("web:youtube.com", AppKind.VAULT)),
        budgetMinutes = 30,
        taperFloorMinutes = 15,
        blockedFeeds = listOf("youtube_shorts"),
        modes = listOf(Backup.Mode("sleep", budgetMinutes = 0, waitStepsSeconds = listOf(60, 120, 300), allowlist = listOf("com.kindle"), scheduleDays = 127, scheduleStartMinute = 1290, scheduleEndMinute = 420)),
        manualMode = "work",
        schedules = listOf(Backup.Schedule("Bedtime", days = 127, startMinute = 1320, endMinute = 420)),
        keys = listOf(Backup.Key("QR", "Printed code", "ab12")),
        friendCodeHashes = listOf("cd34"),
        swaps = listOf(Backup.Swap("Read 5 pages", "com.kindle")),
        digestEnabled = true,
        digestExempt = listOf("com.whatsapp"),
        grayscaleMode = "VAULT_APPS",
        shortcuts = listOf(Backup.Shortcut("PHONE"), Backup.Shortcut("CHAT", "org.thoughtcrime.securesms")),
        appearance = Backup.Appearance(font = "SERIF", textSize = "LARGER", clockSize = "LARGE", appNameSize = "SMALL", iconSize = "LARGE"),
    )

    @Test
    fun `a backup reads back the same`() {
        assertEquals(BackupCodec.Decoded.Ok(backup), BackupCodec.decode(BackupCodec.encode(backup)))
    }

    @Test
    fun `the file says what it is and which version wrote it`() {
        val text = BackupCodec.encode(backup)
        assertTrue(text.contains("\"format\": \"enough-settings\""))
        assertTrue(text.contains("\"version\": ${BackupCodec.VERSION}"))
    }

    @Test
    fun `a file from a newer version is refused`() {
        val newer = BackupCodec.encode(backup.copy(version = BackupCodec.VERSION + 1))
        assertEquals(BackupCodec.Decoded.TooNew(BackupCodec.VERSION + 1), BackupCodec.decode(newer))
    }

    @Test
    fun `other files are unreadable`() {
        assertEquals(BackupCodec.Decoded.Unreadable, BackupCodec.decode("type,packageName\nAPP_SPAN,x"))
        assertEquals(BackupCodec.Decoded.Unreadable, BackupCodec.decode("""{"format":"something-else","version":1}"""))
        assertEquals(BackupCodec.Decoded.Unreadable, BackupCodec.decode("""{"format":"enough-settings","version":1}"""))
    }

    @Test
    fun `fields an older file doesn't have take their defaults`() {
        val old = """{"format":"enough-settings","version":1,"exportedAtMillis":5,"budgetMinutes":20}"""
        val decoded = BackupCodec.decode(old)
        assertEquals(BackupCodec.Decoded.Ok(Backup(version = 1, exportedAtMillis = 5, budgetMinutes = 20)), decoded)
        // No home section: a restore leaves this phone's home screen as it is.
        assertNull((decoded as BackupCodec.Decoded.Ok).backup.home)
    }

    @Test
    fun `a version 2 file from before icons and appearance leaves both alone`() {
        val before = """{"format":"enough-settings","version":2,"exportedAtMillis":5,"budgetMinutes":20,"home":{}}"""
        val decoded = (BackupCodec.decode(before) as BackupCodec.Decoded.Ok).backup
        assertNull(decoded.shortcuts)
        assertNull(decoded.appearance)
    }

    @Test
    fun `breathe apps and the home screen survive a round trip`() {
        val withBreathe = backup.copy(
            apps = listOf(Backup.App("com.whatsapp", AppKind.BREATHE)),
            home = Backup.Home(weather = true, hideStatusBar = true, battery = false),
        )
        assertEquals(BackupCodec.Decoded.Ok(withBreathe), BackupCodec.decode(BackupCodec.encode(withBreathe)))
    }

    @Test
    fun `breathe pauses, the user's own modes and a pick's end survive a round trip`() {
        val withModes = backup.copy(
            apps = listOf(Backup.App("com.whatsapp", AppKind.BREATHE, breatheWaitSeconds = 30)),
            modes = listOf(Backup.Mode(Backup.CUSTOM, budgetMinutes = 10, waitStepsSeconds = listOf(30, 60), name = "Commute")),
            manualMode = "custom:Commute",
            manualModeUntilMillis = 1_000L,
        )
        assertEquals(BackupCodec.Decoded.Ok(withModes), BackupCodec.decode(BackupCodec.encode(withModes)))
    }
}
