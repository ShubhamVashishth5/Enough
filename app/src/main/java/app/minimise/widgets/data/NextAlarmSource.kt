package app.minimise.widgets.data

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** The next alarm the clock app has set (AlarmManager's alarm clock), or null. No permission. */
class NextAlarmSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    val next: Flow<Instant?> = callbackFlow {
        fun send() {
            trySend(runCatching { alarmManager.nextAlarmClock?.triggerTime?.let(Instant::ofEpochMilli) }.getOrNull())
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = send()
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        send()
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
