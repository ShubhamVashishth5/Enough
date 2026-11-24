package app.minimise.guard

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.minimise.R

/**
 * The uninstall guard. While the app is an active device admin, Android won't uninstall it until admin
 * is switched off, and during a lock the app keeps the user out of the page that does that. It asks
 * for no device policies at all.
 */
class GuardAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.admin_disable_warning)

    companion object {
        fun component(context: Context) = ComponentName(context, GuardAdminReceiver::class.java)
    }
}
