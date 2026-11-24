package app.minimise.apps.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.minimise.R
import app.minimise.apps.domain.AppCategory
import app.minimise.apps.domain.AutoVault
import app.minimise.apps.domain.AutoVaultNotice
import app.minimise.core.di.ApplicationScope
import app.minimise.launcher.HomeActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** Listens for new installs for as long as the app runs, and hands them to [AutoVault]. */
@Singleton
class AutoVaultWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val autoVault: Provider<AutoVault>,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        context.getSystemService(LauncherApps::class.java).registerCallback(
            object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) = onAdded(packageName)

                override fun onPackageRemoved(packageName: String, user: UserHandle) = Unit

                override fun onPackageChanged(packageName: String, user: UserHandle) = Unit

                override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = Unit

                override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = Unit
            },
            Handler(Looper.getMainLooper()),
        )
    }

    private fun onAdded(packageName: String) {
        val info = runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.getOrNull() ?: return
        val label = info.loadLabel(context.packageManager).toString()
        scope.launch {
            runCatching { autoVault.get().onInstalled(packageName, label, categoryOf(info)) }
                .onFailure { Timber.e(it, "Auto-Vault failed for %s", packageName) }
        }
    }

    @Suppress("DEPRECATION")
    private fun categoryOf(info: ApplicationInfo): AppCategory = when {
        info.category == ApplicationInfo.CATEGORY_GAME || info.flags and ApplicationInfo.FLAG_IS_GAME != 0 -> AppCategory.GAME
        info.category == ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        info.category == ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
        info.category == ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        else -> AppCategory.OTHER
    }
}

/** "Instagram went into the Vault", as a quiet notification. */
class NotificationAutoVaultNotice @Inject constructor(
    @ApplicationContext private val context: Context,
) : AutoVaultNotice {
    override fun show(appLabel: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.notice_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.auto_vault_notice, appLabel))
            .setContentText(context.getString(R.string.auto_vault_notice_body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(appLabel.hashCode(), notification) }.onFailure { Timber.w(it, "Could not post the notice") }
    }

    private companion object {
        const val CHANNEL = "notices"
    }
}
