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
import app.minimise.gate.rules.BudgetRule
import app.minimise.intervention.ActivityInterventionLauncher
import dagger.hilt.android.AndroidEntryPoint

/** Shown for a Block decision. Phase 1 has no overrides, so the only way on is home. */
@AndroidEntryPoint
class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinimiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackHandler { goHome() }
                    val reason = intent.getStringExtra(EXTRA_REASON)
                    InterventionMessage(
                        title = stringResource(if (reason == BudgetRule.REASON) R.string.blocked_budget_title else R.string.blocked_title),
                        body = stringResource(if (reason == BudgetRule.REASON) R.string.blocked_budget_body else R.string.blocked_body),
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
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_REASON = "reason"

        fun intent(context: Context, packageName: String, reason: String): Intent =
            Intent(context, BlockedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_REASON, reason)
    }
}
