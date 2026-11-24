package app.minimise.monitor

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import app.minimise.core.di.ApplicationScope
import app.minimise.core.time.TimeSource
import app.minimise.display.GrayscaleController
import app.minimise.focus.FocusController
import app.minimise.focus.domain.FocusSessions
import app.minimise.gate.GateMonitor
import app.minimise.gate.GrantWatcher
import app.minimise.launcher.HomeActivity
import app.minimise.monitor.data.AccessibilityForegroundSource
import app.minimise.monitor.data.ForegroundPackageFilter
import app.minimise.monitor.domain.ForegroundEvent
import app.minimise.settings.domain.SettingsGuard
import app.minimise.tasks.domain.TodayPrompt
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import app.minimise.usage.domain.UsageTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The only producer of foreground events. A thin adapter: it classifies window changes and screen
 * on/off, then hands everything to domain classes. It must never crash, so every handler is wrapped.
 */
@AndroidEntryPoint
class GateAccessibilityService : AccessibilityService() {
    @Inject lateinit var foregroundSource: AccessibilityForegroundSource
    @Inject lateinit var gateMonitor: GateMonitor
    @Inject lateinit var grantWatcher: GrantWatcher
    @Inject lateinit var grayscaleController: GrayscaleController
    @Inject lateinit var focusController: FocusController
    @Inject lateinit var focusSessions: FocusSessions
    @Inject lateinit var accessibilityActions: AccessibilityActions
    @Inject lateinit var screenWatcher: ScreenWatcher
    @Inject lateinit var usageTracker: UsageTracker
    @Inject lateinit var eventLog: EventLog
    @Inject lateinit var settingsGuard: SettingsGuard
    @Inject lateinit var timeSource: TimeSource
    @Inject lateinit var todayPrompt: TodayPrompt
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, e -> Timber.e(e, "Service job failed") },
    )
    private var filter: ForegroundPackageFilter? = null
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            safely("screen broadcast") {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> foregroundSource.onScreenOff(ForegroundEvent(null, timeSource.now()))
                    Intent.ACTION_USER_PRESENT -> onUnlock()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        safely("connect") {
            filter = ForegroundPackageFilter(this)
            accessibilityActions.service = this
            val screenEvents = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(this, screenReceiver, screenEvents, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
            scope.launch { foregroundSource.events.collect { usageTracker.onForeground(it) } }
            scope.launch { gateMonitor.run() }
            scope.launch { grantWatcher.run() }
            scope.launch { grayscaleController.run() }
            scope.launch { focusController.run() }
            scope.launch {
                screenWatcher.watchedPackages.collect { packages ->
                    screenWatcher.watched = packages
                    readContentChanges(packages.isNotEmpty())
                }
            }
            scope.launch {
                while (isActive) {
                    delay(CHECKPOINT_INTERVAL)
                    usageTracker.checkpoint()
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
        val verdict = filter?.classify(packageName, className) ?: return
        val now = timeSource.now()
        when (verdict) {
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
            if (receiverRegistered) unregisterReceiver(screenReceiver)
            receiverRegistered = false
            appScope.launch { usageTracker.closeOpenSpan() }
            appScope.launch { grayscaleController.onServiceStopped() }
            scope.cancel()
        }
        super.onDestroy()
    }

    private fun onUnlock() {
        val now = timeSource.now()
        filter?.refresh()
        scope.launch {
            eventLog.log(Event(type = EventType.SCREEN_UNLOCK, startAt = now))
            settingsGuard.applyDue()
            focusSessions.finishIfDue()
            if (todayPrompt.claim()) startActivity(HomeActivity.todayIntent(this@GateAccessibilityService))
        }
        // The app that was in front before the screen went off is in front again, and no window
        // event may say so.
        foregroundSource.emit(ForegroundEvent(foregroundSource.lastAppBeforeScreenOff, now))
    }

    private inline fun safely(what: String, block: () -> Unit) {
        runCatching(block).onFailure { Timber.e(it, "GateAccessibilityService: %s failed", what) }
    }

    private companion object {
        val CHECKPOINT_INTERVAL = 30.seconds
        const val WINDOW_EVENTS = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }
}
