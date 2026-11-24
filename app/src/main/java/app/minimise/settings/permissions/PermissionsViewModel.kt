package app.minimise.settings.permissions

import android.content.Intent
import androidx.lifecycle.ViewModel
import app.minimise.protection.ProtectionStatus
import app.minimise.protection.domain.ProtectionLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionsUiState(
    val protection: ProtectionLevel = ProtectionLevel.LAUNCHER_ONLY,
    val accessibility: Boolean = false,
    /** Android's Advanced Protection is on and has taken accessibility away. */
    val advancedProtection: Boolean = false,
    val usageAccess: Boolean = false,
    val overlay: Boolean = false,
    val defaultHome: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val notificationAccess: Boolean = false,
    val postNotifications: Boolean = false,
    val doNotDisturb: Boolean = false,
    val calendar: Boolean = false,
    val grayscale: Boolean = false,
    val deviceAdmin: Boolean = false,
    val grayscaleCommand: String = "",
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val status: PermissionStatus,
    private val protectionStatus: ProtectionStatus,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    /** Permissions change in system settings, so re-read them every time the screen comes back. */
    fun onResume() {
        protectionStatus.refresh()
        val accessibility = status.isAccessibilityEnabled()
        _uiState.value = PermissionsUiState(
            protection = protectionStatus.current(),
            accessibility = accessibility,
            advancedProtection = !accessibility && status.isAdvancedProtectionOn(),
            usageAccess = status.hasUsageAccess(),
            overlay = status.canDrawOverlays(),
            defaultHome = status.isDefaultHome(),
            batteryUnrestricted = status.isIgnoringBatteryOptimizations(),
            notificationAccess = status.hasNotificationAccess(),
            postNotifications = status.canPostNotifications(),
            doNotDisturb = status.hasDoNotDisturbAccess(),
            calendar = status.canReadCalendar(),
            grayscale = status.canWriteSecureSettings(),
            deviceAdmin = status.isDeviceAdmin(),
            grayscaleCommand = status.grayscaleGrantCommand(),
        )
    }

    fun accessibilityIntent(): Intent = status.accessibilitySettingsIntent()

    fun appInfoIntent(): Intent = status.appInfoIntent()

    fun usageAccessIntent(): Intent = status.usageAccessIntent()

    fun overlayIntent(): Intent = status.overlayIntent()

    fun homeRoleIntent(): Intent = status.homeRoleIntent()

    fun homeSettingsIntent(): Intent = status.homeSettingsIntent()

    fun batteryIntent(): Intent = status.batteryOptimizationIntent()

    fun doNotDisturbIntent(): Intent = status.doNotDisturbIntent()

    fun notificationAccessIntent(): Intent = status.notificationAccessIntent()

    fun appNotificationSettingsIntent(): Intent = status.appNotificationSettingsIntent()

    fun deviceAdminIntent(): Intent = status.deviceAdminIntent()
}
