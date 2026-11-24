package app.minimise.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.TimeSource
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Decides where the launcher starts: onboarding on first run, home after. */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    settingsStore: SettingsStore,
    settingsGuard: SettingsGuard,
    timeSource: TimeSource,
) : ViewModel() {
    /** When a lock period refuses a change: the lock's end. */
    val refusals: Flow<LocalDateTime> = settingsGuard.refusals.mapNotNull { it.until?.atZone(timeSource.zone())?.toLocalDateTime() }

    /** Null until settings have loaded. */
    val onboardingDone: StateFlow<Boolean?> = settingsStore.settings
        .map { it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
