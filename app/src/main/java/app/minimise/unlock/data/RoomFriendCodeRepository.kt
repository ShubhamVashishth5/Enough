package app.minimise.unlock.data

import app.minimise.core.db.FriendCodeDao
import app.minimise.core.db.FriendCodeEntity
import app.minimise.core.time.TimeSource
import app.minimise.unlock.domain.FriendCodeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RoomFriendCodeRepository @Inject constructor(
    private val dao: FriendCodeDao,
    private val timeSource: TimeSource,
) : FriendCodeRepository {
    override val unused: Flow<Int> = dao.observeUnused()

    override suspend fun unusedCount(): Int = dao.unused()

    override suspend fun replaceAll(hashes: List<String>) {
        val now = timeSource.now()
        dao.replaceAll(hashes.map { FriendCodeEntity(codeHash = it, createdAt = now) })
    }

    override suspend fun spend(hash: String): Boolean = dao.spend(hash, timeSource.now()) > 0
}
