package app.minimise.focus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.capture.ui.InboxLink
import app.minimise.core.Defaults
import app.minimise.core.ui.durationText
import app.minimise.keys.ui.KeyCheck

/** The Focus line on home: start a session, see the time left, or end it early. */
@Composable
fun FocusSection(onOpenInbox: () -> Unit, viewModel: FocusViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        state.justEndedMinutes?.let {
            Text(
                text = stringResource(R.string.focus_done, it),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Thoughts captured mid-focus come back now.
            InboxLink(onOpenInbox = onOpenInbox)
        }
        val active = state.active
        Text(
            text = if (active == null) stringResource(R.string.focus_start_line) else stringResource(R.string.focus_left, durationText(state.left)),
            style = MaterialTheme.typography.titleMedium,
            color = if (active == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable(onClick = viewModel::onFocusTapped).padding(vertical = 8.dp),
        )
    }
    when (state.dialog) {
        FocusDialog.START -> StartDialog(canLock = state.keyMethods.isNotEmpty(), onStart = viewModel::onStart, onDismiss = viewModel::onDismiss)
        FocusDialog.END -> EndDialog(state, viewModel)
        null -> Unit
    }
}

@Composable
private fun StartDialog(canLock: Boolean, onStart: (kotlin.time.Duration, Boolean) -> Unit, onDismiss: () -> Unit) {
    var locked by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focus_title)) },
        text = {
            Column {
                Text(stringResource(R.string.focus_explainer), style = MaterialTheme.typography.bodyMedium)
                Defaults.FOCUS_LENGTHS.forEach { length ->
                    Text(
                        text = durationText(length),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth().clickable { onStart(length, locked) }.padding(vertical = 12.dp),
                    )
                }
                if (canLock) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { locked = !locked }) {
                        Checkbox(checked = locked, onCheckedChange = { locked = it })
                        Text(stringResource(R.string.focus_lock))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.swap_cancel)) } },
    )
}

@Composable
private fun EndDialog(state: FocusUiState, viewModel: FocusViewModel) {
    val locked = state.active?.locked == true
    AlertDialog(
        onDismissRequest = viewModel::onDismiss,
        title = { Text(stringResource(R.string.focus_end_title)) },
        text = {
            if (!locked) {
                Text(stringResource(R.string.focus_end_body))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.focus_end_locked))
                    state.keyMethods.forEach { method ->
                        KeyCheck(method = method, failed = false, onNfcTag = viewModel::onNfcTag, onQrScanned = viewModel::onQrScanned)
                    }
                    if (state.verifyFailed) Text(stringResource(R.string.key_not_yours))
                }
            }
        },
        confirmButton = {
            if (!locked) TextButton(onClick = viewModel::onEndNow) { Text(stringResource(R.string.focus_end_now)) }
        },
        dismissButton = { TextButton(onClick = viewModel::onDismiss) { Text(stringResource(R.string.focus_keep_going)) } },
    )
}
