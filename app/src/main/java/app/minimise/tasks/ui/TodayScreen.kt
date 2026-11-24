package app.minimise.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
            OutlinedButton(onClick = onSave) { Text(stringResource(R.string.today_save)) }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.today_cancel)) }
        }
    }
}

/** One task: its text, then up, down and remove. Higher up is more important. */
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Blank lines have nothing to tick yet.
        Checkbox(
            checked = done,
            onCheckedChange = { onDoneToggled() },
            enabled = text.isNotBlank(),
            modifier = Modifier.semantics { contentDescription = doneLabel },
        )
        BasicTextField(
            value = text,
            onValueChange = onChange,
            singleLine = true,
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
        TaskButton(R.drawable.ic_arrow_upward, stringResource(R.string.a11y_task_up, label), enabled = canMoveUp, onClick = onMoveUp)
        TaskButton(R.drawable.ic_arrow_downward, stringResource(R.string.a11y_task_down, label), enabled = canMoveDown, onClick = onMoveDown)
        TaskButton(R.drawable.ic_close, stringResource(R.string.a11y_task_remove, label), enabled = true, onClick = onRemove)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun TaskButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
