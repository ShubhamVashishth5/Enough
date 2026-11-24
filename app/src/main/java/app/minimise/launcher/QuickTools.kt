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

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No app for %s", intent.action)
        false
    }
}
