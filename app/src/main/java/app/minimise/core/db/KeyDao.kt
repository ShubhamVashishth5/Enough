package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {
    @Query("SELECT * FROM keys ORDER BY id")
    fun observeAll(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM keys ORDER BY id")
    suspend fun all(): List<KeyEntity>

    @Query("SELECT * FROM keys WHERE id = :id")
    suspend fun get(id: Long): KeyEntity?

    @Insert
    suspend fun insert(key: KeyEntity): Long

    @Query("DELETE FROM keys WHERE id = :id")
    suspend fun delete(id: Long)
}
