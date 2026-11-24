package app.minimise.apps.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import app.minimise.apps.domain.WebTarget
import app.minimise.core.ui.AppActionsDialog
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
        onAddWebsite = viewModel::onAddWebsite,
        onWebsiteEntered = viewModel::onWebsiteEntered,
        onWebsiteDismissed = viewModel::onWebsiteDismissed,
        onAppLongPressed = viewModel::onAppLongPressed,
        onAppInfo = viewModel::onAppInfo,
        onUninstall = viewModel::onUninstall,
        onActionsDismissed = viewModel::onActionsDismissed,
    )
}

@Composable
fun AppsScreen(
    state: AppsUiState,
    onKindChosen: (AppEntry, AppKind) -> Unit,
    onEditLimit: (AppEntry) -> Unit,
    onLimitChosen: (Int?) -> Unit,
    onLimitDismissed: () -> Unit,
    onAddWebsite: () -> Unit,
    onWebsiteEntered: (String) -> Unit,
    onWebsiteDismissed: () -> Unit,
    onAppLongPressed: (AppEntry) -> Unit = {},
    onAppInfo: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onActionsDismissed: () -> Unit = {},
) {
    ScreenScaffold(title = stringResource(R.string.apps_title)) {
        Text(
            text = stringResource(R.string.apps_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn {
            item(key = "websites") {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = stringResource(R.string.apps_websites), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.apps_websites_explainer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!state.websitesCovered) {
                        Text(
                            text = stringResource(R.string.needs_full_protection),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            items(state.websites, key = { it.app.packageName }) { row ->
                AppKindRow(row = row, kinds = WEBSITE_KINDS, onKindChosen = { onKindChosen(row.app, it) }, onEditLimit = {})
            }
            item(key = "add_website") {
                TextButton(onClick = onAddWebsite) { Text(stringResource(R.string.apps_add_website), style = MaterialTheme.typography.titleMedium) }
            }
            item(key = "apps") {
                Text(
                    text = stringResource(R.string.apps_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }
            items(state.rows, key = { it.app.packageName }) { row ->
                AppKindRow(
                    row = row,
                    onKindChosen = { onKindChosen(row.app, it) },
                    onEditLimit = { onEditLimit(row.app) },
                    onLongPress = { onAppLongPressed(row.app) },
                )
            }
        }
    }
    state.editingLimit?.let { app ->
        OpenLimitDialog(app = app, onChosen = onLimitChosen, onDismiss = onLimitDismissed)
    }
    if (state.addingWebsite) {
        AddWebsiteDialog(invalid = state.websiteInvalid, onEntered = onWebsiteEntered, onDismiss = onWebsiteDismissed)
    }
    state.actionsFor?.let { app ->
        AppActionsDialog(label = app.label, onAppInfo = onAppInfo, onUninstall = onUninstall, onDismiss = onActionsDismissed)
    }
}

/** From least to most strict, as they read best left to right. */
private val APP_KINDS = listOf(AppKind.TOOL, AppKind.BREATHE, AppKind.VAULT, AppKind.HIDDEN)

/**
 * Hidden means nothing for a website, which never shows in the launcher anyway, and the browser rules
 * know only the full Vault.
 */
private val WEBSITE_KINDS = listOf(AppKind.TOOL, AppKind.VAULT)

@Composable
private fun AppKindRow(
    row: AppRow,
    onKindChosen: (AppKind) -> Unit,
    onEditLimit: () -> Unit,
    kinds: List<AppKind> = APP_KINDS,
    onLongPress: (() -> Unit)? = null,
) {
    val app = row.app
    val isWebsite = WebTarget.isWebTarget(app.packageName)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            modifier = if (onLongPress == null) {
                Modifier
            } else {
                Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress,
                    onLongClickLabel = stringResource(R.string.a11y_app_actions),
                )
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 4.dp)) {
            kinds.forEach { kind ->
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
        if (app.kind == AppKind.VAULT && !isWebsite) {
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
    AppKind.BREATHE -> R.string.app_kind_breathe
}
