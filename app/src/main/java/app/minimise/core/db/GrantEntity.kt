package app.minimise.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** One running (or last) time box per app. */
@Entity(tableName = "grants")
data class GrantEntity(
    @PrimaryKey val packageName: String,
    val startAt: Instant,
    val endAt: Instant,
)
