package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SetEarnIt
import app.minimise.settings.domain.SetTaper
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
    /** Taper's floor in force, or null when taper is off. */
    val taperFloor: Int? = null,
    /** The floor the user is about to save. */
    val taperDraft: Int = 0,
    /** A waiting raise or removal of the floor: its new value (null to stop) and how long until it applies. */
    val taperPending: Pair<Int?, Duration>? = null,
    val earnIt: Boolean = false,
    /** Earn-it switching on waits, like any loosening. */
    val earnItOnIn: Duration? = null,
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    settingsStore: SettingsStore,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val draft = MutableStateFlow<Int?>(null)
    private val taperDraft = MutableStateFlow<Int?>(null)
    private val lastResult = MutableStateFlow<GuardResult?>(null)
    private val drafts = combine(draft, taperDraft) { budget, floor -> budget to floor }

    val uiState: StateFlow<BudgetUiState> = combine(
        settingsStore.settings,
        settingsGuard.pending,
        drafts,
        lastResult,
        ticker(15.seconds).onEach { settingsGuard.applyDue() },
    ) { settings, pending, (draftMinutes, floorDraft), result, _ ->
        val now = timeSource.now()
        val current = settings.dailyBudget.inWholeMinutes.toInt()
        val waiting = pending.firstOrNull { it.change is SetBudget }
        val taperWaiting = pending.firstOrNull { it.change is SetTaper }
        BudgetUiState(
            currentMinutes = current,
            draftMinutes = draftMinutes ?: current,
            pendingMinutes = (waiting?.change as? SetBudget)?.minutes,
            pendingRemaining = waiting?.let { durationBetween(now, it.applyAt) },
            lastResult = result,
            taperFloor = settings.taperFloorMinutes,
            taperDraft = floorDraft ?: settings.taperFloorMinutes ?: (current / 2).coerceAtLeast(0),
            taperPending = taperWaiting?.let { (it.change as SetTaper).floorMinutes to durationBetween(now, it.applyAt) },
            earnIt = settings.earnIt,
            earnItOnIn = pending.firstOrNull { it.change == SetEarnIt(true) }?.let { durationBetween(now, it.applyAt) },
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

    fun onTaperStep(deltaMinutes: Int) {
        taperDraft.value = (uiState.value.taperDraft + deltaMinutes).coerceIn(0, MAX_MINUTES)
    }

    /** Starting taper, or lowering its floor, applies at once; raising the floor waits. */
    fun onTaperSave() {
        val floor = uiState.value.taperDraft
        viewModelScope.launch { settingsGuard.request(SetTaper(floor)) }
    }

    fun onTaperStop() {
        viewModelScope.launch { settingsGuard.request(SetTaper(null)) }
    }

    /** On waits 24 hours (more minutes is loosening); off, or cancelling a waiting on, is at once. */
    fun onEarnItTapped() {
        val state = uiState.value
        val enable = !state.earnIt && state.earnItOnIn == null
        viewModelScope.launch { settingsGuard.request(SetEarnIt(enable)) }
    }

    companion object {
        const val STEP_MINUTES = 5
        const val MAX_MINUTES = 8 * 60
    }
}
