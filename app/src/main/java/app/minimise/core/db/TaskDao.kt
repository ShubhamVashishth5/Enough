package app.minimise.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.minimise.tasks.domain.TaskReplace
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

    /** Tasks ticked off on days in [from, until). */
    @Query("SELECT COUNT(*) FROM tasks WHERE done = 1 AND day >= :from AND day < :until")
    suspend fun doneBetween(from: LocalDate, until: LocalDate): Int

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    /** Makes [texts] the day's tasks, in order, as [TaskReplace] plans it. */
    @Transaction
    suspend fun replaceDay(day: LocalDate, texts: List<String>) {
        val existing = tasksFor(day)
        val plan = TaskReplace.plan(existing.map { it.text }, texts)
        plan.steps.forEach { step ->
            when (step) {
                is TaskReplace.Step.Keep -> existing[step.from].let { if (it.position != step.position) update(it.copy(position = step.position)) }
                is TaskReplace.Step.Edit -> update(existing[step.from].copy(text = step.text, done = false, position = step.position))
                is TaskReplace.Step.Add -> insert(TaskEntity(text = step.text, day = day, done = false, position = step.position))
            }
        }
        plan.removed.forEach { delete(existing[it].id) }
    }
}
