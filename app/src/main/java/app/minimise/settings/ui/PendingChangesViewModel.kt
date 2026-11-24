package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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
}

@HiltViewModel
class PendingChangesViewModel @Inject constructor(
    appRepository: AppRepository,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    val uiState: StateFlow<PendingChangesUiState> = combine(
        settingsGuard.pending,
        appRepository.apps,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { pending, apps, _ ->
        val labels = apps.associate { it.packageName to it.label }
        val now = timeSource.now()
        PendingChangesUiState(
            items = pending.map { p ->
                val description = when (val change = p.change) {
                    is SetAppKind -> PendingDescription.AppKindChange(
                        appLabel = labels[change.packageName] ?: change.packageName,
                        kind = change.kind,
                    )
                    is SetBudget -> PendingDescription.BudgetChange(change.minutes)
                }
                PendingItem(id = p.id, description = description, remaining = durationBetween(now, p.applyAt))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingChangesUiState())

    fun onCancel(item: PendingItem) {
        viewModelScope.launch { settingsGuard.cancel(item.id) }
    }

    private companion object {
        val REFRESH = 5.seconds
    }
}
