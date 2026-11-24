package app.minimise.stats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.durationText
import app.minimise.core.ui.timeText
import app.minimise.intervention.ui.labelRes
import java.time.LocalTime

@Composable
fun StatsRoute(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(state)
}

/** One screen of plain lines, no charts: a dashboard to scroll would be another feed. */
@Composable
fun StatsScreen(state: StatsUiState) {
    ScreenScaffold(title = stringResource(R.string.stats_title)) {
        when (state) {
            StatsUiState.Loading -> Unit
            StatsUiState.ComeBackTomorrow -> Text(stringResource(R.string.stats_tomorrow), style = MaterialTheme.typography.bodyLarge)
            is StatsUiState.Ready -> Report(state)
        }
    }
}

@Composable
private fun Report(state: StatsUiState.Ready) {
    val report = state.report
    val now = report.thisWeek
    val before = report.lastWeek
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.stats_this_week_vs_last), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Line(stringResource(R.string.stats_vault, durationText(now.vaultTime), durationText(before.vaultTime)))
        Line(stringResource(R.string.stats_unlocks, now.unlocks, before.unlocks))
        Line(stringResource(R.string.stats_focus, durationText(now.focusTime), durationText(before.focusTime)))
        Line(stringResource(R.string.stats_tasks, now.tasksDone, before.tasksDone))
        if (state.sinks.isNotEmpty()) {
            val sinks = state.sinks.map { (label, time) -> "$label ${durationText(time)}" }
            Line(stringResource(R.string.stats_top_sinks, sinks.joinToString(" · ")))
        }
        val hour = report.busiestSlipHour
        if (hour == null) {
            Line(stringResource(R.string.stats_no_slips))
        } else {
            Line(stringResource(R.string.stats_slips, timeText(LocalTime.of(hour, 0)), timeText(LocalTime.of((hour + 2) % 24, 0))))
        }
        report.topTrigger?.let { Line(stringResource(R.string.stats_trigger, stringResource(it.labelRes()).lowercase())) }
        if (report.daysSoFar > 0) Line(pluralStringResource(R.plurals.stats_days_under, report.daysSoFar, report.daysUnderBudget, report.daysSoFar))
    }
}

@Composable
private fun Line(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 2.dp))
}
