package app.minimise.apps.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.durationText

@Composable
fun AppsRoute(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppsScreen(
        state = state,
        onKindChosen = viewModel::onKindChosen,
        onEditLimit = viewModel::onEditLimit,
        onLimitChosen = viewModel::onLimitChosen,
        onLimitDismissed = viewModel::onLimitDismissed,
    )
}

@Composable
fun AppsScreen(
    state: AppsUiState,
    onKindChosen: (AppEntry, AppKind) -> Unit,
    onEditLimit: (AppEntry) -> Unit,
    onLimitChosen: (Int?) -> Unit,
    onLimitDismissed: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.apps_title)) {
        Text(
            text = stringResource(R.string.apps_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn {
            items(state.rows, key = { it.app.packageName }) { row ->
                AppKindRow(row = row, onKindChosen = { onKindChosen(row.app, it) }, onEditLimit = { onEditLimit(row.app) })
            }
        }
    }
    state.editingLimit?.let { app ->
        OpenLimitDialog(app = app, onChosen = onLimitChosen, onDismiss = onLimitDismissed)
    }
}

@Composable
private fun AppKindRow(row: AppRow, onKindChosen: (AppKind) -> Unit, onEditLimit: () -> Unit) {
    val app = row.app
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(text = app.label, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 4.dp)) {
            AppKind.entries.forEach { kind ->
                KindChoice(kind = kind, selected = app.kind == kind, onClick = { onKindChosen(kind) })
            }
        }
        row.pending?.let { pending ->
            Text(
                text = stringResource(
                    R.string.apps_pending_move,
                    stringResource(pending.kind.labelRes()),
                    durationText(pending.remaining),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (app.kind == AppKind.VAULT) {
            Text(
                text = app.openLimit?.let { stringResource(R.string.apps_open_limit, it) }
                    ?: stringResource(R.string.apps_open_limit_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onEditLimit).padding(vertical = 6.dp),
            )
        }
        row.pendingLimit?.let { pending ->
            Text(
                text = pending.limit
                    ?.let { stringResource(R.string.apps_pending_limit, it, durationText(pending.remaining)) }
                    ?: stringResource(R.string.apps_pending_limit_none, durationText(pending.remaining)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KindChoice(kind: AppKind, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = stringResource(kind.labelRes()),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
    )
}

fun AppKind.labelRes(): Int = when (this) {
    AppKind.TOOL -> R.string.app_kind_tool
    AppKind.VAULT -> R.string.app_kind_vault
    AppKind.HIDDEN -> R.string.app_kind_hidden
}
