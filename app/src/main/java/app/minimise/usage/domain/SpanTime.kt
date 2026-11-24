package app.minimise.usage.domain

import app.minimise.core.time.durationBetween
import java.time.Instant
import kotlin.time.Duration

/** Time spent in apps, from APP_SPAN events, clipped to a range. */
object SpanTime {
    /**
     * Time in [from, to) spent in apps for which [counts] is true, never past [now].
     * @param openSpanId the span still being written: its end is taken as [now].
     */
    fun byPackage(
        events: List<Event>,
        from: Instant,
        to: Instant,
        now: Instant,
        openSpanId: Long? = null,
        counts: (String) -> Boolean = { true },
    ): Map<String, Duration> {
        val totals = mutableMapOf<String, Duration>()
        events.filter { it.type == EventType.APP_SPAN }.forEach { span ->
            val packageName = span.packageName ?: return@forEach
            if (!counts(packageName)) return@forEach
            val rawEnd = if (span.id == openSpanId) now else span.endAt ?: span.startAt
            val start = maxOf(span.startAt, from)
            val end = minOf(rawEnd, to, now)
            if (end.isAfter(start)) totals[packageName] = (totals[packageName] ?: Duration.ZERO) + durationBetween(start, end)
        }
        return totals
    }

    fun total(
        events: List<Event>,
        from: Instant,
        to: Instant,
        now: Instant,
        openSpanId: Long? = null,
        counts: (String) -> Boolean = { true },
    ): Duration = byPackage(events, from, to, now, openSpanId, counts).values.fold(Duration.ZERO, Duration::plus)
}
