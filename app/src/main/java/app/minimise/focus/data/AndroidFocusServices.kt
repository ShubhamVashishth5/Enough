package app.minimise.focus.data

import android.app.NotificationManager
import android.content.Context
import android.telecom.TelecomManager
import app.minimise.focus.domain.DoNotDisturb
import app.minimise.focus.domain.EssentialApps
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class SystemDoNotDisturb @Inject constructor(
    @ApplicationContext context: Context,
) : DoNotDisturb {
    private val notifications = context.getSystemService(NotificationManager::class.java)

    override fun isAvailable(): Boolean = notifications.isNotificationPolicyAccessGranted

    override fun enable(): Int? = runCatching {
        val previous = notifications.currentInterruptionFilter
        if (previous == NotificationManager.INTERRUPTION_FILTER_ALL) {
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            previous
        } else {
            null // Already quieter than this; leave it.
        }
    }.onFailure { Timber.w(it, "Could not turn on Do Not Disturb") }.getOrNull()

    override fun restore(previous: Int) {
        runCatching { if (isAvailable()) notifications.setInterruptionFilter(previous) }
            .onFailure { Timber.w(it, "Could not restore Do Not Disturb") }
    }
}

/** The phone app and the system's call screens, so a call always gets through. */
class AndroidEssentialApps @Inject constructor(
    @ApplicationContext private val context: Context,
) : EssentialApps {
    override fun packages(): Set<String> {
        val dialer = runCatching { context.getSystemService(TelecomManager::class.java).defaultDialerPackage }.getOrNull()
        return setOfNotNull(dialer, "com.android.server.telecom", "com.android.phone")
    }
}
