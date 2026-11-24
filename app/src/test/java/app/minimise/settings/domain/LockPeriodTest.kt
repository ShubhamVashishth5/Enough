package app.minimise.settings.domain

import app.minimise.apps.domain.AppKind
import app.minimise.core.time.FakeTimeSource
import app.minimise.core.time.plus
import app.minimise.testing.FakeEventLog
import app.minimise.testing.FakeLockPeriod
import app.minimise.testing.FakePendingChangeStore
import app.minimise.testing.FakeSettingsTarget
import app.minimise.unlock.domain.UnlockMethodId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPeriodTest {
    private val clock = FakeTimeSource()
    private val target = FakeSettingsTarget()
    private val store = FakePendingChangeStore()
    private val events = FakeEventLog()
    private val guard = SettingsGuard(
        target,
        store,
        clock,
        object : LoosenDelay {
            override suspend fun current(): Duration = 24.hours
        },
        events,
        FakeLockPeriod(target),
    )

    private suspend fun lockFor(length: Duration) = guard.request(StartLock((clock.now + length).toEpochMilli()))

    @Test
    fun `starting a lock is tightening`() = runTest {
        assertEquals(GuardResult.AppliedNow, lockFor(7.days))
        assertEquals(clock.now + 7.days, guard.lockedUntil())
    }

    @Test
    fun `a lock can be lengthened but not shortened`() = runTest {
        lockFor(30.days)
        assertEquals(GuardResult.AppliedNow, lockFor(7.days))
        assertEquals(clock.now + 30.days, target.lockedUntil)
        lockFor(90.days)
        assertEquals(clock.now + 90.days, target.lockedUntil)
    }

    @Test
    fun `during a lock loosening is refused and nothing is scheduled`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        lockFor(7.days)
        val result = guard.request(SetAppKind("insta", AppKind.TOOL))
        assertEquals(GuardResult.Refused(GuardResult.LOCKED, clock.now + 7.days), result)
        assertTrue(store.all().isEmpty())
        assertEquals(AppKind.VAULT, target.kinds["insta"])
    }

    @Test
    fun `during a lock tightening still applies`() = runTest {
        lockFor(7.days)
        assertEquals(GuardResult.AppliedNow, guard.request(SetBudget(30)))
        assertEquals(30, target.budgetMinutes)
    }

    @Test
    fun `a refused loosen leaves an earlier pending one alone`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        guard.request(SetAppKind("insta", AppKind.TOOL))
        lockFor(7.days)
        guard.request(SetAppKind("insta", AppKind.HIDDEN))
        assertEquals(SetAppKind("insta", AppKind.TOOL), store.all().single().change)
    }

    @Test
    fun `a loosen that comes due during a lock waits for the lock to end`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        guard.request(SetAppKind("insta", AppKind.TOOL))
        lockFor(3.days)
        clock.advance(25.hours)
        guard.applyDue()
        assertEquals(AppKind.VAULT, target.kinds["insta"])
        assertEquals(1, store.all().size)
        clock.advance(3.days)
        guard.applyDue()
        assertEquals(AppKind.TOOL, target.kinds["insta"])
    }

    @Test
    fun `outside a lock a pending loosen can be applied now, and the method is logged`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        guard.request(SetAppKind("insta", AppKind.TOOL))
        val pending = store.all().single()
        assertEquals(GuardResult.AppliedNow, guard.applyNow(pending.id, UnlockMethodId.FRIEND_CODE))
        assertEquals(AppKind.TOOL, target.kinds["insta"])
        assertTrue(store.all().isEmpty())
        assertTrue(events.events.value.single().data!!.contains("FRIEND_CODE"))
    }

    @Test
    fun `during a lock nothing can be applied now`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        guard.request(SetAppKind("insta", AppKind.TOOL))
        lockFor(7.days)
        assertTrue(guard.applyNow(store.all().single().id, UnlockMethodId.TYPING) is GuardResult.Refused)
        assertEquals(AppKind.VAULT, target.kinds["insta"])
    }

    @Test
    fun `once the lock ends loosening works again`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        lockFor(7.days)
        clock.advance(7.days)
        assertTrue(guard.request(SetAppKind("insta", AppKind.TOOL)) is GuardResult.Scheduled)
    }
}
