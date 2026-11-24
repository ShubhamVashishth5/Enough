package app.minimise.intervention.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.core.ui.durationText
import app.minimise.gate.rules.BudgetRule
import app.minimise.gate.rules.FirstHourRule
import app.minimise.gate.rules.FocusRule
import app.minimise.gate.rules.ModeAllowlistRule
import app.minimise.gate.rules.OpenLimitRule
import app.minimise.gate.rules.ScheduleRule
import app.minimise.intervention.domain.Trigger
import app.minimise.unlock.domain.UnlockMethodId
import kotlin.time.Duration

/** Each method's own check, shown for [BlockedStep.Verify]. Added by the steps that add methods. */
typealias VerifyContent = @Composable (method: UnlockMethodId, onVerified: () -> Unit) -> Unit

@Composable
fun BlockedScreen(
    state: BlockedUiState,
    onTriggerChosen: (Trigger?) -> Unit,
    onOverrideChosen: (UnlockMethodId) -> Unit,
    onVerified: (UnlockMethodId) -> Unit,
    onTimeBoxChosen: (Duration) -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    verifyContent: VerifyContent,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
    ) {
        when (val step = state.step) {
            BlockedStep.Trigger -> TriggerQuestion(onTriggerChosen)
            BlockedStep.Choose -> Choose(state, onOverrideChosen, onGoHome)
            is BlockedStep.Verify -> {
                verifyContent(step.method) { onVerified(step.method) }
                TextButton(onClick = onBack) { Text(stringResource(R.string.blocked_back)) }
            }
            is BlockedStep.TimeBox -> {
                Text(
                    text = stringResource(R.string.pause_how_long),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                state.timeBoxOptions.forEach { option ->
                    Text(
                        text = durationText(option),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTimeBoxChosen(option) }
                            .padding(vertical = 12.dp),
                    )
                }
                TextButton(onClick = onBack) { Text(stringResource(R.string.blocked_back)) }
            }
        }
    }
}

@Composable
private fun TriggerQuestion(onTriggerChosen: (Trigger?) -> Unit) {
    Text(text = stringResource(R.string.blocked_budget_title), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
    Text(text = stringResource(R.string.trigger_question), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Trigger.entries.forEach { trigger ->
        Text(
            text = stringResource(trigger.labelRes()),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTriggerChosen(trigger) }
                .padding(vertical = 12.dp),
        )
    }
    TextButton(onClick = { onTriggerChosen(null) }) { Text(stringResource(R.string.trigger_skip)) }
}

fun Trigger.labelRes(): Int = when (this) {
    Trigger.NOTIFICATION -> R.string.trigger_notification
    Trigger.BOREDOM -> R.string.trigger_boredom
    Trigger.STRESS -> R.string.trigger_stress
    Trigger.APP_HOP -> R.string.trigger_app_hop
}

@Composable
private fun Choose(state: BlockedUiState, onOverrideChosen: (UnlockMethodId) -> Unit, onGoHome: () -> Unit) {
    Text(text = blockedTitle(state.reason), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
    Text(
        text = blockedBody(state.reason, state.appLabel),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
        state.options.forEach { option ->
            TextButton(onClick = { onOverrideChosen(option.method) }) {
                Text(text = overrideLabel(option), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
        }
    }
    OutlinedButton(onClick = onGoHome) { Text(stringResource(R.string.time_up_go_home)) }
}

@Composable
private fun overrideLabel(option: OverrideOption): String = when (option.method) {
    UnlockMethodId.EMERGENCY_PASS -> {
        val left = option.usesLeft ?: 0
        pluralStringResource(R.plurals.override_emergency_pass, left, left)
    }
    UnlockMethodId.NFC_KEY -> stringResource(R.string.override_nfc_key)
    UnlockMethodId.QR_KEY -> stringResource(R.string.override_qr_key)
    UnlockMethodId.WAIT -> stringResource(R.string.override_wait)
    UnlockMethodId.FRIEND_CODE -> pluralStringResource(R.plurals.override_friend_code, option.usesLeft ?: 0, option.usesLeft ?: 0)
    UnlockMethodId.TYPING -> stringResource(R.string.override_typing)
}

@Composable
private fun blockedTitle(reason: String): String = stringResource(
    when (reason) {
        BudgetRule.REASON -> R.string.blocked_budget_title
        OpenLimitRule.REASON -> R.string.blocked_open_limit_title
        ScheduleRule.REASON -> R.string.blocked_schedule_title
        FirstHourRule.REASON -> R.string.blocked_first_hour_title
        FocusRule.REASON -> R.string.blocked_focus_title
        ModeAllowlistRule.REASON -> R.string.blocked_mode_title
        else -> R.string.blocked_title
    },
)

@Composable
private fun blockedBody(reason: String, appLabel: String): String = when (reason) {
    BudgetRule.REASON -> stringResource(R.string.blocked_budget_body)
    OpenLimitRule.REASON -> stringResource(R.string.blocked_open_limit_body, appLabel)
    ScheduleRule.REASON -> stringResource(R.string.blocked_schedule_body)
    FirstHourRule.REASON -> stringResource(R.string.blocked_first_hour_body)
    FocusRule.REASON -> stringResource(R.string.blocked_focus_body, appLabel)
    ModeAllowlistRule.REASON -> stringResource(R.string.blocked_mode_body, appLabel)
    else -> stringResource(R.string.blocked_body)
}
