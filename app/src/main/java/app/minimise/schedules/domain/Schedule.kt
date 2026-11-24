package app.minimise.schedules.domain

import app.minimise.core.time.WeeklyWindow
import kotlinx.coroutines.flow.Flow

/** A window when every Vault app is blocked, such as bedtime or focus hours. */
data class Schedule(val id: Long, val name: String, val window: WeeklyWindow)

interface ScheduleRepository {
    val schedules: Flow<List<Schedule>>

    suspend fun all(): List<Schedule>

    suspend fun get(id: Long): Schedule?

    /** Writes directly (adds when id is 0). Only SettingsGuard should call this. */
    suspend fun save(schedule: Schedule)

    /** Only SettingsGuard should call this. */
    suspend fun delete(id: Long)
}
