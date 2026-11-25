package app.minimise.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import app.minimise.apps.ui.labelRes
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.dateTimeText
import app.minimise.core.ui.durationText
import kotlin.time.Duration

@Composable
fun PendingChangesRoute(onApplyNow: (Long) -> Unit, viewModel: PendingChangesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PendingChangesScreen(state = state, onCancel = viewModel::onCancel, onApplyNow = { onApplyNow(it.id) })
}

@Composable
fun PendingChangesScreen(
    state: PendingChangesUiState,
    onCancel: (PendingItem) -> Unit,
    onApplyNow: (PendingItem) -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.pending_title)) {
        Text(
            text = stringResource(R.string.pending_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        state.lockedUntil?.let {
            Text(
                text = stringResource(R.string.pending_locked, dateTimeText(it)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (state.items.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_pending_none),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        LazyColumn {
            items(state.items, key = { it.id }) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = describe(item.description), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (state.lockedUntil != null && item.remaining <= Duration.ZERO) {
                                stringResource(R.string.pending_after_lock)
                            } else {
                                stringResource(R.string.pending_applies_in, durationText(item.remaining))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.lockedUntil == null) {
                        TextButton(onClick = { onApplyNow(item) }) { Text(stringResource(R.string.pending_apply_now)) }
                    }
                    TextButton(onClick = { onCancel(item) }) { Text(stringResource(R.string.pending_cancel)) }
                }
            }
        }
    }
}

@Composable
private fun describe(description: PendingDescription): String = when (description) {
    is PendingDescription.AppKindChange ->
        stringResource(R.string.pending_app_kind, description.appLabel, stringResource(description.kind.labelRes()))
    is PendingDescription.BudgetChange -> stringResource(R.string.pending_budget, description.minutes)
    is PendingDescription.OpenLimitChange -> description.limit
        ?.let { pluralStringResource(R.plurals.pending_open_limit, it, description.appLabel, it) }
        ?: stringResource(R.string.pending_open_limit_none, description.appLabel)
    is PendingDescription.BreatheWaitChange -> description.seconds
        ?.let { stringResource(R.string.pending_breathe_wait, description.appLabel, it) }
        ?: stringResource(R.string.pending_breathe_wait_usual, description.appLabel)
    is PendingDescription.ScheduleChange ->
        stringResource(if (description.removed) R.string.pending_schedule_removed else R.string.pending_schedule_changed, description.name)
    is PendingDescription.FirstHourChange ->
        stringResource(if (description.enabled) R.string.pending_first_hour_on else R.string.pending_first_hour_off)
    is PendingDescription.KeyChange ->
        stringResource(if (description.removed) R.string.pending_key_removed else R.string.pending_key_added, description.label)
    is PendingDescription.ModeSwitch -> {
        val name = description.modeName ?: stringResource(R.string.modes_follow)
        description.until
            ?.let { stringResource(R.string.pending_mode_switch_until, name, dateTimeText(it)) }
            ?: stringResource(R.string.pending_mode_switch, name)
    }
    is PendingDescription.ModeEdit -> stringResource(R.string.pending_mode_edit, description.modeName)
    is PendingDescription.ModeAdd -> stringResource(R.string.pending_mode_added, description.modeName)
    is PendingDescription.ModeRemove -> stringResource(R.string.pending_mode_removed, description.modeName)
    PendingDescription.LockChange -> stringResource(R.string.lock_title)
    is PendingDescription.TaperChange -> description.floorMinutes
        ?.let { stringResource(R.string.pending_taper_floor, it) }
        ?: stringResource(R.string.pending_taper_off)
    is PendingDescription.CalendarFocusChange ->
        stringResource(if (description.enabled) R.string.pending_calendar_focus_on else R.string.pending_calendar_focus_off)
    is PendingDescription.EarnItChange -> stringResource(if (description.enabled) R.string.pending_earn_it_on else R.string.pending_earn_it_off)
    is PendingDescription.FeedChange ->
        stringResource(if (description.blocked) R.string.pending_feed_on else R.string.pending_feed_off, description.feedName)
}
