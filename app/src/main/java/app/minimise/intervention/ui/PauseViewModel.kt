package app.minimise.intervention.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.data.AppLauncher
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.core.di.ApplicationScope
import app.minimise.core.time.TimeSource
import app.minimise.intervention.domain.GateOutcomes
import app.minimise.intervention.domain.PauseReason
import app.minimise.swaps.domain.Swap
import app.minimise.swaps.domain.SwapRepository
import app.minimise.swaps.domain.forTurn
import app.minimise.usage.domain.DailyBudget
import app.minimise.usage.domain.TodayUsageProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PauseUiState(
    val appLabel: String = "",
    /** This open's number today, counting itself: "Open 4 today". */
    val openNumber: Int? = null,
    val wait: Duration = Duration.ZERO,
    val remaining: Duration = Duration.ZERO,
    /** Set once a reason is picked; the time box choice comes next. */
    val reason: PauseReason? = null,
    /** Null until loaded. */
    val budgetLeft: Duration? = null,
    /** The usual choices, capped at the budget left. */
    val timeBoxOptions: List<Duration> = Defaults.TIME_BOX_OPTIONS,
    /** The Instead list entry this pause offers for Bored or Habit. Null when the list is empty. */
    val swap: Swap? = null,
    /** The Tool app the swap opens, if it has one. */
    val swapAppLabel: String? = null,
    /** The user chose Open anyway. */
    val swapDeclined: Boolean = false,
    val finish: PauseFinish? = null,
) {
    val waiting: Boolean get() = remaining > Duration.ZERO

    /** The swap is on screen: the reason asks for one and the user hasn't passed on it yet. */
    val offeringSwap: Boolean get() = swap != null && !swapDeclined && reason?.offersSwap == true
}

enum class PauseFinish {
    /** The user goes on to the app. */
    PROCEED,

    /** Cancelled, or took a swap with no app: back to the home screen. */
    GO_HOME,

    /** Took a swap that opened a Tool app, which is now in front. */
    SWAPPED,
}

