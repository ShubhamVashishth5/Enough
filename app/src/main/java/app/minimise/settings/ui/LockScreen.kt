package app.minimise.settings.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.dateTimeText
import kotlin.time.Duration

@Composable
fun LockRoute(viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LockScreen(state = state, onLengthChosen = viewModel::onLengthChosen, onConfirm = viewModel::onConfirm, onDismiss = viewModel::onDismiss)
}

@Composable
fun LockScreen(state: LockUiState, onLengthChosen: (Duration) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.lock_title)) {
        Text(
            text = stringResource(R.string.lock_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        state.lockedUntil?.let {
            Text(
                text = stringResource(R.string.lock_locked_until, dateTimeText(it)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        state.choices.forEach { length ->
            SettingsRow(
                title = stringResource(
                    if (state.lockedUntil == null) R.string.lock_for_days else R.string.lock_extend_days,
                    length.inWholeDays,
                ),
                onClick = { onLengthChosen(length) },
            )
        }
    }
    val length = state.confirming
    val until = state.confirmingUntil
    if (length != null && until != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.lock_confirm_title, length.inWholeDays)) },
            text = { Text(stringResource(R.string.lock_confirm_body, dateTimeText(until))) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.lock_confirm)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.swap_cancel)) } },
        )
    }
}
