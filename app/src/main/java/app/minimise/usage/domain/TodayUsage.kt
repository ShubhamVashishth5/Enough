package app.minimise.usage.domain

import java.time.Instant
import kotlin.time.Duration

/** Today's numbers, all derived from the event log. */
data class TodayUsage(
    /** Time in Vault apps since the day started. */
    val vaultTime: Duration = Duration.ZERO,
    /** Vault opens the user went through with (past the pause screen). */
    val proceededOpens: Int = 0,
    /** [proceededOpens] split by app. */
    val proceededOpensByPackage: Map<String, Int> = emptyMap(),
    /** Pause screens shown, however they ended. */
    val pausesShown: Int = 0,
    val unlocks: Int = 0,
    /** The day's first screen unlock, if there has been one. */
    val firstUnlockAt: Instant? = null,
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
            val vaultTime = SpanTime.total(events, dayStart, dayEnd, now, openSpanId) { it in vaultPackages }
            val unlocks = events.filter { it.type == EventType.SCREEN_UNLOCK && countFrom(it) }
            val proceeded = events.filter { it.type == EventType.GATE_PROCEEDED && countFrom(it) }
            return TodayUsage(
                vaultTime = vaultTime,
                proceededOpens = proceeded.size,
                proceededOpensByPackage = proceeded.groupingBy { it.packageName.orEmpty() }.eachCount(),
                pausesShown = events.count { it.type == EventType.GATE_SHOWN && countFrom(it) },
                unlocks = unlocks.size,
                firstUnlockAt = unlocks.minOfOrNull { it.startAt },
            )
        }
    }
}
