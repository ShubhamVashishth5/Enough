package app.minimise.core.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.minimise.R
import app.minimise.core.time.WeeklyWindow
import java.time.DayOfWeek
import java.time.LocalDateTime
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

/** "Tue 3 Dec, 14:00", for when a lock ends. Usable outside composition (a toast). */
fun dateTimeText(context: Context, at: LocalDateTime): String {
    val locale = Locale.getDefault()
    val date = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale)
    val time = DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)
    return "${at.format(date)}, ${at.format(time)}"
}

@Composable
fun dateTimeText(at: LocalDateTime): String = dateTimeText(LocalContext.current, at)
