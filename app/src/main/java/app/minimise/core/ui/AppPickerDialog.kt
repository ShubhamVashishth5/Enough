package app.minimise.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.minimise.R
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppFilter

/**
 * Picks a Tool app, or none (for a home slot, an icon or a swap). A search box above the list narrows it
 * by name.
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
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(choices, query) { AppFilter.filter(choices, query) { it.label } }
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
                AppFilterField(query = query, onQueryChange = { query = it }, modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp))
                // A long list keeps its full height while a search narrows it, so the dialog (and the box
                // being typed in) doesn't jump. Either way it shrinks to the room left, so "Leave empty"
                // stays on screen with the keyboard up.
                val long = choices.size + (if (automatic != null) 1 else 0) > LONG_LIST
                LazyColumn(modifier = Modifier.weight(1f, fill = false).then(if (long) Modifier.height(LIST_HEIGHT) else Modifier.heightIn(max = LIST_HEIGHT))) {
                    // The automatic choice isn't an app, so a search leaves it out.
                    if (automatic != null && query.isBlank()) {
                        item(key = "automatic") {
                            Choice(text = automatic, onClick = onAutomatic)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }
                    items(shown, key = { it.packageName }) { app -> Choice(text = app.label, onClick = { onPick(app) }) }
                    if (shown.isEmpty() && query.isNotBlank()) {
                        item(key = "none") { AppFilterEmpty(query, modifier = Modifier.padding(horizontal = 24.dp)) }
                    }
                }
                TextButton(onClick = onClear, modifier = Modifier.padding(horizontal = 12.dp).padding(top = 8.dp)) {
                    Text(clearLabel)
                }
            }
        }
    }
}

/** About eight and a half rows: enough to show there are more below. */
private val LIST_HEIGHT = 420.dp
private const val LONG_LIST = 8

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
