package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.minimise.apps.domain.AppKind
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps")
    fun observeAll(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName LIKE 'web:%' ORDER BY packageName")
    fun observeWebTargets(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps")
    suspend fun all(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppEntity?

    /** Adds rows for packages seen for the first time. Existing rows are left alone. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(apps: List<AppEntity>)

    @Query("UPDATE apps SET kind = :kind WHERE packageName = :packageName")
    suspend fun setKind(packageName: String, kind: AppKind)

    @Query("UPDATE apps SET openLimit = :limit WHERE packageName = :packageName")
    suspend fun setOpenLimit(packageName: String, limit: Int?)

    @Query("UPDATE apps SET breatheWaitSeconds = :seconds WHERE packageName = :packageName")
    suspend fun setBreatheWait(packageName: String, seconds: Int?)

    @Query("UPDATE apps SET homeSlot = NULL WHERE homeSlot = :slot")
    suspend fun clearSlot(slot: Int)

    @Query("UPDATE apps SET homeSlot = :slot WHERE packageName = :packageName")
    suspend fun setSlot(packageName: String, slot: Int)

    @Transaction
    suspend fun assignSlot(packageName: String, slot: Int) {
        clearSlot(slot)
        setSlot(packageName, slot)
    }
}
