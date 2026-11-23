package app.minimise.intervention

import android.content.Context
import android.content.Intent
import app.minimise.apps.domain.AppEntry
import app.minimise.gate.domain.Decision
import app.minimise.intervention.domain.InterventionLauncher
import app.minimise.intervention.ui.PauseActivity
import app.minimise.intervention.ui.TimeUpActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ActivityInterventionLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : InterventionLauncher {
    override fun showPause(app: AppEntry, pause: Decision.Pause, launchAfter: Boolean) {
        context.startActivity(PauseActivity.intent(context, app.packageName, pause.wait, launchAfter))
    }

    override fun showBlocked(app: AppEntry, block: Decision.Block) {
        // TODO(question): no Phase 1.6 rule blocks. BlockedActivity arrives with the budget in 1.8;
        // until then a block simply sends the user home.
        context.startActivity(homeIntent())
    }

    override fun showTimeUp(packageName: String) {
        context.startActivity(TimeUpActivity.intent(context))
    }

    companion object {
        fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
