package app.minimise.modes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.Defaults
import app.minimise.core.ui.ChoiceText
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.TimeDialog
import app.minimise.core.ui.durationText
import app.minimise.core.ui.timeText
import app.minimise.core.ui.windowText
import app.minimise.modes.domain.Mode
import java.time.LocalTime

@Composable
fun ModesRoute(onEdit: (Long) -> Unit, viewModel: ModesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.openedModes.collect(onEdit) }
    ScreenScaffold(title = stringResource(R.string.modes_title)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.modes_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            state.pendingSwitch?.let { pending -> PendingSwitchText(pending, state.modes) }
            ModeRow(
                title = stringResource(R.string.modes_follow),
                summary = stringResource(R.string.modes_follow_summary),
                selected = state.manualId == null,
                onUse = viewModel::onFollowSchedules,
                onClick = viewModel::onFollowSchedules,
            )
            state.modes.forEach { mode ->
                val picked = state.manualId == mode.id
                ModeRow(
                    title = if (mode.id == state.activeId) stringResource(R.string.modes_now, mode.name) else mode.name,
                    summary = listOfNotNull(
                        modeSummary(mode),
                        state.manualUntil?.takeIf { picked }?.let { stringResource(R.string.modes_until, timeText(it)) },
                        state.pendingEdits[mode.id]?.let { stringResource(R.string.schedule_changes_in, durationText(it)) },
                    ).joinToString("\n"),
                    selected = picked,
                    onUse = { viewModel.onUse(mode.id) },
                    onClick = { onEdit(mode.id) },
                    // A pick from before picks had ends lasts until changed; give it one the same way.
                    selectedAction = stringResource(R.string.modes_change_end),
                )
            }
            TextButton(onClick = viewModel::onAddMode, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.modes_add), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    state.choosingEndFor?.let { mode ->
        ModeEndDialog(
            mode = mode,
            onFor = viewModel::onUseFor,
            onUntilReset = viewModel::onUseUntilReset,
            onUntil = viewModel::onUseUntil,
            onDismiss = viewModel::onEndDismissed,
            suggestedTime = state.nextHour,
        )
    }
    if (state.addingMode) {
        AddModeDialog(
            modes = state.modes,
            nameTaken = state.nameTaken,
            onAdd = viewModel::onAddConfirmed,
            onDismiss = viewModel::onAddDismissed,
        )
    }
}

@Composable
private fun PendingSwitchText(pending: PendingSwitch, modes: List<Mode>) {
    val target = modes.firstOrNull { it.id == pending.modeId }?.name ?: stringResource(R.string.modes_follow)
    val remaining = durationText(pending.remaining)
    val text = when {
        pending.until == null -> stringResource(R.string.modes_switch_pending, target, remaining)
        pending.endsFirst -> stringResource(R.string.modes_switch_pending_too_late, target, remaining, timeText(pending.until))
        else -> stringResource(R.string.modes_switch_pending_until, target, remaining, timeText(pending.until))
    }
    Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
}

/**
 * @param selectedAction the button a selected row shows instead of Use now; none when null.
 */
@Composable
private fun ModeRow(
    title: String,
    summary: String,
    selected: Boolean,
    onUse: () -> Unit,
    onClick: () -> Unit,
    selectedAction: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        SettingsRow(title = title, summary = summary, onClick = onClick, modifier = Modifier.weight(1f))
        when {
            !selected -> TextButton(onClick = onUse) { Text(stringResource(R.string.modes_use)) }
            selectedAction != null -> TextButton(onClick = onUse) { Text(selectedAction) }
        }
    }
}

@Composable
private fun modeSummary(mode: Mode): String {
    val budget = stringResource(R.string.modes_budget, mode.values.budget.inWholeMinutes)
    val schedule = mode.schedule?.let { windowText(it) } ?: stringResource(R.string.modes_no_schedule)
    val apps = mode.values.allowlist?.let { pluralStringResource(R.plurals.modes_only_apps, it.size, it.size) }
    return listOfNotNull(budget, schedule, apps).joinToString(" · ")
}

/** Until when [mode] stays on: a few hours, the day's reset, or a time picked on the clock. */
@Composable
private fun ModeEndDialog(
    mode: Mode,
    onFor: (Int) -> Unit,
    onUntilReset: () -> Unit,
    onUntil: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    suggestedTime: LocalTime,
) {
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    if (pickingTime) {
        TimeDialog(
            initial = suggestedTime,
            onPicked = { pickingTime = false; onUntil(it) },
            onDismiss = { pickingTime = false },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mode_until_title, mode.name)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.mode_until_explainer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                val style = MaterialTheme.typography.titleMedium
                ModesViewModel.PICK_HOURS.forEach { hours ->
                    ChoiceText(text = pluralStringResource(R.plurals.mode_until_hours, hours, hours), onClick = { onFor(hours) }, style = style)
                }
                ChoiceText(text = stringResource(R.string.mode_until_reset, timeText(Defaults.DAY_RESET)), onClick = onUntilReset, style = style)
                ChoiceText(text = stringResource(R.string.mode_until_time), onClick = { pickingTime = true }, style = style)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.swap_cancel)) } },
    )
}

/** A name, and which mode's budget, waits and apps the new one starts with. */
@Composable
private fun AddModeDialog(modes: List<Mode>, nameTaken: Boolean, onAdd: (String, Mode) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    // Home is the gentlest shipped mode, so the new one starts from no stricter than it.
    var templateId by rememberSaveable { mutableStateOf(modes.firstOrNull { it.presetKey == "home" }?.id ?: modes.firstOrNull()?.id) }
    val template = modes.firstOrNull { it.id == templateId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mode_add_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(Mode.MAX_NAME_LENGTH) },
                    singleLine = true,
                    isError = nameTaken,
                    supportingText = if (nameTaken) {
                        { Text(stringResource(R.string.mode_name_taken)) }
                    } else {
                        null
                    },
                    label = { Text(stringResource(R.string.mode_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.mode_add_start_from),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                modes.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = mode.id == templateId, onClick = { templateId = mode.id }, role = Role.RadioButton),
                    ) {
                        RadioButton(selected = mode.id == templateId, onClick = null)
                        Column {
                            Text(mode.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.modes_budget, mode.values.budget.inWholeMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { template?.let { onAdd(name, it) } }, enabled = name.isNotBlank() && template != null) {
                Text(stringResource(R.string.mode_add_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.swap_cancel)) } },
    )
}
