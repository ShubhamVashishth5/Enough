package app.minimise.testing

import app.minimise.apps.domain.AppKind
import app.minimise.settings.domain.Direction
import app.minimise.settings.domain.PendingChange
import app.minimise.settings.domain.PendingChangeStore
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.AddKey
import app.minimise.settings.domain.RemoveKey
import app.minimise.settings.domain.RemoveSchedule
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SaveSchedule
import app.minimise.settings.domain.SetManualMode
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SetFeedBlocked
import app.minimise.settings.domain.StartLock
import app.minimise.settings.domain.LockPeriod
import app.minimise.settings.domain.SetFirstHourRule
import app.minimise.settings.domain.SetOpenLimit
import app.minimise.settings.domain.SettingChange
import app.minimise.settings.domain.SettingsTarget
import app.minimise.settings.domain.budgetChangeDirection
import app.minimise.settings.domain.kindChangeDirection
import app.minimise.settings.domain.openLimitChangeDirection
import app.minimise.settings.domain.scheduleChangeDirection
import app.minimise.settings.domain.switchDirection
import app.minimise.core.time.WeeklyWindow
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsTarget : SettingsTarget {
    val kinds = mutableMapOf<String, AppKind>()
    var budgetMinutes = 45
    val openLimits = mutableMapOf<String, Int?>()
    val schedules = mutableMapOf<Long, SaveSchedule>()
    var firstHour = false
    val keys = mutableMapOf<Long, AddKey>()
    var manualModeId: Long? = null
    val blockedFeeds = mutableSetOf<String>()
    var lockedUntil: Instant? = null

    override suspend fun direction(change: SettingChange): Direction? = when (change) {
        is SetAppKind -> kindChangeDirection(kinds[change.packageName] ?: AppKind.TOOL, change.kind)
        is SetBudget -> budgetChangeDirection(budgetMinutes, change.minutes)
        is SetOpenLimit -> openLimitChangeDirection(openLimits[change.packageName], change.limit)
        is SaveSchedule -> schedules[change.id].let { old ->
            scheduleChangeDirection(old?.window(), old?.name, change.window(), change.name)
        }
        is RemoveSchedule -> if (change.id in schedules) Direction.LOOSEN else null
        is SetFirstHourRule -> switchDirection(firstHour, change.enabled)
        is AddKey -> if (keys.values.any { it.secretHash == change.secretHash }) null else Direction.TIGHTEN
        is RemoveKey -> if (change.id in keys) Direction.LOOSEN else null
        is SetManualMode -> if (change.modeId == manualModeId) null else Direction.TIGHTEN
        is SaveMode -> Direction.LOOSEN
        is SetFeedBlocked -> switchDirection(change.feedId in blockedFeeds, change.blocked)
        is StartLock -> if (change.untilEpochMillis > (lockedUntil?.toEpochMilli() ?: Long.MIN_VALUE)) Direction.TIGHTEN else null
    }

    override suspend fun apply(change: SettingChange) {
        when (change) {
            is SetAppKind -> kinds[change.packageName] = change.kind
            is SetBudget -> budgetMinutes = change.minutes
            is SetOpenLimit -> openLimits[change.packageName] = change.limit
            is SaveSchedule -> schedules[change.id] = change
            is RemoveSchedule -> schedules.remove(change.id)
            is SetFirstHourRule -> firstHour = change.enabled
            is AddKey -> keys[keys.size + 1L] = change
            is RemoveKey -> keys.remove(change.id)
            is SetManualMode -> manualModeId = change.modeId
            is SaveMode -> Unit
            is SetFeedBlocked -> if (change.blocked) blockedFeeds += change.feedId else blockedFeeds -= change.feedId
            is StartLock -> lockedUntil = Instant.ofEpochMilli(change.untilEpochMillis)
        }
    }

    private fun SaveSchedule.window() = WeeklyWindow.fromStored(days, startMinute, endMinute)
}

class FakePendingChangeStore : PendingChangeStore {
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

/** Reads the lock straight from a [FakeSettingsTarget], as the real one reads settings. */
class FakeLockPeriod(private val target: FakeSettingsTarget) : LockPeriod {
    override suspend fun lockedUntil(): Instant? = target.lockedUntil
}

/** No lock period, ever. */
object NoLockPeriod : LockPeriod {
    override suspend fun lockedUntil(): Instant? = null
}
