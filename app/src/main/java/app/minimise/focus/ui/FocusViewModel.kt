package app.minimise.focus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.focus.domain.FocusSession
import app.minimise.focus.domain.FocusSessions
import app.minimise.keys.domain.NfcKeyMethod
import app.minimise.keys.domain.QrKeyMethod
import app.minimise.unlock.domain.UnlockMethodId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val active: FocusSession? = null,
    val left: Duration = Duration.ZERO,
    /** Minutes of a session that ended a short while ago, for the one-line summary. */
    val justEndedMinutes: Long? = null,
    val dialog: FocusDialog? = null,
    /** Paired key methods: the ways to end a locked session, and whether one can be locked at all. */
    val keyMethods: List<UnlockMethodId> = emptyList(),
    val verifyFailed: Boolean = false,
)

enum class FocusDialog { START, END }

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val focusSessions: FocusSessions,
    private val nfcKey: NfcKeyMethod,
    private val qrKey: QrKeyMethod,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val dialog = MutableStateFlow<DialogState?>(null)

    val uiState: StateFlow<FocusUiState> = combine(
        focusSessions.active,
        focusSessions.lastEnded,
        dialog,
        ticker(REFRESH),
    ) { active, lastEnded, dialog, _ ->
        val now = timeSource.now()
        val ended = lastEnded?.endedAt?.takeIf { durationBetween(it, now) < Defaults.FOCUS_SUMMARY_SHOWN_FOR }
        FocusUiState(
            active = active,
            left = active?.let { durationBetween(now, it.endAt) } ?: Duration.ZERO,
            justEndedMinutes = if (active == null && ended != null) durationBetween(lastEnded.startAt, ended).inWholeMinutes else null,
            dialog = dialog?.kind,
            keyMethods = dialog?.keyMethods.orEmpty(),
            verifyFailed = dialog?.failed == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    fun onFocusTapped() {
        viewModelScope.launch {
            val keys = buildList {
                if (nfcKey.isAvailable()) add(UnlockMethodId.NFC_KEY)
                if (qrKey.isAvailable()) add(UnlockMethodId.QR_KEY)
            }
            dialog.value = DialogState(if (focusSessions.current() == null) FocusDialog.START else FocusDialog.END, keys)
        }
    }

    fun onStart(length: Duration, locked: Boolean) {
        if (length !in Defaults.FOCUS_LENGTHS) return
        val canLock = dialog.value?.keyMethods?.isNotEmpty() == true
        dialog.value = null
        viewModelScope.launch { focusSessions.start(length, locked && canLock) }
    }

    /** Ends an unlocked session at once. A locked one needs a key: see [onNfcTag] and [onQrScanned]. */
    fun onEndNow() {
        viewModelScope.launch {
            if (focusSessions.current()?.locked == false) {
                focusSessions.endEarly()
                dialog.value = null
            }
        }
    }

    fun onNfcTag(tagId: ByteArray) {
        viewModelScope.launch { endWithKey(nfcKey.verify(tagId)) }
    }

    fun onQrScanned(text: String) {
        viewModelScope.launch { endWithKey(qrKey.verify(text)) }
    }

    fun onDismiss() {
        dialog.value = null
    }

    private suspend fun endWithKey(matched: Boolean) {
        if (matched) {
            focusSessions.endEarly()
            dialog.value = null
        } else {
            dialog.update { it?.copy(failed = true) }
        }
    }

    private data class DialogState(val kind: FocusDialog, val keyMethods: List<UnlockMethodId>, val failed: Boolean = false)

    private companion object {
        val REFRESH = 15.seconds
    }
}
