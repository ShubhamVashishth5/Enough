package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.keys.domain.KeyRepository
import app.minimise.modes.domain.ModeRepository
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SetManualMode
import app.minimise.schedules.domain.ScheduleRepository
import app.minimise.settings.domain.AddKey
import app.minimise.settings.domain.RemoveKey
import app.minimise.settings.domain.RemoveSchedule
import app.minimise.settings.domain.SaveSchedule
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetFirstHourRule
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SetOpenLimit
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class PendingChangesUiState(val items: List<PendingItem> = emptyList())

data class PendingItem(val id: Long, val description: PendingDescription, val remaining: Duration)

sealed interface PendingDescription {
    data class AppKindChange(val appLabel: String, val kind: AppKind) : PendingDescription

    data class BudgetChange(val minutes: Int) : PendingDescription

    /** @property limit null when the limit is being removed. */
    data class OpenLimitChange(val appLabel: String, val limit: Int?) : PendingDescription

    data class ScheduleChange(val name: String, val removed: Boolean) : PendingDescription

    data class FirstHourChange(val enabled: Boolean) : PendingDescription

    data class KeyChange(val label: String, val removed: Boolean) : PendingDescription

    /** @property modeName null for "follow schedules". */
    data class ModeSwitch(val modeName: String?) : PendingDescription

    data class ModeEdit(val modeName: String) : PendingDescription
}

@HiltViewModel
class PendingChangesViewModel @Inject constructor(
    appRepository: AppRepository,
    scheduleRepository: ScheduleRepository,
    keyRepository: KeyRepository,
    modeRepository: ModeRepository,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    /** Names of the things pending changes refer to. */
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
            items = pending.map { p ->
                val description = when (val change = p.change) {
                    is SetAppKind -> PendingDescription.AppKindChange(
                        appLabel = labels[change.packageName] ?: change.packageName,
                        kind = change.kind,
                    )
                    is SetBudget -> PendingDescription.BudgetChange(change.minutes)
                    is SetOpenLimit -> PendingDescription.OpenLimitChange(
                        appLabel = labels[change.packageName] ?: change.packageName,
                        limit = change.limit,
                    )
                    is SaveSchedule -> PendingDescription.ScheduleChange(change.name, removed = false)
                    is RemoveSchedule -> PendingDescription.ScheduleChange(names.schedules[change.id].orEmpty(), removed = true)
                    is SetFirstHourRule -> PendingDescription.FirstHourChange(change.enabled)
                    is AddKey -> PendingDescription.KeyChange(change.label, removed = false)
                    is RemoveKey -> PendingDescription.KeyChange(names.keys[change.id].orEmpty(), removed = true)
                    is SetManualMode -> PendingDescription.ModeSwitch(change.modeId?.let { names.modes[it] })
                    is SaveMode -> PendingDescription.ModeEdit(names.modes[change.id].orEmpty())
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
