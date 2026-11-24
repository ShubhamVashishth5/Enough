package app.minimise.keys.data

import app.minimise.core.db.KeyDao
import app.minimise.core.db.KeyEntity
import app.minimise.core.time.TimeSource
import app.minimise.keys.domain.KeyRepository
import app.minimise.keys.domain.KeyType
import app.minimise.keys.domain.PhysicalKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomKeyRepository @Inject constructor(
    private val dao: KeyDao,
    private val timeSource: TimeSource,
) : KeyRepository {
    override val keys: Flow<List<PhysicalKey>> = dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun all(): List<PhysicalKey> = dao.all().mapNotNull { it.toDomain() }

    override suspend fun get(id: Long): PhysicalKey? = dao.get(id)?.toDomain()

    override suspend fun add(type: KeyType, label: String, secretHash: String) {
        dao.insert(KeyEntity(type = type.name, label = label, secretHash = secretHash, createdAt = timeSource.now()))
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    private fun KeyEntity.toDomain(): PhysicalKey? =
        KeyType.entries.firstOrNull { it.name == type }?.let { PhysicalKey(id, it, label, secretHash) }
}
