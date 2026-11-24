package app.minimise.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.durationText
import app.minimise.settings.domain.GuardResult

@Composable
fun BudgetRoute(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetScreen(state = state, onStep = viewModel::onStep, onSave = viewModel::onSave)
}

@Composable
fun BudgetScreen(
    state: BudgetUiState,
    onStep: (Int) -> Unit,
    onSave: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.budget_title)) {
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
    }
}
