package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    /** Open notes first, newest first within each group. */
    @Query("SELECT * FROM notes ORDER BY done, createdAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE done = 0")
    fun observeOpenCount(): Flow<Int>

    @Insert
    suspend fun insert(note: NoteEntity)

    @Query("UPDATE notes SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}
