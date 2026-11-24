package app.minimise.feeds.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.durationText

@Composable
fun FeedsRoute(viewModel: FeedsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeedsScreen(state = state, onFeedTapped = viewModel::onFeedTapped)
}

@Composable
fun FeedsScreen(state: FeedsUiState, onFeedTapped: (FeedRow) -> Unit) {
    ScreenScaffold(title = stringResource(R.string.feeds_title)) {
        Text(
            text = stringResource(R.string.feeds_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn {
            items(state.rows, key = { it.rule.id }) { row ->
                SettingsRow(
                    title = row.rule.name,
                    summary = when {
                        row.offIn != null -> stringResource(R.string.feed_off_in, durationText(row.offIn))
                        row.blocked -> stringResource(R.string.feed_on)
                        else -> stringResource(R.string.feed_off)
                    },
                    onClick = { onFeedTapped(row) },
                )
            }
        }
    }
}
