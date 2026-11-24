package app.minimise.stats

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.minimise.stats.data.WeeklyReportNotifier
import app.minimise.stats.domain.WeeklyReportScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Sunday evening: post the notification, arm next week's. */
@AndroidEntryPoint
class WeeklyReportAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var notifier: WeeklyReportNotifier
    @Inject lateinit var scheduler: WeeklyReportScheduler

    override fun onReceive(context: Context, intent: Intent) {
        try {
            notifier.post()
        } finally {
            scheduler.scheduleNext()
        }
    }
}

/** Alarms don't survive a reboot. */
@AndroidEntryPoint
class WeeklyReportBootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: WeeklyReportScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) scheduler.scheduleNext()
    }
}
