package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY startMinute, id")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules ORDER BY startMinute, id")
    suspend fun all(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun get(id: Long): ScheduleEntity?

    @Upsert
    suspend fun upsert(schedule: ScheduleEntity): Long

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun delete(id: Long)
}
