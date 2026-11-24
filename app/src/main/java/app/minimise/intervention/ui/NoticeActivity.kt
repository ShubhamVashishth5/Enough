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
import androidx.compose.ui.Modifier
import app.minimise.core.ui.theme.MinimiseTheme
import app.minimise.intervention.ActivityInterventionLauncher
import dagger.hilt.android.AndroidEntryPoint

/**
 * A one-button explanation shown over another app: "Feed blocked", or "Locked" when a lock period
 * keeps the user out of a Settings page. Either it closes to reveal the app underneath, or it goes home.
 */
@AndroidEntryPoint
class NoticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val action = intent.getStringExtra(EXTRA_ACTION).orEmpty()
        setContent {
            MinimiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackHandler { close() }
                    InterventionMessage(title = title, body = body, action = action, onAction = ::close)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isFinishing) finish()
    }

    private fun close() {
        if (intent.getBooleanExtra(EXTRA_GO_HOME, false)) startActivity(ActivityInterventionLauncher.homeIntent())
        finish()
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"
        private const val EXTRA_ACTION = "action"
        private const val EXTRA_GO_HOME = "go_home"

        fun intent(context: Context, title: String, body: String, action: String, goHome: Boolean): Intent =
            Intent(context, NoticeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_BODY, body)
                .putExtra(EXTRA_ACTION, action)
                .putExtra(EXTRA_GO_HOME, goHome)
    }
}
