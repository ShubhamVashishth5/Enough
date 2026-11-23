package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.BuildConfig
import app.minimise.core.prefs.SettingsStore
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val pendingCount: Int = 0,
    /** Debug builds only. */
    val showDebug: Boolean = false,
    val debugShortLoosenDelay: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsGuard: SettingsGuard,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(settingsGuard.pending, settingsStore.settings) { pending, settings ->
        SettingsUiState(
            pendingCount = pending.size,
            showDebug = BuildConfig.DEBUG,
            debugShortLoosenDelay = settings.debugShortLoosenDelay,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onDebugShortLoosenDelayToggled() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch { settingsStore.update { it.copy(debugShortLoosenDelay = !it.debugShortLoosenDelay) } }
    }
}
