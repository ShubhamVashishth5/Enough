package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.core.Defaults
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import javax.inject.Inject
import kotlin.time.Duration

/** Every Vault open pauses first, and the pause grows with each open already gone through today. */
class VaultPauseRule @Inject constructor() : Rule {
    override val id: String = "vault_pause"

    override suspend fun evaluate(ctx: GateContext): Decision =
        if (ctx.app.kind == AppKind.VAULT) Decision.Pause(waitFor(ctx.today.proceededOpens)) else Decision.Allow

    companion object {
        /** The last step repeats once the list runs out. */
        fun waitFor(proceededOpens: Int, steps: List<Duration> = Defaults.WAIT_STEPS): Duration =
            steps[proceededOpens.coerceIn(0, steps.lastIndex)]
    }
}
