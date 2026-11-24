package app.minimise.settings.permissions

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import timber.log.Timber

@Composable
fun PermissionsRoute(viewModel: PermissionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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

    PermissionsScreen(
        state = state,
        onOpenAccessibility = { open(viewModel.accessibilityIntent()) },
        onOpenAppInfo = { open(viewModel.appInfoIntent()) },
        onRequestHome = {
            runCatching { roleRequest.launch(viewModel.homeRoleIntent()) }
                .onFailure { open(viewModel.homeSettingsIntent()) }
        },
        onRequestBattery = { open(viewModel.batteryIntent(), fallback = viewModel.appInfoIntent()) },
    )
}

@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    onOpenAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestHome: () -> Unit,
    onRequestBattery: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.permissions_title)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            PermissionItem(
                title = stringResource(R.string.permission_accessibility),
                why = stringResource(R.string.permission_accessibility_why),
                granted = state.accessibility,
                action = stringResource(R.string.permission_open_settings),
                onAction = onOpenAccessibility,
            )
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
