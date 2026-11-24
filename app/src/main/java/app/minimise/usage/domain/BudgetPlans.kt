package app.minimise.usage.domain

import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.TimeSource
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SettingsGuard
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Budget arithmetic for taper and auto-tighten, in whole minutes. */
object BudgetMath {
    /** [budget] lowered by [percent] (rounded, at least a minute), never below [floor]. Null when already at the floor. */
    fun tapered(budget: Int, floor: Int, percent: Int = Defaults.TAPER_PERCENT): Int? {
        if (budget <= floor) return null
        val lowered = (budget * (100 - percent) / 100.0).roundToInt().coerceAtMost(budget - 1)
        return lowered.coerceAtLeast(floor)
    }

    /** The smaller budget auto-tighten offers. Null when there's nothing to take off. */
    fun tightened(budget: Int, percent: Int = Defaults.AUTO_TIGHTEN_PERCENT): Int? =
        if (budget <= 1) null else (budget * (100 - percent) / 100.0).roundToInt().coerceIn(1, budget - 1)
}

/**
 * Taper: on the first unlock of each usage week, the budget drops by [Defaults.TAPER_PERCENT] until it
 * reaches the floor the user set. A tightening change, so it applies at once through SettingsGuard,
 * which logs it.
 */
@Singleton
class Taper @Inject constructor(
    private val settingsStore: SettingsStore,
    private val settingsGuard: SettingsGuard,
    private val dayBoundary: DayBoundary,
) {
    private val mutex = Mutex()

    suspend fun applyIfDue() = mutex.withLock {
        val settings = settingsStore.current()
        val floor = settings.taperFloorMinutes ?: return@withLock
        val week = dayBoundary.weekStartOf(dayBoundary.today())
        if (settings.taperWeek?.isBefore(week) == false) return@withLock
        // Mark the week first, so a failure below can't taper twice.
        settingsStore.update { it.copy(taperWeek = week) }
        val next = BudgetMath.tapered(settings.dailyBudget.inWholeMinutes.toInt(), floor) ?: return@withLock
        settingsGuard.request(SetBudget(next))
    }
}

/**
 * Auto-tighten: after three days in a row over budget, the Today screen offers a slightly smaller
 * budget. Only offered, never forced; once answered, it waits for a fresh run of three days.
 */
class AutoTighten @Inject constructor(
    private val eventLog: EventLog,
    private val dayBoundary: DayBoundary,
    private val settingsStore: SettingsStore,
    private val settingsGuard: SettingsGuard,
) {
    /** The smaller budget on offer today, or null. */
    suspend fun offer(): Int? {
        val settings = settingsStore.current()
        val today = dayBoundary.today()
        val streak = (1..Defaults.AUTO_TIGHTEN_DAYS).map { today.minusDays(it.toLong()) }
        val answered = settings.tightenAnsweredDay
        if (answered != null && !streak.last().isAfter(answered)) return null
        val events = eventLog.between(dayBoundary.startOf(streak.last()), dayBoundary.startOf(today))
        val allOver = streak.all { BudgetDays.ranOut(events, dayBoundary.startOf(it), dayBoundary.endOf(it)) }
        return if (allOver) BudgetMath.tightened(settings.dailyBudget.inWholeMinutes.toInt()) else null
    }

    suspend fun accept(minutes: Int) {
        answered()
        settingsGuard.request(SetBudget(minutes))
    }

    suspend fun decline() = answered()

    private suspend fun answered() {
        val today = dayBoundary.today()
        settingsStore.update { it.copy(tightenAnsweredDay = today) }
    }
}

/**
 * Earn-it (off by default): finishing a task or a whole focus session adds minutes to today's budget,
 * as BUDGET_BONUS events that [DailyBudget] adds up. Each task earns once; the day has a cap.
 */
class EarnIt @Inject constructor(
    private val settingsStore: SettingsStore,
    private val eventLog: EventLog,
    private val dayBoundary: DayBoundary,
    private val timeSource: TimeSource,
) {
    suspend fun onTaskDone(taskId: Long) = earn(SOURCE_TASK, taskId.toString(), Defaults.EARN_IT_TASK_MINUTES)

    /** Only for a session that ran its full length. */
    suspend fun onFocusCompleted(sessionStart: Instant) = earn(SOURCE_FOCUS, sessionStart.toEpochMilli().toString(), Defaults.EARN_IT_FOCUS_MINUTES)

    private suspend fun earn(source: String, ref: String, minutes: Int) {
        if (!settingsStore.current().earnIt) return
        val today = dayBoundary.today()
        val events = eventLog.between(dayBoundary.startOf(today.minusDays(Defaults.EARN_IT_LOOKBACK_DAYS)), dayBoundary.endOf(today), EventType.BUDGET_BONUS)
        with(BudgetDays) {
            if (events.any { it.field("source") == source && it.field("ref") == ref }) return
            val earnedToday = events.filter { !it.startAt.isBefore(dayBoundary.startOf(today)) }.sumOf { it.field("minutes")?.toIntOrNull() ?: 0 }
            val granted = minOf(minutes, Defaults.EARN_IT_DAILY_CAP - earnedToday)
            if (granted <= 0) return
            eventLog.log(
                Event(
                    type = EventType.BUDGET_BONUS,
                    startAt = timeSource.now(),
                    data = buildJsonObject {
                        put("minutes", granted)
                        put("source", source)
                        put("ref", ref)
                    }.toString(),
                ),
            )
        }
    }

    private companion object {
        const val SOURCE_TASK = "task"
        const val SOURCE_FOCUS = "focus"
    }
}

