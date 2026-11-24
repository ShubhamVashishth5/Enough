package app.minimise.settings.permissions

import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionsUiState(
    val accessibility: Boolean = false,
    val defaultHome: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val notificationAccess: Boolean = false,
    val postNotifications: Boolean = false,
    val doNotDisturb: Boolean = false,
    val calendar: Boolean = false,
    val grayscale: Boolean = false,
    val grayscaleCommand: String = "",
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val status: PermissionStatus,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    /** Permissions change in system settings, so re-read them every time the screen comes back. */
    fun onResume() {
        _uiState.value = PermissionsUiState(
            accessibility = status.isAccessibilityEnabled(),
            defaultHome = status.isDefaultHome(),
            batteryUnrestricted = status.isIgnoringBatteryOptimizations(),
            notificationAccess = status.hasNotificationAccess(),
            postNotifications = status.canPostNotifications(),
            doNotDisturb = status.hasDoNotDisturbAccess(),
            calendar = status.canReadCalendar(),
            grayscale = status.canWriteSecureSettings(),
            grayscaleCommand = status.grayscaleGrantCommand(),
        )
    }

    fun accessibilityIntent(): Intent = status.accessibilitySettingsIntent()

    fun appInfoIntent(): Intent = status.appInfoIntent()

    fun homeRoleIntent(): Intent = status.homeRoleIntent()

    fun homeSettingsIntent(): Intent = status.homeSettingsIntent()

    fun batteryIntent(): Intent = status.batteryOptimizationIntent()

    fun doNotDisturbIntent(): Intent = status.doNotDisturbIntent()

    fun notificationAccessIntent(): Intent = status.notificationAccessIntent()

    fun appNotificationSettingsIntent(): Intent = status.appNotificationSettingsIntent()
}
