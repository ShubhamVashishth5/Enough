package app.minimise.apps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.minimise.apps.domain.AppFilter
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.WebTarget
import app.minimise.core.Defaults
import app.minimise.core.ui.AppActionsDialog
import app.minimise.core.ui.AppFilterEmpty
import app.minimise.core.ui.appFilterHeader
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.durationText
import kotlin.time.Duration

@Composable
fun AppsRoute(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppsScreen(
        state = state,
        onKindChosen = viewModel::onKindChosen,
        onEditLimit = viewModel::onEditLimit,
        onLimitChosen = viewModel::onLimitChosen,
        onLimitDismissed = viewModel::onLimitDismissed,
        onEditBreatheWait = viewModel::onEditBreatheWait,
        onBreatheWaitChosen = viewModel::onBreatheWaitChosen,
        onBreatheWaitDismissed = viewModel::onBreatheWaitDismissed,
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
    onEditBreatheWait: (AppEntry) -> Unit = {},
    onBreatheWaitChosen: (Duration?) -> Unit = {},
    onBreatheWaitDismissed: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    // Only apps of one kind, or every app when null.
    var kind by rememberSaveable { mutableStateOf<AppKind?>(null) }
    val searching = query.isNotBlank()
    val narrowed = searching || kind != null
    val websites = remember(state.websites, query, kind) { AppFilter.filter(state.websites, query) { it.app.label }.ofKind(kind) }
    val rows = remember(state.rows, query, kind) { AppFilter.filter(state.rows, query) { it.app.label }.ofKind(kind) }
    val counts = remember(state.rows, state.websites) { (state.rows + state.websites).groupingBy { it.app.kind }.eachCount() }
    val total = state.rows.size + state.websites.size
    val listState = rememberLazyListState()
    val searchHint = stringResource(R.string.app_filter_hint_websites)
    ScreenScaffold(title = stringResource(R.string.apps_title)) {
        LazyColumn(state = listState) {
            // The explainer steps aside while searching or filtering, so the list sits right under the box.
            if (!narrowed) item(key = "explainer") {
                Text(
                    text = stringResource(R.string.apps_explainer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            appFilterHeader(query = query, onQueryChange = { query = it }, listState = listState, hint = searchHint) {
                KindFilter(selected = kind, counts = counts, total = total, onSelect = { kind = it })
            }
            // While searching or filtering, a section shows only if something in it is left.
            if (!narrowed || websites.isNotEmpty()) item(key = "websites") {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = stringResource(R.string.apps_websites), style = MaterialTheme.typography.titleLarge)
                    // Just the title while searching or filtering, to keep the list together.
                    if (!narrowed) {
                        Text(
                            text = stringResource(R.string.apps_websites_explainer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!narrowed && !state.websitesCovered) {
                        Text(
                            text = stringResource(R.string.needs_full_protection),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            items(websites, key = { it.app.packageName }) { row ->
                AppKindRow(row = row, kinds = WEBSITE_KINDS, onKindChosen = { onKindChosen(row.app, it) }, onEditLimit = {})
            }
            if (!narrowed) item(key = "add_website") {
                TextButton(onClick = onAddWebsite) { Text(stringResource(R.string.apps_add_website), style = MaterialTheme.typography.titleMedium) }
            }
            if (!narrowed || rows.isNotEmpty()) item(key = "apps") {
                Text(
                    text = stringResource(R.string.apps_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
            }
            items(rows, key = { it.app.packageName }) { row ->
                AppKindRow(
                    row = row,
                    onKindChosen = { onKindChosen(row.app, it) },
                    onEditLimit = { onEditLimit(row.app) },
                    onEditBreatheWait = { onEditBreatheWait(row.app) },
                    onLongPress = { onAppLongPressed(row.app) },
                )
            }
            if (narrowed && websites.isEmpty() && rows.isEmpty()) {
                item(key = "none") {
                    if (searching) {
                        AppFilterEmpty(query)
                    } else {
                        Text(
                            text = stringResource(R.string.apps_filter_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
    state.editingLimit?.let { app ->
        OpenLimitDialog(app = app, onChosen = onLimitChosen, onDismiss = onLimitDismissed)
    }
    state.editingBreatheWait?.let { app ->
        BreatheWaitDialog(app = app, onChosen = onBreatheWaitChosen, onDismiss = onBreatheWaitDismissed)
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

private fun List<AppRow>.ofKind(kind: AppKind?): List<AppRow> = if (kind == null) this else filter { it.app.kind == kind }

/** The filters' order: the kinds people look for most first, so they show without scrolling sideways. */
private val FILTER_KINDS = listOf(AppKind.VAULT, AppKind.BREATHE, AppKind.TOOL, AppKind.HIDDEN)

/**
 * All, or just the Vault, Breathe, Tool or Hidden apps, each with how many there are. A row of small
 * pills that scrolls sideways when the words don't fit; the chosen one is filled.
 */
@Composable
private fun KindFilter(selected: AppKind?, counts: Map<AppKind, Int>, total: Int, onSelect: (AppKind?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp)
            .selectableGroup(),
    ) {
        FilterPill(label = stringResource(R.string.apps_filter_all), count = total, chosen = selected == null, onClick = { onSelect(null) })
        FILTER_KINDS.forEach { kind ->
            FilterPill(
                label = stringResource(kind.filterLabelRes()),
                count = counts[kind] ?: 0,
                chosen = selected == kind,
                onClick = { onSelect(kind) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, count: Int, chosen: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (chosen) {
                    Modifier.background(MaterialTheme.colorScheme.onSurface)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                },
            )
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.apps_filter_count, label, count),
            style = MaterialTheme.typography.bodyMedium,
            color = if (chosen) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun AppKind.filterLabelRes(): Int = when (this) {
    AppKind.TOOL -> R.string.apps_filter_tool
    AppKind.VAULT -> R.string.app_kind_vault
    AppKind.HIDDEN -> R.string.app_kind_hidden
    AppKind.BREATHE -> R.string.app_kind_breathe
}

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
    onEditBreatheWait: () -> Unit = {},
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
        if (app.kind == AppKind.BREATHE && !isWebsite) {
            Text(
                text = app.breatheWait?.let { stringResource(R.string.apps_breathe_wait, it.inWholeSeconds) }
                    ?: stringResource(R.string.apps_breathe_wait_usual, Defaults.WAIT_STEPS.first().inWholeSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onEditBreatheWait).padding(vertical = 6.dp),
            )
        }
        row.pendingBreatheWait?.let { pending ->
            Text(
                text = pending.seconds
                    ?.let { stringResource(R.string.apps_pending_breathe_wait, it, durationText(pending.remaining)) }
                    ?: stringResource(R.string.apps_pending_breathe_wait_usual, durationText(pending.remaining)),
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
