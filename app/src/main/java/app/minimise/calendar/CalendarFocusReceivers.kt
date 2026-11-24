package app.minimise.calendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.minimise.calendar.domain.CalendarFocus
import app.minimise.calendar.domain.CalendarFocusScheduler
import app.minimise.core.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A focus event is starting (or a day has passed): start its session, then arm the next alarm. */
@AndroidEntryPoint
class CalendarFocusAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var calendarFocus: CalendarFocus
    @Inject lateinit var scheduler: CalendarFocusScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        appScope.launch {
            try {
                calendarFocus.startIfDue()
            } finally {
                scheduler.scheduleNext()
                result.finish()
            }
        }
    }
}

@AndroidEntryPoint
class CalendarFocusBootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: CalendarFocusScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        appScope.launch {
            try {
                scheduler.scheduleNext()
            } finally {
                result.finish()
            }
        }
    }
}
