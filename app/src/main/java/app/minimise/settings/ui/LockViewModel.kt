package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.TimeSource
import app.minimise.core.time.plus
import app.minimise.core.ui.ticker
import app.minimise.settings.domain.SettingsGuard
import app.minimise.settings.domain.StartLock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @property lockedUntil the end of the lock in force, or null when there is none.
 * @property choices lock lengths that would end later than the current lock (all of them when unlocked).
 * @property confirming a length the user picked, waiting for "Lock" (it can't be undone).
 */
data class LockUiState(
    val lockedUntil: LocalDateTime? = null,
    val choices: List<Duration> = Defaults.LOCK_LENGTHS,
    val confirming: Duration? = null,
    val confirmingUntil: LocalDateTime? = null,
)

@HiltViewModel
class LockViewModel @Inject constructor(
    settingsStore: SettingsStore,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val confirming = MutableStateFlow<Duration?>(null)

    val uiState: StateFlow<LockUiState> = combine(settingsStore.settings, confirming, ticker(1.minutes)) { settings, confirming, _ ->
        val now = timeSource.now()
        val until = settings.lockedUntil?.takeIf { it.isAfter(now) }
        val zone = timeSource.zone()
        LockUiState(
            lockedUntil = until?.atZone(zone)?.toLocalDateTime(),
            choices = Defaults.LOCK_LENGTHS.filter { until == null || (now + it).isAfter(until) },
            confirming = confirming,
            confirmingUntil = confirming?.let { (now + it).atZone(zone).toLocalDateTime() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockUiState())

    fun onLengthChosen(length: Duration) {
        if (length in Defaults.LOCK_LENGTHS) confirming.value = length
    }

    fun onConfirm() {
        val length = confirming.value ?: return
        confirming.value = null
        viewModelScope.launch { settingsGuard.request(StartLock((timeSource.now() + length).toEpochMilli())) }
    }

    fun onDismiss() {
        confirming.value = null
    }
}
