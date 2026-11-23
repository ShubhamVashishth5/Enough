package app.minimise.usage.domain

import app.minimise.core.time.durationBetween
import java.time.Instant
import kotlin.time.Duration

/** Today's numbers, all derived from the event log. */
data class TodayUsage(
    /** Time in Vault apps since the day started. */
    val vaultTime: Duration = Duration.ZERO,
    /** Vault opens the user went through with (past the pause screen). */
    val proceededOpens: Int = 0,
    val unlocks: Int = 0,
) {
    companion object {
        /**
         * @param events every event overlapping [dayStart, dayEnd)
         * @param openSpanId the span still being written: its end is taken as [now]
         */
        fun from(
            events: List<Event>,
            dayStart: Instant,
            dayEnd: Instant,
            now: Instant,
            vaultPackages: Set<String>,
            openSpanId: Long?,
        ): TodayUsage {
            val countFrom: (Event) -> Boolean = { !it.startAt.isBefore(dayStart) && it.startAt.isBefore(dayEnd) }
            val vaultTime = events
                .filter { it.type == EventType.APP_SPAN && it.packageName in vaultPackages }
                .fold(Duration.ZERO) { total, span ->
                    val rawEnd = if (span.id == openSpanId) now else span.endAt ?: span.startAt
                    val start = maxOf(span.startAt, dayStart)
                    val end = minOf(rawEnd, dayEnd, now)
                    if (end.isAfter(start)) total + durationBetween(start, end) else total
                }
            return TodayUsage(
                vaultTime = vaultTime,
                proceededOpens = events.count { it.type == EventType.GATE_PROCEEDED && countFrom(it) },
                unlocks = events.count { it.type == EventType.SCREEN_UNLOCK && countFrom(it) },
            )
        }
    }
}
