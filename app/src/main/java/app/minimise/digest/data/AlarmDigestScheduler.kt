package app.minimise.digest.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.digest.DigestAlarmReceiver
import app.minimise.digest.domain.DigestScheduler
import app.minimise.digest.domain.DigestTimes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** An inexact window alarm at the next digest time. Re-armed after each one and after a reboot. */
class AlarmDigestScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeSource: TimeSource,
) : DigestScheduler {
    override fun scheduleNext() {
        val now = timeSource.now().atZone(timeSource.zone())
        val next = DigestTimes.nextAfter(now, Defaults.DIGEST_TIMES).toInstant().toEpochMilli()
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, DigestAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        context.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, next, Defaults.DIGEST_WINDOW.inWholeMilliseconds, intent)
    }
}
