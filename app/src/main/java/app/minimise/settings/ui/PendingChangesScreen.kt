package app.minimise.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.apps.ui.labelRes
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.durationText

@Composable
fun PendingChangesRoute(viewModel: PendingChangesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PendingChangesScreen(state = state, onCancel = viewModel::onCancel)
}

@Composable
fun PendingChangesScreen(
    state: PendingChangesUiState,
    onCancel: (PendingItem) -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.pending_title)) {
        Text(
            text = stringResource(R.string.pending_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (state.items.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_pending_none),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        LazyColumn {
            items(state.items, key = { it.id }) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = describe(item.description), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.pending_applies_in, durationText(item.remaining)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onCancel(item) }) { Text(stringResource(R.string.pending_cancel)) }
                }
            }
        }
    }
}

@Composable
private fun describe(description: PendingDescription): String = when (description) {
    is PendingDescription.AppKindChange ->
        stringResource(R.string.pending_app_kind, description.appLabel, stringResource(description.kind.labelRes()))
    is PendingDescription.BudgetChange -> stringResource(R.string.pending_budget, description.minutes)
    is PendingDescription.OpenLimitChange -> description.limit
        ?.let { stringResource(R.string.pending_open_limit, description.appLabel, it) }
        ?: stringResource(R.string.pending_open_limit_none, description.appLabel)
}
