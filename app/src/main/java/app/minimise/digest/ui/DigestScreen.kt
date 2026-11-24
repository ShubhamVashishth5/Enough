package app.minimise.digest.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold

/** The updates a digest released, grouped by app. */
@Composable
fun DigestRoute(extra: @Composable () -> Unit = {}, viewModel: DigestViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.digest_title)) {
        if (state.loaded && state.groups.isEmpty()) {
            Text(stringResource(R.string.digest_empty), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (state.waiting > 0) {
            Text(
                text = pluralStringResource(R.plurals.home_updates_waiting, state.waiting, state.waiting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        extra()
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            state.groups.forEach { group ->
                item(key = "app:${group.packageName}") {
                    Text(group.appLabel, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                }
                items(group.items, key = { it.id }) { item ->
                    Column(modifier = Modifier.fillMaxWidth().clickable { viewModel.onOpen(item) }.padding(vertical = 8.dp)) {
                        if (item.title.isNotBlank()) Text(item.title, style = MaterialTheme.typography.titleSmall)
                        if (item.text.isNotBlank()) {
                            Text(item.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        }
                    }
                }
            }
        }
        if (state.groups.isNotEmpty()) {
            TextButton(onClick = viewModel::onClear) { Text(stringResource(R.string.digest_clear), style = MaterialTheme.typography.titleMedium) }
        }
    }
}
