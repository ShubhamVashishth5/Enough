package app.minimise.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow

@Composable
fun SettingsScreen(onOpenApps: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.settings_title)) {
        SettingsRow(
            title = stringResource(R.string.settings_apps),
            summary = stringResource(R.string.settings_apps_summary),
            onClick = onOpenApps,
        )
    }
}