@HiltViewModel
class PauseViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val appRepository: AppRepository,
    private val todayUsage: TodayUsageProvider,
    private val swapRepository: SwapRepository,
    private val dailyBudget: DailyBudget,
    private val outcomes: GateOutcomes,
    private val appLauncher: AppLauncher,
    private val timeSource: TimeSource,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val packageName: String = savedState[PauseActivity.EXTRA_PACKAGE] ?: ""
    private val launchAfter: Boolean = savedState[PauseActivity.EXTRA_LAUNCH_AFTER] ?: false
    private val wait: Duration = (savedState.get<Long>(PauseActivity.EXTRA_WAIT_MILLIS) ?: 0L).milliseconds

    /** Monotonic start time, kept in saved state so rotation (or process death) doesn't restart the countdown. */
    private val startedAt: Duration = savedState.get<Long>(KEY_STARTED_AT)?.milliseconds
        ?: timeSource.elapsed().also { savedState[KEY_STARTED_AT] = it.inWholeMilliseconds }

    private var decided: Boolean
        get() = savedState[KEY_DECIDED] ?: false
        set(value) {
            savedState[KEY_DECIDED] = value
        }

    private val _uiState = MutableStateFlow(
        PauseUiState(
            wait = wait,
            remaining = remaining(),
            reason = savedState.get<String>(KEY_REASON)?.let(PauseReason::valueOf),
            swapDeclined = savedState[KEY_SWAP_DECLINED] ?: false,
        ),
    )
    val uiState: StateFlow<PauseUiState> = _uiState.asStateFlow()

    init {
        val logShown = savedState.get<Boolean>(KEY_SHOWN_LOGGED) != true
        savedState[KEY_SHOWN_LOGGED] = true
        viewModelScope.launch {
            // Read today's numbers before logging this pause, so the swap turn counts earlier pauses only.
            val today = todayUsage.today()
            if (logShown) outcomes.shown(packageName, wait)
            val label = appRepository.entry(packageName)?.label ?: packageName
            val budgetLeft = dailyBudget.remaining()
            val turn = savedState.get<Int>(KEY_SWAP_TURN) ?: today.pausesShown.also { savedState[KEY_SWAP_TURN] = it }
            val swap = swapRepository.all().forTurn(turn)
            _uiState.update {
                it.copy(
                    appLabel = label,
                    openNumber = today.proceededOpens + 1,
                    budgetLeft = budgetLeft,
                    timeBoxOptions = timeBoxOptions(budgetLeft),
                    swap = swap,
                    swapAppLabel = swap?.packageName?.let { pkg -> toolApp(pkg)?.label },
                )
            }
        }
        viewModelScope.launch {
            while (true) {
                val remaining = remaining()
                _uiState.update { it.copy(remaining = remaining) }
                if (remaining <= Duration.ZERO) break
                delay(TICK)
            }
        }
    }

    fun onReasonChosen(reason: PauseReason) {
        if (decided || _uiState.value.waiting) return
        savedState[KEY_REASON] = reason.name
        _uiState.update { it.copy(reason = reason) }
    }

    /** Back from the swap or the time box choice to the reasons. */
    fun onChangeReason() {
        if (decided) return
        savedState[KEY_REASON] = null
        savedState[KEY_SWAP_DECLINED] = false
        _uiState.update { it.copy(reason = null, swapDeclined = false) }
    }

    /** Do this instead: log it, then open the swap's Tool app or go home. The Vault app never opens. */
    fun onSwapTaken() {
        val state = _uiState.value
        val swap = state.swap ?: return
        if (decided || !state.offeringSwap) return
        decided = true
        viewModelScope.launch {
            outcomes.swapTaken(packageName, swap)
            val opened = swap.packageName?.let { toolApp(it) }?.let { appLauncher.launch(it.packageName) } ?: false
            _uiState.update { it.copy(finish = if (opened) PauseFinish.SWAPPED else PauseFinish.GO_HOME) }
        }
    }

    /** Open anyway: on to the time box choice. */
    fun onSwapDeclined() {
        if (decided || !_uiState.value.offeringSwap) return
        savedState[KEY_SWAP_DECLINED] = true
        _uiState.update { it.copy(swapDeclined = true) }
    }

    fun onTimeBoxChosen(timeBox: Duration) {
        val state = _uiState.value
        val reason = state.reason ?: return
        if (decided || state.waiting || state.offeringSwap || timeBox !in state.timeBoxOptions) return
        decided = true
        viewModelScope.launch {
            outcomes.proceeded(packageName, reason, timeBox)
            if (launchAfter) appLauncher.launch(packageName)
            _uiState.update { it.copy(finish = PauseFinish.PROCEED) }
        }
    }

    fun onCancel() {
        if (decided) return
        decided = true
        appScope.launch { outcomes.cancelled(packageName) }
        _uiState.update { it.copy(finish = PauseFinish.GO_HOME) }
    }

    /** Leaving the screen any other way (Home, recents, screen off) counts as cancelling. */
    override fun onCleared() {
        if (!decided) {
            decided = true
            appScope.launch { outcomes.cancelled(packageName) }
        }
    }

    /** Every option shorter than the budget left, then the budget left itself if it's shorter than the longest. */
    private fun timeBoxOptions(budgetLeft: Duration): List<Duration> {
        val under = Defaults.TIME_BOX_OPTIONS.filter { it < budgetLeft }
        return if (budgetLeft < Defaults.TIME_BOX_OPTIONS.last()) under + budgetLeft else under
    }

    /** A swap only opens apps that are still Tool apps; one moved to the Vault since is skipped. */
    private suspend fun toolApp(packageName: String) = appRepository.entry(packageName)?.takeIf { it.kind == AppKind.TOOL }

    private fun remaining(): Duration = (wait - (timeSource.elapsed() - startedAt)).coerceAtLeast(Duration.ZERO)

    private companion object {
        const val KEY_STARTED_AT = "started_at_elapsed_ms"
        const val KEY_DECIDED = "decided"
        const val KEY_SHOWN_LOGGED = "shown_logged"
        const val KEY_REASON = "reason"
        const val KEY_SWAP_TURN = "swap_turn"
        const val KEY_SWAP_DECLINED = "swap_declined"
        val TICK = 200.milliseconds
    }
}
