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
import app.minimise.launcher.ui.LauncherNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    /** Emits when the Home button is pressed while this activity is already running. */
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emits when the Today screen should open (the day's first unlock). Replays to a NavHost that starts late. */
    private val todayRequests = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleTodayRequest(intent)
        setContent {
            MinimiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LauncherNavHost(
                        homePresses = homePresses,
                        todayRequests = todayRequests,
                        onTodayRequestHandled = { todayRequests.resetReplayCache() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.tryEmit(Unit)
        handleTodayRequest(intent)
    }

    private fun handleTodayRequest(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) todayRequests.tryEmit(Unit)
    }

    companion object {
        private const val EXTRA_OPEN_TODAY = "open_today"

        /** Opens the launcher on the Today screen. */
        fun todayIntent(context: Context): Intent =
            Intent(context, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_OPEN_TODAY, true)
    }
}
