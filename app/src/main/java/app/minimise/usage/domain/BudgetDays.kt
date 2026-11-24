package app.minimise.usage.domain

import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Whether a day "went over budget": the budget ran out and the user still reached for a Vault app, or was cut off by it. */
object BudgetDays {
    /** Shown by BlockedActivity for BudgetRule. Kept here so usage code needn't know the gate. */
    const val BUDGET_REASON = "daily_budget"

    fun ranOut(events: List<Event>, start: Instant, end: Instant): Boolean =
        events.any { !it.startAt.isBefore(start) && it.startAt.isBefore(end) && (it.isSlip() || it.isBudgetTimeUp()) }

    /** Reaching for a Vault app after the budget was used up. */
    fun Event.isSlip(): Boolean = type == EventType.BLOCK_SHOWN && field("reason") == BUDGET_REASON

    fun Event.isBudgetTimeUp(): Boolean = type == EventType.TIME_UP && field("budgetUsed") == "true"

    fun Event.field(name: String): String? =
        data?.let { runCatching { Json.parseToJsonElement(it).jsonObject[name]?.jsonPrimitive?.content }.getOrNull() }
}
