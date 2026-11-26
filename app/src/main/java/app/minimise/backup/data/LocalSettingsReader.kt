package app.minimise.backup.data

import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.backup.domain.Backup
import app.minimise.backup.domain.LocalSettings
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.time.minuteOfDay
import app.minimise.keys.domain.KeyRepository
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.modes.domain.manualPickAt
import app.minimise.schedules.domain.Schedule
import app.minimise.schedules.domain.ScheduleRepository
import app.minimise.swaps.domain.SwapRepository
import app.minimise.unlock.domain.FriendCodeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Reads every setting on this phone into the backup's shape. Lists are sorted, so two reads compare equal. */
class LocalSettingsReader @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val modeRepository: ModeRepository,
    private val scheduleRepository: ScheduleRepository,
    private val keyRepository: KeyRepository,
    private val friendCodes: FriendCodeRepository,
    private val swapRepository: SwapRepository,
    private val timeSource: TimeSource,
) {
    suspend fun read(): LocalSettings {
        val settings = settingsStore.current()
        val kinds = appRepository.kinds.first()
        val limits = appRepository.openLimits()
        val breatheWaits = appRepository.breatheWaits()
        val modes = modeRepository.all()
        val schedules = scheduleRepository.all()
        val keys = keyRepository.all()
        val pick = settings.manualPickAt(timeSource.now())
        val backup = Backup(
            exportedAtMillis = timeSource.now().toEpochMilli(),
            // A Tool app without a limit or a pause is how every app starts, so only real choices go in.
            apps = (kinds.keys + limits.keys + breatheWaits.keys).sorted()
                .map { Backup.App(it, kinds[it] ?: AppKind.TOOL, limits[it], breatheWaits[it]) }
                .filter { it.kind != AppKind.TOOL || it.openLimit != null || it.breatheWaitSeconds != null },
            budgetMinutes = settings.dailyBudget.inWholeMinutes.toInt(),
            taperFloorMinutes = settings.taperFloorMinutes,
            firstHourRule = settings.firstHourRule,
            earnIt = settings.earnIt,
            calendarFocus = settings.calendarFocus,
            lockedUntilMillis = settings.lockedUntil?.toEpochMilli(),
            blockedFeeds = settings.blockedFeeds.sorted(),
            modes = modes.map { it.toBackup() },
            manualMode = pick?.let { p -> modes.firstOrNull { it.id == p.modeId }?.backupKey() },
            manualModeUntilMillis = pick?.until?.toEpochMilli()?.takeIf { modes.any { it.id == pick.modeId } },
            schedules = schedules.map { it.toBackup() },
            keys = keys.map { Backup.Key(it.type.name, it.label, it.secretHash) },
            // TODO(question): friend codes are 8 characters, so their unsalted hashes in a file the user
            // holds could be brute-forced offline. Keep them in the backup, or make new codes after a move?
            friendCodeHashes = friendCodes.unusedHashes().sorted(),
            swaps = swapRepository.all().map { Backup.Swap(it.text, it.packageName) },
            digestEnabled = settings.digestEnabled,
            digestExempt = settings.digestExempt.sorted(),
            grayscaleMode = settings.grayscaleMode.name,
            focusAllowlist = settings.focusAllowlist?.sorted(),
            home = settings.home.let {
                Backup.Home(
                    lockButton = it.lockButton,
                    swipeDownShade = it.swipeDownShade,
                    hideStatusBar = it.hideStatusBar,
                    battery = it.battery,
                    nextAlarm = it.nextAlarm,
                    music = it.music,
                    weather = it.weather,
                    airQuality = it.airQuality,
                    temperatureUnit = it.temperatureUnit.name,
                )
            },
            shortcuts = settings.shortcuts.map { Backup.Shortcut(it.icon.name, it.packageName) },
            appearance = settings.appearance.let {
                Backup.Appearance(
                    font = it.font.name,
                    textSize = it.textSize.name,
                    clockSize = it.clockSize.name,
                    appNameSize = it.appNameSize.name,
                    iconSize = it.iconSize.name,
                )
            },
        )
        return LocalSettings(
            settings = backup,
            modeIds = modes.mapNotNull { mode -> mode.presetKey?.let { it to mode.id } }.toMap(),
            customModeIds = modes.filter { it.isCustom }.associate { it.name to it.id },
            scheduleIds = schedules.map { it.id to it.toBackup() },
            keyIds = keys.associate { it.secretHash to it.id },
        )
    }

    private fun Mode.backupKey(): String = presetKey ?: (Backup.CUSTOM_PREFIX + name)

    private fun Mode.toBackup(): Backup.Mode {
        val window: WeeklyWindow? = schedule
        return Backup.Mode(
            preset = presetKey ?: Backup.CUSTOM,
            name = name.takeIf { isCustom },
            budgetMinutes = values.budget.inWholeMinutes.toInt(),
            waitStepsSeconds = values.waitSteps.map { it.inWholeSeconds },
            allowlist = values.allowlist?.sorted(),
            scheduleDays = window?.daysMask() ?: 0,
            scheduleStartMinute = window?.start?.minuteOfDay ?: 0,
            scheduleEndMinute = window?.end?.minuteOfDay ?: 0,
        )
    }

    private fun Schedule.toBackup() =
        Backup.Schedule(name = name, days = window.daysMask(), startMinute = window.start.minuteOfDay, endMinute = window.end.minuteOfDay)
}
