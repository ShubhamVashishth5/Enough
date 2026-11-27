package app.minimise.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import app.minimise.core.ui.SettingsRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.durationText
import app.minimise.core.ui.timeText
import app.minimise.settings.domain.GuardResult

@Composable
fun BudgetRoute(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetScreen(
        state = state,
        onStep = viewModel::onStep,
        onSave = viewModel::onSave,
        onTaperStep = viewModel::onTaperStep,
        onTaperSave = viewModel::onTaperSave,
        onTaperStop = viewModel::onTaperStop,
        onEarnItTapped = viewModel::onEarnItTapped,
        onDayStartStep = viewModel::onDayStartStep,
        onDayStartSave = viewModel::onDayStartSave,
    )
}

@Composable
fun BudgetScreen(
    state: BudgetUiState,
    onStep: (Int) -> Unit,
    onSave: () -> Unit,
    onTaperStep: (Int) -> Unit = {},
    onTaperSave: () -> Unit = {},
    onTaperStop: () -> Unit = {},
    onEarnItTapped: () -> Unit = {},
    onDayStartStep: (Int) -> Unit = {},
    onDayStartSave: () -> Unit = {},
) {
    ScreenScaffold(title = stringResource(R.string.budget_title), modifier = Modifier.verticalScroll(rememberScrollState())) {
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = stringResource(R.string.budget_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
        )
        val draft = state.draftMinutes ?: return@ScreenScaffold
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 24.dp),
        ) {
            OutlinedButton(onClick = { onStep(-BudgetViewModel.STEP_MINUTES) }) { Text("−") }
            Text(text = stringResource(R.string.duration_minutes, draft), style = MaterialTheme.typography.displaySmall)
            OutlinedButton(onClick = { onStep(BudgetViewModel.STEP_MINUTES) }) { Text("+") }
        }
        state.currentMinutes?.let {
            Text(text = stringResource(R.string.budget_current, it), style = MaterialTheme.typography.bodyLarge, color = muted)
        }
        if (state.pendingMinutes != null && state.pendingRemaining != null) {
            Text(
                text = stringResource(R.string.budget_pending, state.pendingMinutes, durationText(state.pendingRemaining)),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        OutlinedButton(
            onClick = onSave,
            enabled = draft != state.currentMinutes || state.pendingMinutes != null,
            modifier = Modifier.padding(top = 16.dp),
        ) { Text(stringResource(R.string.budget_save)) }
        when (val result = state.lastResult) {
            GuardResult.AppliedNow -> Text(stringResource(R.string.budget_applied), style = MaterialTheme.typography.bodyLarge)
            is GuardResult.Scheduled -> Text(stringResource(R.string.budget_scheduled), style = MaterialTheme.typography.bodyLarge)
            is GuardResult.Refused -> Text(stringResource(R.string.lock_refused_short), style = MaterialTheme.typography.bodyLarge)
            null -> Unit
        }
        DayStart(state, onDayStartStep, onDayStartSave)
        Taper(state, onTaperStep, onTaperSave, onTaperStop)
        SettingsRow(
            title = stringResource(R.string.earn_it_title),
            summary = when {
                state.earnItOnIn != null -> stringResource(R.string.earn_it_on_in, durationText(state.earnItOnIn))
                state.earnIt -> stringResource(R.string.earn_it_on)
                else -> stringResource(R.string.earn_it_off)
            },
            onClick = onEarnItTapped,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** When a new day starts: the budget, today's tasks and the day's counts start fresh. */
@Composable
private fun DayStart(state: BudgetUiState, onStep: (Int) -> Unit, onSave: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val draft = state.dayStartDraft ?: return
    Text(
        text = stringResource(R.string.day_start_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 32.dp),
    )
    Text(text = stringResource(R.string.day_start_explainer), style = MaterialTheme.typography.bodyMedium, color = muted)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        val earlier = stringResource(R.string.a11y_day_start_earlier)
        val later = stringResource(R.string.a11y_day_start_later)
        OutlinedButton(onClick = { onStep(-1) }, enabled = draft.hour > 0, modifier = Modifier.semantics { contentDescription = earlier }) { Text("−") }
        Text(text = timeText(draft), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = { onStep(1) }, enabled = draft.hour < 23, modifier = Modifier.semantics { contentDescription = later }) { Text("+") }
    }
    state.dayStart?.let {
        Text(text = stringResource(R.string.day_start_current, timeText(it)), style = MaterialTheme.typography.bodyMedium, color = muted)
    }
    state.dayStartPending?.let { (start, left) ->
        Text(text = stringResource(R.string.day_start_pending, timeText(start), durationText(left)), style = MaterialTheme.typography.bodyMedium)
    }
    OutlinedButton(
        onClick = onSave,
        enabled = draft != state.dayStart || state.dayStartPending != null,
        modifier = Modifier.padding(top = 12.dp),
    ) { Text(stringResource(R.string.budget_save)) }
    when (state.dayStartResult) {
        GuardResult.AppliedNow -> Text(stringResource(R.string.budget_applied), style = MaterialTheme.typography.bodyLarge)
        is GuardResult.Scheduled -> Text(stringResource(R.string.day_start_scheduled), style = MaterialTheme.typography.bodyLarge)
        is GuardResult.Refused -> Text(stringResource(R.string.lock_refused_short), style = MaterialTheme.typography.bodyLarge)
        null -> Unit
    }
}

/** Taper: the budget drops 5% a week until it reaches a floor. */
@Composable
private fun Taper(state: BudgetUiState, onStep: (Int) -> Unit, onSave: () -> Unit, onStop: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = stringResource(R.string.taper_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 32.dp),
    )
    Text(
        text = state.taperFloor?.let { stringResource(R.string.taper_on, it) } ?: stringResource(R.string.taper_off),
        style = MaterialTheme.typography.bodyMedium,
        color = muted,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        OutlinedButton(onClick = { onStep(-BudgetViewModel.STEP_MINUTES) }) { Text("−") }
        Text(text = stringResource(R.string.taper_floor, state.taperDraft), style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = { onStep(BudgetViewModel.STEP_MINUTES) }) { Text("+") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onSave, enabled = state.taperDraft != state.taperFloor) {
            Text(stringResource(if (state.taperFloor == null) R.string.taper_start else R.string.taper_save))
        }
        if (state.taperFloor != null) TextButton(onClick = onStop) { Text(stringResource(R.string.taper_stop)) }
    }
    state.taperPending?.let { (floor, left) ->
        Text(
            text = floor?.let { stringResource(R.string.taper_pending_floor, it, durationText(left)) }
                ?: stringResource(R.string.taper_pending_off, durationText(left)),
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
        )
    }
}
