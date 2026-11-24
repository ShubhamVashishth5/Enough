package app.minimise.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import timber.log.Timber

/** Gestures on home open these tools, so the phone is a tool first. */
object QuickTools {
    fun openCamera(context: Context) = start(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

    fun openTimer(context: Context) {
        if (!start(context, Intent(AlarmClock.ACTION_SHOW_TIMERS))) {
            start(context, Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_SKIP_UI, false))
        }
    }

    /** The clock app's alarm list, from home's alarm line. */
    fun openAlarms(context: Context) = start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))

    /** The system's battery page, from home's battery line. */
    fun openBatteryUsage(context: Context) = start(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No app for %s", intent.action)
        false
    }
}
