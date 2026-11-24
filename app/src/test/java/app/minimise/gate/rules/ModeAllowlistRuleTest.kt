package app.minimise.gate.rules

import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.testing.app
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.usage.domain.TodayUsage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ModeAllowlistRuleTest {
    private val now = Instant.parse("2026-11-24T22:00:00Z")
    private val kindle = app("Kindle")
    private val phone = app("Phone")
    private val maps = app("Maps")
    private val instagram = app("Instagram", AppKind.VAULT)

    private fun ctx(appUsed: AppEntry) = GateContext(appUsed, TodayUsage(), now)

    private val blocked = Decision.Block(ModeAllowlistRule.REASON, listOf(UnlockMethodId.WAIT, UnlockMethodId.EMERGENCY_PASS))

    @Test
    fun `a mode without an allowlist allows everything`() {
        listOf(kindle, maps, instagram).forEach { appUsed ->
            assertEquals(Decision.Allow, ModeAllowlistRule.evaluate(ctx(appUsed), allowlist = null, essential = emptySet()))
        }
    }

    @Test
    fun `allowlisted and essential apps open`() {
        val allowlist = setOf(kindle.packageName)
        val essential = setOf(phone.packageName)
        assertEquals(Decision.Allow, ModeAllowlistRule.evaluate(ctx(kindle), allowlist, essential))
        assertEquals(Decision.Allow, ModeAllowlistRule.evaluate(ctx(phone), allowlist, essential))
    }

    @Test
    fun `any other app is blocked with a wait or an emergency pass`() {
        val allowlist = setOf(kindle.packageName)
        assertEquals(blocked, ModeAllowlistRule.evaluate(ctx(maps), allowlist, essential = emptySet()))
        assertEquals(blocked, ModeAllowlistRule.evaluate(ctx(instagram), allowlist, essential = emptySet()))
    }

    @Test
    fun `an empty allowlist blocks everything but essential apps`() {
        val essential = setOf(phone.packageName)
        assertEquals(blocked, ModeAllowlistRule.evaluate(ctx(kindle), allowlist = emptySet(), essential = essential))
        assertEquals(Decision.Allow, ModeAllowlistRule.evaluate(ctx(phone), allowlist = emptySet(), essential = essential))
    }
}
