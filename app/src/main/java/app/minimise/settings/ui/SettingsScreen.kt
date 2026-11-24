package app.minimise.settings.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.backup.data.BackupImporter
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.dateTimeText
import app.minimise.display.domain.GrayscaleMode

@Composable
fun SettingsRoute(
    onOpenApps: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSwaps: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenModes: () -> Unit,
    onOpenDigest: () -> Unit,
    onOpenFeeds: () -> Unit,
    onOpenLock: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenWindDown: () -> Unit,
    onOpenPrivateSpace: () -> Unit,
    onOpenFriendKey: () -> Unit,
    onOpenPendingChanges: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenDebugUsage: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onExport(it) }
    }
    val backupFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.onBackup(it) }
    }
    val restoreFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onRestore(it) }
    }
    LaunchedEffect(state.backupMessage) {
        state.backupMessage?.let { message ->
            Toast.makeText(context, backupMessageText(context, message), Toast.LENGTH_LONG).show()
            viewModel.onBackupMessageShown()
        }
    }
    LaunchedEffect(state.exported) {
        state.exported?.let { count ->
            val text = if (count >= 0) context.resources.getQuantityString(R.plurals.export_done, count, count) else context.getString(R.string.export_failed)
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.onExportShown()
        }
    }
    SettingsScreen(
        state = state,
        onOpenApps = onOpenApps,
        onOpenHome = onOpenHome,
        onOpenBudget = onOpenBudget,
        onOpenSwaps = onOpenSwaps,
        onOpenSchedules = onOpenSchedules,
        onOpenKeys = onOpenKeys,
        onOpenFocus = onOpenFocus,
        onOpenModes = onOpenModes,
        onOpenDigest = onOpenDigest,
        onOpenFeeds = onOpenFeeds,
        onOpenLock = onOpenLock,
        onOpenStats = onOpenStats,
        onOpenWindDown = onOpenWindDown,
        onOpenPrivateSpace = onOpenPrivateSpace,
        onExport = { exportFile.launch(EXPORT_FILE_NAME) },
        onBackup = { backupFile.launch(BACKUP_FILE_NAME) },
        onRestore = { restoreFile.launch(arrayOf("application/json", "*/*")) },
        onOpenFriendKey = onOpenFriendKey,
        onOpenPendingChanges = onOpenPendingChanges,
        onOpenPermissions = onOpenPermissions,
        onOpenDebugUsage = onOpenDebugUsage,
        onGrayscaleTapped = viewModel::onGrayscaleTapped,
        onDebugShortLoosenDelayToggled = viewModel::onDebugShortLoosenDelayToggled,
        onDebugDumpScreen = viewModel::onDebugDumpScreen,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenApps: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSwaps: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenModes: () -> Unit,
    onOpenDigest: () -> Unit,
    onOpenFeeds: () -> Unit,
    onOpenLock: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenWindDown: () -> Unit,
    onOpenPrivateSpace: () -> Unit,
    onOpenFriendKey: () -> Unit,
    onOpenPendingChanges: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenDebugUsage: () -> Unit,
    onGrayscaleTapped: () -> Unit,
    onDebugShortLoosenDelayToggled: () -> Unit,
    onDebugDumpScreen: () -> Unit,
    onExport: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.settings_title), scrollable = true) {
        SettingsRow(
            title = stringResource(R.string.settings_apps),
            summary = stringResource(R.string.settings_apps_summary),
            onClick = onOpenApps,
        )
        SettingsRow(
            title = stringResource(R.string.home_settings_title),
            summary = stringResource(R.string.settings_home_summary),
            onClick = onOpenHome,
        )
        // Hidden below Android 15 and when no Private space is set up.
        if (state.privateSpace != null) {
            SettingsRow(
                title = stringResource(R.string.private_space_title),
                summary = stringResource(if (state.privateSpace) R.string.private_space_locked else R.string.private_space_unlocked),
                onClick = onOpenPrivateSpace,
            )
        }
        SettingsRow(
            title = stringResource(R.string.settings_budget),
            summary = state.budgetMinutes?.let { stringResource(R.string.settings_budget_summary, it) },
            onClick = onOpenBudget,
        )
        SettingsRow(
            title = stringResource(R.string.settings_modes),
            summary = state.activeModeName?.let { stringResource(R.string.settings_modes_now, it) }
                ?: stringResource(R.string.settings_modes_none),
            onClick = onOpenModes,
        )
        SettingsRow(
            title = stringResource(R.string.wind_down_title),
            summary = stringResource(R.string.settings_wind_down_summary),
            onClick = onOpenWindDown,
        )
        SettingsRow(
            title = stringResource(R.string.settings_schedules),
            summary = stringResource(R.string.settings_schedules_summary),
            onClick = onOpenSchedules,
        )
        SettingsRow(
            title = stringResource(R.string.settings_focus),
            summary = stringResource(R.string.settings_focus_summary),
            onClick = onOpenFocus,
        )
        SettingsRow(
            title = stringResource(R.string.settings_keys),
            summary = stringResource(R.string.settings_keys_summary),
            onClick = onOpenKeys,
        )
        SettingsRow(
            title = stringResource(R.string.settings_feeds),
            summary = if (state.screensCovered) stringResource(R.string.settings_feeds_summary) else stringResource(R.string.needs_full_protection),
            onClick = onOpenFeeds,
        )
        val lockSummary = state.lockedUntil?.let { stringResource(R.string.lock_locked_until, dateTimeText(it)) }
            ?: stringResource(R.string.settings_lock_summary)
        SettingsRow(
            title = stringResource(R.string.lock_title),
            summary = if (state.screensCovered) lockSummary else lockSummary + "\n" + stringResource(R.string.lock_guard_needs_full_protection),
            onClick = onOpenLock,
        )
        SettingsRow(
            title = stringResource(R.string.friend_key_title),
            summary = stringResource(R.string.settings_friend_key_summary),
            onClick = onOpenFriendKey,
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
            title = stringResource(R.string.settings_digest),
            summary = stringResource(R.string.settings_digest_summary),
            onClick = onOpenDigest,
        )
        SettingsRow(
            title = stringResource(R.string.settings_grayscale),
            summary = if (state.grayscaleAvailable) {
                val mode = stringResource(
                    when (state.grayscaleMode) {
                        GrayscaleMode.OFF -> R.string.grayscale_off
                        GrayscaleMode.VAULT_APPS -> R.string.grayscale_vault
                        GrayscaleMode.ALWAYS -> R.string.grayscale_always
                    },
                )
                stringResource(R.string.grayscale_summary, mode)
            } else {
                stringResource(R.string.grayscale_needs_grant)
            },
            onClick = if (state.grayscaleAvailable) onGrayscaleTapped else onOpenPermissions,
        )
        SettingsRow(
            title = stringResource(R.string.stats_title),
            summary = stringResource(R.string.settings_stats_summary),
            onClick = onOpenStats,
        )
        SettingsRow(
            title = stringResource(R.string.settings_export),
            summary = stringResource(R.string.settings_export_summary),
            onClick = onExport,
        )
        SettingsRow(
            title = stringResource(R.string.settings_backup),
            summary = stringResource(R.string.settings_backup_summary),
            onClick = onBackup,
        )
        SettingsRow(
            title = stringResource(R.string.settings_restore),
            summary = stringResource(R.string.settings_restore_summary),
            onClick = onRestore,
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
            SettingsRow(
                title = stringResource(R.string.debug_dump_screen),
                summary = stringResource(R.string.debug_dump_screen_summary),
                onClick = onDebugDumpScreen,
            )
        }
    }
}

private const val EXPORT_FILE_NAME = "enough-events.csv"
private const val BACKUP_FILE_NAME = "enough-settings.json"

private fun backupMessageText(context: android.content.Context, message: BackupMessage): String = when (message) {
    BackupMessage.Saved -> context.getString(R.string.backup_saved)
    BackupMessage.SaveFailed -> context.getString(R.string.export_failed)
    is BackupMessage.Restored -> when (val result = message.result) {
        is BackupImporter.Result.TooNew -> context.getString(R.string.restore_too_new)
        BackupImporter.Result.Unreadable -> context.getString(R.string.restore_unreadable)
        is BackupImporter.Result.Restored -> when {
            result.refused > 0 -> context.getString(R.string.restore_locked)
            result.waiting > 0 -> context.resources.getQuantityString(R.plurals.restore_waiting, result.waiting, result.waiting)
            else -> context.getString(R.string.restore_done)
        }
    }
}
