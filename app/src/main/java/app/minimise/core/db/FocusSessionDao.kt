package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    /** The session that hasn't been ended yet, if any. */
    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeOpen(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun open(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NOT NULL ORDER BY endedAt DESC LIMIT 1")
    fun observeLastEnded(): Flow<FocusSessionEntity?>

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("UPDATE focus_sessions SET endedAt = :endedAt WHERE id = :id AND endedAt IS NULL")
    suspend fun end(id: Long, endedAt: Instant): Int
}
