package app.minimise.apps.data

import android.content.Context
import android.content.pm.LauncherApps
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

/** Starts an app's main activity, in whichever profile it lives. */
class AppLauncher @Inject constructor(
    @ApplicationContext context: Context,
    private val source: LauncherAppsSource,
) {
    private val launcherApps: LauncherApps = context.getSystemService(LauncherApps::class.java)

    /** Returns false when the app can't be started (uninstalled, disabled, profile paused). */
    fun launch(packageName: String): Boolean {
        val activity = source.activityFor(packageName) ?: return false
        return runCatching {
            launcherApps.startMainActivity(activity.componentName, activity.user, null, null)
        }.onFailure { Timber.w(it, "Could not launch %s", packageName) }.isSuccess
    }
}
