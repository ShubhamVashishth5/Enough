package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.minimise.usage.domain.EventType
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Rows that overlap [from, to). Point events have no endAt, so they fall back to startAt. */
private const val OVERLAP_QUERY = """
    SELECT * FROM events
    WHERE startAt < :to AND COALESCE(endAt, startAt) >= :from
      AND (:type IS NULL OR type = :type)
    ORDER BY startAt, id
"""

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query(OVERLAP_QUERY)
    suspend fun between(from: Instant, to: Instant, type: EventType?): List<EventEntity>

    @Query(OVERLAP_QUERY)
    fun observeBetween(from: Instant, to: Instant, type: EventType?): Flow<List<EventEntity>>
}
