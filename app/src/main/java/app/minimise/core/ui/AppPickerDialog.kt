package app.minimise.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.minimise.R
import app.minimise.apps.domain.AppEntry

/**
 * Picks a Tool app, or none (for a home slot, an icon or a swap).
 *
 * @param automatic a first choice above the apps, such as "Automatic: Phone" for one of home's icons,
 * with what it does ([onAutomatic]).
 */
@Composable
fun AppPickerDialog(
    choices: List<AppEntry>,
    onPick: (AppEntry) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.home_pick_app_title),
    clearLabel: String = stringResource(R.string.home_pick_app_clear),
    automatic: String? = null,
    onAutomatic: () -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp).semantics { heading() },
                )
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    if (automatic != null) {
                        item(key = "automatic") {
                            Choice(text = automatic, onClick = onAutomatic)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }
                    items(choices, key = { it.packageName }) { app -> Choice(text = app.label, onClick = { onPick(app) }) }
                }
                TextButton(onClick = onClear, modifier = Modifier.padding(horizontal = 12.dp).padding(top = 8.dp)) {
                    Text(clearLabel)
                }
            }
        }
    }
}

@Composable
private fun Choice(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
    )
}
