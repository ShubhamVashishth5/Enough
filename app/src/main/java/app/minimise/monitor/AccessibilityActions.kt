package app.minimise.monitor

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import app.minimise.BuildConfig
import app.minimise.monitor.data.ScreenDump
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Lets the launcher ask the running accessibility service for a system action (locking the screen, the shade). */
@Singleton
class AccessibilityActions @Inject constructor() {
    @Volatile
    internal var service: AccessibilityService? = null

    /** False when the service isn't running. */
    fun lockScreen(): Boolean =
        runCatching { service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true }
            .onFailure { Timber.w(it, "Could not lock the screen") }
            .getOrDefault(false)

    /** Pulls down the notification shade. False when the service isn't running. */
    fun openNotifications(): Boolean =
        runCatching { service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true }
            .onFailure { Timber.w(it, "Could not open the notification shade") }
            .getOrDefault(false)

    /** Debug builds: in 5 seconds, log whatever window is in front. False when the service isn't running. */
    fun dumpScreenSoon(): Boolean {
        if (!BuildConfig.DEBUG || service == null) return false
        Handler(Looper.getMainLooper()).postDelayed(
            { runCatching { service?.rootInActiveWindow?.let(ScreenDump::log) }.onFailure { Timber.w(it, "Dump failed") } },
            DUMP_DELAY_MILLIS,
        )
        return true
    }

    private companion object {
        const val DUMP_DELAY_MILLIS = 5_000L
    }
}
