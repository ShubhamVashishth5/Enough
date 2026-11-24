package app.minimise.settings.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.prefs.HomeOptions
import app.minimise.core.prefs.SettingsStore
import app.minimise.settings.permissions.PermissionStatus
import app.minimise.widgets.data.DeviceLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the home screen's options need that may be missing. */
data class HomeGrants(
    /** Locking the screen and, preferably, the notification shade. */
    val accessibility: Boolean = true,
    /** Music: Android lists media sessions only to an app with notification access. */
    val notificationAccess: Boolean = true,
    /** Weather and air quality. */
    val location: Boolean = true,
)

/** Null [options] until settings load. */
data class HomeSettingsUiState(val options: HomeOptions? = null, val grants: HomeGrants = HomeGrants())

/**
 * The home screen's options. None of them is a rule, so they change at once (they don't go through
 * SettingsGuard, and a lock period doesn't hold them).
 */
@HiltViewModel
class HomeSettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val permissionStatus: PermissionStatus,
    private val deviceLocation: DeviceLocation,
) : ViewModel() {
    private val grants = MutableStateFlow(HomeGrants())

    val uiState: StateFlow<HomeSettingsUiState> = combine(settingsStore.settings, grants) { settings, grants ->
        HomeSettingsUiState(options = settings.home, grants = grants)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeSettingsUiState())

    /** Permissions may have changed in system settings meanwhile. */
    fun onResume() {
        grants.value = HomeGrants(
            accessibility = permissionStatus.isAccessibilityEnabled(),
            notificationAccess = permissionStatus.hasNotificationAccess(),
            location = deviceLocation.hasPermission(),
        )
    }

    fun onToggle(change: (HomeOptions) -> HomeOptions) {
        viewModelScope.launch { settingsStore.update { it.copy(home = change(it.home)) } }
    }

    fun notificationAccessIntent(): Intent = permissionStatus.notificationAccessIntent()
}
