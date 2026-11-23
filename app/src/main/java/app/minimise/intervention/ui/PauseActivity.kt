package app.minimise.intervention.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.core.ui.theme.MinimiseTheme
import app.minimise.intervention.ActivityInterventionLauncher
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import kotlin.time.Duration

/**
 * The pause screen: a breathing countdown, then "What are you here for?". Runs in its own task, so
 * finishing it reveals whatever was underneath (the Vault app, when opened from elsewhere).
 */
@AndroidEntryPoint
class PauseActivity : ComponentActivity() {
    /** Each gate request gets its own ViewModel, so a second Vault app replaces the first cleanly. */
    private var requestId by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
        setContent {
            MinimiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    key(requestId) { PauseRoute(requestId = requestId, onFinish = ::onFinish) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
    }

    override fun onStop() {
        super.onStop()
        // Leaving without a choice (Home, recents, screen off) ends the pause; the ViewModel logs a cancel.
        if (!isChangingConfigurations && !isFinishing) finish()
    }

    private fun onFinish(finish: PauseFinish) {
        if (finish == PauseFinish.GO_HOME) startActivity(ActivityInterventionLauncher.homeIntent())
        finish()
    }

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_WAIT_MILLIS = "wait_millis"
        const val EXTRA_LAUNCH_AFTER = "launch_after"

        fun intent(context: Context, packageName: String, wait: Duration, launchAfter: Boolean): Intent =
            Intent(context, PauseActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(EXTRA_REQUEST_ID, UUID.randomUUID().toString())
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_WAIT_MILLIS, wait.inWholeMilliseconds)
                .putExtra(EXTRA_LAUNCH_AFTER, launchAfter)
    }
}

@Composable
private fun PauseRoute(requestId: String, onFinish: (PauseFinish) -> Unit) {
    val viewModel: PauseViewModel = hiltViewModel(key = requestId)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finish) { state.finish?.let(onFinish) }
    BackHandler { viewModel.onCancel() }
    PauseScreen(state = state, onReasonChosen = viewModel::onReasonChosen, onCancel = viewModel::onCancel)
}
