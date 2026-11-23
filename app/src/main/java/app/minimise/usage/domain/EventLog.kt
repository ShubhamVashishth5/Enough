package app.minimise.usage.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface EventLog {
    suspend fun log(event: Event): Long

    /** Events that overlap [from, to): spans that cross [from] are included. Oldest first. */
    suspend fun between(from: Instant, to: Instant, type: EventType? = null): List<Event>

    /** Like [between], but re-emits whenever the log changes. */
    fun observeBetween(from: Instant, to: Instant, type: EventType? = null): Flow<List<Event>>
}
