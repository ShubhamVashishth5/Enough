package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetUiState(
    val currentMinutes: Int? = null,
    /** What the user is about to save; starts at the current budget. */
    val draftMinutes: Int? = null,
    val pendingMinutes: Int? = null,
    val pendingRemaining: Duration? = null,
    val lastResult: GuardResult? = null,
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    settingsStore: SettingsStore,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val draft = MutableStateFlow<Int?>(null)
    private val lastResult = MutableStateFlow<GuardResult?>(null)

    val uiState: StateFlow<BudgetUiState> = combine(
        settingsStore.settings,
        settingsGuard.pending,
        draft,
        lastResult,
        ticker(15.seconds).onEach { settingsGuard.applyDue() },
    ) { settings, pending, draftMinutes, result, _ ->
        val current = settings.dailyBudget.inWholeMinutes.toInt()
        val waiting = pending.firstOrNull { it.change is SetBudget }
        BudgetUiState(
            currentMinutes = current,
            draftMinutes = draftMinutes ?: current,
            pendingMinutes = (waiting?.change as? SetBudget)?.minutes,
            pendingRemaining = waiting?.let { durationBetween(timeSource.now(), it.applyAt) },
            lastResult = result,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun onStep(deltaMinutes: Int) {
        val base = uiState.value.draftMinutes ?: return
        draft.value = (base + deltaMinutes).coerceIn(0, MAX_MINUTES)
        lastResult.update { null }
    }

    fun onSave() {
        val minutes = uiState.value.draftMinutes ?: return
        viewModelScope.launch { lastResult.value = settingsGuard.request(SetBudget(minutes)) }
    }

    companion object {
        const val STEP_MINUTES = 5
        const val MAX_MINUTES = 8 * 60
    }
}
