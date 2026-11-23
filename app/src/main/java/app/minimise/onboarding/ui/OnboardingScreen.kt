package app.minimise.onboarding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.apps.domain.AppEntry
import app.minimise.core.Defaults
import app.minimise.core.ui.ScreenScaffold
import app.minimise.settings.permissions.PermissionsRoute

@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    // Back steps back through onboarding; on the first step it does nothing (this is the home screen).
    BackHandler { viewModel.onBack() }
    OnboardingScreen(
        state = state,
        onNext = viewModel::onNext,
        onBack = viewModel::onBack,
        onHomePickToggled = viewModel::onHomePickToggled,
        onVaultPickToggled = viewModel::onVaultPickToggled,
        onBudgetStep = viewModel::onBudgetStep,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onHomePickToggled: (AppEntry) -> Unit,
    onVaultPickToggled: (AppEntry) -> Unit,
    onBudgetStep: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(modifier = Modifier.weight(1f)) {
            when (state.step) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.PERMISSIONS -> PermissionsRoute()
                OnboardingStep.HOME_APPS -> PickStep(
                    title = stringResource(R.string.onboarding_home_title),
                    body = stringResource(R.string.onboarding_home_body, state.homePicks.size, Defaults.HOME_SLOT_COUNT),
                    apps = state.apps,
                    isPicked = { it.packageName in state.homePicks },
                    onToggle = onHomePickToggled,
                )
                OnboardingStep.VAULT -> PickStep(
                    title = stringResource(R.string.onboarding_vault_title),
                    body = stringResource(R.string.onboarding_vault_body),
                    apps = state.vaultChoices,
                    isPicked = { it.packageName in state.vaultPicks },
                    onToggle = onVaultPickToggled,
                )
                OnboardingStep.BUDGET -> BudgetStep(minutes = state.budgetMinutes, onStep = onBudgetStep)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            if (state.step != OnboardingStep.WELCOME) TextButton(onClick = onBack) { Text(stringResource(R.string.onboarding_back)) }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.onboarding_step, state.step.ordinal + 1, OnboardingStep.entries.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(end = 16.dp),
            )
            OutlinedButton(onClick = onNext, enabled = !state.saving) {
                Text(
                    stringResource(
                        when (state.step) {
                            OnboardingStep.WELCOME -> R.string.onboarding_start
                            OnboardingStep.BUDGET -> R.string.onboarding_finish
                            else -> R.string.onboarding_next
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    ScreenScaffold(title = stringResource(R.string.app_name)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                R.string.onboarding_welcome_1,
                R.string.onboarding_welcome_2,
                R.string.onboarding_welcome_3,
                R.string.onboarding_welcome_4,
            ).forEach { line ->
                Text(text = stringResource(line), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun PickStep(
    title: String,
    body: String,
    apps: List<AppEntry>,
    isPicked: (AppEntry) -> Boolean,
    onToggle: (AppEntry) -> Unit,
) {
    ScreenScaffold(title = title) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onToggle(app) }.padding(vertical = 2.dp),
                ) {
                    Checkbox(checked = isPicked(app), onCheckedChange = null)
                    Text(text = app.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun BudgetStep(minutes: Int, onStep: (Int) -> Unit) {
    ScreenScaffold(title = stringResource(R.string.budget_title)) {
        Text(
            text = stringResource(R.string.onboarding_budget_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 24.dp),
        ) {
            OutlinedButton(onClick = { onStep(-BUDGET_STEP_MINUTES) }) { Text("−") }
            Text(text = stringResource(R.string.duration_minutes, minutes), style = MaterialTheme.typography.displaySmall)
            OutlinedButton(onClick = { onStep(BUDGET_STEP_MINUTES) }) { Text("+") }
        }
    }
}

private const val BUDGET_STEP_MINUTES = 5
