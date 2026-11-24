package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SwapDao {
    @Query("SELECT * FROM swaps ORDER BY id")
    fun observeAll(): Flow<List<SwapEntity>>

    @Query("SELECT * FROM swaps ORDER BY id")
    suspend fun all(): List<SwapEntity>

    @Upsert
    suspend fun upsert(swap: SwapEntity)

    @Insert
    suspend fun insertAll(swaps: List<SwapEntity>)

    @Query("DELETE FROM swaps WHERE id = :id")
    suspend fun delete(id: Long)
}
