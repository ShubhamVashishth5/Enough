package app.minimise.usage.domain

import app.minimise.core.time.TimeSource
import app.minimise.monitor.domain.ForegroundEvent
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The app span being written right now. Its row's endAt lags by at most one checkpoint. */
data class OpenSpan(val eventId: Long, val packageName: String, val startAt: Instant)

/**
 * Turns foreground changes into one [EventType.APP_SPAN] event per continuous stretch of app use.
 * The span's row is written when it opens and its end is moved forward on every [checkpoint], so a
 * killed process loses at most one checkpoint interval.
 */
@Singleton
class UsageTracker @Inject constructor(
    private val eventLog: EventLog,
    private val timeSource: TimeSource,
) {
    private val mutex = Mutex()
    private val _openSpan = MutableStateFlow<OpenSpan?>(null)
    val openSpan: StateFlow<OpenSpan?> = _openSpan.asStateFlow()

    suspend fun onForeground(event: ForegroundEvent) = mutex.withLock {
        val open = _openSpan.value
        if (open?.packageName == event.packageName) return@withLock
        if (open != null) eventLog.updateEnd(open.eventId, event.at)
        _openSpan.value = event.packageName?.let { packageName ->
            val id = eventLog.log(
                Event(type = EventType.APP_SPAN, startAt = event.at, endAt = event.at, packageName = packageName),
            )
            OpenSpan(eventId = id, packageName = packageName, startAt = event.at)
        }
    }

    suspend fun checkpoint() = mutex.withLock {
        _openSpan.value?.let { eventLog.updateEnd(it.eventId, timeSource.now()) }
    }

    /** Ends the open span now, e.g. when the service stops. */
    suspend fun closeOpenSpan() = mutex.withLock {
        _openSpan.value?.let { eventLog.updateEnd(it.eventId, timeSource.now()) }
        _openSpan.value = null
    }
}
