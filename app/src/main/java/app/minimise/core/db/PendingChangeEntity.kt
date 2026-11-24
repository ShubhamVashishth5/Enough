package app.minimise.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** A loosening change waiting for its delay. [payload] is the SettingChange as JSON. */
@Entity(tableName = "pending_changes", indices = [Index("changeKey")])
data class PendingChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val changeKey: String,
    val payload: String,
    val requestedAt: Instant,
    val applyAt: Instant,
)
