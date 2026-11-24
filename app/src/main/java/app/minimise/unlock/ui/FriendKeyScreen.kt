package app.minimise.unlock.ui

import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold

@Composable
fun FriendKeyRoute(viewModel: FriendKeyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.friend_key_share_title)
    val shareIntro = stringResource(R.string.friend_key_share_text)
    // The codes must not end up in a screenshot or the recents preview.
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    FriendKeyScreen(
        state = state,
        onMakeCodes = viewModel::onMakeCodes,
        onShare = { codes ->
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, shareIntro + "\n\n" + codes.joinToString("\n"))
            runCatching { context.startActivity(Intent.createChooser(send, shareTitle)) }
        },
        onDone = viewModel::onDone,
    )
}

@Composable
fun FriendKeyScreen(state: FriendKeyUiState, onMakeCodes: () -> Unit, onShare: (List<String>) -> Unit, onDone: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.friend_key_title)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.friend_key_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            val codes = state.shownCodes
            if (codes == null) {
                Text(
                    text = if (state.unused == 0) {
                        stringResource(R.string.friend_key_none)
                    } else {
                        pluralStringResource(R.plurals.friend_key_left, state.unused, state.unused)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(onClick = onMakeCodes, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(if (state.unused == 0) R.string.friend_key_make else R.string.friend_key_remake))
                }
            } else {
                codes.forEach { code ->
                    Text(text = code, style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace))
                }
                Text(
                    text = stringResource(R.string.friend_key_once),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                OutlinedButton(onClick = { onShare(codes) }) { Text(stringResource(R.string.friend_key_share)) }
                TextButton(onClick = onDone) { Text(stringResource(R.string.friend_key_done)) }
            }
        }
    }
}
