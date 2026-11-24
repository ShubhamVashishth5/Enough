package app.minimise.digest.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow

data class HeldNotification(val id: Long, val packageName: String, val title: String, val text: String, val postedAt: Instant)

/** What a digest releases: how many updates, from how many apps. */
data class DigestSummary(val updates: Int, val apps: Int)

interface DigestRepository {
    /** Held and not yet released: home shows this as "4 updates waiting". */
    val waitingCount: Flow<Int>

    /** Released by a digest and not yet cleared: the Digest screen. */
    val released: Flow<List<HeldNotification>>

    suspend fun hold(packageName: String, title: String, text: String, postedAt: Instant)

    /** Releases everything held so far. */
    suspend fun releaseAll(at: Instant): DigestSummary

    suspend fun clearReleased()

    suspend fun remove(id: Long)
}

/** Schedules the next digest alarm. */
interface DigestScheduler {
    fun scheduleNext()
}

object DigestTimes {
    /** The first of [times] strictly after [now], today or tomorrow. */
    fun nextAfter(now: ZonedDateTime, times: List<LocalTime>): ZonedDateTime {
        val today = times.sorted().map { now.with(it).withSecond(0).withNano(0) }
        return today.firstOrNull { it.isAfter(now) } ?: today.first().plusDays(1)
    }
}
