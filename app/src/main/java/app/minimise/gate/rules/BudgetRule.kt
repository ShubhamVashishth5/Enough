package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.usage.domain.DailyBudget
import javax.inject.Inject
import kotlin.time.Duration

/** Blocks every Vault app once today's shared budget is used up. */
class BudgetRule @Inject constructor(
    private val settingsStore: SettingsStore,
) : Rule {
    override val id: String = "budget"

    override suspend fun evaluate(ctx: GateContext): Decision = evaluate(ctx, settingsStore.current().dailyBudget)

    companion object {
        /** Shown by BlockedActivity as "Daily budget used". */
        const val REASON = "daily_budget"

        fun evaluate(ctx: GateContext, budget: Duration): Decision =
            if (ctx.app.kind == AppKind.VAULT && DailyBudget.remaining(budget, ctx.today) <= Duration.ZERO) {
                // TODO(question): Phase 2.2 adds EMERGENCY_PASS as an override.
                Decision.Block(reason = REASON, overrides = emptyList())
            } else {
                Decision.Allow
            }
    }
}
