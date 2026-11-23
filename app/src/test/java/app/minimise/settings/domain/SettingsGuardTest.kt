package app.minimise.settings.domain

import app.minimise.apps.domain.AppKind
import app.minimise.core.time.FakeTimeSource
import app.minimise.core.time.plus
import app.minimise.testing.FakeEventLog
import app.minimise.usage.domain.EventType
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsGuardTest {
    private val clock = FakeTimeSource()
    private val target = FakeSettingsTarget()
    private val store = FakePendingChangeStore()
    private val eventLog = FakeEventLog()
    private val delay = object : LoosenDelay {
        override suspend fun current(): Duration = 24.hours
    }
    private val guard = SettingsGuard(target, store, clock, delay, eventLog)

    @Test
    fun `moving a tool app into the vault applies at once`() = runTest {
        val result = guard.request(SetAppKind("insta", AppKind.VAULT))
        assertEquals(GuardResult.AppliedNow, result)
        assertEquals(AppKind.VAULT, target.kinds["insta"])
        assertTrue(store.all().isEmpty())
        assertEquals(EventType.SETTING_CHANGED, eventLog.events.value.single().type)
    }

    @Test
    fun `hiding a tool app is tightening`() = runTest {
        assertEquals(GuardResult.AppliedNow, guard.request(SetAppKind("game", AppKind.HIDDEN)))
        assertEquals(AppKind.HIDDEN, target.kinds["game"])
    }

    @Test
    fun `moving out of the vault waits 24 hours`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        val result = guard.request(SetAppKind("insta", AppKind.TOOL))
        assertEquals(GuardResult.Scheduled(clock.now + 24.hours), result)
        assertEquals(AppKind.VAULT, target.kinds["insta"])
        assertEquals(1, store.all().size)
        assertTrue(eventLog.events.value.isEmpty())
    }

    @Test
    fun `vault to hidden is loosening, hidden to vault is tightening`() = runTest {
        target.kinds["a"] = AppKind.VAULT
        target.kinds["b"] = AppKind.HIDDEN
        assertTrue(guard.request(SetAppKind("a", AppKind.HIDDEN)) is GuardResult.Scheduled)
        assertEquals(GuardResult.AppliedNow, guard.request(SetAppKind("b", AppKind.VAULT)))
    }

    @Test
    fun `a pending loosen applies once its time has come, not before`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        guard.request(SetAppKind("insta", AppKind.TOOL))

        clock.advance(24.hours - 1.minutes)
        guard.applyDue()
        assertEquals(AppKind.VAULT, target.kinds["insta"])

        clock.advance(1.minutes)
        guard.applyDue()
        assertEquals(AppKind.TOOL, target.kinds["insta"])
        assertTrue(store.all().isEmpty())
        assertEquals(1, eventLog.events.value.count { it.type == EventType.SETTING_CHANGED })
    }

    @Test
    fun `a pending loosen can be cancelled`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        guard.request(SetAppKind("insta", AppKind.TOOL))
        guard.cancel(store.all().single().id)

        clock.advance(48.hours)
        guard.applyDue()
        assertEquals(AppKind.VAULT, target.kinds["insta"])
    }

    @Test
    fun `choosing the current value again cancels the pending change`() = runTest {
        target.kinds["insta"] = AppKind.VAULT
        guard.request(SetAppKind("insta", AppKind.TOOL))
        assertEquals(GuardResult.AppliedNow, guard.request(SetAppKind("insta", AppKind.VAULT)))
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun `asking again for a loosen restarts its wait`() = runTest {
        target.budgetMinutes = 45
        guard.request(SetBudget(60))
        clock.advance(23.hours)
        val again = guard.request(SetBudget(60))
        assertEquals(GuardResult.Scheduled(clock.now + 24.hours), again)
        assertEquals(1, store.all().size)
    }

    @Test
    fun `a lower budget applies at once and replaces a pending raise`() = runTest {
        target.budgetMinutes = 45
        assertTrue(guard.request(SetBudget(60)) is GuardResult.Scheduled)
        assertEquals(GuardResult.AppliedNow, guard.request(SetBudget(30)))
        assertEquals(30, target.budgetMinutes)
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun `a higher budget waits`() = runTest {
        target.budgetMinutes = 45
        assertTrue(guard.request(SetBudget(50)) is GuardResult.Scheduled)
        assertEquals(45, target.budgetMinutes)
    }

    @Test
    fun `pending changes for different settings are independent`() = runTest {
        target.kinds["a"] = AppKind.VAULT
        target.kinds["b"] = AppKind.VAULT
        guard.request(SetAppKind("a", AppKind.TOOL))
        guard.request(SetAppKind("b", AppKind.TOOL))
        assertEquals(2, store.all().size)
    }

    @Test
    fun `direction helpers`() {
        assertEquals(Direction.TIGHTEN, kindChangeDirection(AppKind.TOOL, AppKind.VAULT))
        assertEquals(Direction.LOOSEN, kindChangeDirection(AppKind.VAULT, AppKind.TOOL))
        assertEquals(Direction.LOOSEN, kindChangeDirection(AppKind.HIDDEN, AppKind.TOOL))
        assertEquals(null, kindChangeDirection(AppKind.TOOL, AppKind.TOOL))
        assertEquals(Direction.TIGHTEN, budgetChangeDirection(45, 30))
        assertEquals(Direction.LOOSEN, budgetChangeDirection(45, 46))
        assertEquals(null, budgetChangeDirection(45, 45))
    }
}

private class FakeSettingsTarget : SettingsTarget {
    val kinds = mutableMapOf<String, AppKind>()
    var budgetMinutes = 45

    override suspend fun direction(change: SettingChange): Direction? = when (change) {
        is SetAppKind -> kindChangeDirection(kinds[change.packageName] ?: AppKind.TOOL, change.kind)
        is SetBudget -> budgetChangeDirection(budgetMinutes, change.minutes)
    }

    override suspend fun apply(change: SettingChange) {
        when (change) {
            is SetAppKind -> kinds[change.packageName] = change.kind
            is SetBudget -> budgetMinutes = change.minutes
        }
    }
}

private class FakePendingChangeStore : PendingChangeStore {
    private val rows = MutableStateFlow<List<PendingChange>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<PendingChange>> = rows

    override suspend fun all(): List<PendingChange> = rows.value.sortedBy { it.applyAt }

    override suspend fun add(change: SettingChange, requestedAt: Instant, applyAt: Instant): Long {
        val id = nextId++
        rows.value = rows.value + PendingChange(id, change, requestedAt, applyAt)
        return id
    }

    override suspend fun remove(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun removeKey(key: String) {
        rows.value = rows.value.filterNot { it.change.key == key }
    }
}
