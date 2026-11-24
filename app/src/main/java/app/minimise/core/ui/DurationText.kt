package app.minimise.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.minimise.R
import kotlin.time.Duration

/** "23 h 59 min", "24 h", "5 min" or "under a minute". Rounds up to the next minute. */
@Composable
fun durationText(duration: Duration): String {
    val totalMinutes = (duration.inWholeSeconds + 59).coerceAtLeast(0) / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.duration_hours_minutes, hours, minutes)
        hours > 0 -> stringResource(R.string.duration_hours, hours)
        minutes > 0 -> stringResource(R.string.duration_minutes, minutes)
        else -> stringResource(R.string.duration_under_a_minute)
    }
}

/** The same length as [durationText], written out for TalkBack: "1 hour 5 minutes" rather than "1 h 5 min". */
@Composable
fun durationSpoken(duration: Duration): String {
    val totalMinutes = (duration.inWholeSeconds + 59).coerceAtLeast(0) / 60
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    val hoursText = pluralStringResource(R.plurals.duration_hours_spoken, hours, hours)
    val minutesText = pluralStringResource(R.plurals.duration_minutes_spoken, minutes, minutes)
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.duration_spoken_both, hoursText, minutesText)
        hours > 0 -> hoursText
        minutes > 0 -> minutesText
        else -> stringResource(R.string.duration_under_a_minute)
    }
}
