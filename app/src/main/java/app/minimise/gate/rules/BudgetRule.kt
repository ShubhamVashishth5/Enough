package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.usage.domain.BudgetDays
import app.minimise.usage.domain.DailyBudget
import javax.inject.Inject
import kotlin.time.Duration

/** Blocks every Vault app once today's shared budget (the active mode's, plus any earned minutes) is used up. */
class BudgetRule @Inject constructor(
    private val dailyBudget: DailyBudget,
) : Rule {
    override val id: String = "budget"

    override suspend fun evaluate(ctx: GateContext): Decision = evaluate(ctx, dailyBudget.allowance(ctx.today))

    companion object {
        /** Shown by BlockedActivity as "Daily budget used". */
        const val REASON = BudgetDays.BUDGET_REASON

        fun evaluate(ctx: GateContext, budget: Duration): Decision =
            if (ctx.app.kind == AppKind.VAULT && DailyBudget.remaining(budget, ctx.today) <= Duration.ZERO) {
                Decision.Block(reason = REASON, overrides = listOf(UnlockMethodId.EMERGENCY_PASS))
            } else {
                Decision.Allow
            }
    }
}
