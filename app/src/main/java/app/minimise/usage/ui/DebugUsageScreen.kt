package app.minimise.usage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import java.time.format.DateTimeFormatter

private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss")
private val DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM HH:mm")

@Composable
fun DebugUsageRoute(viewModel: DebugUsageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DebugUsageScreen(
        state = state,
        onShiftClock = viewModel::onShiftClock,
        onResetClock = viewModel::onResetClock,
    )
}

@Composable
fun DebugUsageScreen(
    state: DebugUsageUiState,
    onShiftClock: (Int) -> Unit,
    onResetClock: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.debug_usage_title)) {
        val body = MaterialTheme.typography.bodyMedium
        Text(stringResource(R.string.debug_usage_now, state.now?.format(DATE_TIME).orEmpty(), state.clockOffset.toString()), style = body)
        Text(stringResource(R.string.debug_usage_day, state.day?.toString().orEmpty()), style = body)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            OutlinedButton(onClick = { onShiftClock(-1) }) { Text("−1 h") }
            OutlinedButton(onClick = { onShiftClock(1) }) { Text("+1 h") }
            OutlinedButton(onClick = onResetClock) { Text(stringResource(R.string.debug_usage_reset)) }
        }
        Text(
            text = stringResource(R.string.debug_usage_unlocks, state.unlocks),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LazyColumn {
            items(state.spans, key = { it.id }) { span ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = span.label, style = body, modifier = Modifier.weight(1f))
                    Text(text = "${span.start.format(TIME)}–${span.end.format(TIME)}  ${span.length.inWholeSeconds} s", style = body)
                }
            }
        }
    }
}
