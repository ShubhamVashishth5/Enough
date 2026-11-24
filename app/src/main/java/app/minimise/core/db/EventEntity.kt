package app.minimise.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.minimise.usage.domain.EventType
import java.time.Instant

@Entity(
    tableName = "events",
    indices = [Index("startAt"), Index("type", "startAt")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EventType,
    val packageName: String?,
    val startAt: Instant,
    val endAt: Instant?,
    @ColumnInfo(name = "data") val data: String?,
)
