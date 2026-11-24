package app.minimise.gate.rules

import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.core.Defaults
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.GateEngine
import app.minimise.testing.app
import app.minimise.usage.domain.TodayUsage
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class BreathePauseRuleTest {
    private val whatsapp = app("WhatsApp", AppKind.BREATHE)
    private val instagram = app("Instagram", AppKind.VAULT)
    private val maps = app("Maps")
    private val steps = listOf(10.seconds, 20.seconds, 40.seconds)

    private fun ctx(app: AppEntry, today: TodayUsage = TodayUsage()) = GateContext(app = app, today = today, now = Instant.EPOCH)

    @Test
    fun `a breathe app pauses for the first step, breathe-only`() {
        assertEquals(Decision.Pause(10.seconds, breatheOnly = true), BreathePauseRule.evaluate(ctx(whatsapp), steps))
    }

    @Test
    fun `the breathe pause doesn't grow with the day's vault opens`() {
        val busyDay = TodayUsage(proceededOpens = 7, vaultTime = 40.minutes)
        assertEquals(Decision.Pause(10.seconds, breatheOnly = true), BreathePauseRule.evaluate(ctx(whatsapp, busyDay), steps))
    }

    @Test
    fun `tool and vault apps are left to the other rules`() {
        assertEquals(Decision.Allow, BreathePauseRule.evaluate(ctx(maps), steps))
        assertEquals(Decision.Allow, BreathePauseRule.evaluate(ctx(instagram), steps))
    }

    @Test
    fun `a used-up budget, open limit and first hour don't touch a breathe app`() {
        val overBudget = TodayUsage(vaultTime = Defaults.DAILY_BUDGET + 1.minutes, proceededOpensByPackage = mapOf(whatsapp.packageName to 9))
        val limited = whatsapp.copy(openLimit = 1)
        val decisions = listOf(
            BreathePauseRule.evaluate(ctx(limited, overBudget), steps),
            BudgetRule.evaluate(ctx(limited, overBudget), Defaults.DAILY_BUDGET),
            OpenLimitRule.evaluate(ctx(limited, overBudget)),
            FirstHourRule.evaluate(GateContext(limited, overBudget.copy(firstUnlockAt = Instant.EPOCH), Instant.EPOCH)),
        )
        assertEquals(Decision.Pause(10.seconds, breatheOnly = true), GateEngine.strictest(decisions))
    }
}
