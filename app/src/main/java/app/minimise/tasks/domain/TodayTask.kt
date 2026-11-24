package app.minimise.tasks.domain

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class TodayTask(
    val id: Long,
    val text: String,
    val day: LocalDate,
    val done: Boolean,
    val position: Int,
)

interface TaskRepository {
    /** In position order. */
    fun observeDay(day: LocalDate): Flow<List<TodayTask>>

    suspend fun tasksFor(day: LocalDate): List<TodayTask>

    /**
     * Makes [texts] the day's tasks, in order. A task whose text is unchanged keeps its done mark;
     * blank entries are dropped.
     */
    suspend fun replaceDay(day: LocalDate, texts: List<String>)

    suspend fun setDone(id: Long, done: Boolean)

    /** Tasks ticked off on usage days in [from, until). */
    suspend fun doneBetween(from: LocalDate, until: LocalDate): Int
}
