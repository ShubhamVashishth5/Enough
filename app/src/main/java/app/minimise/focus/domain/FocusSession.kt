package app.minimise.focus.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** 25, 50 or 90 minutes when only the allowlist opens. A locked session ends early only with a physical key. */
data class FocusSession(
    val id: Long,
    val startAt: Instant,
    val endAt: Instant,
    val endedAt: Instant?,
    val locked: Boolean,
    val allowlist: Set<String>,
    val previousInterruptionFilter: Int? = null,
) {
    fun isActive(now: Instant): Boolean = endedAt == null && now.isBefore(endAt)
}

interface FocusRepository {
    /** The session not yet ended (it may be past its end time and waiting to be finished). */
    val open: Flow<FocusSession?>

    val lastEnded: Flow<FocusSession?>

    suspend fun open(): FocusSession?

    suspend fun start(startAt: Instant, endAt: Instant, locked: Boolean, allowlist: Set<String>, previousInterruptionFilter: Int?)

    /** False when it had already ended. */
    suspend fun end(id: Long, at: Instant): Boolean
}

/** Do Not Disturb, when the user has given notification policy access. */
interface DoNotDisturb {
    fun isAvailable(): Boolean

    /** Turns on priority-only mode. Returns the filter to restore, or null when nothing changed. */
    fun enable(): Int?

    fun restore(previous: Int)
}

/** Apps a focus session must never block, such as the phone app for an incoming call. */
interface EssentialApps {
    fun packages(): Set<String>
}
