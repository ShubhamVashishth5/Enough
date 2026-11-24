package app.minimise.gate.domain

import app.minimise.apps.domain.AppKind
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.FakeTimeSource
import app.minimise.core.time.plus
import app.minimise.gate.rules.VaultPauseRule
import app.minimise.settings.domain.LoosenDelay
import app.minimise.settings.domain.SettingsGuard
import app.minimise.testing.FakeActiveMode
import app.minimise.testing.FakeAppRepository
import app.minimise.testing.FakeEventLog
import app.minimise.testing.FakeGrantStore
import app.minimise.testing.FakePendingChangeStore
import app.minimise.testing.NoLockPeriod
import app.minimise.testing.FakeSettingsTarget
import app.minimise.testing.app
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventType
import app.minimise.usage.domain.TodayUsageProvider
import app.minimise.usage.domain.UsageTracker
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GateEngineTest {
    private val clock = FakeTimeSource()
    private val eventLog = FakeEventLog()
    private val instagram = app("Instagram", AppKind.VAULT)
    private val maps = app("Maps")
    private val apps = FakeAppRepository(listOf(instagram, maps))
    private val grants = FakeGrantStore()
    private val guard = SettingsGuard(
        FakeSettingsTarget(),
        FakePendingChangeStore(),
        clock,
        object : LoosenDelay {
            override suspend fun current(): Duration = 24.hours
        },
        eventLog,
        NoLockPeriod,
    )
    private val todayUsage = TodayUsageProvider(eventLog, DayBoundary(clock), clock, apps, UsageTracker(eventLog, clock))

    private fun engine(rules: Set<Rule> = setOf(VaultPauseRule(FakeActiveMode()))) = GateEngine(rules, grants, todayUsage, clock, guard)

    private suspend fun proceededOnce() {
        eventLog.log(Event(type = EventType.GATE_PROCEEDED, startAt = clock.now, packageName = instagram.packageName))
    }

    @Test
    fun `tool apps are allowed`() = runTest {
        assertEquals(Decision.Allow, engine().check(maps))
    }

    @Test
    fun `the first vault open of the day pauses for the first step`() = runTest {
        assertEquals(Decision.Pause(10.seconds), engine().check(instagram))
    }

    @Test
    fun `the wait grows with each open already gone through today`() = runTest {
        proceededOnce()
        assertEquals(Decision.Pause(20.seconds), engine().check(instagram))
        proceededOnce()
        proceededOnce()
        assertEquals(Decision.Pause(60.seconds), engine().check(instagram))
    }

    @Test
    fun `opens from yesterday don't count`() = runTest {
        repeat(3) { proceededOnce() } // 2026-01-15 12:00 UTC
        clock.now = java.time.Instant.parse("2026-01-16T05:00:00Z") // the next usage day (reset 04:00, zone UTC)
        assertEquals(Decision.Pause(10.seconds), engine().check(instagram))
    }

    @Test
    fun `an active grant lets the app through, an expired one doesn't`() = runTest {
        grants.put(Grant(instagram.packageName, startAt = clock.now, endAt = clock.now + 5.minutes))
        assertEquals(Decision.Allow, engine().check(instagram))
        clock.advance(5.minutes)
        assertEquals(Decision.Pause(10.seconds), engine().check(instagram))
    }

    @Test
    fun `strictest result wins`() {
        val block = Decision.Block("budget", listOf(UnlockMethodId.EMERGENCY_PASS))
        assertEquals(Decision.Allow, GateEngine.strictest(emptyList()))
        assertEquals(Decision.Pause(40.seconds), GateEngine.strictest(listOf(Decision.Allow, Decision.Pause(10.seconds), Decision.Pause(40.seconds))))
        assertEquals(block, GateEngine.strictest(listOf(Decision.Pause(40.seconds), block, Decision.Allow)))
        // At the same length, the full Vault pause wins over a breathe-only one.
        assertEquals(
            Decision.Pause(10.seconds),
            GateEngine.strictest(listOf(Decision.Pause(10.seconds, breatheOnly = true), Decision.Pause(10.seconds))),
        )
    }

    @Test
    fun `several blocks keep only the overrides every block allows`() {
        val schedule = Decision.Block("schedule", listOf(UnlockMethodId.NFC_KEY, UnlockMethodId.EMERGENCY_PASS))
        val budget = Decision.Block("budget", listOf(UnlockMethodId.EMERGENCY_PASS))
        assertEquals(
            Decision.Block("schedule", listOf(UnlockMethodId.EMERGENCY_PASS)),
            GateEngine.strictest(listOf(schedule, budget)),
        )
    }
}
