package app.minimise.protection

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber

/**
 * The system app lock (Android 17 QPR2, API 37.2): an app behind it asks for the fingerprint, face or
 * screen-lock PIN before it opens, and its notifications hide their content. The home app holds
 * LOCK_APPS through its role, so Enough can ask for it; the system confirms each change itself.
 */
interface AppLocker {
    /** The system app lock exists here and this app may set it. */
    fun isAvailable(): Boolean

    /** Whether each app is locked, for those of [packageNames] that support the lock. */
    fun lockStates(packageNames: Collection<String>): Map<String, Boolean>

    /** The system screen that locks or unlocks [packageName] once the user confirms; null when there's nothing to ask. */
    fun request(packageName: String, locked: Boolean): IntentSender?
}

/** Below Android 17 QPR2 there is no system app lock. */
class NoOpAppLocker : AppLocker {
    override fun isAvailable(): Boolean = false

    override fun lockStates(packageNames: Collection<String>): Map<String, Boolean> = emptyMap()

    override fun request(packageName: String, locked: Boolean): IntentSender? = null
}

/** Only created on Android 16 and later; every call checks for 37.2 itself, where the app lock arrived. */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class PlatformAppLocker(private val context: Context) : AppLocker {
    // TODO(question): LOCK_APPS is granted to the holder of the home role. Confirm on a QPR2 phone that
    // the grant arrives when Enough becomes the default home app, without a restart.
    override fun isAvailable(): Boolean =
        Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_2 &&
            context.checkSelfPermission(Manifest.permission.LOCK_APPS) == PackageManager.PERMISSION_GRANTED

    override fun lockStates(packageNames: Collection<String>): Map<String, Boolean> {
        if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.CINNAMON_BUN_2 || !isAvailable()) return emptyMap()
        val flags = PackageManager.ApplicationInfoFlags.of(PackageManager.GET_APP_LOCK_INFO)
        return packageNames.mapNotNull { packageName ->
            runCatching { context.packageManager.getApplicationInfo(packageName, flags) }
                .getOrNull()
                ?.takeIf { it.isAppLockSupported }
                ?.let { packageName to it.isAppLockEnabled }
        }.toMap()
    }

    override fun request(packageName: String, locked: Boolean): IntentSender? {
        if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.CINNAMON_BUN_2 || !isAvailable()) return null
        return runCatching { context.packageManager.getEnableAppLockIntentForPackage(packageName, locked)?.intentSender }
            .onFailure { Timber.w(it, "No app lock request for %s", packageName) }
            .getOrNull()
    }
}
