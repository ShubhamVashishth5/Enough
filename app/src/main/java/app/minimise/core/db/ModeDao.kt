package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {
    @Query("SELECT * FROM modes ORDER BY id")
    fun observeAll(): Flow<List<ModeEntity>>

    @Query("SELECT * FROM modes ORDER BY id")
    suspend fun all(): List<ModeEntity>

    @Query("SELECT * FROM modes WHERE id = :id")
    suspend fun get(id: Long): ModeEntity?

    @Insert
    suspend fun insertAll(modes: List<ModeEntity>)

    @Insert
    suspend fun insert(mode: ModeEntity): Long

    /** Only the user's own modes ([ModeEntity.CUSTOM]); the shipped ones stay. */
    @Query("DELETE FROM modes WHERE id = :id AND presetKey = 'custom'")
    suspend fun deleteCustom(id: Long)

    @Update
    suspend fun update(mode: ModeEntity)
}
