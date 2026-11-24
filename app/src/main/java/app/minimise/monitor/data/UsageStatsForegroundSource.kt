package app.minimise.monitor.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.core.time.minus
import app.minimise.monitor.domain.ForegroundEvent
import app.minimise.monitor.domain.ResumedActivity
import app.minimise.monitor.domain.ResumedActivityCursor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The second producer behind the foreground source, for Lite protection: while the accessibility
 * service is off, reads usage access once every [Defaults.LITE_POLL_INTERVAL] and reports each
 * activity that came to the front. Only runs while the screen is on.
 */
class UsageStatsForegroundSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foreground: ForegroundEventBus,
    private val filter: ForegroundPackageFilter,
    private val timeSource: TimeSource,
) {
    private val usageStats: UsageStatsManager? = context.getSystemService(UsageStatsManager::class.java)

    /** Runs until cancelled. Call on the main thread. */
    suspend fun run(screenOn: StateFlow<Boolean>) {
        // Usage access indexes events by the system wall clock, so reads use it too. Only the age of an
        // event is taken from it; the time logged comes from the trusted TimeSource.
        val start = System.currentTimeMillis()
        // The app already in front when Lite protection starts (say, the Settings page that switched
        // accessibility off). It counts from now: earlier time was the other watcher's.
        read(start - Defaults.LITE_LOOK_BACK.inWholeMilliseconds, start)
            .asReversed()
            .firstNotNullOfOrNull(::appOf)
            ?.let { foreground.emit(ForegroundEvent(it.packageName, timeSource.now())) }
        val cursor = ResumedActivityCursor(start, Defaults.LITE_POLL_OVERLAP.inWholeMilliseconds)
        while (true) {
            screenOn.first { it }
            val now = System.currentTimeMillis()
            cursor.accept(read(cursor.readFrom(), now), now).forEach { activity ->
                val app = appOf(activity) ?: return@forEach
                val age = (now - activity.atMillis).coerceAtLeast(0).milliseconds
                foreground.emit(ForegroundEvent(app.packageName, timeSource.now() - age))
            }
            delay(Defaults.LITE_POLL_INTERVAL)
        }
    }

    /** What [activity] coming to the front means, or null when it isn't a change of app (a keyboard, system UI). */
    private fun appOf(activity: ResumedActivity): InFront? = when (val verdict = filter.classifyResumed(activity.packageName)) {
        ForegroundPackageFilter.Verdict.Ignore -> null
        ForegroundPackageFilter.Verdict.NoApp -> InFront(null)
        is ForegroundPackageFilter.Verdict.App -> InFront(verdict.packageName)
    }

    /** @property packageName null for home or one of this app's screens. */
    private data class InFront(val packageName: String?)

    /** Every activity that came to the front in [from, to), oldest first. Empty without usage access. */
    private suspend fun read(from: Long, to: Long): List<ResumedActivity> = withContext(Dispatchers.Default) {
        val manager = usageStats ?: return@withContext emptyList()
        runCatching {
            val events = manager.queryEvents(from, to) ?: return@runCatching emptyList()
            val event = UsageEvents.Event()
            buildList {
                while (events.getNextEvent(event)) {
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        add(ResumedActivity(event.packageName, event.className, event.timeStamp))
                    }
                }
            }
        }.onFailure { Timber.w(it, "Could not read usage events") }.getOrDefault(emptyList())
    }
}
