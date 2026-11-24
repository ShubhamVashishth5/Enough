package app.minimise.apps.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import app.minimise.R
import app.minimise.apps.domain.AppEntry

/** Sets a daily cap on opens for one Vault app. Empty, or "No limit", removes it. */
@Composable
fun OpenLimitDialog(app: AppEntry, onChosen: (Int?) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(app.openLimit?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.open_limit_title, app.label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter { it.isDigit() }.take(3) },
                singleLine = true,
                label = { Text(stringResource(R.string.open_limit_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { onChosen(text.toIntOrNull()) }) { Text(stringResource(R.string.open_limit_save)) } },
        dismissButton = { TextButton(onClick = { onChosen(null) }) { Text(stringResource(R.string.open_limit_remove)) } },
    )
}
