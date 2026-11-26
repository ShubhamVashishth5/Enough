package app.minimise.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import app.minimise.apps.domain.AppEntry
import app.minimise.core.prefs.HomeTextSize
import app.minimise.core.ui.theme.LocalAppearance
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    onSwipeDown: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    /** Double-tap, and the lock icon. */
    onLock: () -> Unit,
    onClockLongPress: () -> Unit,
    onLongPressBackground: () -> Unit,
    onFixProtection: () -> Unit,
    onTaskTapped: (TodayTask) -> Unit,
    onEditTasks: () -> Unit,
    onShortcutTapped: (HomeShortcut) -> Unit,
    onShortcutLongPressed: (HomeShortcut) -> Unit,
    /** Null: the phone's own app for the icon. */
    onShortcutAppPicked: (String?) -> Unit,
    onShortcutRemoved: () -> Unit,
    onShortcutPickerDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    focusSection: @Composable () -> Unit = {},
    digestLine: @Composable () -> Unit = {},
    eventLine: @Composable () -> Unit = {},
    appLockLine: @Composable () -> Unit = {},
    widgets: @Composable () -> Unit = {},
) {
    val options = state.options
    Column(
        modifier = modifier
            .fillMaxSize()
            .swipeVertical(onUp = onSwipeUp, onDown = if (options.swipeDownShade) onSwipeDown else null)
            .swipeSideways(onLeft = onSwipeLeft, onRight = onSwipeRight)
            .pointerInput(onLongPressBackground, onLock) {
                detectTapGestures(onLongPress = { onLongPressBackground() }, onDoubleTap = { onLock() })
            }
            .safeDrawingPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        // TalkBack takes over swipes and double taps, so the home gestures are actions on the clock too.
        val gestures = listOfNotNull(
            CustomAccessibilityAction(stringResource(R.string.a11y_search_apps)) { onSwipeUp(); true },
            if (options.swipeDownShade) CustomAccessibilityAction(stringResource(R.string.a11y_notifications)) { onSwipeDown(); true } else null,
            CustomAccessibilityAction(stringResource(R.string.a11y_settings)) { onLongPressBackground(); true },
            CustomAccessibilityAction(stringResource(R.string.a11y_capture)) { onSwipeLeft(); true },
            CustomAccessibilityAction(stringResource(R.string.a11y_camera)) { onSwipeRight(); true },
            CustomAccessibilityAction(stringResource(R.string.a11y_lock_screen)) { onLock(); true },
        )
        state.now?.let { now ->
            Clock(now, onLongPress = onClockLongPress, actions = gestures, modifier = Modifier.fillMaxWidth())
        }
        widgets()
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
        val appNameStyle = appNameStyle(LocalAppearance.current.appNameSize)
        // App names on the left; the user's icons in a small column on the right, as on a watch face.
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            WholeRows(gap = APP_NAME_GAP, modifier = Modifier.weight(1f).fillMaxHeight()) {
                state.slots.forEach { slot ->
                    SlotRow(slot = slot, style = appNameStyle, onTap = { onSlotTapped(slot) }, onLongPress = { onSlotLongPressed(slot) })
                }
            }
            if (state.shortcuts.isNotEmpty()) {
                WholeRows(gap = SHORTCUT_GAP, modifier = Modifier.padding(start = 16.dp).fillMaxHeight()) {
                    state.shortcuts.forEach { shortcut ->
                        ShortcutButton(shortcut, onTap = { onShortcutTapped(shortcut) }, onLongPress = { onShortcutLongPressed(shortcut) })
                    }
                }
            }
        }
        // Focus and the numbers on the left; the lock at the bottom right, under the icons, within reach of a thumb.
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
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
            if (options.lockButton) {
                RoundIcon(
                    icon = R.drawable.ic_lock,
                    description = stringResource(R.string.a11y_lock_screen),
                    onTap = onLock,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
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

    state.shortcutPicker?.let { picker ->
        val name = stringResource(picker.icon.label)
        AppPickerDialog(
            choices = picker.choices,
            onPick = { onShortcutAppPicked(it.packageName) },
            onClear = onShortcutRemoved,
            onDismiss = onShortcutPickerDismissed,
            title = stringResource(R.string.shortcut_pick_title, name),
            clearLabel = stringResource(R.string.shortcut_remove),
            automatic = if (picker.icon.default != null) automaticText(picker.automatic) else null,
            onAutomatic = { onShortcutAppPicked(null) },
        )
    }
}

/**
 * Rows centred in the height they're given: the app names between the lines at the top and the numbers at
 * the bottom, and the icons beside them. When they don't all fit (a small screen, large names, many lines
 * at the top) only those that fit whole are shown, from the top, rather than cutting one in two.
 */
@Composable
private fun WholeRows(gap: Dp, modifier: Modifier, rows: @Composable () -> Unit) {
    Layout(content = rows, modifier = modifier) { measurables, constraints ->
        val space = gap.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)) }
        val shown = mutableListOf<Placeable>()
        var height = 0
        for (row in placeables) {
            val next = height + (if (shown.isEmpty()) 0 else space) + row.height
            if (next > constraints.maxHeight) break
            shown += row
            height = next
        }
        val width = if (constraints.hasBoundedWidth && constraints.minWidth == constraints.maxWidth) {
            constraints.maxWidth
        } else {
            placeables.maxOfOrNull { it.width }?.coerceIn(constraints.minWidth, constraints.maxWidth) ?: constraints.minWidth
        }
        layout(width, constraints.maxHeight) {
            var y = (constraints.maxHeight - height) / 2
            shown.forEach {
                it.place(0, y)
                y += it.height + space
            }
        }
    }
}

