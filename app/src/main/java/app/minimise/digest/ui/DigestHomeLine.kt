package app.minimise.digest.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** "4 updates waiting" (no content, not tappable), or "Digest ready" once one has gone out. */
@Composable
fun DigestHomeLine(onOpenDigest: () -> Unit, viewModel: DigestViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val released = state.groups.sumOf { it.items.size }
    when {
        released > 0 -> Text(
            text = pluralStringResource(app.minimise.R.plurals.home_digest_ready, released, released),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable(onClick = onOpenDigest).padding(vertical = 6.dp),
        )
        state.waiting > 0 -> Text(
            text = pluralStringResource(app.minimise.R.plurals.home_updates_waiting, state.waiting, state.waiting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}
