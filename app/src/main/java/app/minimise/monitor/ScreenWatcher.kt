package app.minimise.monitor

import android.accessibilityservice.AccessibilityService
import android.content.Context
import app.minimise.R
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.feeds.domain.FeedAction
import app.minimise.feeds.domain.FeedBlocker
import app.minimise.feeds.domain.FeedRule
import app.minimise.intervention.ui.NoticeActivity
import app.minimise.monitor.data.AccessibilityScreenNode
import app.minimise.monitor.domain.ScreenNode
import app.minimise.monitor.domain.Throttle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads the screen of the few apps that need it (those with a blocked feed) and acts on what it
 * finds. A thin adapter: what counts as a match is decided in domain classes.
 *
 * Content changes arrive many times a second; checks run at most once per
 * [Defaults.SCREEN_CHECK_INTERVAL], and a burst of changes ends with one check after the last.
 */
class ScreenWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val feedBlocker: FeedBlocker,
    private val timeSource: TimeSource,
) {
    private val throttle = Throttle(Defaults.SCREEN_CHECK_INTERVAL)
    private var pending: Job? = null

    /** Packages whose screen is read. Empty means content events can be switched off altogether. */
    @Volatile
    var watched: Set<String> = emptySet()

    val watchedPackages: Flow<Set<String>> get() = feedBlocker.watchedPackages

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
        // Walking the tree is one call to the other app per node: keep it off the main thread.
        val feed = withContext(Dispatchers.Default) { feedBlocker.check(packageName, node) }
        feed?.let { act(service, it) }
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
