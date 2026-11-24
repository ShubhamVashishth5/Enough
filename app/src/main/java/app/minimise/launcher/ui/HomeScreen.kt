package app.minimise.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.core.ui.AppPickerDialog
import app.minimise.protection.domain.ProtectionLevel
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
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onDoubleTap: () -> Unit,
    onClockLongPress: () -> Unit,
    onLongPressBackground: () -> Unit,
    onFixProtection: () -> Unit,
    onTaskTapped: (TodayTask) -> Unit,
    onEditTasks: () -> Unit,
    modifier: Modifier = Modifier,
    focusSection: @Composable () -> Unit = {},
    digestLine: @Composable () -> Unit = {},
    eventLine: @Composable () -> Unit = {},
    appLockLine: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .swipeUp(onSwipeUp)
            .swipeSideways(onLeft = onSwipeLeft, onRight = onSwipeRight)
            .pointerInput(onLongPressBackground, onDoubleTap) {
                detectTapGestures(onLongPress = { onLongPressBackground() }, onDoubleTap = { onDoubleTap() })
            }
            .safeDrawingPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        // TalkBack takes over swipes and double taps, so the home gestures are actions on the clock too.
        val gestures = listOf(
            CustomAccessibilityAction(stringResource(R.string.a11y_search_apps)) { onSwipeUp(); true },
            CustomAccessibilityAction(stringResource(R.string.a11y_settings)) { onLongPressBackground(); true },
            CustomAccessibilityAction(stringResource(R.string.a11y_capture)) { onSwipeLeft(); true },
            CustomAccessibilityAction(stringResource(R.string.a11y_camera)) { onSwipeRight(); true },
            CustomAccessibilityAction(stringResource(R.string.a11y_lock_screen)) { onDoubleTap(); true },
        )
        state.now?.let { Clock(it, onLongPress = onClockLongPress, actions = gestures) }
        eventLine()
        TodayTaskList(tasks = state.tasks, onTaskTapped = onTaskTapped, onEditTasks = onEditTasks)
        digestLine()
        if (!state.isDefaultHome) {
            Text(
                text = stringResource(R.string.home_make_default),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .combinedClickable(role = Role.Button, onClick = onMakeDefaultHome),
            )
        }
        if (state.protection != ProtectionLevel.FULL) {
            Text(
                text = stringResource(
                    if (state.protection == ProtectionLevel.LITE) R.string.home_protection_lite else R.string.home_protection_off,
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .combinedClickable(role = Role.Button, onClick = onFixProtection),
            )
        }
        appLockLine()
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.slots.forEach { slot ->
                SlotRow(slot = slot, onTap = { onSlotTapped(slot) }, onLongPress = { onSlotLongPressed(slot) })
            }
        }
        Spacer(Modifier.weight(1f))
        focusSection()
        state.numbers?.let { numbers ->
            val minutes = numbers.vaultTime.inWholeMinutes.toInt()
            val spoken = pluralStringResource(R.plurals.a11y_vault_minutes, minutes, minutes) + ". " +
                pluralStringResource(R.plurals.a11y_unlocks, numbers.unlocks, numbers.unlocks)
            Text(
                text = stringResource(R.string.home_numbers, numbers.vaultTime.inWholeMinutes, numbers.unlocks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
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

/** Calls [onLeft] or [onRight] once per sideways drag longer than a short threshold. */
private fun Modifier.swipeSideways(onLeft: () -> Unit, onRight: () -> Unit): Modifier = pointerInput(onLeft, onRight) {
    val threshold = 64.dp.toPx()
    var dragged = 0f
    var fired = false
    detectHorizontalDragGestures(
        onDragStart = {
            dragged = 0f
            fired = false
        },
        onHorizontalDrag = { _, amount ->
            dragged += amount
            if (!fired && dragged < -threshold) {
                fired = true
                onLeft()
            } else if (!fired && dragged > threshold) {
                fired = true
                onRight()
            }
        },
    )
}

@Composable
private fun Clock(now: LocalDateTime, onLongPress: () -> Unit, actions: List<CustomAccessibilityAction>) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeFormat = remember(is24Hour) { DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm") }
    val dateFormat = remember {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM"))
    }
    Column(
        modifier = Modifier
            .combinedClickable(onClick = {}, onLongClick = onLongPress, onLongClickLabel = stringResource(R.string.a11y_open_timer))
            .semantics(mergeDescendants = true) { customActions = actions },
    ) {
        Text(text = now.format(timeFormat), style = MaterialTheme.typography.displayLarge)
        Text(
            text = now.format(dateFormat),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SlotRow(slot: HomeSlot, onTap: () -> Unit, onLongPress: () -> Unit) {
    val app = slot.app
    val emptyDescription = stringResource(R.string.home_slot_empty_description)
    val changeLabel = stringResource(R.string.a11y_change_app)
    Text(
        text = app?.label ?: stringResource(R.string.home_slot_empty),
        style = MaterialTheme.typography.headlineMedium,
        color = if (app == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(role = Role.Button, onClick = onTap, onLongClick = onLongPress, onLongClickLabel = changeLabel)
            .padding(vertical = 10.dp)
            .then(if (app == null) Modifier.semantics { contentDescription = emptyDescription } else Modifier),
    )
}
