package app.minimise.intervention.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.data.AppLauncher
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.intervention.domain.GateOutcomes
import app.minimise.intervention.domain.TimeBoxOptions
import app.minimise.intervention.domain.Trigger
import app.minimise.gate.rules.BudgetRule
import app.minimise.keys.domain.NfcKeyMethod
import app.minimise.keys.domain.QrKeyMethod
import app.minimise.unlock.domain.UnlockMethod
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.unlock.domain.WaitMethod
import app.minimise.usage.domain.DailyBudget
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUiState(
    val appLabel: String = "",
    val reason: String = "",
    /** The overrides this block allows that can be used right now. */
    val options: List<OverrideOption> = emptyList(),
    val step: BlockedStep = BlockedStep.Choose,
    val timeBoxOptions: List<Duration> = Defaults.TIME_BOX_OPTIONS,
    /** Left of the wait, while waiting is the method. */
    val waitRemaining: Duration = Duration.ZERO,
    /** The last tap or scan didn't match a paired key. */
    val verifyFailed: Boolean = false,
    val finish: PauseFinish? = null,
)

/** @property usesLeft for rationed methods (emergency passes), how many are left. */
data class OverrideOption(val method: UnlockMethodId, val usesLeft: Int?)

sealed interface BlockedStep {
    /** Budget blocks first ask "What pulled you in?", for the weekly report. */
    data object Trigger : BlockedStep

    /** The block's reason and the overrides on offer. */
    data object Choose : BlockedStep

    /** The method's own check: tap a key, scan a code, wait. */
    data class Verify(val method: UnlockMethodId) : BlockedStep

    /** Passed the method: pick how long. */
    data class TimeBox(val method: UnlockMethodId) : BlockedStep
}

