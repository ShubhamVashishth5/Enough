package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** A one-time code held by a friend. Only its hash is kept; [usedAt] is set once it's spent. */
@Entity(tableName = "friend_codes")
data class FriendCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codeHash: String,
    val createdAt: Instant,
    val usedAt: Instant? = null,
)

@Dao
interface FriendCodeDao {
    @Query("SELECT COUNT(*) FROM friend_codes WHERE usedAt IS NULL")
    fun observeUnused(): Flow<Int>

    @Query("SELECT COUNT(*) FROM friend_codes WHERE usedAt IS NULL")
    suspend fun unused(): Int

    @Query("SELECT codeHash FROM friend_codes WHERE usedAt IS NULL")
    suspend fun unusedHashes(): List<String>

    @Query("DELETE FROM friend_codes")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(codes: List<FriendCodeEntity>)

    @Transaction
    suspend fun replaceAll(codes: List<FriendCodeEntity>) {
        deleteAll()
        insertAll(codes)
    }

    /** Spends an unused code. Returns how many rows changed: 1 when the code was good. */
    @Query("UPDATE friend_codes SET usedAt = :at WHERE codeHash = :hash AND usedAt IS NULL")
    suspend fun spend(hash: String, at: Instant): Int
}
