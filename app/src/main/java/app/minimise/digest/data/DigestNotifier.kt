package app.minimise.digest.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.minimise.R
import app.minimise.digest.domain.DigestSummary
import app.minimise.launcher.HomeActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

/** Posts the one summary notification per digest: "12 updates from 5 apps". */
class DigestNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun post(summary: DigestSummary) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.digest_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            HomeActivity.digestIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = context.resources.getQuantityString(R.plurals.digest_summary_updates, summary.updates, summary.updates) +
            " " + context.resources.getQuantityString(R.plurals.digest_summary_apps, summary.apps, summary.apps)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.digest_summary_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }.onFailure { Timber.w(it, "Could not post the digest") }
    }

    private companion object {
        const val CHANNEL = "digest"
        const val NOTIFICATION_ID = 1
    }
}
