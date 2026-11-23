package app.minimise.intervention.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.core.ui.theme.MinimiseTheme
import app.minimise.intervention.ActivityInterventionLauncher
import dagger.hilt.android.AndroidEntryPoint

/** Shown when a time box ends while its app is in front. The only way on is home. */
@AndroidEntryPoint
class TimeUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinimiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackHandler { goHome() }
                    TimeUpScreen(onGoHome = ::goHome)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isFinishing) goHome()
    }

    private fun goHome() {
        startActivity(ActivityInterventionLauncher.homeIntent())
        finish()
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, TimeUpActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

@Composable
private fun TimeUpScreen(onGoHome: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
    ) {
        Text(text = stringResource(R.string.time_up_title), style = MaterialTheme.typography.displaySmall)
        Text(
            text = stringResource(R.string.time_up_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onGoHome, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.time_up_go_home))
        }
    }
}
