package app.minimise.digest.data

import app.minimise.core.db.BufferedNotificationDao
import app.minimise.core.db.BufferedNotificationEntity
import app.minimise.digest.domain.DigestRepository
import app.minimise.digest.domain.DigestSummary
import app.minimise.digest.domain.HeldNotification
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDigestRepository @Inject constructor(
    private val dao: BufferedNotificationDao,
) : DigestRepository {
    override val waitingCount: Flow<Int> = dao.observeWaitingCount()

    override val released: Flow<List<HeldNotification>> = dao.observeReleased().map { rows ->
        rows.map { HeldNotification(it.id, it.packageName, it.title, it.text, it.postedAt) }
    }

    override suspend fun hold(packageName: String, title: String, text: String, postedAt: Instant) =
        dao.insert(BufferedNotificationEntity(packageName = packageName, title = title, text = text, postedAt = postedAt))

    override suspend fun releaseAll(at: Instant): DigestSummary {
        val waiting = dao.waiting()
        dao.releaseAll(at)
        return DigestSummary(updates = waiting.size, apps = waiting.map { it.packageName }.distinct().size)
    }

    override suspend fun clearReleased() = dao.clearReleased()

    override suspend fun remove(id: Long) = dao.delete(id)
}
