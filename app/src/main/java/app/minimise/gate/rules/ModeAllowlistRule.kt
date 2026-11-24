package app.minimise.gate.rules

import app.minimise.focus.domain.EssentialApps
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.unlock.domain.UnlockMethodId
import javax.inject.Inject

/** A mode with an allowlist (say Sleep: calls, alarm, an e-book app) blocks every other app while it's on. */
class ModeAllowlistRule @Inject constructor(
    private val activeMode: ActiveModeProvider,
    private val essentialApps: EssentialApps,
) : Rule {
    override val id: String = "mode_allowlist"

    override suspend fun evaluate(ctx: GateContext): Decision =
        evaluate(ctx, activeMode.values().allowlist, essentialApps.packages())

    companion object {
        const val REASON = "mode_allowlist"

        fun evaluate(ctx: GateContext, allowlist: Set<String>?, essential: Set<String>): Decision {
            if (allowlist == null) return Decision.Allow
            val packageName = ctx.app.packageName
            if (packageName in allowlist || packageName in essential) return Decision.Allow
            return Decision.Block(REASON, listOf(UnlockMethodId.WAIT, UnlockMethodId.EMERGENCY_PASS))
        }
    }
}
