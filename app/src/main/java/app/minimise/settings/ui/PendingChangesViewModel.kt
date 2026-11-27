package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.apps.domain.WebTarget
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.feeds.domain.FeedRuleSource
import app.minimise.keys.domain.KeyRepository
import app.minimise.modes.domain.ModeRepository
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SetManualMode
import app.minimise.settings.domain.RemoveMode
import app.minimise.settings.domain.SetBreatheWait
import app.minimise.schedules.domain.ScheduleRepository
import app.minimise.settings.domain.AddKey
import app.minimise.settings.domain.RemoveKey
import app.minimise.settings.domain.RemoveSchedule
import app.minimise.settings.domain.SaveSchedule
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetFeedBlocked
import app.minimise.settings.domain.StartLock
import app.minimise.settings.domain.SetTaper
import app.minimise.settings.domain.SetEarnIt
import app.minimise.settings.domain.SetCalendarFocus
import app.minimise.settings.domain.SetFirstHourRule
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SetDayStart
import app.minimise.settings.domain.SetOpenLimit
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** @property lockedUntil while a lock period runs, nothing can be applied early and waiting changes wait for it to end. */
data class PendingChangesUiState(val items: List<PendingItem> = emptyList(), val lockedUntil: LocalDateTime? = null)

data class PendingItem(val id: Long, val description: PendingDescription, val remaining: Duration)

sealed interface PendingDescription {
    data class AppKindChange(val appLabel: String, val kind: AppKind) : PendingDescription

    data class BudgetChange(val minutes: Int) : PendingDescription

    data class DayStartChange(val time: LocalTime) : PendingDescription

    /** @property limit null when the limit is being removed. */
    data class OpenLimitChange(val appLabel: String, val limit: Int?) : PendingDescription

    /** @property seconds null when the app goes back to the usual pause. */
    data class BreatheWaitChange(val appLabel: String, val seconds: Int?) : PendingDescription

    data class ScheduleChange(val name: String, val removed: Boolean) : PendingDescription

    data class FirstHourChange(val enabled: Boolean) : PendingDescription

    data class KeyChange(val label: String, val removed: Boolean) : PendingDescription

    /** @property modeName null for "follow schedules". @property until when the picked mode would end. */
    data class ModeSwitch(val modeName: String?, val until: LocalDateTime? = null) : PendingDescription

    data class ModeEdit(val modeName: String) : PendingDescription

    data class ModeAdd(val modeName: String) : PendingDescription

    data class ModeRemove(val modeName: String) : PendingDescription

    data class FeedChange(val feedName: String, val blocked: Boolean) : PendingDescription

    /** Never pending in practice (a lock only tightens), but every change needs a description. */
    data object LockChange : PendingDescription

    /** @property floorMinutes null when taper stops. */
    data class TaperChange(val floorMinutes: Int?) : PendingDescription

    data class EarnItChange(val enabled: Boolean) : PendingDescription

    data class CalendarFocusChange(val enabled: Boolean) : PendingDescription
}

@HiltViewModel
class PendingChangesViewModel @Inject constructor(
    appRepository: AppRepository,
    scheduleRepository: ScheduleRepository,
    keyRepository: KeyRepository,
    modeRepository: ModeRepository,
    feedRules: FeedRuleSource,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    /** Names of the things pending changes refer to. */
    private val feedNames: Map<String, String> = feedRules.rules().associate { it.id to it.name }

    private val names: Flow<Names> = combine(
        appRepository.apps,
        scheduleRepository.schedules,
        keyRepository.keys,
        modeRepository.modes,
    ) { apps, schedules, keys, modes ->
        Names(
            apps = apps.associate { it.packageName to it.label },
            schedules = schedules.associate { it.id to it.name },
            keys = keys.associate { it.id to it.label },
            modes = modes.associate { it.id to it.name },
        )
    }

    val uiState: StateFlow<PendingChangesUiState> = combine(
        settingsGuard.pending,
        names,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { pending, names, _ ->
        val labels = names.apps
        val now = timeSource.now()
        PendingChangesUiState(
            lockedUntil = settingsGuard.lockedUntil()?.atZone(timeSource.zone())?.toLocalDateTime(),
            items = pending.map { p ->
                val description = when (val change = p.change) {
                    is SetAppKind -> PendingDescription.AppKindChange(
                        appLabel = labels[change.packageName] ?: WebTarget.domainOf(change.packageName),
                        kind = change.kind,
                    )
                    is SetBudget -> PendingDescription.BudgetChange(change.minutes)
                    is SetDayStart -> PendingDescription.DayStartChange(change.time)
                    is SetOpenLimit -> PendingDescription.OpenLimitChange(
                        appLabel = labels[change.packageName] ?: change.packageName,
                        limit = change.limit,
                    )
                    is SaveSchedule -> PendingDescription.ScheduleChange(change.name, removed = false)
                    is RemoveSchedule -> PendingDescription.ScheduleChange(names.schedules[change.id].orEmpty(), removed = true)
                    is SetFirstHourRule -> PendingDescription.FirstHourChange(change.enabled)
                    is AddKey -> PendingDescription.KeyChange(change.label, removed = false)
                    is RemoveKey -> PendingDescription.KeyChange(names.keys[change.id].orEmpty(), removed = true)
                    is SetBreatheWait -> PendingDescription.BreatheWaitChange(
                        appLabel = labels[change.packageName] ?: change.packageName,
                        seconds = change.seconds,
                    )
                    is SetManualMode -> PendingDescription.ModeSwitch(
                        modeName = change.modeId?.let { names.modes[it] },
                        until = change.untilEpochMillis?.takeIf { change.modeId != null }
                            ?.let { Instant.ofEpochMilli(it).atZone(timeSource.zone()).toLocalDateTime() },
                    )
                    is SaveMode -> if (change.id == 0L) {
                        PendingDescription.ModeAdd(change.name.orEmpty())
                    } else {
                        PendingDescription.ModeEdit(names.modes[change.id].orEmpty())
                    }
                    is RemoveMode -> PendingDescription.ModeRemove(names.modes[change.id].orEmpty())
                    is StartLock -> PendingDescription.LockChange
                    is SetTaper -> PendingDescription.TaperChange(change.floorMinutes)
                    is SetEarnIt -> PendingDescription.EarnItChange(change.enabled)
                    is SetCalendarFocus -> PendingDescription.CalendarFocusChange(change.enabled)
                    is SetFeedBlocked -> PendingDescription.FeedChange(feedNames[change.feedId] ?: change.feedId, change.blocked)
                }
                PendingItem(id = p.id, description = description, remaining = durationBetween(now, p.applyAt))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingChangesUiState())

    fun onCancel(item: PendingItem) {
        viewModelScope.launch { settingsGuard.cancel(item.id) }
    }

    private data class Names(
        val apps: Map<String, String>,
        val schedules: Map<Long, String>,
        val keys: Map<Long, String>,
        val modes: Map<Long, String>,
    )

    private companion object {
        val REFRESH = 5.seconds
    }
}
