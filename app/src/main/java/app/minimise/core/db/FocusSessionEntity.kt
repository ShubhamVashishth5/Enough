package app.minimise.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * One focus session. [endAt] is when it's planned to end; [endedAt] is set once it has.
 * [allowlist] is comma-separated package names. [previousInterruptionFilter] is the Do Not Disturb
 * state to put back, when Do Not Disturb was switched on for it.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startAt: Instant,
    val endAt: Instant,
    val endedAt: Instant?,
    val locked: Boolean,
    val allowlist: String,
    val previousInterruptionFilter: Int?,
)
