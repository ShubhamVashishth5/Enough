package app.minimise.digest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.minimise.core.di.ApplicationScope
import app.minimise.core.time.TimeSource
import app.minimise.digest.data.DigestNotifier
import app.minimise.digest.domain.DigestRepository
import app.minimise.digest.domain.DigestScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A digest time: release what's held, post one summary, arm the next. */
@AndroidEntryPoint
class DigestAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: DigestRepository
    @Inject lateinit var scheduler: DigestScheduler
    @Inject lateinit var notifier: DigestNotifier
    @Inject lateinit var timeSource: TimeSource
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        appScope.launch {
            try {
                val summary = repository.releaseAll(timeSource.now())
                if (summary.updates > 0) notifier.post(summary)
            } finally {
                scheduler.scheduleNext()
                result.finish()
            }
        }
    }
}

/** Alarms don't survive a reboot. */
@AndroidEntryPoint
class DigestBootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: DigestScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) scheduler.scheduleNext()
    }
}
