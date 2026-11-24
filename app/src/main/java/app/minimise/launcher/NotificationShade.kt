package app.minimise.launcher

import android.annotation.SuppressLint
import android.content.Context
import app.minimise.monitor.AccessibilityActions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

/** Swipe down on home: the notification shade, as on any other home screen. */
class NotificationShade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibilityActions: AccessibilityActions,
) {
    /** False when neither way worked. */
    fun open(): Boolean = accessibilityActions.openNotifications() || expandWithStatusBarManager()

    /**
     * Without the accessibility service, the way home screens have always done it: StatusBarManager's
     * expandNotificationsPanel, with the EXPAND_STATUS_BAR permission. It isn't in the public SDK, so it's
     * reached by reflection and may stop working on a future Android.
     */
    @SuppressLint("WrongConstant")
    private fun expandWithStatusBarManager(): Boolean = runCatching {
        val statusBar = context.getSystemService(STATUS_BAR_SERVICE) ?: return false
        Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(statusBar)
        true
    }.onFailure { Timber.w(it, "Could not expand the notification shade") }.getOrDefault(false)

    private companion object {
        const val STATUS_BAR_SERVICE = "statusbar"
    }
}
