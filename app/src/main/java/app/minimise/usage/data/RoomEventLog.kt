package app.minimise.usage.data

import app.minimise.core.db.EventDao
import app.minimise.core.db.EventEntity
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomEventLog @Inject constructor(
    private val dao: EventDao,
) : EventLog {
    override suspend fun log(event: Event): Long = dao.insert(event.toEntity())

    override suspend fun updateEnd(id: Long, endAt: Instant) = dao.updateEnd(id, endAt)

    override suspend fun between(from: Instant, to: Instant, type: EventType?): List<Event> =
        dao.between(from, to, type).map { it.toEvent() }

    override fun observeBetween(from: Instant, to: Instant, type: EventType?): Flow<List<Event>> =
        dao.observeBetween(from, to, type).map { rows -> rows.map { it.toEvent() } }

    override suspend fun latest(type: EventType, before: Instant): Event? = dao.latest(type, before)?.toEvent()
}

private fun Event.toEntity() = EventEntity(
    id = id,
    type = type,
    packageName = packageName,
    startAt = startAt,
    endAt = endAt,
    data = data,
)

private fun EventEntity.toEvent() = Event(
    type = type,
    startAt = startAt,
    endAt = endAt,
    packageName = packageName,
    data = data,
    id = id,
)
