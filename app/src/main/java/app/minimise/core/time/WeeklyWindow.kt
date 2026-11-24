package app.minimise.core.time

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.BitSet

/**
 * A stretch of time that repeats on chosen days, such as Mon–Fri 22:00–07:00. A window whose [end] is
 * at or before its [start] runs past midnight into the next day; [days] are the days it starts on.
 */
data class WeeklyWindow(val days: Set<DayOfWeek>, val start: LocalTime, val end: LocalTime) {
    val crossesMidnight: Boolean get() = !end.isAfter(start)

    fun isActive(at: LocalDateTime): Boolean {
        val time = at.toLocalTime()
        val day = at.dayOfWeek
        return if (!crossesMidnight) {
            day in days && !time.isBefore(start) && time.isBefore(end)
        } else {
            (day in days && !time.isBefore(start)) || (day.minus(1) in days && time.isBefore(end))
        }
    }

    /** True when every minute of [other] is also inside this window. */
    fun covers(other: WeeklyWindow): Boolean {
        val mine = minutesOfWeek()
        val theirs = other.minutesOfWeek()
        theirs.andNot(mine)
        return theirs.isEmpty
    }

    /** Each covered minute of the week, Monday 00:00 = 0. */
    private fun minutesOfWeek(): BitSet {
        val bits = BitSet(MINUTES_PER_WEEK)
        val startMinute = start.toSecondOfDay() / 60
        val endMinute = end.toSecondOfDay() / 60
        val length = if (crossesMidnight) MINUTES_PER_DAY - startMinute + endMinute else endMinute - startMinute
        days.forEach { day ->
            val from = (day.value - 1) * MINUTES_PER_DAY + startMinute
            for (m in 0 until length) bits.set((from + m) % MINUTES_PER_WEEK)
        }
        return bits
    }

    /** Days stored as bits, Monday = bit 0. */
    fun daysMask(): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
        private const val MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY

        fun daysFromMask(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

        fun fromStored(daysMask: Int, startMinute: Int, endMinute: Int) = WeeklyWindow(
            days = daysFromMask(daysMask),
            start = LocalTime.of(startMinute / 60, startMinute % 60),
            end = LocalTime.of(endMinute / 60, endMinute % 60),
        )
    }
}

val LocalTime.minuteOfDay: Int get() = hour * 60 + minute
