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

    @Update
    suspend fun update(mode: ModeEntity)
}
