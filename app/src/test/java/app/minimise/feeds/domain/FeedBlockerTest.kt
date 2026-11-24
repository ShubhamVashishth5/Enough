package app.minimise.feeds.domain

import app.minimise.core.prefs.Settings
import app.minimise.core.time.FakeTimeSource
import app.minimise.monitor.domain.NodeMatcher
import app.minimise.testing.FakeEventLog
import app.minimise.testing.FakeSettingsStore
import app.minimise.testing.node
import app.minimise.usage.domain.EventType
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedBlockerTest {
    private val shorts = FeedRule(
        id = "shorts",
        packageName = "com.youtube",
        name = "Shorts",
        matchers = listOf(NodeMatcher(viewId = "com.youtube:id/reel_recycler")),
    )
    private val shortsScreen = node("com.youtube:id/root", null, node("com.youtube:id/reel_recycler"))
    private val searchScreen = node("com.youtube:id/root", null, node("com.youtube:id/search_box", "cats"))

    private val clock = FakeTimeSource()
    private val settings = FakeSettingsStore(Settings(blockedFeeds = setOf("shorts")))
    private val events = FakeEventLog()
    private val blocker = FeedBlocker(
        source = object : FeedRuleSource {
            override fun rules() = listOf(shorts)
        },
        settingsStore = settings,
        eventLog = events,
        timeSource = clock,
    )

    @Test
    fun `a blocked feed on screen is caught and logged`() = runTest {
        assertEquals(shorts, blocker.check("com.youtube", shortsScreen))
        assertEquals(EventType.FEED_BLOCKED, events.events.value.single().type)
    }

    @Test
    fun `the rest of the app is left alone`() = runTest {
        assertNull(blocker.check("com.youtube", searchScreen))
    }

    @Test
    fun `a switched-off feed is left alone`() = runTest {
        settings.state.value = Settings(blockedFeeds = emptySet())
        assertNull(blocker.check("com.youtube", shortsScreen))
    }

    @Test
    fun `another app with the same view id is left alone`() = runTest {
        assertNull(blocker.check("com.other", shortsScreen))
    }

    @Test
    fun `waits for the cooldown before acting again`() = runTest {
        blocker.check("com.youtube", shortsScreen)
        clock.advance(500.milliseconds)
        assertNull(blocker.check("com.youtube", shortsScreen))
        clock.advance(1500.milliseconds)
        assertEquals(shorts, blocker.check("com.youtube", shortsScreen))
    }

    @Test
    fun `only packages with a switched-on rule are watched`() = runTest {
        assertEquals(setOf("com.youtube"), blocker.watchedPackages.first())
        settings.state.value = Settings()
        assertTrue(blocker.watchedPackages.first().isEmpty())
    }

    @Test
    fun `the shipped rule file parses`() {
        val rules = FeedRules.parse(File("src/main/assets/feed_rules.json").readText())
        assertTrue(rules.isNotEmpty())
        assertEquals(rules.size, rules.map { it.id }.toSet().size)
    }

    @Test
    fun `a rule without matchers is dropped`() {
        val text = """{"feeds":[{"id":"x","package":"p","name":"X","matchers":[]}]}"""
        assertTrue(FeedRules.parse(text).isEmpty())
    }
}
