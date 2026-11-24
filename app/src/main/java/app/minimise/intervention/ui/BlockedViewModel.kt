package app.minimise.intervention.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.data.AppLauncher
import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.intervention.domain.GateOutcomes
import app.minimise.intervention.domain.TimeBoxOptions
import app.minimise.keys.domain.NfcKeyMethod
import app.minimise.keys.domain.QrKeyMethod
import app.minimise.unlock.domain.UnlockMethod
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.usage.domain.DailyBudget
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
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
    /** The last tap or scan didn't match a paired key. */
    val verifyFailed: Boolean = false,
    val finish: PauseFinish? = null,
)

/** @property usesLeft for rationed methods (emergency passes), how many are left. */
data class OverrideOption(val method: UnlockMethodId, val usesLeft: Int?)

sealed interface BlockedStep {
    /** The block's reason and the overrides on offer. */
    data object Choose : BlockedStep

    /** The method's own check: tap a key, scan a code, wait. */
    data class Verify(val method: UnlockMethodId) : BlockedStep

    /** Passed the method: pick how long. */
    data class TimeBox(val method: UnlockMethodId) : BlockedStep
}

@HiltViewModel
class BlockedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appRepository: AppRepository,
    private val methods: Map<UnlockMethodId, @JvmSuppressWildcards UnlockMethod>,
    private val dailyBudget: DailyBudget,
    private val outcomes: GateOutcomes,
    private val appLauncher: AppLauncher,
    private val nfcKey: NfcKeyMethod,
    private val qrKey: QrKeyMethod,
) : ViewModel() {
    private val packageName: String = savedState[BlockedActivity.EXTRA_PACKAGE] ?: ""
    private val launchAfter: Boolean = savedState[BlockedActivity.EXTRA_LAUNCH_AFTER] ?: false
    private val allowed: List<UnlockMethodId> = savedState.get<Array<String>>(BlockedActivity.EXTRA_OVERRIDES)
        ?.mapNotNull { name -> UnlockMethodId.entries.firstOrNull { it.name == name } }
        .orEmpty()

    private val _uiState = MutableStateFlow(BlockedUiState(reason = savedState[BlockedActivity.EXTRA_REASON] ?: ""))
    val uiState: StateFlow<BlockedUiState> = _uiState.asStateFlow()

    private var decided = false

    init {
        viewModelScope.launch {
            val label = appRepository.entry(packageName)?.label ?: packageName
            val options = allowed.mapNotNull { id ->
                val method = methods[id] ?: return@mapNotNull null
                if (method.isAvailable()) OverrideOption(id, method.usesLeft()) else null
            }
            _uiState.update {
                it.copy(appLabel = label, options = options, timeBoxOptions = TimeBoxOptions.cappedAt(dailyBudget.remaining()))
            }
        }
    }

    fun onOverrideChosen(method: UnlockMethodId) {
        if (decided || _uiState.value.options.none { it.method == method }) return
        val step = if (method in VERIFIED_METHODS) BlockedStep.Verify(method) else BlockedStep.TimeBox(method)
        _uiState.update { it.copy(step = step, verifyFailed = false) }
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
        if (_uiState.value.step == BlockedStep.Choose) onGoHome() else _uiState.update { it.copy(step = BlockedStep.Choose) }
    }

    fun onGoHome() {
        if (decided) return
        decided = true
        _uiState.update { it.copy(finish = PauseFinish.GO_HOME) }
    }

    private companion object {
        /** Methods with a check of their own before the time box. An emergency pass needs none. */
        val VERIFIED_METHODS = setOf(UnlockMethodId.NFC_KEY, UnlockMethodId.QR_KEY, UnlockMethodId.WAIT)
    }
}
