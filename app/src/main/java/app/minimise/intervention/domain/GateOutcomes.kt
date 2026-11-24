package app.minimise.intervention.domain

import app.minimise.core.time.TimeSource
import app.minimise.core.time.plus
import app.minimise.gate.domain.Grant
import app.minimise.gate.domain.GrantStore
import app.minimise.swaps.domain.Swap
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Records what happened on an intervention screen and, when access is given, the grant. */
class GateOutcomes @Inject constructor(
    private val eventLog: EventLog,
    private val grantStore: GrantStore,
    private val timeSource: TimeSource,
) {
    suspend fun shown(packageName: String, wait: Duration) {
        eventLog.log(
            Event(
                type = EventType.GATE_SHOWN,
                startAt = timeSource.now(),
                packageName = packageName,
                data = buildJsonObject { put("waitSeconds", wait.inWholeSeconds) }.toString(),
            ),
        )
    }

    suspend fun proceeded(packageName: String, reason: PauseReason, timeBox: Duration) {
        val now = timeSource.now()
        grantStore.put(Grant(packageName = packageName, startAt = now, endAt = now + timeBox))
        eventLog.log(
            Event(
                type = EventType.GATE_PROCEEDED,
                startAt = now,
                packageName = packageName,
                data = buildJsonObject {
                    put("reason", reason.name)
                    put("timeBoxMinutes", timeBox.inWholeMinutes)
                }.toString(),
            ),
        )
    }

    /** The user took [swap] instead of opening [packageName]. Like a cancel, no grant is given. */
    suspend fun swapTaken(packageName: String, swap: Swap) {
        eventLog.log(
            Event(
                type = EventType.SWAP_TAKEN,
                startAt = timeSource.now(),
                packageName = packageName,
                data = buildJsonObject {
                    put("swapId", swap.id)
                    put("text", swap.text)
                }.toString(),
            ),
        )
    }

    suspend fun cancelled(packageName: String) {
        eventLog.log(Event(type = EventType.GATE_CANCELLED, startAt = timeSource.now(), packageName = packageName))
    }
}
