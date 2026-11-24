package app.minimise.protection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.minimise.R
import app.minimise.launcher.HomeActivity
import app.minimise.protection.domain.ProtectionLevel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * Lite protection's home: a foreground service with one quiet ongoing notification, so the usage
 * access poller keeps running while the accessibility service is off. A thin adapter; the work is
 * in [ProtectionRuntime]. It stops itself whenever the level isn't Lite any more.
 */
// TODO(question): foreground service type and its Play declaration. specialUse is the closest fit
// (no other type covers watching which app is in front), and Play asks for a written justification.
@AndroidEntryPoint
class ProtectionService : Service() {
    @Inject lateinit var runtime: ProtectionRuntime
    @Inject lateinit var status: ProtectionStatus

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every start must be answered with startForeground, even one that's about to stop.
        runCatching { goForeground() }.onFailure {
            Timber.w(it, "Could not start Lite protection in the foreground")
            stopSelf()
            return START_NOT_STICKY
        }
        if (status.current() != ProtectionLevel.LITE || !runtime.start(ProtectionLevel.LITE)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { runtime.stop(ProtectionLevel.LITE) }.onFailure { Timber.e(it, "Stopping Lite protection failed") }
        super.onDestroy()
    }

    private fun goForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.protection_channel), NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            },
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.protection_notification_title))
            .setContentText(getString(R.string.protection_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val CHANNEL = "protection"
        private const val NOTIFICATION_ID = 4

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ProtectionService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProtectionService::class.java))
        }
    }
}

/** Lite protection doesn't survive a reboot by itself. */
@AndroidEntryPoint
class ProtectionBootReceiver : BroadcastReceiver() {
    @Inject lateinit var status: ProtectionStatus

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) status.refresh()
    }
}
