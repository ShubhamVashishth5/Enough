package app.minimise.tasks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.Defaults
import app.minimise.core.ui.ScreenScaffold

@Composable
fun TodayRoute(onClose: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onClose() }
    TodayScreen(
        state = state,
        onDraftChange = viewModel::onDraftChange,
        onDoneToggled = viewModel::onDoneToggled,
        onSave = viewModel::onSave,
        onCancel = viewModel::onCancel,
        onAdd = viewModel::onAdd,
        onMove = viewModel::onMove,
        onRemove = viewModel::onRemove,
        onCarryOver = viewModel::onCarryOver,
        onTightenAccepted = viewModel::onTightenAccepted,
        onTightenDeclined = viewModel::onTightenDeclined,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onDraftChange: (Int, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onTightenAccepted: () -> Unit = {},
    onTightenDeclined: () -> Unit = {},
    onAdd: () -> Unit = {},
    onMove: (index: Int, up: Boolean) -> Unit = { _, _ -> },
    onRemove: (Int) -> Unit = {},
    onDoneToggled: (Int) -> Unit = {},
    onCarryOver: (String) -> Unit = {},
) {
    ScreenScaffold(title = stringResource(R.string.today_title), modifier = Modifier.imePadding(), scrollable = true) {
        state.tightenOffer?.let { minutes ->
            Text(text = stringResource(R.string.tighten_offer, minutes), style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                OutlinedButton(onClick = onTightenAccepted) { Text(stringResource(R.string.tighten_accept, minutes)) }
                TextButton(onClick = onTightenDeclined) { Text(stringResource(R.string.tighten_decline)) }
            }
        }
        Text(
            text = stringResource(R.string.today_question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        if (!state.loaded) return@ScreenScaffold
        state.drafts.forEachIndexed { index, draft ->
            if (index == Defaults.HOME_TASKS) {
                Text(
                    text = stringResource(R.string.today_later),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            TaskField(
                text = draft.text,
                done = draft.done,
                onDoneToggled = { onDoneToggled(index) },
                hint = stringResource(R.string.today_hint, index + 1),
                onChange = { onDraftChange(index, it) },
                last = index == state.drafts.lastIndex,
                canMoveUp = index > 0,
                canMoveDown = index < state.drafts.lastIndex,
                onMoveUp = { onMove(index, true) },
                onMoveDown = { onMove(index, false) },
                onRemove = { onRemove(index) },
            )
        }
        if (state.canAdd) {
            TextButton(onClick = onAdd, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.today_add), style = MaterialTheme.typography.titleMedium)
            }
        }
        val leftovers = state.leftovers
        if (leftovers.isNotEmpty()) {
            Text(
                text = stringResource(if (state.canCarryOver) R.string.today_yesterday else R.string.today_yesterday_full),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            leftovers.forEach { text -> Leftover(text = text, enabled = state.canCarryOver, onClick = { onCarryOver(text) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
            OutlinedButton(onClick = onSave) { Text(stringResource(R.string.today_save)) }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.today_cancel)) }
        }
    }
}

/**
 * One task: its text, then up, down and remove. Higher up is more important. The text wraps onto as
 * many lines as it needs; the tick and the buttons stay level with its first line.
 */
@Composable
private fun TaskField(
    text: String,
    done: Boolean,
    onDoneToggled: () -> Unit,
    hint: String,
    onChange: (String) -> Unit,
    last: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    val label = text.ifBlank { hint }
    val doneLabel = stringResource(R.string.a11y_task_done, label)
    val focusManager = LocalFocusManager.current
    Row(verticalAlignment = Alignment.Top) {
        // Blank lines have nothing to tick yet.
        Checkbox(
            checked = done,
            onCheckedChange = { onDoneToggled() },
            enabled = text.isNotBlank(),
            modifier = Modifier.semantics { contentDescription = doneLabel },
        )
        BasicTextField(
            value = text,
            onValueChange = { value ->
                // A task stays one paragraph. Enter, from a keyboard that types it rather than showing
                // Next, moves on as Next would; line breaks pasted in become spaces.
                if (value.length == text.length + 1 && value.count { it == '\n' } == 1) {
                    onChange(value.replace("\n", ""))
                    if (last) focusManager.clearFocus() else focusManager.moveFocus(FocusDirection.Next)
                } else {
                    onChange(value.replace(LINE_BREAKS, " "))
                }
            },
            textStyle = if (done) style.copy(color = MaterialTheme.colorScheme.outline, textDecoration = TextDecoration.LineThrough) else style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (last) ImeAction.Done else ImeAction.Next,
            ),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            decorationBox = { field ->
                Box {
                    if (text.isEmpty()) Text(text = hint, style = style, color = MaterialTheme.colorScheme.outline)
                    field()
                }
            },
        )
        // Level with the tick and the first line of text (whose letters sit a little above its middle).
        Row(modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            TaskButton(R.drawable.ic_arrow_upward, stringResource(R.string.a11y_task_up, label), enabled = canMoveUp, onClick = onMoveUp)
            TaskButton(R.drawable.ic_arrow_downward, stringResource(R.string.a11y_task_down, label), enabled = canMoveDown, onClick = onMoveDown)
            TaskButton(R.drawable.ic_close, stringResource(R.string.a11y_task_remove, label), enabled = true, onClick = onRemove)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** A small icon button: a row of three leaves the text most of the width. */
@Composable
private fun TaskButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** One of yesterday's undone tasks. A tap carries it over into today's list. */
@Composable
private fun Leftover(text: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = stringResource(R.string.a11y_carry_over), onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** Line breaks and the spaces around them, which become a single space in a task. */
private val LINE_BREAKS = Regex("\\s*[\\r\\n]+\\s*")
