package app.minimise.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.minimise.core.ui.theme.MinimiseTheme
import app.minimise.launcher.ui.Destination
import app.minimise.launcher.ui.LauncherNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    /** Emits when the Home button is pressed while this activity is already running. */
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** A screen to open from outside: Today on the day's first unlock, the digest from its notification. Replays to a NavHost that starts late. */
    private val openRequests = MutableSharedFlow<Destination>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleOpenRequest(intent)
        setContent {
            MinimiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LauncherNavHost(
                        homePresses = homePresses,
                        openRequests = openRequests,
                        onOpenRequestHandled = { openRequests.resetReplayCache() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.tryEmit(Unit)
        handleOpenRequest(intent)
    }

    private fun handleOpenRequest(intent: Intent) {
        val destination = when (intent.getStringExtra(EXTRA_OPEN)) {
            OPEN_TODAY -> Destination.Today
            OPEN_DIGEST -> Destination.Digest
            else -> return
        }
        openRequests.tryEmit(destination)
    }

    companion object {
        private const val EXTRA_OPEN = "open"
        private const val OPEN_TODAY = "today"
        private const val OPEN_DIGEST = "digest"

        /** Opens the launcher on the Today screen. */
        fun todayIntent(context: Context): Intent = openIntent(context, OPEN_TODAY)

        /** Opens the launcher on the Digest screen. */
        fun digestIntent(context: Context): Intent = openIntent(context, OPEN_DIGEST)

        private fun openIntent(context: Context, screen: String): Intent =
            Intent(context, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_OPEN, screen)
    }
}
