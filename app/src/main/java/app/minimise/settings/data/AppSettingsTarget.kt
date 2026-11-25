package app.minimise.settings.data

import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.core.time.WeeklyWindow
import app.minimise.keys.domain.KeyRepository
import app.minimise.keys.domain.KeyType
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.modes.domain.ManualPick
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.modes.domain.ModeTimeline
import app.minimise.modes.domain.ModeValues
import app.minimise.modes.domain.manualPickAt
import app.minimise.modes.domain.modeEditDirection
import app.minimise.settings.domain.RemoveMode
import app.minimise.settings.domain.SetBreatheWait
import app.minimise.settings.domain.breatheWaitChangeDirection
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
import kotlin.time.Duration.Companion.seconds

class AppSettingsTarget @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val scheduleRepository: ScheduleRepository,
    private val keyRepository: KeyRepository,
    private val modeRepository: ModeRepository,
    private val activeMode: ActiveModeProvider,
    private val dayBoundary: DayBoundary,
    private val timeSource: TimeSource,
) : SettingsTarget {
    override suspend fun direction(change: SettingChange): Direction? = when (change) {
        is SetAppKind -> kindChangeDirection(from = appRepository.kindOf(change.packageName), to = change.kind)
        is SetBudget -> budgetChangeDirection(
            fromMinutes = settingsStore.current().dailyBudget.inWholeMinutes.toInt(),
            toMinutes = change.minutes,
        )
        is SetOpenLimit -> openLimitChangeDirection(from = appRepository.openLimitOf(change.packageName), to = change.limit)
        is SetBreatheWait -> breatheWaitChangeDirection(
            from = appRepository.breatheWaitOf(change.packageName),
            to = change.seconds,
            usualSeconds = Defaults.WAIT_STEPS.first().inWholeSeconds.toInt(),
        )
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
        // Weighed over time, until both the old pick and the new one have handed back to the schedules.
        // The same values throughout (say, picking the mode whose schedule is on now) just records it.
        is SetManualMode -> {
            val now = timeSource.now()
            val to = change.modeId?.let { ManualPick(it, change.untilEpochMillis?.let(Instant::ofEpochMilli)) }
            switchDirection(now, from = settingsStore.current().manualPickAt(now), to = to)
        }
        is SaveMode -> if (change.id == NEW_MODE) {
            // A new mode does nothing until it's picked or its hours come, so only its schedule counts.
            val mode = change.newMode()
            modeEditDirection(activeMode.baseValues(), mode.copy(schedule = null), mode.values, mode.schedule) ?: Direction.TIGHTEN
        } else {
            modeRepository.get(change.id)?.let { old ->
                val edited = change.applyTo(old)
                // A new name alone changes no rule.
                modeEditDirection(activeMode.baseValues(), old, edited.values, edited.schedule)
                    ?: Direction.TIGHTEN.takeIf { edited.name != old.name }
            }
        }
        is RemoveMode -> modeRepository.get(change.id)?.takeIf { it.isCustom }?.let { mode ->
            // Its hours go, and if it's the hand-picked mode the pick ends now.
            val hours = modeEditDirection(activeMode.baseValues(), mode, mode.values, newSchedule = null)
            val now = timeSource.now()
            val pick = settingsStore.current().manualPickAt(now)
            val pickEnds = if (pick?.modeId == mode.id) switchDirection(now, from = pick, to = null) else null
            if (hours == Direction.LOOSEN || pickEnds == Direction.LOOSEN) Direction.LOOSEN else Direction.TIGHTEN
        }
    }

    override suspend fun apply(change: SettingChange) = when (change) {
        is SetAppKind -> appRepository.setKind(change.packageName, change.kind)
        is SetBudget -> settingsStore.update { it.copy(dailyBudget = change.minutes.minutes) }
        is SetOpenLimit -> appRepository.setOpenLimit(change.packageName, change.limit)
        is SetBreatheWait -> appRepository.setBreatheWait(change.packageName, change.seconds)
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
        is SetManualMode -> {
            // A switch that waited may name a mode removed since: then it's back to the schedules.
            val pick = change.modeId?.takeIf { modeRepository.get(it) != null }
            settingsStore.update {
                it.copy(manualModeId = pick, manualModeUntil = change.untilEpochMillis?.takeIf { pick != null }?.let(Instant::ofEpochMilli))
            }
        }
        is SaveMode -> if (change.id == NEW_MODE) {
            modeRepository.add(change.newMode())
            Unit
        } else {
            modeRepository.get(change.id)?.let { modeRepository.update(change.applyTo(it)) } ?: Unit
        }
        is RemoveMode -> {
            modeRepository.delete(change.id)
            settingsStore.update { if (it.manualModeId == change.id) it.copy(manualModeId = null, manualModeUntil = null) else it }
        }
    }

    private suspend fun switchDirection(now: Instant, from: ManualPick?, to: ManualPick?): Direction? =
        ModeTimeline.switchDirection(now, timeSource.zone(), modeRepository.all(), activeMode.baseValues(), from, to)

    private fun SaveMode.applyTo(mode: Mode) = mode.copy(
        // Only the user's own modes are renamed; the shipped ones keep their names.
        name = name?.trim()?.takeIf { it.isNotEmpty() && mode.isCustom } ?: mode.name,
        values = mode.values.copy(
            budget = budgetMinutes.minutes,
            waitSteps = waitStepsSeconds?.takeIf { it.isNotEmpty() }?.map { it.seconds } ?: mode.values.waitSteps,
            allowlist = allowlist?.toSet(),
        ),
        schedule = schedule(),
    )

    private fun SaveMode.newMode() = Mode(
        id = NEW_MODE,
        name = name?.trim().orEmpty(),
        values = ModeValues(
            budget = budgetMinutes.minutes,
            waitSteps = waitStepsSeconds?.takeIf { it.isNotEmpty() }?.map { it.seconds } ?: Defaults.WAIT_STEPS,
            allowlist = allowlist?.toSet(),
        ),
        schedule = schedule(),
    )

    private fun SaveMode.schedule() =
        if (scheduleDays == 0) null else WeeklyWindow.fromStored(scheduleDays, scheduleStartMinute, scheduleEndMinute)

    private fun SaveSchedule.window() = WeeklyWindow.fromStored(days, startMinute, endMinute)

    private companion object {
        /** [SaveMode] with this id adds a mode. */
        const val NEW_MODE = 0L
    }
}
