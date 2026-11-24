package app.minimise.monitor

import android.accessibilityservice.AccessibilityService
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Lets the launcher ask the running accessibility service for a system action (locking the screen). */
@Singleton
class AccessibilityActions @Inject constructor() {
    @Volatile
    internal var service: AccessibilityService? = null

    /** False when the service isn't running. */
    fun lockScreen(): Boolean =
        runCatching { service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true }
            .onFailure { Timber.w(it, "Could not lock the screen") }
            .getOrDefault(false)
}
