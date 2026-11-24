package app.minimise.launcher

import app.minimise.apps.data.AppLauncher
import app.minimise.apps.domain.AppEntry
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateEngine
import app.minimise.intervention.domain.InterventionLauncher
import javax.inject.Inject

/**
 * Opens an app from the launcher. The gate runs before the app starts, so a Vault app never flashes on
 * screen: its pause screen comes first and starts the app only if the user proceeds.
 */
class AppOpener @Inject constructor(
    private val gateEngine: GateEngine,
    private val appLauncher: AppLauncher,
    private val interventions: InterventionLauncher,
) {
    /** Returns true when the app or its intervention was shown. */
    suspend fun open(app: AppEntry): Boolean = when (val decision = gateEngine.check(app)) {
        Decision.Allow -> appLauncher.launch(app.packageName)
        is Decision.Pause -> {
            interventions.showPause(app, decision, launchAfter = true)
            true
        }
        is Decision.Block -> {
            interventions.showBlocked(app, decision)
            true
        }
    }
}
