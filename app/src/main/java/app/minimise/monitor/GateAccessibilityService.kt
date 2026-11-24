package app.minimise.monitor

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import app.minimise.core.time.TimeSource
import app.minimise.monitor.data.ForegroundEventBus
import app.minimise.monitor.data.ForegroundPackageFilter
import app.minimise.monitor.domain.ForegroundEvent
import app.minimise.protection.ProtectionRuntime
import app.minimise.protection.ProtectionStatus
import app.minimise.protection.domain.ProtectionLevel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The Full protection watcher. A thin adapter: it classifies window changes and reads the screens a
 * rule needs; the gate, usage spans and the rest run in [ProtectionRuntime], shared with Lite
 * protection. It must never crash, so every handler is wrapped.
 */
@AndroidEntryPoint
class GateAccessibilityService : AccessibilityService() {
    @Inject lateinit var foregroundSource: ForegroundEventBus
    @Inject lateinit var filter: ForegroundPackageFilter
    @Inject lateinit var runtime: ProtectionRuntime
    @Inject lateinit var protectionStatus: ProtectionStatus
    @Inject lateinit var accessibilityActions: AccessibilityActions
    @Inject lateinit var screenWatcher: ScreenWatcher
    @Inject lateinit var timeSource: TimeSource

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, e -> Timber.e(e, "Service job failed") },
    )
    private var connected = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        safely("connect") {
            accessibilityActions.service = this
            filter.refresh()
            // Takes over from Lite protection if it was running, keeping the open usage span.
            runtime.start(ProtectionLevel.FULL)
            connected = true
            protectionStatus.refresh()
            scope.launch {
                screenWatcher.watchedPackages.collect { packages ->
                    screenWatcher.watched = packages
                    readContentChanges(packages.isNotEmpty())
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        safely("accessibility event") {
            val packageName = event.packageName?.toString() ?: return
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    onWindowChanged(packageName, event.className?.toString())
                    screenWatcher.onScreenChanged(this, scope, packageName)
                }
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> screenWatcher.onScreenChanged(this, scope, packageName)
            }
        }
    }

    private fun onWindowChanged(packageName: String, className: String?) {
        if (!connected) return
        val now = timeSource.now()
        when (val verdict = filter.classify(packageName, className)) {
            ForegroundPackageFilter.Verdict.Ignore -> Unit
            ForegroundPackageFilter.Verdict.NoApp -> foregroundSource.emit(ForegroundEvent(null, now))
            is ForegroundPackageFilter.Verdict.App ->
                if (!screenWatcher.showsWebsiteFrom(verdict.packageName)) foregroundSource.emit(ForegroundEvent(verdict.packageName, now))
        }
    }

    /** Content changes come many times a second from every app, so they're only asked for while a screen is watched. */
    private fun readContentChanges(on: Boolean) {
        val info = serviceInfo ?: return
        val wanted = if (on) WINDOW_EVENTS or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED else WINDOW_EVENTS
        if (info.eventTypes == wanted) return
        info.eventTypes = wanted
        serviceInfo = info
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        safely("destroy") {
            accessibilityActions.service = null
            connected = false
            scope.cancel()
            runtime.stop(ProtectionLevel.FULL)
            // Lite protection takes over at once if its permissions are there.
            protectionStatus.refresh(accessibilityStopped = true)
        }
        super.onDestroy()
    }

    private inline fun safely(what: String, block: () -> Unit) {
        runCatching(block).onFailure { Timber.e(it, "GateAccessibilityService: %s failed", what) }
    }

    private companion object {
        const val WINDOW_EVENTS = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }
}
