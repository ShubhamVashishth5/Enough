package app.minimise.apps.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import app.minimise.apps.domain.PrivateSpace
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/** Private space through LauncherApps and UserManager. Needs the home role and ACCESS_HIDDEN_PROFILES. */
@Singleton
class AndroidPrivateSpace @Inject constructor(
    @ApplicationContext private val context: Context,
) : PrivateSpace {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    override fun exists(): Boolean = supported() && profile() != null

    override fun isLocked(): Boolean = supported() && profile()?.let { isQuiet(it) } == true

    override fun setLocked(locked: Boolean): Boolean {
        if (!supported()) return false
        val profile = profile() ?: return false
        return runCatching { userManager.requestQuietModeEnabled(locked, profile) }
            .onFailure { Timber.w(it, "Could not change Private space") }
            .getOrDefault(false)
    }

    /** True for the private profile on Android 15+. */
    fun isPrivate(user: UserHandle): Boolean = supported() && isPrivateProfile(user)

    fun isQuiet(user: UserHandle): Boolean = runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false)

    override val changes: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            PROFILE_ACTIONS.forEach(::addAction)
            if (supported()) {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun profile(): UserHandle? = runCatching { launcherApps.profiles.firstOrNull(::isPrivateProfile) }.getOrNull()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun isPrivateProfile(user: UserHandle): Boolean =
        runCatching { launcherApps.getLauncherUserInfo(user)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE }.getOrDefault(false)

    private fun supported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    private companion object {
        val PROFILE_ACTIONS = listOf(
            Intent.ACTION_PROFILE_ACCESSIBLE,
            Intent.ACTION_PROFILE_INACCESSIBLE,
            Intent.ACTION_MANAGED_PROFILE_AVAILABLE,
            Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE,
        )
    }
}
