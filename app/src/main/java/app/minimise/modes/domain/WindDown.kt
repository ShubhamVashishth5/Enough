package app.minimise.modes.domain

import app.minimise.core.Defaults
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.time.minuteOfDay
import app.minimise.settings.domain.SaveMode
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Wind-down: every night from [Defaults.WIND_DOWN_LEAD] before [bedtime] until [wake], the Sleep mode is on.
 * Vault apps get no budget and only calls, the alarm and [ebook] open (the screen goes grey too).
 */
data class WindDownPlan(val bedtime: LocalTime, val wake: LocalTime, val ebook: String?)

object WindDownPlans {
    val DEFAULT = WindDownPlan(bedtime = LocalTime.of(23, 0), wake = LocalTime.of(7, 0), ebook = null)

    fun window(plan: WindDownPlan): WeeklyWindow =
        WeeklyWindow(days = DayOfWeek.entries.toSet(), start = plan.bedtime.minusMinutes(Defaults.WIND_DOWN_LEAD.inWholeMinutes), end = plan.wake)

    /** The plan a Sleep mode holds, or null when it isn't on every night. */
    fun of(sleep: Mode): WindDownPlan? {
        val schedule = sleep.schedule?.takeIf { it.days.size == DayOfWeek.entries.size } ?: return null
        return WindDownPlan(
            bedtime = schedule.start.plusMinutes(Defaults.WIND_DOWN_LEAD.inWholeMinutes),
            wake = schedule.end,
            ebook = sleep.values.allowlist?.firstOrNull(),
        )
    }

    /** The Sleep mode edit for [plan]; null switches wind-down off and leaves the rest of the mode alone. */
    fun change(sleep: Mode, plan: WindDownPlan?): SaveMode {
        if (plan == null) {
            return SaveMode(
                id = sleep.id,
                budgetMinutes = sleep.values.budget.inWholeMinutes.toInt(),
                allowlist = sleep.values.allowlist?.toList(),
                scheduleDays = 0,
                scheduleStartMinute = 0,
                scheduleEndMinute = 0,
            )
        }
        val window = window(plan)
        return SaveMode(
            id = sleep.id,
            budgetMinutes = 0,
            allowlist = listOfNotNull(plan.ebook),
            scheduleDays = window.daysMask(),
            scheduleStartMinute = window.start.minuteOfDay,
            scheduleEndMinute = window.end.minuteOfDay,
        )
    }
}
