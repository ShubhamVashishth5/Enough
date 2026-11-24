package app.minimise.gate.rules

import app.minimise.focus.domain.EssentialApps
import app.minimise.focus.domain.FocusSession
import app.minimise.focus.domain.FocusSessions
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.unlock.domain.UnlockMethodId
import javax.inject.Inject

/**
 * During a focus session, any app not on its allowlist is blocked, Tool apps included. An unlocked
 * session lets the user past after a wait; a locked one only with a physical key.
 */
class FocusRule @Inject constructor(
    private val focusSessions: FocusSessions,
    private val essentialApps: EssentialApps,
) : Rule {
    override val id: String = "focus"

    override suspend fun evaluate(ctx: GateContext): Decision =
        evaluate(ctx, focusSessions.current(), essentialApps.packages())

    companion object {
        const val REASON = "focus"

        fun evaluate(ctx: GateContext, session: FocusSession?, essential: Set<String>): Decision {
            if (session == null || !session.isActive(ctx.now)) return Decision.Allow
            val packageName = ctx.app.packageName
            if (packageName in session.allowlist || packageName in essential) return Decision.Allow
            val overrides = if (session.locked) listOf(UnlockMethodId.NFC_KEY, UnlockMethodId.QR_KEY) else listOf(UnlockMethodId.WAIT)
            return Decision.Block(REASON, overrides)
        }
    }
}
