package app.minimise.modes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.durationText
import app.minimise.core.ui.windowText
import app.minimise.modes.domain.Mode

@Composable
fun ModesRoute(onEdit: (Long) -> Unit, viewModel: ModesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.modes_title)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.modes_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            state.pendingSwitch?.let { pending ->
                val target = state.modes.firstOrNull { it.id == pending.modeId }?.name ?: stringResource(R.string.modes_follow)
                Text(
                    text = stringResource(R.string.modes_switch_pending, target, durationText(pending.remaining)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            ModeRow(
                title = stringResource(R.string.modes_follow),
                summary = stringResource(R.string.modes_follow_summary),
                selected = state.manualId == null,
                onUse = { viewModel.onUse(null) },
                onClick = { viewModel.onUse(null) },
            )
            state.modes.forEach { mode ->
                ModeRow(
                    title = if (mode.id == state.activeId) stringResource(R.string.modes_now, mode.name) else mode.name,
                    summary = listOfNotNull(
                        modeSummary(mode),
                        state.pendingEdits[mode.id]?.let { stringResource(R.string.schedule_changes_in, durationText(it)) },
                    ).joinToString("\n"),
                    selected = state.manualId == mode.id,
                    onUse = { viewModel.onUse(mode.id) },
                    onClick = { onEdit(mode.id) },
                )
            }
        }
    }
}

@Composable
private fun ModeRow(title: String, summary: String, selected: Boolean, onUse: () -> Unit, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        SettingsRow(title = title, summary = summary, onClick = onClick, modifier = Modifier.weight(1f))
        if (!selected) TextButton(onClick = onUse) { Text(stringResource(R.string.modes_use)) }
    }
}

@Composable
private fun modeSummary(mode: Mode): String {
    val budget = stringResource(R.string.modes_budget, mode.values.budget.inWholeMinutes)
    val schedule = mode.schedule?.let { windowText(it) } ?: stringResource(R.string.modes_no_schedule)
    val apps = mode.values.allowlist?.let { pluralStringResource(R.plurals.modes_only_apps, it.size, it.size) }
    return listOfNotNull(budget, schedule, apps).joinToString(" · ")
}
