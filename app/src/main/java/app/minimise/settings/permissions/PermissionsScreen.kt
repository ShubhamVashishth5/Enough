package app.minimise.settings.permissions

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.protection.domain.ProtectionLevel
import timber.log.Timber

@Composable
fun PermissionsRoute(viewModel: PermissionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onResume()
    }
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onResume()
    }
    val calendarRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onResume()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    fun open(intent: Intent, fallback: Intent? = null) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "No screen for %s", intent.action)
            fallback?.let { runCatching { context.startActivity(it) } }
        }
    }

    // Play's accessibility policy: say what's read and why, and get a yes, before the system setting.
    var disclosing by rememberSaveable { mutableStateOf(false) }
    if (disclosing) {
        AccessibilityDisclosure(
            onAgree = {
                disclosing = false
                open(viewModel.accessibilityIntent())
            },
            onDecline = { disclosing = false },
        )
    }

    PermissionsScreen(
        state = state,
        onOpenAccessibility = { disclosing = true },
        onOpenAppInfo = { open(viewModel.appInfoIntent()) },
        onRequestUsageAccess = { open(viewModel.usageAccessIntent()) },
        onRequestOverlay = { open(viewModel.overlayIntent(), fallback = viewModel.appInfoIntent()) },
        onRequestHome = {
            runCatching { roleRequest.launch(viewModel.homeRoleIntent()) }
                .onFailure { open(viewModel.homeSettingsIntent()) }
        },
        onRequestBattery = { open(viewModel.batteryIntent(), fallback = viewModel.appInfoIntent()) },
        onRequestDoNotDisturb = { open(viewModel.doNotDisturbIntent()) },
        onRequestNotificationAccess = { open(viewModel.notificationAccessIntent()) },
        onRequestCalendar = { calendarRequest.launch(Manifest.permission.READ_CALENDAR) },
        onRequestDeviceAdmin = { open(viewModel.deviceAdminIntent()) },
        onRequestPostNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                open(viewModel.appNotificationSettingsIntent())
            }
        },
    )
}

@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    onOpenAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestHome: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestDoNotDisturb: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onRequestCalendar: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.permissions_title)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(
                    when (state.protection) {
                        ProtectionLevel.FULL -> R.string.protection_level_full
                        ProtectionLevel.LITE -> R.string.protection_level_lite
                        ProtectionLevel.LAUNCHER_ONLY -> R.string.protection_level_launcher_only
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            PermissionItem(
                title = stringResource(R.string.permission_accessibility),
                why = stringResource(R.string.permission_accessibility_why),
                granted = state.accessibility,
                action = stringResource(R.string.permission_open_settings),
                onAction = onOpenAccessibility,
            )
            if (state.advancedProtection) {
                Text(
                    text = stringResource(R.string.permission_advanced_protection),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (!state.accessibility) {
                Text(
                    text = stringResource(R.string.permission_accessibility_restricted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.permission_open_app_info))
                }
            }
            PermissionItem(
                title = stringResource(R.string.permission_usage_access),
                why = stringResource(R.string.permission_usage_access_why),
                granted = state.usageAccess,
                action = stringResource(R.string.permission_open_settings),
                onAction = onRequestUsageAccess,
            )
            PermissionItem(
                title = stringResource(R.string.permission_overlay),
                why = stringResource(R.string.permission_overlay_why),
                granted = state.overlay,
                action = stringResource(R.string.permission_allow),
                onAction = onRequestOverlay,
            )
            PermissionItem(
                title = stringResource(R.string.permission_home),
                why = stringResource(R.string.permission_home_why),
                granted = state.defaultHome,
                action = stringResource(R.string.permission_make_default),
                onAction = onRequestHome,
            )
            PermissionItem(
                title = stringResource(R.string.permission_battery),
                why = stringResource(R.string.permission_battery_why),
                granted = state.batteryUnrestricted,
                action = stringResource(R.string.permission_allow),
                onAction = onRequestBattery,
            )
            PermissionItem(
                title = stringResource(R.string.permission_notification_access),
                why = stringResource(R.string.permission_notification_access_why),
                granted = state.notificationAccess,
                action = stringResource(R.string.permission_open_settings),
                onAction = onRequestNotificationAccess,
            )
            PermissionItem(
                title = stringResource(R.string.permission_post_notifications),
                why = stringResource(R.string.permission_post_notifications_why),
                granted = state.postNotifications,
                action = stringResource(R.string.permission_allow),
                onAction = onRequestPostNotifications,
            )
            PermissionItem(
                title = stringResource(R.string.permission_dnd),
                why = stringResource(R.string.permission_dnd_why),
                granted = state.doNotDisturb,
                action = stringResource(R.string.permission_allow),
                onAction = onRequestDoNotDisturb,
            )
            PermissionItem(
                title = stringResource(R.string.permission_calendar),
                why = stringResource(R.string.permission_calendar_why),
                granted = state.calendar,
                action = stringResource(R.string.permission_allow),
                onAction = onRequestCalendar,
            )
            PermissionItem(
                title = stringResource(R.string.permission_device_admin),
                why = stringResource(R.string.permission_device_admin_why),
                granted = state.deviceAdmin,
                action = stringResource(R.string.permission_allow),
                onAction = onRequestDeviceAdmin,
            )
            GrayscaleItem(granted = state.grayscale, command = state.grayscaleCommand)
        }
    }
}

/**
 * The prominent disclosure Google Play asks for before an app sends anyone to turn on its accessibility
 * service: what is read, what it's used for, that nothing leaves the phone, and an explicit choice.
 */
@Composable
private fun AccessibilityDisclosure(onAgree: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(stringResource(R.string.disclosure_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                listOf(
                    R.string.disclosure_intro,
                    R.string.disclosure_apps,
                    R.string.disclosure_screen,
                    R.string.disclosure_actions,
                    R.string.disclosure_private,
                ).forEach { line ->
                    Text(text = stringResource(line), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onAgree) { Text(stringResource(R.string.disclosure_agree)) } },
        dismissButton = { TextButton(onClick = onDecline) { Text(stringResource(R.string.disclosure_decline)) } },
    )
}

/** Can't be granted from the phone: show the ADB command to run once from a computer. */
@Composable
private fun GrayscaleItem(granted: Boolean, command: String) {
    val clipboard = LocalClipboardManager.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(text = stringResource(R.string.permission_grayscale), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(if (granted) R.string.permission_granted else R.string.permission_missing),
            style = MaterialTheme.typography.titleSmall,
            color = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.permission_grayscale_why),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (!granted) {
            SelectionContainer {
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(command)) }, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.permission_copy_command))
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    why: String,
    granted: Boolean,
    action: String,
    onAction: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(if (granted) R.string.permission_granted else R.string.permission_missing),
            style = MaterialTheme.typography.titleSmall,
            color = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = why,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (!granted) {
            OutlinedButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(action) }
        }
    }
}
