package app.minimise.settings.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.prefs.HomeOptions
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.ToggleRow
import timber.log.Timber

@Composable
fun HomeSettingsRoute(onOpenPermissions: () -> Unit, viewModel: HomeSettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    val askForLocation = { locationRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
    HomeSettingsScreen(
        state = state,
        onToggle = { change ->
            val before = state.options
            viewModel.onToggle(change)
            // Switching on weather or air quality asks for location straight away, rather than on home.
            val after = before?.let(change)
            if (after != null && after.usesLocation && !before.usesLocation && !state.grants.location) askForLocation()
        },
        onAllowLocation = askForLocation,
        onAllowNotificationAccess = {
            runCatching { context.startActivity(viewModel.notificationAccessIntent()) }
                .onFailure { Timber.w(it, "No notification access screen") }
        },
        onOpenPermissions = onOpenPermissions,
    )
}

@Composable
fun HomeSettingsScreen(
    state: HomeSettingsUiState,
    onToggle: ((HomeOptions) -> HomeOptions) -> Unit,
    onAllowLocation: () -> Unit,
    onAllowNotificationAccess: () -> Unit,
    onOpenPermissions: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.home_settings_title), scrollable = true) {
        Text(
            text = stringResource(R.string.home_settings_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        val options = state.options ?: return@ScreenScaffold
        val grants = state.grants

        ToggleRow(
            title = stringResource(R.string.home_option_lock),
            summary = stringResource(R.string.home_option_lock_summary),
            checked = options.lockButton,
            onToggle = { onToggle { it.copy(lockButton = !it.lockButton) } },
            note = if (!grants.accessibility) stringResource(R.string.home_option_needs_accessibility) else null,
            onNoteClick = onOpenPermissions,
        )
        ToggleRow(
            title = stringResource(R.string.home_option_shade),
            summary = stringResource(R.string.home_option_shade_summary),
            checked = options.swipeDownShade,
            onToggle = { onToggle { it.copy(swipeDownShade = !it.swipeDownShade) } },
        )
        ToggleRow(
            title = stringResource(R.string.home_option_status_bar),
            summary = stringResource(R.string.home_option_status_bar_summary),
            checked = options.hideStatusBar,
            onToggle = { onToggle { it.copy(hideStatusBar = !it.hideStatusBar) } },
        )
        ToggleRow(
            title = stringResource(R.string.home_option_battery),
            summary = stringResource(R.string.home_option_battery_summary),
            checked = options.battery,
            onToggle = { onToggle { it.copy(battery = !it.battery) } },
        )
        ToggleRow(
            title = stringResource(R.string.home_option_alarm),
            summary = stringResource(R.string.home_option_alarm_summary),
            checked = options.nextAlarm,
            onToggle = { onToggle { it.copy(nextAlarm = !it.nextAlarm) } },
        )
        ToggleRow(
            title = stringResource(R.string.home_option_music),
            summary = stringResource(R.string.home_option_music_summary),
            checked = options.music,
            onToggle = { onToggle { it.copy(music = !it.music) } },
            note = if (options.music && !grants.notificationAccess) stringResource(R.string.home_option_needs_notification_access) else null,
            onNoteClick = onAllowNotificationAccess,
        )
        ToggleRow(
            title = stringResource(R.string.home_option_weather),
            summary = stringResource(R.string.home_option_weather_summary),
            checked = options.weather,
            onToggle = { onToggle { it.copy(weather = !it.weather) } },
        )
        ToggleRow(
            title = stringResource(R.string.home_option_aqi),
            summary = stringResource(R.string.home_option_aqi_summary),
            checked = options.airQuality,
            onToggle = { onToggle { it.copy(airQuality = !it.airQuality) } },
            note = if (options.usesLocation && !grants.location) stringResource(R.string.home_option_needs_location) else null,
            onNoteClick = onAllowLocation,
        )
        Text(
            text = stringResource(R.string.home_option_internet_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Text(
            text = stringResource(R.string.home_gestures_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() },
        )
        Gesture(R.string.gesture_tap_app, R.string.gesture_tap_app_does)
        Gesture(R.string.gesture_long_press_app, R.string.gesture_long_press_app_does)
        Gesture(R.string.gesture_tap_task, R.string.gesture_tap_task_does)
        Gesture(R.string.gesture_long_press_task, R.string.gesture_long_press_task_does)
        Gesture(R.string.gesture_swipe_up, R.string.gesture_swipe_up_does)
        Gesture(R.string.gesture_swipe_down, if (options.swipeDownShade) R.string.gesture_swipe_down_does else R.string.gesture_off)
        Gesture(R.string.gesture_swipe_left, R.string.gesture_swipe_left_does)
        Gesture(R.string.gesture_swipe_right, R.string.gesture_swipe_right_does)
        Gesture(R.string.gesture_double_tap, R.string.gesture_double_tap_does)
        Gesture(R.string.gesture_long_press_clock, R.string.gesture_long_press_clock_does)
        Gesture(R.string.gesture_long_press_space, R.string.gesture_long_press_space_does)
    }
}

@Composable
private fun Gesture(gesture: Int, does: Int) {
    Column(modifier = Modifier.padding(vertical = 6.dp).semantics(mergeDescendants = true) {}) {
        Text(text = stringResource(gesture), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(does),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
