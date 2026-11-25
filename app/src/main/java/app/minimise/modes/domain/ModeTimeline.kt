package app.minimise.modes.domain

import app.minimise.core.prefs.Settings
import app.minimise.settings.domain.Direction
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** A mode picked by hand, and when it hands back to the schedules (null: not until it's changed). */
data class ManualPick(val modeId: Long, val until: Instant?) {
    fun lastsAt(at: Instant): Boolean = until == null || at.isBefore(until)
}

/** The hand-picked mode still in force at [now], if any. One whose end has passed is no pick at all. */
fun Settings.manualPickAt(now: Instant): ManualPick? =
    manualModeId?.let { ManualPick(it, manualModeUntil) }?.takeIf { it.lastsAt(now) }

/** Which mode is on at a given moment, and how changing the hand-picked one compares over time. */
object ModeTimeline {
    /** How far ahead a switch is weighed at most: a pick of up to a day, then a whole week of schedules. */
    private const val HORIZON_DAYS = 8L

    /** The hand-picked mode while it lasts, else the first mode whose schedule is on, else none. */
    fun activeAt(at: Instant, zone: ZoneId, modes: List<Mode>, pick: ManualPick?): Mode? {
        if (pick != null && pick.lastsAt(at)) modes.firstOrNull { it.id == pick.modeId }?.let { return it }
        val local = at.atZone(zone).toLocalDateTime()
        return modes.firstOrNull { it.schedule?.isActive(local) == true }
    }

    /**
     * Changing the hand-picked mode from [from] to [to] (null: follow the schedules), weighed minute by
     * minute from [now] until both have handed back to the schedules. Stricter or the same throughout is
     * tightening; looser at any moment is loosening. So picking a stricter mode that ends sooner than
     * the one on now can loosen, and so can picking a mode that would cover a stricter scheduled one.
     * Null when nothing would change.
     */
    fun switchDirection(
        now: Instant,
        zone: ZoneId,
        modes: List<Mode>,
        base: ModeValues,
        from: ManualPick?,
        to: ManualPick?,
    ): Direction? {
        val before = from?.takeIf { it.lastsAt(now) }
        val after = to?.takeIf { it.lastsAt(now) }
        if (before == after) return null
        val limit = now.plus(HORIZON_DAYS, ChronoUnit.DAYS)
        val end = listOfNotNull(before, after).maxOf { pick -> pick.until?.takeIf { it.isBefore(limit) } ?: limit }
        val samples = buildList {
            add(now)
            listOfNotNull(before?.until, after?.until).filterTo(this) { it.isBefore(end) }
            var minute = now.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES)
            while (minute.isBefore(end)) {
                add(minute)
                minute = minute.plus(1, ChronoUnit.MINUTES)
            }
        }
        // Values only change when the mode in force does, so each pair of modes is compared once.
        val seen = mutableMapOf<Pair<Long?, Long?>, Strictness>()
        val loosens = samples.any { at ->
            val a = activeAt(at, zone, modes, before)
            val b = activeAt(at, zone, modes, after)
            val strictness = seen.getOrPut(a?.id to b?.id) { compare(a?.values ?: base, b?.values ?: base) }
            strictness == Strictness.LOOSER || strictness == Strictness.MIXED
        }
        return if (loosens) Direction.LOOSEN else Direction.TIGHTEN
    }
}
