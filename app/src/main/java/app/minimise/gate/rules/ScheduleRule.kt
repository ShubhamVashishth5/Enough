package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.core.time.TimeSource
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.schedules.domain.Schedule
import app.minimise.schedules.domain.ScheduleRepository
import app.minimise.unlock.domain.UnlockMethodId
import java.time.LocalDateTime
import javax.inject.Inject

/** Blocks every Vault app inside any schedule window (bedtime, focus hours). */
class ScheduleRule @Inject constructor(
    private val schedules: ScheduleRepository,
    private val timeSource: TimeSource,
) : Rule {
    override val id: String = "schedule"

    override suspend fun evaluate(ctx: GateContext): Decision =
        evaluate(ctx, schedules.all(), ctx.now.atZone(timeSource.zone()).toLocalDateTime())

    companion object {
        const val REASON = "schedule"
        val OVERRIDES = listOf(UnlockMethodId.NFC_KEY, UnlockMethodId.QR_KEY, UnlockMethodId.EMERGENCY_PASS)

        fun evaluate(ctx: GateContext, schedules: List<Schedule>, localNow: LocalDateTime): Decision =
            if (ctx.app.kind == AppKind.VAULT && schedules.any { it.window.isActive(localNow) }) {
                Decision.Block(REASON, OVERRIDES)
            } else {
                Decision.Allow
            }
    }
}
