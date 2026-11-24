package app.minimise.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** One running (or last) time box per app. */
@Entity(tableName = "grants")
data class GrantEntity(
    @PrimaryKey val packageName: String,
    val startAt: Instant,
    val endAt: Instant,
    /** Granted by an override (emergency pass, key) rather than a pause. */
    @ColumnInfo(defaultValue = "0") val viaOverride: Boolean = false,
)
