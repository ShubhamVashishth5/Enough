package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE day = :day ORDER BY position")
    fun observeDay(day: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE day = :day ORDER BY position")
    suspend fun tasksFor(day: LocalDate): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Transaction
    suspend fun replaceDay(day: LocalDate, texts: List<String>) {
        val existing = tasksFor(day)
        texts.forEachIndexed { position, text ->
            val old = existing.getOrNull(position)
            when {
                old == null -> insert(TaskEntity(text = text, day = day, done = false, position = position))
                old.text != text -> update(old.copy(text = text, done = false))
            }
        }
        existing.drop(texts.size).forEach { delete(it.id) }
    }
}
