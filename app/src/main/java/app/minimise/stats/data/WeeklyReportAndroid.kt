package app.minimise.stats.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.minimise.R
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.launcher.HomeActivity
import app.minimise.stats.WeeklyReportAlarmReceiver
import app.minimise.stats.domain.EventCsv
import app.minimise.stats.domain.ReportTimes
import app.minimise.stats.domain.WeeklyReportScheduler
import app.minimise.usage.domain.EventLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AlarmWeeklyReportScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeSource: TimeSource,
) : WeeklyReportScheduler {
    override fun scheduleNext() {
        val now = timeSource.now().atZone(timeSource.zone())
        val next = ReportTimes.nextAfter(now, Defaults.WEEKLY_REPORT_DAY, Defaults.WEEKLY_REPORT_TIME).toInstant().toEpochMilli()
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WeeklyReportAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            context.getSystemService(AlarmManager::class.java)
                .setWindow(AlarmManager.RTC_WAKEUP, next, Defaults.WEEKLY_REPORT_WINDOW.inWholeMilliseconds, intent)
        }.onFailure { Timber.w(it, "Could not arm the weekly report") }
    }
}

/** "Your week is ready", linking to the report. */
class WeeklyReportNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun post() {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.report_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            context,
            2,
            HomeActivity.statsIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.report_ready))
            .setContentText(context.getString(R.string.report_ready_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }.onFailure { Timber.w(it, "Could not post the weekly report") }
    }

    private companion object {
        const val CHANNEL = "weekly_report"
        const val NOTIFICATION_ID = 3
    }
}

/** Writes the event log to a file the user picked, so no storage permission is needed. */
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventLog: EventLog,
) {
    /** The number of events written, or null when the file couldn't be written. */
    suspend fun export(uri: Uri): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val events = eventLog.between(Instant.EPOCH, Instant.ofEpochMilli(Long.MAX_VALUE))
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { EventCsv.write(events, it) } ?: return@runCatching null
            events.size
        }.onFailure { Timber.e(it, "CSV export failed") }.getOrNull()
    }
}
