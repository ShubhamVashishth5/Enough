package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.BuildConfig
import app.minimise.core.prefs.SettingsStore
import app.minimise.display.GrayscaleController
import app.minimise.display.domain.DisplayFilter
import app.minimise.display.domain.GrayscaleMode
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.monitor.AccessibilityActions
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import app.minimise.core.time.TimeSource
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val budgetMinutes: Int? = null,
    val activeModeName: String? = null,
    val pendingCount: Int = 0,
    val grayscaleMode: GrayscaleMode = GrayscaleMode.OFF,
    /** False until the one-time ADB grant is made. */
    val grayscaleAvailable: Boolean = false,
    /** Debug builds only. */
    val showDebug: Boolean = false,
    val debugShortLoosenDelay: Boolean = false,
    /** The end of the lock period in force, if any. */
    val lockedUntil: LocalDateTime? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsGuard: SettingsGuard,
    private val settingsStore: SettingsStore,
    private val displayFilter: Provider<DisplayFilter>,
    private val activeMode: ActiveModeProvider,
    private val accessibilityActions: AccessibilityActions,
    private val timeSource: TimeSource,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(settingsGuard.pending, settingsStore.settings) { pending, settings ->
        SettingsUiState(
            budgetMinutes = settings.dailyBudget.inWholeMinutes.toInt(),
            activeModeName = activeMode.active()?.name,
            pendingCount = pending.size,
            grayscaleMode = settings.grayscaleMode,
            grayscaleAvailable = displayFilter.get().isAvailable(),
            showDebug = BuildConfig.DEBUG,
            debugShortLoosenDelay = settings.debugShortLoosenDelay,
            lockedUntil = settings.lockedUntil?.takeIf { it.isAfter(timeSource.now()) }?.atZone(timeSource.zone())?.toLocalDateTime(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Cycles Off → Vault apps → Always. Applies at once here too, in case the accessibility service is off. */
    fun onGrayscaleTapped() {
        val next = GrayscaleMode.entries[(uiState.value.grayscaleMode.ordinal + 1) % GrayscaleMode.entries.size]
        viewModelScope.launch {
            settingsStore.update { it.copy(grayscaleMode = next) }
            displayFilter.get().setGrayscale(GrayscaleController.wanted(next, inVaultApp = false))
        }
    }

    fun onDebugShortLoosenDelayToggled() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch { settingsStore.update { it.copy(debugShortLoosenDelay = !it.debugShortLoosenDelay) } }
    }

    fun onDebugDumpScreen() {
        accessibilityActions.dumpScreenSoon()
    }
}
