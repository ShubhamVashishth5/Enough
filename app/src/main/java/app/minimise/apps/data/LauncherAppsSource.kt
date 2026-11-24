package app.minimise.apps.data

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import app.minimise.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import app.minimise.apps.domain.PrivateSpaceApps

/** A launchable app as the system reports it. */
data class InstalledApp(val packageName: String, val label: String)

/**
 * Lists launchable apps in every profile (personal and work) through [LauncherApps], and re-lists them
 * whenever a package is added, removed or changed.
 */
@Singleton
class LauncherAppsSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
    private val privateSpace: AndroidPrivateSpace,
) {
    private val launcherApps: LauncherApps = context.getSystemService(LauncherApps::class.java)

    val installed: SharedFlow<List<InstalledApp>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        // Locking or unlocking Private space hides or shows its apps.
        val profileChanges = launch { privateSpace.changes.collect { trySend(Unit) } }
        trySend(Unit)
        awaitClose {
            profileChanges.cancel()
            launcherApps.unregisterCallback(callback)
        }
    }
        .conflate()
        .map { queryAll() }
        .flowOn(Dispatchers.IO)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /**
     * The activity to start for [packageName], preferring the personal profile.
     * TODO(question): an app installed in both profiles shows once and opens the personal copy.
     */
    fun activityFor(packageName: String): LauncherActivityInfo? =
        activitiesByProfile().firstOrNull { it.applicationInfo.packageName == packageName }

    private fun queryAll(): List<InstalledApp> =
        activitiesByProfile()
            .filter { it.applicationInfo.packageName != context.packageName }
            .distinctBy { it.applicationInfo.packageName }
            .map { InstalledApp(packageName = it.applicationInfo.packageName, label = it.label.toString()) }

    /** Every profile's apps, personal first. A locked Private space shows nothing. */
    private fun activitiesByProfile(): List<LauncherActivityInfo> {
        val me = Process.myUserHandle()
        val profiles = launcherApps.profiles
            .filter { PrivateSpaceApps.showsProfile(isPrivate = privateSpace.isPrivate(it), locked = privateSpace.isQuiet(it)) }
            .sortedBy { if (it == me) 0 else 1 }
        return profiles.flatMap { user -> launcherApps.getActivityList(null, user) }
    }
}
