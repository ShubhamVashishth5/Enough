package app.minimise.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.core.ui.AppPickerDialog
import app.minimise.tasks.domain.TodayTask
import app.minimise.tasks.ui.TodayTaskList
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSlotTapped: (HomeSlot) -> Unit,
    onSlotLongPressed: (HomeSlot) -> Unit,
    onMakeDefaultHome: () -> Unit,
    onSlotAppPicked: (String) -> Unit,
    onSlotCleared: () -> Unit,
    onPickerDismissed: () -> Unit,
    onSwipeUp: () -> Unit,
    onLongPressBackground: () -> Unit,
    onFixProtection: () -> Unit,
    onTaskTapped: (TodayTask) -> Unit,
    onEditTasks: () -> Unit,
    modifier: Modifier = Modifier,
    focusSection: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .swipeUp(onSwipeUp)
            .pointerInput(onLongPressBackground) { detectTapGestures(onLongPress = { onLongPressBackground() }) }
            .safeDrawingPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        state.now?.let { Clock(it) }
        TodayTaskList(tasks = state.tasks, onTaskTapped = onTaskTapped, onEditTasks = onEditTasks)
        if (!state.isDefaultHome) {
            Text(
                text = stringResource(R.string.home_make_default),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .combinedClickable(onClick = onMakeDefaultHome),
            )
        }
        if (state.protectionOff) {
            Text(
                text = stringResource(R.string.home_protection_off),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .combinedClickable(onClick = onFixProtection),
            )
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.slots.forEach { slot ->
                SlotRow(slot = slot, onTap = { onSlotTapped(slot) }, onLongPress = { onSlotLongPressed(slot) })
            }
        }
        Spacer(Modifier.weight(1f))
        focusSection()
        state.numbers?.let { numbers ->
            Text(
                text = stringResource(R.string.home_numbers, numbers.vaultTime.inWholeMinutes, numbers.unlocks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }

    state.slotPicker?.let { picker ->
        AppPickerDialog(
            choices = picker.choices,
            onPick = { onSlotAppPicked(it.packageName) },
            onClear = onSlotCleared,
            onDismiss = onPickerDismissed,
        )
    }
}

/** Calls [onSwipeUp] once per upward drag longer than a short threshold. */
private fun Modifier.swipeUp(onSwipeUp: () -> Unit): Modifier = pointerInput(onSwipeUp) {
    val threshold = 48.dp.toPx()
    var dragged = 0f
    var fired = false
    detectVerticalDragGestures(
        onDragStart = {
            dragged = 0f
            fired = false
        },
        onVerticalDrag = { _, amount ->
            dragged += amount
            if (!fired && dragged < -threshold) {
                fired = true
                onSwipeUp()
            }
        },
    )
}

@Composable
private fun Clock(now: LocalDateTime) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeFormat = remember(is24Hour) { DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm") }
    val dateFormat = remember {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM"))
    }
    Text(text = now.format(timeFormat), style = MaterialTheme.typography.displayLarge)
    Text(
        text = now.format(dateFormat),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SlotRow(slot: HomeSlot, onTap: () -> Unit, onLongPress: () -> Unit) {
    val app = slot.app
    val emptyDescription = stringResource(R.string.home_slot_empty_description)
    Text(
        text = app?.label ?: stringResource(R.string.home_slot_empty),
        style = MaterialTheme.typography.headlineMedium,
        color = if (app == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(vertical = 10.dp)
            .then(if (app == null) Modifier.semantics { contentDescription = emptyDescription } else Modifier),
    )
}
