package app.minimise.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.text.format.DateFormat
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.core.time.WeeklyWindow
import java.time.DayOfWeek
import java.time.LocalTime

/** Day toggles plus start and end times, for schedules and mode schedules. */
@Composable
fun WindowFields(window: WeeklyWindow, onChange: (WeeklyWindow) -> Unit) {
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DayOfWeek.entries.forEach { day ->
                val on = day in window.days
                Text(
                    text = dayName(day).take(2),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .semantics { selected = on }
                        .clickable(role = Role.Checkbox) {
                            onChange(window.copy(days = if (on) window.days - day else window.days + day))
                        }
                        .padding(vertical = 8.dp),
                )
            }
        }
        SettingsRow(title = stringResource(R.string.window_starts), summary = timeText(window.start), onClick = { picking = START })
        SettingsRow(title = stringResource(R.string.window_ends), summary = timeText(window.end), onClick = { picking = END })
    }
    when (picking) {
        START -> TimeDialog(window.start, onPicked = { onChange(window.copy(start = it)); picking = null }, onDismiss = { picking = null })
        END -> TimeDialog(window.end, onPicked = { onChange(window.copy(end = it)); picking = null }, onDismiss = { picking = null })
    }
}

/** Picks a time of day, following the phone's 24-hour setting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initial: LocalTime, onPicked: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.window_set)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.swap_cancel)) } },
    )
}

private const val START = "start"
private const val END = "end"
