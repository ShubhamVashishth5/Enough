package app.minimise.schedules.data

import app.minimise.core.db.ScheduleDao
import app.minimise.core.db.ScheduleEntity
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.time.minuteOfDay
import app.minimise.schedules.domain.Schedule
import app.minimise.schedules.domain.ScheduleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScheduleRepository @Inject constructor(
    private val dao: ScheduleDao,
) : ScheduleRepository {
    override val schedules: Flow<List<Schedule>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun all(): List<Schedule> = dao.all().map { it.toDomain() }

    override suspend fun get(id: Long): Schedule? = dao.get(id)?.toDomain()

    override suspend fun save(schedule: Schedule) {
        dao.upsert(
            ScheduleEntity(
                id = schedule.id,
                name = schedule.name,
                days = schedule.window.daysMask(),
                startMinute = schedule.window.start.minuteOfDay,
                endMinute = schedule.window.end.minuteOfDay,
            ),
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    private fun ScheduleEntity.toDomain() = Schedule(id, name, WeeklyWindow.fromStored(days, startMinute, endMinute))
}
