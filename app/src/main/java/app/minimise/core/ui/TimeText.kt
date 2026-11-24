package app.minimise.core.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.minimise.R
import app.minimise.core.time.WeeklyWindow
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** "22:00" or "10:00 PM", following the phone's 24-hour setting. */
@Composable
fun timeText(time: LocalTime): String {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val format = remember(is24Hour) { DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a") }
    return time.format(format)
}

/** "Mon Tue Wed · 22:00–07:00", or "Every day · ..." */
@Composable
fun windowText(window: WeeklyWindow): String {
    val days = when (window.days.size) {
        7 -> stringResource(R.string.days_every_day)
        0 -> stringResource(R.string.days_none)
        else -> DayOfWeek.entries.filter { it in window.days }.joinToString(" ") { dayName(it) }
    }
    return stringResource(R.string.window_summary, days, timeText(window.start), timeText(window.end))
}

fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
