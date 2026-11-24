package app.minimise.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A blocking window. [days] has Monday as bit 0; times are minutes after midnight. */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val days: Int,
    val startMinute: Int,
    val endMinute: Int,
)
