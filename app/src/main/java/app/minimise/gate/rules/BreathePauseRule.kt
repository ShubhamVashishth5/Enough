package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.core.Defaults
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.modes.domain.ActiveModeProvider
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Every Breathe app open pauses: for the app's own wait if it has one, else the active mode's first
 * (shortest) wait. Unlike the Vault pause it doesn't grow through the day: Breathe is the light version,
 * a moment to notice the reach.
 */
class BreathePauseRule @Inject constructor(
    private val activeMode: ActiveModeProvider,
) : Rule {
    override val id: String = "breathe_pause"

    override suspend fun evaluate(ctx: GateContext): Decision = evaluate(ctx, activeMode.values().waitSteps)

    companion object {
        /**
         * A mode stricter than none ([baseSteps]) can lengthen an app's own wait, never shorten it: in
         * Sleep, a 5-second app still waits the mode's minute.
         */
        fun evaluate(ctx: GateContext, waitSteps: List<Duration>, baseSteps: List<Duration> = Defaults.WAIT_STEPS): Decision {
            if (ctx.app.kind != AppKind.BREATHE) return Decision.Allow
            val modeWait = waitSteps.first()
            val own = ctx.app.breatheWait ?: return Decision.Pause(modeWait, breatheOnly = true)
            val wait = if (modeWait > baseSteps.first()) maxOf(own, modeWait) else own
            return Decision.Pause(wait, breatheOnly = true)
        }
    }
}
