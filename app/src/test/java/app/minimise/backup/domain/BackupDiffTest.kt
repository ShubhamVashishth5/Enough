package app.minimise.backup.domain

import app.minimise.apps.domain.AppKind
import app.minimise.core.time.FakeTimeSource
import app.minimise.settings.domain.AddKey
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.LoosenDelay
import app.minimise.settings.domain.RemoveKey
import app.minimise.settings.domain.RemoveMode
import app.minimise.settings.domain.SetBreatheWait
import app.minimise.settings.domain.RemoveSchedule
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SaveSchedule
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SetFeedBlocked
import app.minimise.settings.domain.SetManualMode
import app.minimise.settings.domain.SetOpenLimit
import app.minimise.settings.domain.SettingsGuard
import app.minimise.settings.domain.StartLock
import app.minimise.testing.FakeEventLog
import app.minimise.testing.FakeLockPeriod
import app.minimise.testing.FakePendingChangeStore
import app.minimise.testing.FakeSettingsTarget
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDiffTest {
    private val now = 1_764_000_000_000L
    private val fresh = Backup(exportedAtMillis = now, budgetMinutes = 45)
    private val bedtime = Backup.Schedule("Bedtime", days = 127, startMinute = 1320, endMinute = 420)
    private val sleep = Backup.Mode("sleep", budgetMinutes = 0, waitStepsSeconds = listOf(60, 120, 300))
    private val modeIds = mapOf("work" to 1L, "study" to 2L, "home" to 3L, "sleep" to 4L)

    private fun local(settings: Backup = fresh, schedules: List<Pair<Long, Backup.Schedule>> = emptyList(), keyIds: Map<String, Long> = emptyMap()) =
        LocalSettings(settings.copy(schedules = schedules.map { it.second }), modeIds, schedules, keyIds)

    @Test
    fun `the same settings mean no changes`() {
        val here = fresh.copy(apps = listOf(Backup.App("pkg.insta", AppKind.VAULT)), modes = listOf(sleep))
        assertEquals(emptyList<Any>(), BackupDiff.changes(here.copy(exportedAtMillis = 1), local(here), now))
    }

    @Test
    fun `apps, limits, budget and feeds that differ become changes`() {
        val incoming = fresh.copy(
            apps = listOf(Backup.App("pkg.insta", AppKind.VAULT, openLimit = 3)),
            budgetMinutes = 60,
            blockedFeeds = listOf("youtube_shorts"),
        )
        val here = fresh.copy(blockedFeeds = listOf("instagram_reels"))
        assertEquals(
            listOf(
                SetAppKind("pkg.insta", AppKind.VAULT),
                SetOpenLimit("pkg.insta", 3),
                SetBudget(60),
                SetFeedBlocked("instagram_reels", false),
                SetFeedBlocked("youtube_shorts", true),
            ),
            BackupDiff.changes(incoming, local(here), now),
        )
    }

    @Test
    fun `apps the backup doesn't mention are left alone`() {
        val here = fresh.copy(apps = listOf(Backup.App("pkg.tiktok", AppKind.VAULT)))
        assertEquals(emptyList<Any>(), BackupDiff.changes(fresh, local(here), now))
    }

    @Test
    fun `schedules match by name`() {
        val focus = Backup.Schedule("Focus hours", days = 31, startMinute = 540, endMinute = 720)
        val incoming = fresh.copy(schedules = listOf(bedtime.copy(startMinute = 1260), focus))
        val here = local(schedules = listOf(7L to bedtime, 8L to Backup.Schedule("Lunch", 31, 720, 780)))
        assertEquals(
            listOf(
                SaveSchedule(7, "Bedtime", 127, 1260, 420),
                SaveSchedule(0, "Focus hours", 31, 540, 720),
                RemoveSchedule(8),
            ),
            BackupDiff.changes(incoming, here, now),
        )
    }

    @Test
    fun `modes match by preset, and the hand-picked mode comes after them`() {
        val incoming = fresh.copy(modes = listOf(sleep.copy(allowlist = listOf("pkg.kindle"))), manualMode = "sleep")
        val here = fresh.copy(modes = listOf(sleep))
        assertEquals(
            listOf(SaveMode(4, 0, listOf("pkg.kindle"), 0, 0, 0, waitStepsSeconds = listOf(60, 120, 300)), SetManualMode(4)),
            BackupDiff.changes(incoming, local(here), now),
        )
    }

    @Test
    fun `the user's own modes match by name, and a running pick keeps its end`() {
        val commute = Backup.Mode(Backup.CUSTOM, budgetMinutes = 10, waitStepsSeconds = listOf(30), name = "Commute")
        val gym = Backup.Mode(Backup.CUSTOM, budgetMinutes = 0, waitStepsSeconds = listOf(60), name = "Gym")
        val old = Backup.Mode(Backup.CUSTOM, budgetMinutes = 30, waitStepsSeconds = listOf(10), name = "Old")
        val later = now + 3_600_000L
        val incoming = fresh.copy(modes = listOf(commute.copy(budgetMinutes = 5), gym), manualMode = "custom:Commute", manualModeUntilMillis = later)
        val here = LocalSettings(fresh.copy(modes = listOf(commute, old)), modeIds, emptyList(), emptyMap(), customModeIds = mapOf("Commute" to 7L, "Old" to 8L))
        assertEquals(
            listOf(
                SaveMode(7, 5, null, 0, 0, 0, waitStepsSeconds = listOf(30), name = "Commute"),
                SaveMode(0, 0, null, 0, 0, 0, waitStepsSeconds = listOf(60), name = "Gym"),
                RemoveMode(8),
                SetManualMode(7, untilEpochMillis = later),
            ),
            BackupDiff.changes(incoming, here, now),
        )
    }

    @Test
    fun `a pick that has ended since the backup was made is no pick`() {
        val incoming = fresh.copy(manualMode = "work", manualModeUntilMillis = now - 1)
        assertTrue(BackupDiff.changes(incoming, local(), now).isEmpty())
        val running = local(fresh.copy(manualMode = "work", manualModeUntilMillis = now + 60_000L))
        assertEquals(listOf(SetManualMode(null)), BackupDiff.changes(incoming, running, now))
    }

    @Test
    fun `breathe pauses that differ become changes`() {
        val incoming = fresh.copy(apps = listOf(Backup.App("pkg.whatsapp", AppKind.BREATHE, breatheWaitSeconds = 30)))
        val here = fresh.copy(apps = listOf(Backup.App("pkg.whatsapp", AppKind.BREATHE)))
        assertEquals(listOf(SetBreatheWait("pkg.whatsapp", 30)), BackupDiff.changes(incoming, local(here), now))
    }

    @Test
    fun `keys are added and removed by hash`() {
        val incoming = fresh.copy(keys = listOf(Backup.Key("QR", "Printed code", "new")))
        val here = fresh.copy(keys = listOf(Backup.Key("NFC", "Sticker", "old")))
        assertEquals(
            listOf(AddKey("QR", "Printed code", "new"), RemoveKey(5)),
            BackupDiff.changes(incoming, local(here, keyIds = mapOf("old" to 5L)), now),
        )
    }

    @Test
    fun `a running lock comes last, and one already over is skipped`() {
        val later = now + 7 * 24 * 3_600_000L
        val changes = BackupDiff.changes(fresh.copy(budgetMinutes = 60, lockedUntilMillis = later), local(), now)
        assertEquals(StartLock(later), changes.last())
        assertTrue(BackupDiff.changes(fresh.copy(lockedUntilMillis = now - 1), local(), now).isEmpty())
    }

    @Test
    fun `through the guard, stricter applies now and a bigger budget waits`() = runTest {
        val target = FakeSettingsTarget()
        val clock = FakeTimeSource()
        val guard = SettingsGuard(
            target,
            FakePendingChangeStore(),
            clock,
            object : LoosenDelay {
                override suspend fun current(): Duration = 24.hours
            },
            FakeEventLog(),
            FakeLockPeriod(target),
        )
        val incoming = fresh.copy(apps = listOf(Backup.App("pkg.insta", AppKind.VAULT)), budgetMinutes = 60, schedules = listOf(bedtime))
        val results = BackupDiff.changes(incoming, local(), now).map { guard.request(it) }
        assertEquals(AppKind.VAULT, target.kinds["pkg.insta"])
        assertEquals(1, target.schedules.size)
        assertEquals(45, target.budgetMinutes)
        assertEquals(listOf(SetBudget(60)), results.zip(BackupDiff.changes(incoming, local(), now)).filter { it.first is GuardResult.Scheduled }.map { it.second })
    }
}
