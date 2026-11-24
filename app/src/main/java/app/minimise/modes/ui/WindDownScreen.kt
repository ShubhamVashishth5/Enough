package app.minimise.modes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.AppPickerDialog
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.TimeDialog
import app.minimise.core.ui.durationText
import app.minimise.core.ui.timeText
import app.minimise.settings.domain.GuardResult
import kotlin.time.Duration

@Composable
fun WindDownRoute(viewModel: WindDownViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenScaffold(title = stringResource(R.string.wind_down_title)) {
        Text(
            text = stringResource(R.string.wind_down_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (!state.loaded) return@ScreenScaffold
        Text(
            text = stringResource(if (state.on) R.string.wind_down_on else R.string.wind_down_off),
            style = MaterialTheme.typography.titleMedium,
        )
        SettingsRow(title = stringResource(R.string.wind_down_bedtime), summary = timeText(state.draft.bedtime), onClick = { picking = BEDTIME })
        SettingsRow(title = stringResource(R.string.wind_down_wake), summary = timeText(state.draft.wake), onClick = { picking = WAKE })
        SettingsRow(
            title = stringResource(R.string.wind_down_ebook),
            summary = state.ebookLabel ?: stringResource(R.string.wind_down_ebook_none),
            onClick = { picking = EBOOK },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
            OutlinedButton(onClick = viewModel::onSave) { Text(stringResource(if (state.on) R.string.wind_down_save else R.string.wind_down_start)) }
            if (state.on) TextButton(onClick = viewModel::onTurnOff) { Text(stringResource(R.string.wind_down_stop)) }
        }
        when {
            state.result is GuardResult.Scheduled || state.pendingIn != null -> Text(
                text = stringResource(R.string.wind_down_pending, durationText(state.pendingIn ?: Duration.ZERO)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            state.result == GuardResult.AppliedNow -> Text(
                text = stringResource(R.string.wind_down_saved),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
    when (picking) {
        BEDTIME -> TimeDialog(state.draft.bedtime, onPicked = { viewModel.onBedtime(it); picking = null }, onDismiss = { picking = null })
        WAKE -> TimeDialog(state.draft.wake, onPicked = { viewModel.onWake(it); picking = null }, onDismiss = { picking = null })
        EBOOK -> AppPickerDialog(
            choices = state.toolApps,
            onPick = { viewModel.onEbook(it.packageName); picking = null },
            onClear = { viewModel.onEbook(null); picking = null },
            onDismiss = { picking = null },
        )
    }
}

private const val BEDTIME = "bedtime"
private const val WAKE = "wake"
private const val EBOOK = "ebook"
