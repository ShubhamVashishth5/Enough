package app.minimise.calendar.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.minimise.calendar.CalendarFocusAlarmReceiver
import app.minimise.calendar.domain.CalendarFocus
import app.minimise.calendar.domain.CalendarFocusScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import timber.log.Timber

/** A one-minute window alarm at the next focus event's start. Cancelled while calendar focus is off. */
class AlarmCalendarFocusScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarFocus: Provider<CalendarFocus>,
) : CalendarFocusScheduler {
    override suspend fun scheduleNext() {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, CalendarFocusAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val next = calendarFocus.get().nextCheck()
        runCatching {
            if (next == null) alarms.cancel(intent) else alarms.setWindow(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), WINDOW_MILLIS, intent)
        }.onFailure { Timber.w(it, "Could not arm calendar focus") }
    }

    private companion object {
        const val WINDOW_MILLIS = 60_000L
    }
}
