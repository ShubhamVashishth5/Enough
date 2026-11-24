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
import app.minimise.unlock.domain.UnlockMethodId
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

/**
 * Shown for a Block decision: why, then the overrides the block allows (an emergency pass, a key).
 * Passing one grants a time box; otherwise the only way on is home.
 */
@AndroidEntryPoint
class BlockedActivity : ComponentActivity() {
    private var requestId by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
        setContent {
            MinimiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    key(requestId) { BlockedRoute(requestId = requestId, onFinish = ::onFinish) }
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
        // Leaving without passing an override ends the attempt. Coming back to the app means a new gate check.
        if (!isChangingConfigurations && !isFinishing) finish()
    }

    private fun onFinish(finish: PauseFinish) {
        if (finish == PauseFinish.GO_HOME) startActivity(ActivityInterventionLauncher.homeIntent())
        finish()
    }

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_REASON = "reason"
        const val EXTRA_OVERRIDES = "overrides"
        const val EXTRA_LAUNCH_AFTER = "launch_after"

        fun intent(
            context: Context,
            packageName: String,
            reason: String,
            overrides: List<UnlockMethodId>,
            launchAfter: Boolean,
        ): Intent =
            Intent(context, BlockedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(EXTRA_REQUEST_ID, UUID.randomUUID().toString())
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_REASON, reason)
                .putExtra(EXTRA_OVERRIDES, overrides.map { it.name }.toTypedArray())
                .putExtra(EXTRA_LAUNCH_AFTER, launchAfter)
    }
}

@Composable
private fun BlockedRoute(requestId: String, onFinish: (PauseFinish) -> Unit) {
    val viewModel: BlockedViewModel = hiltViewModel(key = requestId)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finish) { state.finish?.let(onFinish) }
    BackHandler { viewModel.onBack() }
    BlockedScreen(
        state = state,
        onOverrideChosen = viewModel::onOverrideChosen,
        onVerified = viewModel::onVerified,
        onTimeBoxChosen = viewModel::onTimeBoxChosen,
        onBack = viewModel::onBack,
        onGoHome = viewModel::onGoHome,
        verifyContent = { _, _ -> },
    )
}
