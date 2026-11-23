package app.minimise.testing

import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEventLog : EventLog {
    val events = MutableStateFlow<List<Event>>(emptyList())

    override suspend fun log(event: Event): Long {
        val id = events.value.size + 1L
        events.value = events.value + event.copy(id = id)
        return id
    }

    override suspend fun updateEnd(id: Long, endAt: Instant) {
        events.value = events.value.map { if (it.id == id) it.copy(endAt = endAt) else it }
    }

    override suspend fun between(from: Instant, to: Instant, type: EventType?): List<Event> =
        events.value.filter { it.overlaps(from, to) && (type == null || it.type == type) }.sortedBy { it.startAt }

    override fun observeBetween(from: Instant, to: Instant, type: EventType?): Flow<List<Event>> =
        events.map { all -> all.filter { it.overlaps(from, to) && (type == null || it.type == type) }.sortedBy { it.startAt } }

    private fun Event.overlaps(from: Instant, to: Instant): Boolean =
        startAt < to && (endAt ?: startAt) >= from
}
