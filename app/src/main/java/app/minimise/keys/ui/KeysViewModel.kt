package app.minimise.keys.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.keys.domain.KeyRepository
import app.minimise.keys.domain.KeySecrets
import app.minimise.keys.domain.KeyType
import app.minimise.keys.domain.PhysicalKey
import app.minimise.settings.domain.AddKey
import app.minimise.settings.domain.RemoveKey
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
import kotlinx.coroutines.launch

data class KeysUiState(
    val rows: List<KeyRow> = emptyList(),
    val pairing: Pairing? = null,
    /** A key the user asked to remove, waiting for confirmation. */
    val confirmingRemoval: PhysicalKey? = null,
)

/** @property removalIn how long until a waiting removal applies. */
data class KeyRow(val key: PhysicalKey, val removalIn: Duration?)

sealed interface Pairing {
    /** Waiting for a sticker. [alreadyPaired] when the last one tapped is paired already. */
    data class Nfc(val alreadyPaired: Boolean = false) : Pairing

    /** Showing a new code to print. Only its hash is kept, once the user says it's saved. */
    data class Qr(val secret: String) : Pairing
}

@HiltViewModel
class KeysViewModel @Inject constructor(
    private val keyRepository: KeyRepository,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val pairing = MutableStateFlow<Pairing?>(null)
    private val confirming = MutableStateFlow<PhysicalKey?>(null)

    val uiState: StateFlow<KeysUiState> = combine(
        keyRepository.keys,
        settingsGuard.pending,
        pairing,
        confirming,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { keys, pending, pairing, confirming, _ ->
        val now = timeSource.now()
        KeysUiState(
            rows = keys.map { key ->
                KeyRow(key, pending.firstOrNull { it.change == RemoveKey(key.id) }?.let { durationBetween(now, it.applyAt) })
            },
            pairing = pairing,
            confirmingRemoval = confirming,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KeysUiState())

    fun onPairNfc() {
        pairing.value = Pairing.Nfc()
    }

    fun onNfcTag(tagId: ByteArray, label: String) {
        if (pairing.value !is Pairing.Nfc) return
        val hash = KeySecrets.hashOf(tagId)
        viewModelScope.launch {
            if (keyRepository.all().any { it.secretHash == hash }) {
                pairing.value = Pairing.Nfc(alreadyPaired = true)
            } else {
                settingsGuard.request(AddKey(KeyType.NFC.name, label, hash))
                pairing.value = null
            }
        }
    }

    fun onMakeQr() {
        pairing.value = Pairing.Qr(KeySecrets.newQrSecret())
    }

    fun onQrSaved(label: String) {
        val qr = pairing.value as? Pairing.Qr ?: return
        pairing.value = null
        viewModelScope.launch { settingsGuard.request(AddKey(KeyType.QR.name, label, KeySecrets.hashOf(qr.secret))) }
    }

    fun onPairingDismissed() {
        pairing.value = null
    }

    fun onRemove(key: PhysicalKey) {
        confirming.value = key
    }

    fun onRemoveConfirmed() {
        val key = confirming.value ?: return
        confirming.value = null
        viewModelScope.launch { settingsGuard.request(RemoveKey(key.id)) }
    }

    fun onRemoveDismissed() {
        confirming.value = null
    }

    private companion object {
        val REFRESH = 15.seconds
    }
}
