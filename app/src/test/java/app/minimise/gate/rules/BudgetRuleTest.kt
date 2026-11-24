package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.GateEngine
import app.minimise.testing.app
import app.minimise.usage.domain.DailyBudget
import app.minimise.usage.domain.TodayUsage
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetRuleTest {
    private val instagram = app("Instagram", AppKind.VAULT)
    private val maps = app("Maps")
    private val budget = 5.minutes

    private fun ctx(appUsed: app.minimise.apps.domain.AppEntry, vaultTime: kotlin.time.Duration) =
        GateContext(app = appUsed, today = TodayUsage(vaultTime = vaultTime), now = Instant.EPOCH)

    @Test
    fun `under budget allows`() {
        assertEquals(Decision.Allow, BudgetRule.evaluate(ctx(instagram, 4.minutes + 59.seconds), budget))
    }

    @Test
    fun `at or over budget blocks every vault app`() {
        assertTrue(BudgetRule.evaluate(ctx(instagram, 5.minutes), budget) is Decision.Block)
        assertTrue(BudgetRule.evaluate(ctx(app("YouTube", AppKind.VAULT), 6.minutes), budget) is Decision.Block)
    }

    @Test
    fun `tool apps never count against the budget`() {
        assertEquals(Decision.Allow, BudgetRule.evaluate(ctx(maps, 60.minutes), budget))
    }

    @Test
    fun `a used-up budget beats the pause`() {
        val decisions = listOf(Decision.Pause(10.seconds), BudgetRule.evaluate(ctx(instagram, 5.minutes), budget))
        assertEquals(Decision.Block(BudgetRule.REASON, emptyList()), GateEngine.strictest(decisions))
    }

    @Test
    fun `remaining budget never goes negative`() {
        assertEquals(2.minutes, DailyBudget.remaining(budget, TodayUsage(vaultTime = 3.minutes)))
        assertEquals(kotlin.time.Duration.ZERO, DailyBudget.remaining(budget, TodayUsage(vaultTime = 9.minutes)))
    }
}
