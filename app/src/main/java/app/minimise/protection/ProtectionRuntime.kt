package app.minimise.protection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import app.minimise.calendar.domain.CalendarFocus
import app.minimise.core.di.ApplicationScope
import app.minimise.core.time.TimeSource
import app.minimise.display.GrayscaleController
import app.minimise.focus.FocusController
import app.minimise.focus.domain.FocusSessions
import app.minimise.gate.GateMonitor
import app.minimise.gate.GrantWatcher
import app.minimise.launcher.HomeActivity
import app.minimise.monitor.data.ForegroundEventBus
import app.minimise.monitor.data.ForegroundPackageFilter
import app.minimise.monitor.data.UsageStatsForegroundSource
import app.minimise.monitor.domain.ForegroundEvent
import app.minimise.protection.domain.ProtectionLevel
import app.minimise.settings.domain.SettingsGuard
import app.minimise.tasks.domain.TodayPrompt
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import app.minimise.usage.domain.Taper
import app.minimise.usage.domain.UsageTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The work that runs while something watches the app in front, whichever watcher it is: the
 * accessibility service at [ProtectionLevel.FULL] or the usage access poller at [ProtectionLevel.LITE].
 * The gate, usage spans, time boxes, grayscale, focus and screen unlocks all run here once.
 *
 * Only one session runs at a time, so nothing is gated or counted twice. The accessibility service
 * wins: it takes over from a running Lite session, and Lite can't start while it runs. A takeover
 * keeps the open usage span; only stopping for good closes it.
 */
@Singleton
class ProtectionRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foreground: ForegroundEventBus,
    private val filter: ForegroundPackageFilter,
    private val usageStatsSource: UsageStatsForegroundSource,
    private val usageTracker: UsageTracker,
    private val gateMonitor: GateMonitor,
    private val grantWatcher: GrantWatcher,
    private val grayscaleController: GrayscaleController,
    private val focusController: FocusController,
    private val focusSessions: FocusSessions,
    private val eventLog: EventLog,
    private val settingsGuard: SettingsGuard,
    private val taper: Taper,
    private val calendarFocus: CalendarFocus,
    private val todayPrompt: TodayPrompt,
    private val timeSource: TimeSource,
    private val protectionStatus: Provider<ProtectionStatus>,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private class Session(val level: ProtectionLevel, val scope: CoroutineScope)

    @Volatile
    private var session: Session? = null
    private val screenOn = MutableStateFlow(true)
    private var receiverRegistered = false

    /** The watcher running now, or null. */
    val running: ProtectionLevel? get() = session?.level

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatching {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOn.value = false
                        foreground.onScreenOff(ForegroundEvent(null, timeSource.now()))
                    }
                    Intent.ACTION_SCREEN_ON -> screenOn.value = true
                    Intent.ACTION_USER_PRESENT -> onUnlock()
                }
            }.onFailure { Timber.e(it, "Screen broadcast failed") }
        }
    }

    /** Starts the shared work for [level]. False when the accessibility service is running and [level] is Lite. */
    @MainThread
    fun start(level: ProtectionLevel): Boolean {
        require(level != ProtectionLevel.LAUNCHER_ONLY)
        val current = session
        if (current?.level == level) return true
        if (current?.level == ProtectionLevel.FULL) return false
        current?.scope?.cancel()
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, e -> Timber.e(e, "Protection job failed") },
        )
        session = Session(level, scope)
        registerScreenReceiver()
        scope.launch { foreground.events.collect { usageTracker.onForeground(it) } }
        scope.launch { gateMonitor.run() }
        scope.launch { grantWatcher.run() }
        scope.launch { grayscaleController.run() }
        scope.launch { focusController.run() }
        scope.launch {
            while (isActive) {
                delay(CHECKPOINT_INTERVAL)
                usageTracker.checkpoint()
            }
        }
        if (level == ProtectionLevel.LITE) scope.launch { usageStatsSource.run(screenOn.asStateFlow()) }
        Timber.d("Protection running at %s", level)
        return true
    }

    /** Stops the shared work if [level] is what's running. Nothing watches the app in front afterwards. */
    @MainThread
    fun stop(level: ProtectionLevel) {
        val current = session ?: return
        if (current.level != level) return
        current.scope.cancel()
        session = null
        unregisterScreenReceiver()
        foreground.reset(ForegroundEvent(null, timeSource.now()))
        appScope.launch { usageTracker.closeOpenSpan() }
        appScope.launch { grayscaleController.onServiceStopped() }
        Timber.d("Protection stopped at %s", level)
    }

    private fun onUnlock() {
        val now = timeSource.now()
        filter.refresh()
        // Permissions may have changed while the screen was off.
        protectionStatus.get().refresh()
        val scope = session?.scope ?: return
        scope.launch {
            eventLog.log(Event(type = EventType.SCREEN_UNLOCK, startAt = now))
            settingsGuard.applyDue()
            taper.applyIfDue()
            focusSessions.finishIfDue()
            calendarFocus.startIfDue()
            if (todayPrompt.claim()) context.startActivity(HomeActivity.todayIntent(context))
        }
        // The app that was in front before the screen went off is in front again, and no window
        // event may say so.
        foreground.emit(ForegroundEvent(foreground.lastAppBeforeScreenOff, now))
    }

    private fun registerScreenReceiver() {
        if (receiverRegistered) return
        screenOn.value = context.getSystemService(PowerManager::class.java)?.isInteractive != false
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(context, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(screenReceiver) }
        receiverRegistered = false
    }

    private companion object {
        val CHECKPOINT_INTERVAL = 30.seconds
    }
}
