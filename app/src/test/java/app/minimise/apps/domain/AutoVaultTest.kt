package app.minimise.apps.domain

import app.minimise.core.prefs.Settings
import app.minimise.core.time.FakeTimeSource
import app.minimise.testing.AppSettingsTargetForTest
import app.minimise.settings.domain.LoosenDelay
import app.minimise.settings.domain.SettingsGuard
import app.minimise.testing.FakeAppRepository
import app.minimise.testing.FakeEventLog
import app.minimise.testing.FakePendingChangeStore
import app.minimise.testing.NoLockPeriod
import app.minimise.testing.FakeSettingsStore
import app.minimise.testing.app
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoVaultTest {
    private val clock = FakeTimeSource()
    private val events = FakeEventLog()
    private val game = app("Game")
    private val apps = FakeAppRepository(listOf(game))
    private val settings = FakeSettingsStore(Settings(onboardingDone = true))
    private val notices = mutableListOf<String>()
    private val guard = SettingsGuard(
        AppSettingsTargetForTest(apps),
        FakePendingChangeStore(),
        clock,
        object : LoosenDelay {
            override suspend fun current(): Duration = 24.hours
        },
        events,
        NoLockPeriod,
    )
    private val autoVault = AutoVault(apps, guard, settings, events, clock, object : AutoVaultNotice {
        override fun show(appLabel: String) {
            notices += appLabel
        }
    })

    @Test
    fun `a new game goes straight into the vault, with a notice`() = runTest {
        autoVault.onInstalled(game.packageName, "Game", AppCategory.GAME)
        assertEquals(AppKind.VAULT, apps.kindOf(game.packageName))
        assertEquals(listOf("Game"), notices)
    }

    @Test
    fun `other apps, first-run listing and earlier choices are left alone`() = runTest {
        autoVault.onInstalled(game.packageName, "Game", AppCategory.OTHER)
        assertEquals(AppKind.TOOL, apps.kindOf(game.packageName))

        settings.state.value = Settings(onboardingDone = false)
        autoVault.onInstalled(game.packageName, "Game", AppCategory.GAME)
        assertEquals(AppKind.TOOL, apps.kindOf(game.packageName))

        settings.state.value = Settings(onboardingDone = true)
        events.log(Event(type = EventType.SETTING_CHANGED, startAt = clock.now, packageName = game.packageName))
        autoVault.onInstalled(game.packageName, "Game", AppCategory.GAME)
        assertEquals(AppKind.TOOL, apps.kindOf(game.packageName))
        assertEquals(emptyList<String>(), notices)
    }
}
