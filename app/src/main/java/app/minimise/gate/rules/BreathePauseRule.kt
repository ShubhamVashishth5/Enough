package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.modes.domain.ActiveModeProvider
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Every Breathe app open pauses for the active mode's first (shortest) wait. Unlike the Vault pause it
 * doesn't grow through the day: Breathe is the light version, a moment to notice the reach.
 */
class BreathePauseRule @Inject constructor(
    private val activeMode: ActiveModeProvider,
) : Rule {
    override val id: String = "breathe_pause"

    override suspend fun evaluate(ctx: GateContext): Decision = evaluate(ctx, activeMode.values().waitSteps)

    companion object {
        fun evaluate(ctx: GateContext, waitSteps: List<Duration>): Decision =
            if (ctx.app.kind == AppKind.BREATHE) Decision.Pause(waitSteps.first(), breatheOnly = true) else Decision.Allow
    }
}
