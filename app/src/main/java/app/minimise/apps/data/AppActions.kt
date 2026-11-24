package app.minimise.apps.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import app.minimise.apps.domain.WebTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

/** What a long-press on an app offers besides opening it: its system App info page, and uninstalling. */
class AppActions @Inject constructor(
    @ApplicationContext private val context: Context,
    private val source: LauncherAppsSource,
) {
    private val launcherApps: LauncherApps = context.getSystemService(LauncherApps::class.java)

    /** Websites in the Vault are only rules, not apps, so there's nothing to uninstall. */
    fun canManage(packageName: String): Boolean = !WebTarget.isWebTarget(packageName)

    fun openAppInfo(packageName: String): Boolean {
        val activity = source.activityFor(packageName) ?: return false
        return runCatching { launcherApps.startAppDetailsActivity(activity.componentName, activity.user, null, null) }
            .onFailure { Timber.w(it, "No App info for %s", packageName) }
            .isSuccess
    }

    /**
     * Opens the system's uninstall confirmation. The system asks; nothing is removed without a yes.
     * An app in the work profile or Private space is uninstalled from that profile.
     */
    fun uninstall(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        source.activityFor(packageName)?.user?.let { intent.putExtra(Intent.EXTRA_USER, it) }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "No uninstaller for %s", packageName)
            false
        }
    }
}
