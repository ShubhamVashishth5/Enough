package app.minimise.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.prefs.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Decides where the launcher starts: onboarding on first run, home after. */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    settingsStore: SettingsStore,
) : ViewModel() {
    /** Null until settings have loaded. */
    val onboardingDone: StateFlow<Boolean?> = settingsStore.settings
        .map { it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
