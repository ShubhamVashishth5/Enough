package app.minimise.settings.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class PendingChange(
    val id: Long,
    val change: SettingChange,
    val requestedAt: Instant,
    val applyAt: Instant,
)

interface PendingChangeStore {
    /** Soonest first. */
    fun observeAll(): Flow<List<PendingChange>>

    /** Soonest first. */
    suspend fun all(): List<PendingChange>

    suspend fun add(change: SettingChange, requestedAt: Instant, applyAt: Instant): Long

    suspend fun remove(id: Long)

    suspend fun removeKey(key: String)
}