@HiltViewModel
class BlockedViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val appRepository: AppRepository,
    private val methods: Map<UnlockMethodId, @JvmSuppressWildcards UnlockMethod>,
    private val dailyBudget: DailyBudget,
    private val outcomes: GateOutcomes,
    private val appLauncher: AppLauncher,
    private val nfcKey: NfcKeyMethod,
    private val qrKey: QrKeyMethod,
    private val waitMethod: WaitMethod,
    private val timeSource: TimeSource,
) : ViewModel() {
    private var waitJob: Job? = null

    private val packageName: String = savedState[BlockedActivity.EXTRA_PACKAGE] ?: ""
    private val launchAfter: Boolean = savedState[BlockedActivity.EXTRA_LAUNCH_AFTER] ?: false
    private val allowed: List<UnlockMethodId> = savedState.get<Array<String>>(BlockedActivity.EXTRA_OVERRIDES)
        ?.mapNotNull { name -> UnlockMethodId.entries.firstOrNull { it.name == name } }
        .orEmpty()

    private val reason: String = savedState[BlockedActivity.EXTRA_REASON] ?: ""
    private val askTrigger = reason == BudgetRule.REASON && savedState.get<Boolean>(KEY_TRIGGER_ANSWERED) != true
    private val _uiState = MutableStateFlow(BlockedUiState(reason = reason, step = if (askTrigger) BlockedStep.Trigger else BlockedStep.Choose))
    val uiState: StateFlow<BlockedUiState> = _uiState.asStateFlow()

    private var decided = false

    init {
        val logShown = savedState.get<Boolean>(KEY_SHOWN_LOGGED) != true
        savedState[KEY_SHOWN_LOGGED] = true
        viewModelScope.launch {
            if (logShown) outcomes.blockShown(packageName, reason)
            val app = appRepository.entry(packageName)
            val label = app?.label ?: packageName
            // Only Vault apps spend the budget, so only they have their time box capped by it.
            val timeBoxes = if (app?.kind == AppKind.VAULT) TimeBoxOptions.cappedAt(dailyBudget.remaining()) else Defaults.TIME_BOX_OPTIONS
            val options = allowed.mapNotNull { id ->
                val method = methods[id] ?: return@mapNotNull null
                if (method.isAvailable()) OverrideOption(id, method.usesLeft()) else null
            }
            _uiState.update {
                it.copy(appLabel = label, options = options, timeBoxOptions = timeBoxes)
            }
        }
    }

    /** Null skips the question. Either way, on to the block itself. */
    fun onTriggerChosen(trigger: Trigger?) {
        if (_uiState.value.step != BlockedStep.Trigger) return
        savedState[KEY_TRIGGER_ANSWERED] = true
        _uiState.update { it.copy(step = BlockedStep.Choose) }
        if (trigger != null) viewModelScope.launch { outcomes.trigger(packageName, trigger) }
    }

    fun onOverrideChosen(method: UnlockMethodId) {
        if (decided || _uiState.value.options.none { it.method == method }) return
        val step = if (method in VERIFIED_METHODS) BlockedStep.Verify(method) else BlockedStep.TimeBox(method)
        _uiState.update { it.copy(step = step, verifyFailed = false) }
        if (method == UnlockMethodId.WAIT) startWait()
    }

    /** Counts down on the monotonic clock, then passes the wait. */
    private fun startWait() {
        waitJob?.cancel()
        val endsAt = timeSource.elapsed() + waitMethod.wait
        waitJob = viewModelScope.launch {
            while (true) {
                val left = (endsAt - timeSource.elapsed()).coerceAtLeast(Duration.ZERO)
                _uiState.update { it.copy(waitRemaining = left) }
                if (left == Duration.ZERO) break
                delay(TICK)
            }
            onVerified(UnlockMethodId.WAIT)
        }
    }

    fun onNfcTag(tagId: ByteArray) {
        viewModelScope.launch { checked(UnlockMethodId.NFC_KEY, nfcKey.verify(tagId)) }
    }

    fun onQrScanned(text: String) {
        viewModelScope.launch { checked(UnlockMethodId.QR_KEY, qrKey.verify(text)) }
    }

    private fun checked(method: UnlockMethodId, matched: Boolean) {
        if (matched) onVerified(method) else _uiState.update { it.copy(verifyFailed = true) }
    }

    /** The method's screen confirmed the attempt (a matching tap, scan or a finished wait). */
    fun onVerified(method: UnlockMethodId) {
        val step = _uiState.value.step
        if (decided || step !is BlockedStep.Verify || step.method != method) return
        _uiState.update { it.copy(step = BlockedStep.TimeBox(method)) }
    }

    fun onTimeBoxChosen(timeBox: Duration) {
        val state = _uiState.value
        val step = state.step as? BlockedStep.TimeBox ?: return
        if (decided || timeBox !in state.timeBoxOptions) return
        decided = true
        viewModelScope.launch {
            // A rationed method may have run out on another screen since this one opened.
            if (methods[step.method]?.isAvailable() != true) {
                _uiState.update { it.copy(finish = PauseFinish.GO_HOME) }
                return@launch
            }
            outcomes.overridden(packageName, step.method, timeBox)
            if (launchAfter) appLauncher.launch(packageName)
            _uiState.update { it.copy(finish = PauseFinish.PROCEED) }
        }
    }

    /** Back steps out of a method; from the first step it goes home. */
    fun onBack() {
        if (decided) return
        waitJob?.cancel()
        when (_uiState.value.step) {
            BlockedStep.Choose, BlockedStep.Trigger -> onGoHome()
            else -> _uiState.update { it.copy(step = BlockedStep.Choose) }
        }
    }

    fun onGoHome() {
        if (decided) return
        decided = true
        _uiState.update { it.copy(finish = PauseFinish.GO_HOME) }
    }

    private companion object {
        const val KEY_SHOWN_LOGGED = "shown_logged"
        const val KEY_TRIGGER_ANSWERED = "trigger_answered"

        /** Methods with a check of their own before the time box. An emergency pass needs none. */
        val VERIFIED_METHODS = setOf(UnlockMethodId.NFC_KEY, UnlockMethodId.QR_KEY, UnlockMethodId.WAIT)
        val TICK = 200.milliseconds
    }
}
