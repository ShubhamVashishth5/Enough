package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface BufferedNotificationDao {
    @Insert
    suspend fun insert(item: BufferedNotificationEntity)

    @Query("SELECT COUNT(*) FROM buffered_notifications WHERE releasedAt IS NULL")
    fun observeWaitingCount(): Flow<Int>

    @Query("SELECT * FROM buffered_notifications WHERE releasedAt IS NOT NULL ORDER BY postedAt DESC")
    fun observeReleased(): Flow<List<BufferedNotificationEntity>>

    @Query("SELECT * FROM buffered_notifications WHERE releasedAt IS NULL")
    suspend fun waiting(): List<BufferedNotificationEntity>

    @Query("UPDATE buffered_notifications SET releasedAt = :at WHERE releasedAt IS NULL")
    suspend fun releaseAll(at: Instant)

    @Query("DELETE FROM buffered_notifications WHERE releasedAt IS NOT NULL")
    suspend fun clearReleased()

    @Query("DELETE FROM buffered_notifications WHERE id = :id")
    suspend fun delete(id: Long)
}
