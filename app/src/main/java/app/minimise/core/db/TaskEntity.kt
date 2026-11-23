package app.minimise.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tasks", indices = [Index("day")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val day: LocalDate,
    val done: Boolean,
    val position: Int,
)
