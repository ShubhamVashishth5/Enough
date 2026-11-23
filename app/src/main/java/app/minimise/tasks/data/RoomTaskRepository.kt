package app.minimise.tasks.data

import app.minimise.core.db.TaskDao
import app.minimise.core.db.TaskEntity
import app.minimise.tasks.domain.TaskRepository
import app.minimise.tasks.domain.TodayTask
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskRepository @Inject constructor(
    private val dao: TaskDao,
) : TaskRepository {
    override fun observeDay(day: LocalDate): Flow<List<TodayTask>> = dao.observeDay(day).map { rows -> rows.map { it.toDomain() } }

    override suspend fun tasksFor(day: LocalDate): List<TodayTask> = dao.tasksFor(day).map { it.toDomain() }

    override suspend fun replaceDay(day: LocalDate, texts: List<String>) =
        dao.replaceDay(day, texts.map { it.trim() }.filter { it.isNotEmpty() })

    override suspend fun setDone(id: Long, done: Boolean) = dao.setDone(id, done)

    private fun TaskEntity.toDomain() = TodayTask(id = id, text = text, day = day, done = done, position = position)
}
