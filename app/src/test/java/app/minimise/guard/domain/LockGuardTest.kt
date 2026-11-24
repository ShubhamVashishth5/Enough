package app.minimise.guard.domain

import app.minimise.core.prefs.Settings
import app.minimise.core.time.FakeTimeSource
import app.minimise.core.time.plus
import app.minimise.testing.FakeScreenNode
import app.minimise.testing.FakeSettingsStore
import app.minimise.testing.node
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockGuardTest {
    private val labels = mapOf("app_label" to "Enough", "service_label" to "Enough: Vault gate")
    private val screens = GuardScreens.parse(File("src/main/assets/guard_rules.json").readText())
    private val clock = FakeTimeSource()
    private val settings = FakeSettingsStore()
    private val guard = LockGuard(
        source = object : GuardScreenSource {
            override fun screens() = this@LockGuardTest.screens
        },
        settingsStore = settings,
        timeSource = clock,
    )

    private val ourAppInfo = node(
        "com.android.settings:id/root",
        null,
        FakeScreenNode(viewId = "com.android.settings:id/entity_header_title", text = "Enough"),
        node("com.android.settings:id/button2", "Force stop"),
    )
    private val otherAppInfo = node(
        "com.android.settings:id/root",
        null,
        FakeScreenNode(viewId = "com.android.settings:id/entity_header_title", text = "Maps"),
    )

    @Test
    fun `outside a lock nothing is guarded`() = runTest {
        assertNull(guard.check("com.android.settings", ourAppInfo, labels))
        assertTrue(guard.watchedPackages.first().isEmpty())
    }

    @Test
    fun `during a lock this app's App info is guarded`() = runTest {
        settings.state.value = Settings(lockedUntil = clock.now + 7.days)
        assertEquals("app_info", guard.check("com.android.settings", ourAppInfo, labels)?.id)
        assertTrue("com.android.settings" in guard.watchedPackages.first())
    }

    @Test
    fun `other apps' pages stay open during a lock`() = runTest {
        settings.state.value = Settings(lockedUntil = clock.now + 7.days)
        assertNull(guard.check("com.android.settings", otherAppInfo, labels))
    }

    @Test
    fun `the accessibility page needs every matcher`() = runTest {
        settings.state.value = Settings(lockedUntil = clock.now + 7.days)
        val list = node(null, null, node(text = "Enough: Vault gate"))
        val detail = node(null, null, node(text = "Enough: Vault gate"), node(text = "Use Enough: Vault gate"))
        assertNull(guard.check("com.android.settings", list, labels))
        assertNotNull(guard.check("com.android.settings", detail, labels))
    }

    @Test
    fun `an ended lock guards nothing`() = runTest {
        settings.state.value = Settings(lockedUntil = clock.now + 1.days)
        clock.advance(1.days)
        assertNull(guard.check("com.android.settings", ourAppInfo, labels))
    }
}
