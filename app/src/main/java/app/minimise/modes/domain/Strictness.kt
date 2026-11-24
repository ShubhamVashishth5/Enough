package app.minimise.modes.domain

import app.minimise.core.time.WeeklyWindow
import app.minimise.settings.domain.Direction

/** How one set of values compares with another. MIXED: stricter in one way, looser in another. */
enum class Strictness { STRICTER, SAME, LOOSER, MIXED }

/** How [to] compares with [from]: a smaller budget, longer waits or a narrower allowlist is stricter. */
fun compare(from: ModeValues, to: ModeValues): Strictness {
    val parts = listOf(
        compareBy(to.budget.compareTo(from.budget), lowerIsStricter = true),
        compareWaits(from, to),
        compareAllowlists(from.allowlist, to.allowlist),
    )
    return combine(parts)
}

/** Moving to stricter values tightens; anything looser, even in part, waits. */
fun Strictness.toDirection(): Direction? = when (this) {
    Strictness.STRICTER -> Direction.TIGHTEN
    Strictness.SAME -> null
    Strictness.LOOSER, Strictness.MIXED -> Direction.LOOSEN
}

/**
 * Editing a mode: its values, and its schedule weighed by how it compares with having no mode
 * ([base]). Giving a stricter mode more hours tightens; giving it fewer loosens, and the other way
 * round for a looser mode.
 */
fun modeEditDirection(base: ModeValues, old: Mode, newValues: ModeValues, newSchedule: WeeklyWindow?): Direction? {
    val values = compare(old.values, newValues).toDirection()
    val schedule = scheduleDirection(old.schedule, newSchedule, compare(base, newValues))
    return when {
        values == Direction.LOOSEN || schedule == Direction.LOOSEN -> Direction.LOOSEN
        values == Direction.TIGHTEN || schedule == Direction.TIGHTEN -> Direction.TIGHTEN
        else -> null
    }
}

private fun scheduleDirection(old: WeeklyWindow?, new: WeeklyWindow?, modeVsBase: Strictness): Direction? {
    if (old == new) return null
    // A mode no different from having none: its hours change nothing.
    if (modeVsBase == Strictness.SAME) return Direction.TIGHTEN
    val grows = new != null && (old == null || new.covers(old))
    val shrinks = old != null && (new == null || old.covers(new))
    return when {
        grows && modeVsBase == Strictness.STRICTER -> Direction.TIGHTEN
        shrinks && modeVsBase == Strictness.LOOSER -> Direction.TIGHTEN
        else -> Direction.LOOSEN
    }
}

private fun compareBy(order: Int, lowerIsStricter: Boolean): Strictness = when {
    order == 0 -> Strictness.SAME
    (order < 0) == lowerIsStricter -> Strictness.STRICTER
    else -> Strictness.LOOSER
}

private fun compareWaits(from: ModeValues, to: ModeValues): Strictness {
    val length = maxOf(from.waitSteps.size, to.waitSteps.size)
    val parts = (0 until length).map { i ->
        val a = from.waitSteps.getOrElse(i) { from.waitSteps.last() }
        val b = to.waitSteps.getOrElse(i) { to.waitSteps.last() }
        compareBy(b.compareTo(a), lowerIsStricter = false)
    }
    return combine(parts)
}

private fun compareAllowlists(from: Set<String>?, to: Set<String>?): Strictness = when {
    from == to -> Strictness.SAME
    from == null -> Strictness.STRICTER
    to == null -> Strictness.LOOSER
    from.containsAll(to) -> Strictness.STRICTER
    to.containsAll(from) -> Strictness.LOOSER
    else -> Strictness.MIXED
}

private fun combine(parts: List<Strictness>): Strictness {
    val stricter = Strictness.STRICTER in parts
    val looser = Strictness.LOOSER in parts
    return when {
        Strictness.MIXED in parts || (stricter && looser) -> Strictness.MIXED
        stricter -> Strictness.STRICTER
        looser -> Strictness.LOOSER
        else -> Strictness.SAME
    }
}
