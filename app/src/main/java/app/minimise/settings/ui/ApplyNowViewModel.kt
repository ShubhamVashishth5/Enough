package app.minimise.settings.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.minimise.R
import app.minimise.core.time.TimeSource
import app.minimise.keys.domain.NfcKeyMethod
import app.minimise.keys.domain.QrKeyMethod
import app.minimise.launcher.ui.Destination
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.SettingsGuard
import app.minimise.unlock.domain.FriendCodeMethod
import app.minimise.unlock.domain.TypingPenance
import app.minimise.unlock.domain.UnlockMethod
import app.minimise.unlock.domain.UnlockMethodId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** @property usesLeft for friend codes, how many are left. */
data class SkipMethod(val method: UnlockMethodId, val usesLeft: Int?)

data class ApplyNowUiState(
    val methods: List<SkipMethod> = emptyList(),
    /** Null while choosing a method. */
    val method: UnlockMethodId? = null,
    val passage: String = "",
    val typed: String = "",
    /** The last code, tap or scan didn't match. */
    val failed: Boolean = false,
    /** Set when a lock period refused the change. */
    val lockedUntil: LocalDateTime? = null,
    val done: Boolean = false,
)

/**
 * Applies one pending loosen now, once the user passes a friend code, typing penance or a physical
 * key. Never available during a lock period.
 */
@HiltViewModel
class ApplyNowViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val settingsGuard: SettingsGuard,
    private val methods: Map<UnlockMethodId, @JvmSuppressWildcards UnlockMethod>,
    private val friendCode: FriendCodeMethod,
    private val nfcKey: NfcKeyMethod,
    private val qrKey: QrKeyMethod,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val pendingId = savedState.toRoute<Destination.ApplyNow>().pendingId
    private val _uiState = MutableStateFlow(ApplyNowUiState())
    val uiState: StateFlow<ApplyNowUiState> = _uiState.asStateFlow()
    private var applying = false

    init {
        viewModelScope.launch {
            val offered = OFFERED.mapNotNull { id ->
                val method = methods[id] ?: return@mapNotNull null
                if (method.isAvailable()) SkipMethod(id, if (id == UnlockMethodId.FRIEND_CODE) method.usesLeft() else null) else null
            }
            _uiState.update { it.copy(methods = offered, lockedUntil = lockedUntil()) }
        }
    }

    fun onMethodChosen(method: UnlockMethodId) {
        if (_uiState.value.methods.none { it.method == method }) return
        val passage = if (method == UnlockMethodId.TYPING) {
            TypingPenance.passage(context.resources.getStringArray(R.array.typing_words).toList())
        } else {
            ""
        }
        _uiState.update { it.copy(method = method, passage = passage, typed = "", failed = false) }
    }

    fun onBack() {
        _uiState.update { it.copy(method = null, failed = false) }
    }

    /** Only keystroke-sized edits are taken, so the passage can't be pasted in. */
    fun onTyped(text: String) {
        val state = _uiState.value
        if (state.method != UnlockMethodId.TYPING || !TypingPenance.accepts(state.typed, text)) return
        _uiState.update { it.copy(typed = text) }
        if (TypingPenance.isDone(state.passage, text)) apply(UnlockMethodId.TYPING)
    }

    fun onFriendCode(code: String) {
        viewModelScope.launch { checked(UnlockMethodId.FRIEND_CODE, friendCode.verify(code)) }
    }

    fun onNfcTag(tagId: ByteArray) {
        viewModelScope.launch { checked(UnlockMethodId.NFC_KEY, nfcKey.verify(tagId)) }
    }

    fun onQrScanned(text: String) {
        viewModelScope.launch { checked(UnlockMethodId.QR_KEY, qrKey.verify(text)) }
    }

    private fun checked(method: UnlockMethodId, matched: Boolean) {
        if (_uiState.value.method != method) return
        if (matched) apply(method) else _uiState.update { it.copy(failed = true) }
    }

    private fun apply(method: UnlockMethodId) {
        if (applying) return
        applying = true
        viewModelScope.launch {
            when (val result = settingsGuard.applyNow(pendingId, method)) {
                is GuardResult.Refused -> _uiState.update { it.copy(lockedUntil = result.until?.atZone(timeSource.zone())?.toLocalDateTime()) }
                else -> _uiState.update { it.copy(done = true) }
            }
            applying = false
        }
    }

    private suspend fun lockedUntil(): LocalDateTime? = settingsGuard.lockedUntil()?.atZone(timeSource.zone())?.toLocalDateTime()

    private companion object {
        val OFFERED = listOf(UnlockMethodId.FRIEND_CODE, UnlockMethodId.NFC_KEY, UnlockMethodId.QR_KEY, UnlockMethodId.TYPING)
    }
}
