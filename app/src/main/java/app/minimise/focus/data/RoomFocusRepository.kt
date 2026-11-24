package app.minimise.focus.data

import app.minimise.core.db.FocusSessionDao
import app.minimise.core.db.FocusSessionEntity
import app.minimise.focus.domain.FocusRepository
import app.minimise.focus.domain.FocusSession
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFocusRepository @Inject constructor(
    private val dao: FocusSessionDao,
) : FocusRepository {
    override val open: Flow<FocusSession?> = dao.observeOpen().map { it?.toDomain() }

    override val lastEnded: Flow<FocusSession?> = dao.observeLastEnded().map { it?.toDomain() }

    override suspend fun open(): FocusSession? = dao.open()?.toDomain()

    override suspend fun start(startAt: Instant, endAt: Instant, locked: Boolean, allowlist: Set<String>, previousInterruptionFilter: Int?) {
        dao.insert(
            FocusSessionEntity(
                startAt = startAt,
                endAt = endAt,
                endedAt = null,
                locked = locked,
                allowlist = allowlist.joinToString(","),
                previousInterruptionFilter = previousInterruptionFilter,
            ),
        )
    }

    override suspend fun end(id: Long, at: Instant): Boolean = dao.end(id, at) > 0

    private fun FocusSessionEntity.toDomain() = FocusSession(
        id = id,
        startAt = startAt,
        endAt = endAt,
        endedAt = endedAt,
        locked = locked,
        allowlist = allowlist.split(',').filter { it.isNotBlank() }.toSet(),
        previousInterruptionFilter = previousInterruptionFilter,
    )
}