private val APP_NAME_GAP = 4.dp

/** "Automatic: Phone": the phone's own app for an icon, or that it has none. */
@Composable
fun automaticText(app: AppEntry?): String =
    app?.let { stringResource(R.string.shortcut_automatic_app, it.label) } ?: stringResource(R.string.shortcut_automatic_none)

/** One of the user's icons, which can be long-pressed to choose its app. */
@Composable
private fun ShortcutButton(shortcut: HomeShortcut, onTap: () -> Unit, onLongPress: () -> Unit) {
    val name = stringResource(shortcut.icon.label)
    RoundIcon(
        icon = shortcut.icon.drawable,
        description = if (name == shortcut.app.label) name else stringResource(R.string.a11y_shortcut, name, shortcut.app.label),
        onTap = onTap,
        onLongPress = onLongPress,
        longPressLabel = stringResource(R.string.a11y_change_app),
    )
}

/**
 * A plain white glyph with a round touch area: the icons on the right, and the lock. Nudged right by the
 * touch area's margin, so the glyphs line up with the right edge of the text above.
 */
@Composable
private fun RoundIcon(
    icon: Int,
    description: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    longPressLabel: String? = null,
) {
    val size = iconSize(LocalAppearance.current.iconSize)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset(x = (size.touch - size.glyph) / 2)
            .size(size.touch)
            .clip(CircleShape)
            .combinedClickable(role = Role.Button, onClick = onTap, onLongClick = onLongPress, onLongClickLabel = longPressLabel)
            .semantics { contentDescription = description },
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size.glyph),
        )
    }
}

/** An icon's glyph, and its round touch area: never under 44dp, so a small icon is as easy to tap. */
data class IconSize(val glyph: Dp, val touch: Dp)

/** Home's icons and lock, in the size chosen in Settings → Appearance. */
fun iconSize(size: HomeTextSize): IconSize = when (size) {
    HomeTextSize.SMALL -> IconSize(glyph = 18.dp, touch = 44.dp)
    HomeTextSize.MEDIUM -> IconSize(glyph = 22.dp, touch = 44.dp)
    HomeTextSize.LARGE -> IconSize(glyph = 28.dp, touch = 52.dp)
}

private val SHORTCUT_GAP = 8.dp

/** Home's clock, in the size chosen in Settings → Appearance. */
@Composable
fun clockStyle(size: HomeTextSize): TextStyle {
    val sp = when (size) {
        HomeTextSize.SMALL -> 44
        HomeTextSize.MEDIUM -> 60
        HomeTextSize.LARGE -> 84
    }
    return MaterialTheme.typography.displayLarge.copy(fontSize = sp.sp, lineHeight = (sp * 1.1f).sp)
}

/** Home's app names, in the size chosen in Settings → Appearance. */
@Composable
fun appNameStyle(size: HomeTextSize): TextStyle {
    val sp = when (size) {
        HomeTextSize.SMALL -> 22
        HomeTextSize.MEDIUM -> 28
        HomeTextSize.LARGE -> 36
    }
    return MaterialTheme.typography.headlineMedium.copy(fontSize = sp.sp, lineHeight = (sp * 1.25f).sp)
}

/** Calls [onUp] or [onDown] once per vertical drag longer than a short threshold. No [onDown], no down swipe. */
private fun Modifier.swipeVertical(onUp: () -> Unit, onDown: (() -> Unit)?): Modifier = pointerInput(onUp, onDown) {
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
                onUp()
            } else if (!fired && onDown != null && dragged > threshold) {
                fired = true
                onDown()
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
private fun Clock(now: LocalDateTime, onLongPress: () -> Unit, actions: List<CustomAccessibilityAction>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeFormat = remember(is24Hour) { DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm") }
    val dateFormat = remember {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM"))
    }
    Column(
        modifier = modifier
            .combinedClickable(onClick = {}, onLongClick = onLongPress, onLongClickLabel = stringResource(R.string.a11y_open_timer))
            .semantics(mergeDescendants = true) { customActions = actions },
    ) {
        Text(text = now.format(timeFormat), style = clockStyle(LocalAppearance.current.clockSize))
        Text(
            text = now.format(dateFormat),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SlotRow(slot: HomeSlot, style: TextStyle, onTap: () -> Unit, onLongPress: () -> Unit) {
    val app = slot.app
    val emptyDescription = stringResource(R.string.home_slot_empty_description)
    val changeLabel = stringResource(R.string.a11y_change_app)
    Text(
        text = app?.label ?: stringResource(R.string.home_slot_empty),
        style = style,
        color = if (app == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(role = Role.Button, onClick = onTap, onLongClick = onLongPress, onLongClickLabel = changeLabel)
            .padding(vertical = 10.dp)
            .then(if (app == null) Modifier.semantics { contentDescription = emptyDescription } else Modifier),
    )
}
