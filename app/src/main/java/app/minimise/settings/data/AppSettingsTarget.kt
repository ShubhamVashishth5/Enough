package app.minimise.settings.data

import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.WeeklyWindow
import app.minimise.keys.domain.KeyRepository
import app.minimise.keys.domain.KeyType
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.modes.domain.ModeRepository
import app.minimise.modes.domain.compare
import app.minimise.modes.domain.modeEditDirection
import app.minimise.modes.domain.toDirection
import app.minimise.schedules.domain.Schedule
import app.minimise.schedules.domain.ScheduleRepository
import app.minimise.settings.domain.AddKey
import app.minimise.settings.domain.Direction
import app.minimise.settings.domain.RemoveKey
import app.minimise.settings.domain.RemoveSchedule
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SaveSchedule
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SetFeedBlocked
import app.minimise.settings.domain.StartLock
import app.minimise.settings.domain.SetTaper
import app.minimise.settings.domain.SetEarnIt
import app.minimise.settings.domain.SetCalendarFocus
import app.minimise.core.time.DayBoundary
import app.minimise.settings.domain.SetFirstHourRule
import app.minimise.settings.domain.SetManualMode
import app.minimise.settings.domain.SetOpenLimit
import app.minimise.settings.domain.SettingChange
import app.minimise.settings.domain.SettingsTarget
import app.minimise.settings.domain.budgetChangeDirection
import app.minimise.settings.domain.kindChangeDirection
import app.minimise.settings.domain.openLimitChangeDirection
import app.minimise.settings.domain.scheduleChangeDirection
import app.minimise.settings.domain.switchDirection
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class AppSettingsTarget @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val scheduleRepository: ScheduleRepository,
    private val keyRepository: KeyRepository,
    private val modeRepository: ModeRepository,
    private val activeMode: ActiveModeProvider,
    private val dayBoundary: DayBoundary,
) : SettingsTarget {
    override suspend fun direction(change: SettingChange): Direction? = when (change) {
        is SetAppKind -> kindChangeDirection(from = appRepository.kindOf(change.packageName), to = change.kind)
        is SetBudget -> budgetChangeDirection(
            fromMinutes = settingsStore.current().dailyBudget.inWholeMinutes.toInt(),
            toMinutes = change.minutes,
        )
        is SetOpenLimit -> openLimitChangeDirection(from = appRepository.openLimitOf(change.packageName), to = change.limit)
        is SaveSchedule -> {
            val old = scheduleRepository.get(change.id)
            scheduleChangeDirection(old?.window, old?.name, change.window(), change.name)
        }
        is RemoveSchedule -> if (scheduleRepository.get(change.id) != null) Direction.LOOSEN else null
        is SetFirstHourRule -> switchDirection(from = settingsStore.current().firstHourRule, to = change.enabled)
        is SetFeedBlocked -> switchDirection(from = change.feedId in settingsStore.current().blockedFeeds, to = change.blocked)
        // A floor behaves like a limit: adding or lowering one tightens, raising or removing one loosens.
        is SetTaper -> openLimitChangeDirection(from = settingsStore.current().taperFloorMinutes, to = change.floorMinutes)
        is SetEarnIt -> switchDirection(from = !settingsStore.current().earnIt, to = !change.enabled)
        is SetCalendarFocus -> switchDirection(from = settingsStore.current().calendarFocus, to = change.enabled)
        // Only ever lengthens a lock; anything else is no change at all.
        is StartLock -> {
            val current = settingsStore.current().lockedUntil?.toEpochMilli() ?: Long.MIN_VALUE
            if (change.untilEpochMillis > current) Direction.TIGHTEN else null
        }
        // As the plan has it: a key is what ends locked sessions, so adding one is tightening.
        is AddKey -> if (keyRepository.all().any { it.secretHash == change.secretHash }) null else Direction.TIGHTEN
        is RemoveKey -> if (keyRepository.get(change.id) != null) Direction.LOOSEN else null
        is SetManualMode -> when {
            change.modeId == settingsStore.current().manualModeId -> null
            // Same values either way (say, picking the mode whose schedule is on now): just record the choice.
            else -> compare(activeMode.values(), activeMode.valuesWith(change.modeId)).toDirection() ?: Direction.TIGHTEN
        }
        is SaveMode -> modeRepository.get(change.id)?.let { old ->
            val edited = change.applyTo(old)
            modeEditDirection(activeMode.baseValues(), old, edited.values, edited.schedule)
        }
    }

    override suspend fun apply(change: SettingChange) = when (change) {
        is SetAppKind -> appRepository.setKind(change.packageName, change.kind)
        is SetBudget -> settingsStore.update { it.copy(dailyBudget = change.minutes.minutes) }
        is SetOpenLimit -> appRepository.setOpenLimit(change.packageName, change.limit)
        is SaveSchedule -> scheduleRepository.save(Schedule(change.id, change.name, change.window()))
        is RemoveSchedule -> scheduleRepository.delete(change.id)
        is SetFirstHourRule -> settingsStore.update { it.copy(firstHourRule = change.enabled) }
        is StartLock -> settingsStore.update { it.copy(lockedUntil = Instant.ofEpochMilli(change.untilEpochMillis)) }
        is SetTaper -> {
            // Switching taper on starts counting from this week, so the first cut is next week.
            val week = dayBoundary.weekStartOf(dayBoundary.today())
            settingsStore.update {
                it.copy(taperFloorMinutes = change.floorMinutes, taperWeek = if (it.taperFloorMinutes == null) week else it.taperWeek)
            }
        }
        is SetEarnIt -> settingsStore.update { it.copy(earnIt = change.enabled) }
        is SetCalendarFocus -> settingsStore.update { it.copy(calendarFocus = change.enabled) }
        is SetFeedBlocked -> settingsStore.update {
            it.copy(blockedFeeds = if (change.blocked) it.blockedFeeds + change.feedId else it.blockedFeeds - change.feedId)
        }
        is AddKey -> keyRepository.add(KeyType.valueOf(change.type), change.label, change.secretHash)
        is RemoveKey -> keyRepository.delete(change.id)
        is SetManualMode -> settingsStore.update { it.copy(manualModeId = change.modeId) }
        is SaveMode -> modeRepository.get(change.id)?.let { modeRepository.update(change.applyTo(it)) } ?: Unit
    }

    private fun SaveMode.applyTo(mode: app.minimise.modes.domain.Mode) = mode.copy(
        values = mode.values.copy(budget = budgetMinutes.minutes, allowlist = allowlist?.toSet()),
        schedule = if (scheduleDays == 0) null else WeeklyWindow.fromStored(scheduleDays, scheduleStartMinute, scheduleEndMinute),
    )

    private fun SaveSchedule.window() = WeeklyWindow.fromStored(days, startMinute, endMinute)
}
