package app.minimise.monitor

import android.accessibilityservice.AccessibilityService
import android.content.Context
import app.minimise.R
import app.minimise.apps.domain.WebTarget
import app.minimise.browsers.domain.AddressBarRead
import app.minimise.browsers.domain.BrowserCoverage
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.feeds.domain.FeedAction
import app.minimise.feeds.domain.FeedBlocker
import app.minimise.feeds.domain.FeedRule
import app.minimise.guard.domain.LockGuard
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.ui.dateTimeText
import app.minimise.intervention.ui.NoticeActivity
import app.minimise.monitor.data.AccessibilityForegroundSource
import app.minimise.monitor.data.AccessibilityScreenNode
import app.minimise.monitor.domain.ForegroundEvent
import app.minimise.monitor.domain.ScreenNode
import app.minimise.monitor.domain.Throttle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads the screen of the few apps that need it (those with a blocked feed, browsers while a website
 * is in the Vault, Settings during a lock period) and acts on what it finds. A thin adapter: what counts as a match is decided in
 * domain classes.
 *
 * Content changes arrive many times a second; checks run at most once per
 * [Defaults.SCREEN_CHECK_INTERVAL], and a burst of changes ends with one check after the last.
 */
class ScreenWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val feedBlocker: FeedBlocker,
    private val browserCoverage: BrowserCoverage,
    private val lockGuard: LockGuard,
    private val settingsStore: SettingsStore,
    private val foregroundSource: AccessibilityForegroundSource,
    private val timeSource: TimeSource,
) {
    private val throttle = Throttle(Defaults.SCREEN_CHECK_INTERVAL)
    private var pending: Job? = null

    /** The browser the website in front was read from. */
    private var webTargetBrowser: String? = null

    /** Packages whose screen is read. Empty means content events can be switched off altogether. */
    @Volatile
    var watched: Set<String> = emptySet()

    val watchedPackages: Flow<Set<String>>
        get() = combine(feedBlocker.watchedPackages, browserCoverage.watchedPackages, lockGuard.watchedPackages) { a, b, c -> a + b + c }

    private val labels: Map<String, String> by lazy {
        mapOf(
            "app_label" to context.getString(R.string.app_name),
            "service_label" to context.getString(R.string.accessibility_service_label),
        )
    }

    /**
     * True while a website read from [packageName] is in front. A window change inside that browser
     * then mustn't report the browser itself; the next address-bar read decides.
     */
    fun showsWebsiteFrom(packageName: String): Boolean =
        webTargetBrowser == packageName && foregroundSource.current.value?.packageName?.let(WebTarget::isWebTarget) == true

    /** Called on the main thread for every window or content change. */
    fun onScreenChanged(service: AccessibilityService, scope: CoroutineScope, packageName: String) {
        if (packageName !in watched || pending?.isActive == true) return
        pending = scope.launch {
            delay(throttle.waitBeforeNext(timeSource.elapsed()))
            throttle.markRun(timeSource.elapsed())
            check(service, packageName)
        }
    }

    private suspend fun check(service: AccessibilityService, packageName: String) {
        val root = service.rootInActiveWindow ?: return
        if (root.packageName?.toString() != packageName) return
        val node: ScreenNode = AccessibilityScreenNode(root)
        if (withContext(Dispatchers.Default) { lockGuard.check(packageName, node, labels) } != null) {
            keepOut(service)
            return
        }
        if (browserCoverage.isBrowser(packageName)) {
            val read = withContext(Dispatchers.Default) { browserCoverage.read(packageName, node) }
            if (read is AddressBarRead.Page) {
                webTargetBrowser = packageName.takeIf { read.targetId != null }
                foregroundSource.emit(ForegroundEvent(read.targetId ?: packageName, timeSource.now()))
            }
            return
        }
        // Walking the tree is one call to the other app per node: keep it off the main thread.
        val feed = withContext(Dispatchers.Default) { feedBlocker.check(packageName, node) }
        feed?.let { act(service, it) }
    }

    /** A page that could switch the app off, during a lock: home, and say why. */
    private suspend fun keepOut(service: AccessibilityService) {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        val until = settingsStore.current().lockedUntil ?: return
        context.startActivity(
            NoticeActivity.intent(
                context,
                title = context.getString(R.string.guard_title),
                body = context.getString(R.string.lock_refused, dateTimeText(context, until.atZone(timeSource.zone()).toLocalDateTime())),
                action = context.getString(R.string.time_up_go_home),
                goHome = true,
            ),
        )
    }

    private fun act(service: AccessibilityService, feed: FeedRule) {
        when (feed.action) {
            FeedAction.BACK -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            FeedAction.HOME -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            FeedAction.SCREEN -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                context.startActivity(
                    NoticeActivity.intent(
                        context,
                        title = context.getString(R.string.feed_blocked_title),
                        body = context.getString(R.string.feed_blocked_body, feed.name),
                        action = context.getString(R.string.feed_blocked_ok),
                        goHome = false,
                    ),
                )
            }
        }
    }
}
