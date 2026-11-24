package app.minimise.backup.domain

import app.minimise.apps.domain.AppKind
import app.minimise.settings.domain.AddKey
import app.minimise.settings.domain.RemoveKey
import app.minimise.settings.domain.RemoveSchedule
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SaveSchedule
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SetCalendarFocus
import app.minimise.settings.domain.SetEarnIt
import app.minimise.settings.domain.SetFeedBlocked
import app.minimise.settings.domain.SetFirstHourRule
import app.minimise.settings.domain.SetManualMode
import app.minimise.settings.domain.SetOpenLimit
import app.minimise.settings.domain.SetTaper
import app.minimise.settings.domain.SettingChange
import app.minimise.settings.domain.StartLock

/**
 * The settings on this phone now, in the same shape as a backup, plus the local ids that changes have
 * to name: modes by preset key, schedules by name, keys by secret hash.
 */
data class LocalSettings(
    val settings: Backup,
    val modeIds: Map<String, Long>,
    val scheduleIds: List<Pair<Long, Backup.Schedule>>,
    val keyIds: Map<String, Long>,
)

/**
 * Turns a backup into the [SettingChange]s that would make this phone match it. They all go through
 * SettingsGuard, so tighter ones apply at once, looser ones wait and nothing loosens during a lock.
 */
object BackupDiff {
    /**
     * In the order to request them. A lock comes last, so the changes before it are weighed (and
     * scheduled) first rather than refused. Apps the backup doesn't mention are left as they are.
     */
    fun changes(incoming: Backup, local: LocalSettings, nowMillis: Long): List<SettingChange> = buildList {
        val here = local.settings
        val localApps = here.apps.associateBy { it.packageName }
        incoming.apps.forEach { app ->
            val current = localApps[app.packageName]
            if (app.kind != (current?.kind ?: AppKind.TOOL)) add(SetAppKind(app.packageName, app.kind))
            if (app.openLimit != current?.openLimit) add(SetOpenLimit(app.packageName, app.openLimit))
        }
        if (incoming.budgetMinutes != here.budgetMinutes) add(SetBudget(incoming.budgetMinutes))
        if (incoming.taperFloorMinutes != here.taperFloorMinutes) add(SetTaper(incoming.taperFloorMinutes))
        if (incoming.firstHourRule != here.firstHourRule) add(SetFirstHourRule(incoming.firstHourRule))
        if (incoming.earnIt != here.earnIt) add(SetEarnIt(incoming.earnIt))
        if (incoming.calendarFocus != here.calendarFocus) add(SetCalendarFocus(incoming.calendarFocus))
        (incoming.blockedFeeds + here.blockedFeeds).distinct().sorted().forEach { feed ->
            val wanted = feed in incoming.blockedFeeds
            if (wanted != (feed in here.blockedFeeds)) add(SetFeedBlocked(feed, wanted))
        }
        addModes(incoming, local)
        addSchedules(incoming.schedules, local.scheduleIds)
        addKeys(incoming.keys, here.keys, local.keyIds)
        incoming.lockedUntilMillis
            ?.takeIf { it > nowMillis && it > (here.lockedUntilMillis ?: Long.MIN_VALUE) }
            ?.let { add(StartLock(it)) }
    }

    private fun MutableList<SettingChange>.addModes(incoming: Backup, local: LocalSettings) {
        val localModes = local.settings.modes.associateBy { it.preset }
        incoming.modes.forEach { mode ->
            val id = local.modeIds[mode.preset] ?: return@forEach
            if (mode == localModes[mode.preset]) return@forEach
            add(
                SaveMode(
                    id = id,
                    budgetMinutes = mode.budgetMinutes,
                    allowlist = mode.allowlist,
                    scheduleDays = mode.scheduleDays,
                    scheduleStartMinute = mode.scheduleStartMinute,
                    scheduleEndMinute = mode.scheduleEndMinute,
                    waitStepsSeconds = mode.waitStepsSeconds,
                ),
            )
        }
        // After the modes, so switching compares against their restored values.
        if (incoming.manualMode != local.settings.manualMode) {
            val id = incoming.manualMode?.let { local.modeIds[it] }
            if (incoming.manualMode == null || id != null) add(SetManualMode(id))
        }
    }

    /** Matched by name: new ones are added, changed ones edited, and ones the backup doesn't have removed. */
    private fun MutableList<SettingChange>.addSchedules(incoming: List<Backup.Schedule>, local: List<Pair<Long, Backup.Schedule>>) {
        val unmatched = local.toMutableList()
        incoming.forEach { schedule ->
            val match = unmatched.firstOrNull { it.second.name == schedule.name }
            if (match != null) unmatched.remove(match)
            if (match?.second == schedule) return@forEach
            add(SaveSchedule(id = match?.first ?: 0, name = schedule.name, days = schedule.days, startMinute = schedule.startMinute, endMinute = schedule.endMinute))
        }
        unmatched.forEach { (id, _) -> add(RemoveSchedule(id)) }
    }

    private fun MutableList<SettingChange>.addKeys(incoming: List<Backup.Key>, local: List<Backup.Key>, localIds: Map<String, Long>) {
        val localHashes = local.map { it.secretHash }.toSet()
        val incomingHashes = incoming.map { it.secretHash }.toSet()
        incoming.filter { it.secretHash !in localHashes }.forEach { add(AddKey(it.type, it.label, it.secretHash)) }
        local.filter { it.secretHash !in incomingHashes }.forEach { key -> localIds[key.secretHash]?.let { add(RemoveKey(it)) } }
    }
}
