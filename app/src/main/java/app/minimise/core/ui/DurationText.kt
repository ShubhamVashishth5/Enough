package app.minimise.core.ui

import androidx.compose.runtime.Composable
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
