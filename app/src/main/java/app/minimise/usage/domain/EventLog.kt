package app.minimise.usage.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface EventLog {
    suspend fun log(event: Event): Long

    /** Moves a span's end. Only for [EventType.APP_SPAN] rows that are still open. */
    suspend fun updateEnd(id: Long, endAt: Instant)

    /** Events that overlap [from, to): spans that cross [from] are included. Oldest first. */
    suspend fun between(from: Instant, to: Instant, type: EventType? = null): List<Event>

    /** Like [between], but re-emits whenever the log changes. */
    fun observeBetween(from: Instant, to: Instant, type: EventType? = null): Flow<List<Event>>

    /** The most recent event of [type] that started before [before], or null. */
    suspend fun latest(type: EventType, before: Instant): Event? = between(Instant.EPOCH, before, type).lastOrNull()
}
