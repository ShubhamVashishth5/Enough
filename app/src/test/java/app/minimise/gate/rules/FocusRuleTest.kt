package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.core.time.plus
import app.minimise.focus.domain.FocusSession
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.testing.app
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.usage.domain.TodayUsage
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusRuleTest {
    private val start = Instant.parse("2026-11-24T10:00:00Z")
    private val maps = app("Maps")
    private val chrome = app("Chrome")
    private val instagram = app("Instagram", AppKind.VAULT)
    private val session = FocusSession(1, start, start + 25.minutes, endedAt = null, locked = false, allowlist = setOf(maps.packageName))

    private fun ctx(appUsed: app.minimise.apps.domain.AppEntry, at: Instant = start + 5.minutes) = GateContext(appUsed, TodayUsage(), at)

    @Test
    fun `during a session only the allowlist opens, tool apps included`() {
        assertEquals(Decision.Allow, FocusRule.evaluate(ctx(maps), session, emptySet()))
        assertEquals(Decision.Block(FocusRule.REASON, listOf(UnlockMethodId.WAIT)), FocusRule.evaluate(ctx(chrome), session, emptySet()))
        assertEquals(Decision.Block(FocusRule.REASON, listOf(UnlockMethodId.WAIT)), FocusRule.evaluate(ctx(instagram), session, emptySet()))
    }

    @Test
    fun `a locked session can only be passed with a key`() {
        val locked = session.copy(locked = true)
        assertEquals(
            Decision.Block(FocusRule.REASON, listOf(UnlockMethodId.NFC_KEY, UnlockMethodId.QR_KEY)),
            FocusRule.evaluate(ctx(chrome), locked, emptySet()),
        )
    }

    @Test
    fun `essential apps always open, and nothing is blocked once the session is over`() {
        assertEquals(Decision.Allow, FocusRule.evaluate(ctx(chrome), session, setOf(chrome.packageName)))
        assertEquals(Decision.Allow, FocusRule.evaluate(ctx(chrome, at = start + 25.minutes), session, emptySet()))
        assertEquals(Decision.Allow, FocusRule.evaluate(ctx(chrome), session.copy(endedAt = start + 1.minutes), emptySet()))
        assertEquals(Decision.Allow, FocusRule.evaluate(ctx(chrome), null, emptySet()))
    }
}
