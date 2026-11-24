package app.minimise.unlock.domain

import app.minimise.core.Defaults
import app.minimise.core.time.DayBoundary
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A fixed ration of passes per usage week that skip a block. Each use is an OVERRIDE_USED event. */
class EmergencyPassMethod @Inject constructor(
    private val eventLog: EventLog,
    private val dayBoundary: DayBoundary,
) : UnlockMethod {
    override val id: UnlockMethodId = UnlockMethodId.EMERGENCY_PASS

    override suspend fun isAvailable(): Boolean = (usesLeft() ?: 0) > 0

    override suspend fun usesLeft(): Int {
        val weekStart = dayBoundary.weekStartOf(dayBoundary.today())
        val events = eventLog.between(dayBoundary.startOf(weekStart), dayBoundary.startOf(weekStart.plusDays(7)), EventType.OVERRIDE_USED)
        return remaining(events)
    }

    companion object {
        fun remaining(overrideEvents: List<Event>, perWeek: Int = Defaults.EMERGENCY_PASSES_PER_WEEK): Int =
            (perWeek - overrideEvents.count { methodOf(it) == UnlockMethodId.EMERGENCY_PASS.name }).coerceAtLeast(0)

        private fun methodOf(event: Event): String? =
            event.data?.let { runCatching { Json.parseToJsonElement(it).jsonObject["method"]?.jsonPrimitive?.content }.getOrNull() }
    }
}
