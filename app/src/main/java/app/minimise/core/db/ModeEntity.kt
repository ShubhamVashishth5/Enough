package app.minimise.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A mode's rule values. [waitStepsSeconds] and [allowlist] are comma-separated; a null allowlist
 * means every app keeps its usual kind. [scheduleDays] 0 means the mode has no schedule. [presetKey]
 * is [CUSTOM] for a mode the user made.
 */
@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetKey: String,
    val name: String,
    val budgetMinutes: Int,
    val waitStepsSeconds: String,
    val allowlist: String?,
    val scheduleDays: Int,
    val scheduleStartMinute: Int,
    val scheduleEndMinute: Int,
) {
    companion object {
        const val CUSTOM = "custom"
    }
}
