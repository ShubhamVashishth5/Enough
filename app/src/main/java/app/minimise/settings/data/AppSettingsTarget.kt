package app.minimise.settings.data

import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.WeeklyWindow
import app.minimise.schedules.domain.Schedule
import app.minimise.schedules.domain.ScheduleRepository
import app.minimise.settings.domain.Direction
import app.minimise.settings.domain.RemoveSchedule
import app.minimise.settings.domain.SaveSchedule
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SetFirstHourRule
import app.minimise.settings.domain.SetOpenLimit
import app.minimise.settings.domain.SettingChange
import app.minimise.settings.domain.SettingsTarget
import app.minimise.settings.domain.budgetChangeDirection
import app.minimise.settings.domain.kindChangeDirection
import app.minimise.settings.domain.openLimitChangeDirection
import app.minimise.settings.domain.scheduleChangeDirection
import app.minimise.settings.domain.switchDirection
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class AppSettingsTarget @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val scheduleRepository: ScheduleRepository,
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
    }

    override suspend fun apply(change: SettingChange) = when (change) {
        is SetAppKind -> appRepository.setKind(change.packageName, change.kind)
        is SetBudget -> settingsStore.update { it.copy(dailyBudget = change.minutes.minutes) }
        is SetOpenLimit -> appRepository.setOpenLimit(change.packageName, change.limit)
        is SaveSchedule -> scheduleRepository.save(Schedule(change.id, change.name, change.window()))
        is RemoveSchedule -> scheduleRepository.delete(change.id)
        is SetFirstHourRule -> settingsStore.update { it.copy(firstHourRule = change.enabled) }
    }

    private fun SaveSchedule.window() = WeeklyWindow.fromStored(days, startMinute, endMinute)
}
