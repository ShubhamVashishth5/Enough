package app.minimise.gate

import app.minimise.apps.domain.AppRepository
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateEngine
import app.minimise.intervention.domain.InterventionLauncher
import app.minimise.monitor.domain.ForegroundAppSource
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

/**
 * Gates apps opened from anywhere but the launcher (a notification, recents, another app): when an app
 * comes to the front, ask the GateEngine and show the intervention on top of it.
 */
class GateMonitor @Inject constructor(
    private val foregroundSource: ForegroundAppSource,
    private val appRepository: AppRepository,
    private val gateEngine: GateEngine,
    private val interventions: InterventionLauncher,
) {
    suspend fun run() {
        // collectLatest: a quick switch to another app abandons the check for the one before it.
        foregroundSource.events.collectLatest { event ->
            val packageName = event.packageName ?: return@collectLatest
            try {
                val app = appRepository.entry(packageName) ?: return@collectLatest
                when (val decision = gateEngine.check(app)) {
                    Decision.Allow -> Unit
                    is Decision.Pause -> interventions.showPause(app, decision, launchAfter = false)
                    is Decision.Block -> interventions.showBlocked(app, decision, launchAfter = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Gate check failed for %s", packageName)
            }
        }
    }
}
