package app.minimise.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.minimise.R

/** A long-press on an app in search or the Apps list: App info or Uninstall. */
@Composable
fun AppActionsDialog(
    label: String,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column {
                TextButton(onClick = onAppInfo) { Text(stringResource(R.string.app_actions_info)) }
                TextButton(onClick = onUninstall) { Text(stringResource(R.string.app_actions_uninstall)) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.app_actions_close)) } },
    )
}
