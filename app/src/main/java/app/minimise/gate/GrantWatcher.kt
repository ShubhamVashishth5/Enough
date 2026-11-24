package app.minimise.gate

import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.core.time.plus
import app.minimise.core.time.durationBetween
import app.minimise.gate.domain.Grant
import app.minimise.gate.domain.GrantStore
import app.minimise.intervention.domain.InterventionLauncher
import app.minimise.monitor.domain.ForegroundAppSource
import app.minimise.usage.domain.DailyBudget
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber

/**
 * While an app with a running time box is in front, waits for the box to end, then shows "Time's up".
 * Leaving the app stops the watch; coming back within the box resumes it.
 *
 * A Breathe app's grant has no box: while the app is in front its end keeps moving out, so it only
 * runs out [Defaults.BREATHE_RETURN_WINDOW] after the user leaves.
 */
class GrantWatcher @Inject constructor(
    private val foregroundSource: ForegroundAppSource,
    private val appRepository: AppRepository,
    private val grantStore: GrantStore,
    private val dailyBudget: DailyBudget,
    private val timeSource: TimeSource,
    private val interventions: InterventionLauncher,
    private val eventLog: EventLog,
) {
    suspend fun run() {
        foregroundSource.current.collectLatest { event ->
            val packageName = event?.packageName ?: return@collectLatest
            try {
                watch(packageName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Grant watch failed for %s", packageName)
            }
        }
    }

    private suspend fun watch(packageName: String) {
        // Only a box that is running when the app comes to the front is watched. An app that comes back
        // after its box ended goes through the gate instead.
        val grant = grantStore.get(packageName)?.takeIf { it.isActive(timeSource.now()) } ?: return
        val kind = appRepository.kindOf(packageName)
        // An override's grant (past a focus block, say) keeps its box, Breathe app or not.
        if (kind == AppKind.BREATHE && !grant.viaOverride) return keepOpen(grant)
        val usesBudget = kind == AppKind.VAULT && !grant.viaOverride
        while (true) {
            val boxLeft = durationBetween(timeSource.now(), grant.endAt)
            val budgetLeft = if (usesBudget) dailyBudget.remaining() else Duration.INFINITE
            when {
                budgetLeft <= Duration.ZERO -> return timeUp(packageName, budgetUsed = true)
                boxLeft <= Duration.ZERO -> return timeUp(packageName, budgetUsed = false)
            }
            // Wake up regularly rather than once, so wall-clock changes are noticed.
            delay(minOf(boxLeft, budgetLeft, CHECK_INTERVAL))
        }
    }

    /**
     * Runs until the app leaves the front, which cancels it. If the app is moved out of Breathe meanwhile
     * (into the Vault, say), the grant goes at once, so its next open meets the new rules.
     */
    private suspend fun keepOpen(grant: Grant) {
        while (true) {
            if (appRepository.kindOf(grant.packageName) != AppKind.BREATHE) return grantStore.remove(grant.packageName)
            grantStore.put(grant.copy(endAt = timeSource.now() + Defaults.BREATHE_RETURN_WINDOW))
            delay(CHECK_INTERVAL)
        }
    }

    private suspend fun timeUp(packageName: String, budgetUsed: Boolean) {
        grantStore.remove(packageName)
        eventLog.log(
            Event(
                type = EventType.TIME_UP,
                startAt = timeSource.now(),
                packageName = packageName,
                data = buildJsonObject { put("budgetUsed", budgetUsed) }.toString(),
            ),
        )
        interventions.showTimeUp(packageName, budgetUsed)
    }

    private companion object {
        val CHECK_INTERVAL = 15.seconds
    }
}
