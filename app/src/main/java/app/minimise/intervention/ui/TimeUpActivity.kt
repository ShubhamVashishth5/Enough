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
import androidx.compose.ui.res.stringResource
import app.minimise.R
import app.minimise.core.ui.theme.MinimiseTheme
import app.minimise.intervention.ActivityInterventionLauncher
import dagger.hilt.android.AndroidEntryPoint

/** Shown when a time box (or the daily budget) runs out while its app is in front. The only way on is home. */
@AndroidEntryPoint
class TimeUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinimiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackHandler { goHome() }
                    val budgetUsed = intent.getBooleanExtra(EXTRA_BUDGET_USED, false)
                    InterventionMessage(
                        title = stringResource(if (budgetUsed) R.string.blocked_budget_title else R.string.time_up_title),
                        body = stringResource(if (budgetUsed) R.string.blocked_budget_body else R.string.time_up_body),
                        action = stringResource(R.string.time_up_go_home),
                        onAction = ::goHome,
                    )
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
        const val EXTRA_BUDGET_USED = "budget_used"

        fun intent(context: Context, budgetUsed: Boolean): Intent =
            Intent(context, TimeUpActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(EXTRA_BUDGET_USED, budgetUsed)
    }
}
