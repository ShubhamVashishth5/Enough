package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.unlock.domain.UnlockMethodId
import javax.inject.Inject

/**
 * An optional daily cap on opens per app. Opens are proceeded pauses, so the cap applies to Vault apps.
 * TODO(question): Tool apps never pause, so they can't have a limit; put an app in the Vault to cap it.
 */
class OpenLimitRule @Inject constructor() : Rule {
    override val id: String = "open_limit"

    override suspend fun evaluate(ctx: GateContext): Decision = evaluate(ctx)

    companion object {
        /** Shown by the Blocked screen as "Open limit reached". */
        const val REASON = "open_limit"

        fun evaluate(ctx: GateContext): Decision {
            val limit = ctx.app.openLimit ?: return Decision.Allow
            if (ctx.app.kind != AppKind.VAULT) return Decision.Allow
            val opens = ctx.today.proceededOpensByPackage[ctx.app.packageName] ?: 0
            return if (opens >= limit) Decision.Block(REASON, listOf(UnlockMethodId.EMERGENCY_PASS)) else Decision.Allow
        }
    }
}
