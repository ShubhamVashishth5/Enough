package app.minimise.settings.data

import app.minimise.core.db.PendingChangeDao
import app.minimise.core.db.PendingChangeEntity
import app.minimise.settings.domain.PendingChange
import app.minimise.settings.domain.PendingChangeStore
import app.minimise.settings.domain.SettingChange
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

class RoomPendingChangeStore @Inject constructor(
    private val dao: PendingChangeDao,
) : PendingChangeStore {
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeAll(): Flow<List<PendingChange>> = dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun all(): List<PendingChange> = dao.all().mapNotNull { it.toDomain() }

    override suspend fun add(change: SettingChange, requestedAt: Instant, applyAt: Instant): Long {
        val payload = json.encodeToString(SettingChange.serializer(), change)
        val type = json.parseToJsonElement(payload).jsonObject["type"]?.jsonPrimitive?.content ?: "unknown"
        return dao.insert(
            PendingChangeEntity(
                type = type,
                changeKey = change.key,
                payload = payload,
                requestedAt = requestedAt,
                applyAt = applyAt,
            ),
        )
    }

    override suspend fun remove(id: Long) = dao.delete(id)

    override suspend fun removeKey(key: String) = dao.deleteKey(key)

    private fun PendingChangeEntity.toDomain(): PendingChange? =
        runCatching { json.decodeFromString(SettingChange.serializer(), payload) }
            .onFailure { Timber.e(it, "Unreadable pending change %d", id) }
            .getOrNull()
            ?.let { PendingChange(id = id, change = it, requestedAt = requestedAt, applyAt = applyAt) }
}
