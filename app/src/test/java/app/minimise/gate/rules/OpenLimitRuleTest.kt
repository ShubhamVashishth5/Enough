package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.testing.app
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.usage.domain.TodayUsage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenLimitRuleTest {
    private val whatsapp = app("WhatsApp", AppKind.VAULT).copy(openLimit = 3)

    private fun ctx(opens: Int, appUsed: app.minimise.apps.domain.AppEntry = whatsapp) = GateContext(
        app = appUsed,
        today = TodayUsage(proceededOpensByPackage = mapOf(appUsed.packageName to opens)),
        now = Instant.EPOCH,
    )

    @Test
    fun `allows opens under the limit and blocks once it's reached`() {
        assertEquals(Decision.Allow, OpenLimitRule.evaluate(ctx(opens = 2)))
        assertEquals(
            Decision.Block(OpenLimitRule.REASON, listOf(UnlockMethodId.EMERGENCY_PASS)),
            OpenLimitRule.evaluate(ctx(opens = 3)),
        )
    }

    @Test
    fun `no limit, or a tool app, is never blocked`() {
        assertEquals(Decision.Allow, OpenLimitRule.evaluate(ctx(opens = 50, appUsed = whatsapp.copy(openLimit = null))))
        assertEquals(Decision.Allow, OpenLimitRule.evaluate(ctx(opens = 50, appUsed = whatsapp.copy(kind = AppKind.TOOL))))
    }

    @Test
    fun `other apps' opens don't count`() {
        val today = TodayUsage(proceededOpensByPackage = mapOf("pkg.instagram" to 10))
        assertEquals(Decision.Allow, OpenLimitRule.evaluate(GateContext(whatsapp, today, Instant.EPOCH)))
    }
}
