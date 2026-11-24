package app.minimise.swaps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.apps.domain.AppEntry
import app.minimise.core.ui.AppPickerDialog
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow

@Composable
fun SwapsRoute(viewModel: SwapsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SwapsScreen(
        state = state,
        onAdd = viewModel::onAdd,
        onEdit = viewModel::onEdit,
        editorCallbacks = SwapEditorCallbacks(
            onTextChange = viewModel::onTextChange,
            onPickApp = viewModel::onPickApp,
            onAppChosen = viewModel::onAppChosen,
            onAppCleared = viewModel::onAppCleared,
            onPickerDismissed = viewModel::onPickerDismissed,
            onSave = viewModel::onSave,
            onDelete = viewModel::onDelete,
            onDismiss = viewModel::onEditorDismissed,
        ),
    )
}

class SwapEditorCallbacks(
    val onTextChange: (String) -> Unit,
    val onPickApp: () -> Unit,
    val onAppChosen: (AppEntry) -> Unit,
    val onAppCleared: () -> Unit,
    val onPickerDismissed: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
fun SwapsScreen(
    state: SwapsUiState,
    onAdd: () -> Unit,
    onEdit: (SwapRow) -> Unit,
    editorCallbacks: SwapEditorCallbacks,
) {
    ScreenScaffold(title = stringResource(R.string.swaps_title)) {
        Text(
            text = stringResource(R.string.swaps_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (state.loaded && state.rows.isEmpty()) {
            Text(
                text = stringResource(R.string.swaps_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(state.rows, key = { it.swap.id }) { row ->
                SettingsRow(
                    title = row.swap.text,
                    summary = row.appLabel?.let { stringResource(R.string.swaps_opens, it) }
                        ?: stringResource(R.string.swaps_goes_home),
                    onClick = { onEdit(row) },
                )
            }
        }
        TextButton(onClick = onAdd, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = stringResource(R.string.swaps_add), style = MaterialTheme.typography.titleMedium)
        }
    }
    state.editor?.let { editor ->
        SwapEditorDialog(editor = editor, callbacks = editorCallbacks)
        if (editor.pickingApp) {
            AppPickerDialog(
                choices = state.toolApps,
                onPick = editorCallbacks.onAppChosen,
                onClear = editorCallbacks.onAppCleared,
                onDismiss = editorCallbacks.onPickerDismissed,
            )
        }
    }
}

@Composable
private fun SwapEditorDialog(editor: SwapEditor, callbacks: SwapEditorCallbacks) {
    Dialog(onDismissRequest = callbacks.onDismiss) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(if (editor.isNew) R.string.swap_new else R.string.swap_edit),
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    value = editor.text,
                    onValueChange = callbacks.onTextChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.swap_hint)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingsRow(
                    title = stringResource(R.string.swap_app),
                    summary = editor.appLabel ?: stringResource(R.string.swap_app_none),
                    onClick = callbacks.onPickApp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    if (!editor.isNew) TextButton(onClick = callbacks.onDelete) { Text(stringResource(R.string.swap_delete)) }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.weight(1f)) {
                        TextButton(onClick = callbacks.onDismiss) { Text(stringResource(R.string.swap_cancel)) }
                        TextButton(onClick = callbacks.onSave, enabled = editor.canSave) {
                            Text(stringResource(R.string.swap_save))
                        }
                    }
                }
            }
        }
    }
}
