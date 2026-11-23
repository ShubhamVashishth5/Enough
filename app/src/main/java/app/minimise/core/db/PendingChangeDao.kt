package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingChangeDao {
    @Query("SELECT * FROM pending_changes ORDER BY applyAt, id")
    fun observeAll(): Flow<List<PendingChangeEntity>>

    @Query("SELECT * FROM pending_changes ORDER BY applyAt, id")
    suspend fun all(): List<PendingChangeEntity>

    @Insert
    suspend fun insert(change: PendingChangeEntity): Long

    @Query("DELETE FROM pending_changes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_changes WHERE changeKey = :key")
    suspend fun deleteKey(key: String)
}
