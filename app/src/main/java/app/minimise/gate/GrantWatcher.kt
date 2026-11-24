package app.minimise.gate

import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
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
import timber.log.Timber

/**
 * While an app with a running time box is in front, waits for the box to end, then shows "Time's up".
 * Leaving the app stops the watch; coming back within the box resumes it.
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
        val usesBudget = appRepository.kindOf(packageName) == AppKind.VAULT
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

    private suspend fun timeUp(packageName: String, budgetUsed: Boolean) {
        grantStore.remove(packageName)
        eventLog.log(Event(type = EventType.TIME_UP, startAt = timeSource.now(), packageName = packageName))
        interventions.showTimeUp(packageName, budgetUsed)
    }

    private companion object {
        val CHECK_INTERVAL = 15.seconds
    }
}
