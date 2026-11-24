package app.minimise.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow

@Composable
fun SettingsRoute(
    onOpenApps: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSwaps: () -> Unit,
    onOpenPendingChanges: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenDebugUsage: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onOpenApps = onOpenApps,
        onOpenBudget = onOpenBudget,
        onOpenSwaps = onOpenSwaps,
        onOpenPendingChanges = onOpenPendingChanges,
        onOpenPermissions = onOpenPermissions,
        onOpenDebugUsage = onOpenDebugUsage,
        onDebugShortLoosenDelayToggled = viewModel::onDebugShortLoosenDelayToggled,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenApps: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSwaps: () -> Unit,
    onOpenPendingChanges: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenDebugUsage: () -> Unit,
    onDebugShortLoosenDelayToggled: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.settings_title)) {
        SettingsRow(
            title = stringResource(R.string.settings_apps),
            summary = stringResource(R.string.settings_apps_summary),
            onClick = onOpenApps,
        )
        SettingsRow(
            title = stringResource(R.string.settings_budget),
            summary = state.budgetMinutes?.let { stringResource(R.string.settings_budget_summary, it) },
            onClick = onOpenBudget,
        )
        SettingsRow(
            title = stringResource(R.string.settings_swaps),
            summary = stringResource(R.string.settings_swaps_summary),
            onClick = onOpenSwaps,
        )
        SettingsRow(
            title = stringResource(R.string.settings_pending),
            summary = if (state.pendingCount == 0) {
                stringResource(R.string.settings_pending_none)
            } else {
                pluralStringResource(R.plurals.settings_pending_count, state.pendingCount, state.pendingCount)
            },
            onClick = onOpenPendingChanges,
        )
        SettingsRow(
            title = stringResource(R.string.settings_permissions),
            summary = stringResource(R.string.settings_permissions_summary),
            onClick = onOpenPermissions,
        )
        if (state.showDebug) {
            SettingsRow(
                title = stringResource(R.string.debug_usage_title),
                onClick = onOpenDebugUsage,
            )
            SettingsRow(
                title = stringResource(R.string.debug_short_loosen_delay),
                summary = stringResource(if (state.debugShortLoosenDelay) R.string.debug_on else R.string.debug_off),
                onClick = onDebugShortLoosenDelayToggled,
            )
        }
    }
}
