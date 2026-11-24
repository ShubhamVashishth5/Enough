package app.minimise.stats.domain

import app.minimise.core.time.durationBetween
import app.minimise.gate.rules.BudgetRule
import app.minimise.intervention.domain.Trigger
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventType
import app.minimise.usage.domain.SpanTime
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One week's headline numbers. */
data class WeekNumbers(val vaultTime: Duration, val unlocks: Int, val focusTime: Duration, val tasksDone: Int)

data class TimeSink(val packageName: String, val time: Duration)

/**
 * The Sunday report: this week so far against last week, where the time went, and when slips happen.
 * A slip is reaching for a Vault app after the day's budget was used up.
 *
 * @property busiestSlipHour the start of the two-hour stretch with the most slips, or null without any.
 * @property daysUnderBudget completed days this month that never ran out of budget, of [daysSoFar].
 */
data class WeeklyReport(
    val thisWeek: WeekNumbers,
    val lastWeek: WeekNumbers,
    val topSinks: List<TimeSink>,
    val slips: Int,
    val busiestSlipHour: Int?,
    val topTrigger: Trigger?,
    val daysUnderBudget: Int,
    val daysSoFar: Int,
)

/** A stretch of time: one week, or one usage day. Ends are exclusive. */
data class Period(val start: Instant, val end: Instant)

object WeeklyReports {
    private const val TOP_SINKS = 3
    private const val SLIP_WINDOW_HOURS = 2

    /**
     * @param events every event overlapping last week up to now, plus the month's days
     * @param monthDays this month's completed usage days
     * @param tasksDone tasks ticked off this week and last week
     */
    fun build(
        events: List<Event>,
        thisWeek: Period,
        lastWeek: Period,
        monthDays: List<Period>,
        now: Instant,
        zone: ZoneId,
        vaultPackages: Set<String>,
        tasksDone: Pair<Int, Int>,
        openSpanId: Long? = null,
    ): WeeklyReport {
        val inVault: (String) -> Boolean = { it in vaultPackages }
        val sinks = SpanTime.byPackage(events, thisWeek.start, thisWeek.end, now, openSpanId, inVault)
            .entries.sortedByDescending { it.value }.take(TOP_SINKS).map { TimeSink(it.key, it.value) }
        val slips = events.filter { it.isSlip() && it.startAt in thisWeek }
        val triggers = events.filter { it.type == EventType.TRIGGER && it.startAt in thisWeek }.mapNotNull { it.trigger() }
        return WeeklyReport(
            thisWeek = numbers(events, thisWeek, now, openSpanId, inVault, tasksDone.first),
            lastWeek = numbers(events, lastWeek, now, openSpanId, inVault, tasksDone.second),
            topSinks = sinks,
            slips = slips.size,
            busiestSlipHour = busiestWindow(slips.map { it.startAt.atZone(zone).hour }),
            topTrigger = triggers.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
            daysUnderBudget = monthDays.count { day -> !ranOutOfBudget(events, day) },
            daysSoFar = monthDays.size,
        )
    }

    /** The day saw the budget run out: a budget block, or a time box cut short by it. */
    fun ranOutOfBudget(events: List<Event>, day: Period): Boolean =
        events.any { it.startAt in day && (it.isSlip() || it.isBudgetTimeUp()) }

    /** The start hour of the [SLIP_WINDOW_HOURS]-hour stretch (wrapping past midnight) with the most [hours]. */
    fun busiestWindow(hours: List<Int>): Int? {
        if (hours.isEmpty()) return null
        val perHour = IntArray(24).also { counts -> hours.forEach { counts[it]++ } }
        return (0 until 24).maxBy { start -> (0 until SLIP_WINDOW_HOURS).sumOf { perHour[(start + it) % 24] } }
    }

    private fun numbers(events: List<Event>, week: Period, now: Instant, openSpanId: Long?, inVault: (String) -> Boolean, tasks: Int) =
        WeekNumbers(
            vaultTime = SpanTime.total(events, week.start, week.end, now, openSpanId, inVault),
            unlocks = events.count { it.type == EventType.SCREEN_UNLOCK && it.startAt in week },
            focusTime = events.filter { it.type == EventType.FOCUS_SESSION }.fold(Duration.ZERO) { total, session ->
                val start = maxOf(session.startAt, week.start)
                val end = minOf(session.endAt ?: session.startAt, week.end, now)
                if (end.isAfter(start)) total + durationBetween(start, end) else total
            },
            tasksDone = tasks,
        )

    private operator fun Period.contains(instant: Instant): Boolean = !instant.isBefore(start) && instant.isBefore(end)

    private fun Event.isSlip(): Boolean = type == EventType.BLOCK_SHOWN && field("reason") == BudgetRule.REASON

    private fun Event.isBudgetTimeUp(): Boolean = type == EventType.TIME_UP && field("budgetUsed") == "true"

    private fun Event.trigger(): Trigger? = field("trigger")?.let { name -> Trigger.entries.firstOrNull { it.name == name } }

    private fun Event.field(name: String): String? =
        data?.let { runCatching { Json.parseToJsonElement(it).jsonObject[name]?.jsonPrimitive?.content }.getOrNull() }
}
