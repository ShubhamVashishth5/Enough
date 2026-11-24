package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface GrantDao {
    @Query("SELECT * FROM grants WHERE packageName = :packageName")
    suspend fun get(packageName: String): GrantEntity?

    @Upsert
    suspend fun upsert(grant: GrantEntity)

    @Query("DELETE FROM grants WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
