package app.minimise.feeds.domain

import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.monitor.domain.ScreenNode
import app.minimise.monitor.domain.ScreenTree
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object FeedMatcher {
    /** The first switched-on rule for [packageName] whose feed is on screen. */
    fun match(
        root: ScreenNode,
        packageName: String,
        rules: List<FeedRule>,
        enabled: Set<String>,
        maxDepth: Int = Defaults.SCREEN_MAX_DEPTH,
        maxNodes: Int = Defaults.SCREEN_MAX_NODES,
    ): FeedRule? = rules
        .filter { it.packageName == packageName && it.id in enabled }
        .firstOrNull { ScreenTree.firstMatch(root, it.matchers, maxDepth, maxNodes) != null }

    /** Packages that need their screen read: those with a switched-on rule. */
    fun watchedPackages(rules: List<FeedRule>, enabled: Set<String>): Set<String> =
        rules.filter { it.id in enabled }.map { it.packageName }.toSet()
}

/**
 * Decides, from a screen the accessibility service read, whether a blocked feed is showing. After
 * acting on one it waits [Defaults.FEED_ACTION_COOLDOWN], so a Back that takes a moment to land
 * isn't sent twice (which would leave the app altogether).
 */
@Singleton
class FeedBlocker @Inject constructor(
    private val source: FeedRuleSource,
    private val settingsStore: SettingsStore,
    private val eventLog: EventLog,
    private val timeSource: TimeSource,
) {
    private val rules: List<FeedRule> by lazy { source.rules() }
    private var lastActionAt: Duration? = null

    val watchedPackages: Flow<Set<String>> = settingsStore.settings
        .map { FeedMatcher.watchedPackages(rules, it.blockedFeeds) }
        .distinctUntilChanged()

    fun allRules(): List<FeedRule> = rules

    /** The rule to act on now, or null. Logs a FEED_BLOCKED event when there is one. */
    suspend fun check(packageName: String, root: ScreenNode): FeedRule? {
        val now = timeSource.elapsed()
        lastActionAt?.let { if (now - it < Defaults.FEED_ACTION_COOLDOWN) return null }
        val rule = FeedMatcher.match(root, packageName, rules, settingsStore.current().blockedFeeds) ?: return null
        lastActionAt = now
        eventLog.log(
            Event(
                type = EventType.FEED_BLOCKED,
                startAt = timeSource.now(),
                packageName = packageName,
                data = buildJsonObject { put("feed", rule.id) }.toString(),
            ),
        )
        return rule
    }
}
