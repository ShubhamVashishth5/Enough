package app.minimise.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** A held notification. [releasedAt] is set when a digest goes out; until then home shows only a count. */
@Entity(tableName = "buffered_notifications", indices = [Index("releasedAt")])
data class BufferedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val text: String,
    val postedAt: Instant,
    val releasedAt: Instant? = null,
)
