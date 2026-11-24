package app.minimise.schedules.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.WindowFields
import app.minimise.core.ui.durationText
import app.minimise.core.ui.windowText

@Composable
fun SchedulesRoute(viewModel: SchedulesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SchedulesScreen(
        state = state,
        onFirstHourToggled = viewModel::onFirstHourToggled,
        onAdd = viewModel::onAdd,
        onEdit = viewModel::onEdit,
    )
    state.editor?.let { draft ->
        ScheduleEditorDialog(
            draft = draft,
            onNameChange = viewModel::onNameChange,
            onWindowChange = viewModel::onWindowChange,
            onSave = viewModel::onSave,
            onDelete = viewModel::onDelete,
            onDismiss = viewModel::onDismiss,
        )
    }
}

@Composable
fun SchedulesScreen(
    state: SchedulesUiState,
    onFirstHourToggled: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ScheduleRow) -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.schedules_title)) {
        Text(
            text = stringResource(R.string.schedules_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SettingsRow(
            title = stringResource(R.string.first_hour_title),
            summary = when {
                state.firstHourOffIn != null -> stringResource(R.string.first_hour_off_in, durationText(state.firstHourOffIn))
                state.firstHour -> stringResource(R.string.first_hour_on)
                else -> stringResource(R.string.first_hour_off)
            },
            onClick = onFirstHourToggled,
        )
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(state.rows, key = { it.schedule.id }) { row ->
                val pending = row.pendingIn?.let {
                    stringResource(if (row.pendingRemoval) R.string.schedule_removed_in else R.string.schedule_changes_in, durationText(it))
                }
                SettingsRow(
                    title = row.schedule.name,
                    summary = listOfNotNull(windowText(row.schedule.window), pending).joinToString("\n"),
                    onClick = { onEdit(row) },
                )
            }
        }
        TextButton(onClick = onAdd, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = stringResource(R.string.schedules_add), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ScheduleEditorDialog(
    draft: ScheduleDraft,
    onNameChange: (String) -> Unit,
    onWindowChange: (WeeklyWindow) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (draft.isNew) R.string.schedule_new else R.string.schedule_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.schedule_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                WindowFields(window = draft.window, onChange = onWindowChange)
                Text(
                    text = stringResource(R.string.schedule_loosen_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Row {
                if (!draft.isNew) TextButton(onClick = onDelete) { Text(stringResource(R.string.swap_delete)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.swap_cancel)) }
                TextButton(onClick = onSave, enabled = draft.canSave) { Text(stringResource(R.string.swap_save)) }
            }
        },
    )
}
