package app.minimise.settings.permissions

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import app.minimise.launcher.HomeRole
import app.minimise.monitor.GateAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** One place to ask whether each permission the app depends on is granted, and where to grant it. */
class PermissionStatus @Inject constructor(
    @ApplicationContext private val context: Context,
    private val homeRole: HomeRole,
) {
    fun isAccessibilityEnabled(): Boolean {
        val ours = ComponentName(context, GateAccessibilityService::class.java)
        return context.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.let { info -> ComponentName(info.packageName, info.name) == ours } }
    }

    fun isDefaultHome(): Boolean = homeRole.isDefaultHome()

    fun isIgnoringBatteryOptimizations(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun canWriteSecureSettings(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** Run once from a computer with USB debugging on. */
    fun grayscaleGrantCommand(): String = "adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    /** App info, where sideloaded installs find "Allow restricted settings" in the ⋮ menu. */
    fun appInfoIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun homeRoleIntent(): Intent = homeRole.requestIntent()

    fun homeSettingsIntent(): Intent = Intent(Settings.ACTION_HOME_SETTINGS)

    fun batteryOptimizationIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.fromParts("package", context.packageName, null))
}
