package app.minimise.settings.permissions

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.os.Process
import android.security.advancedprotection.AdvancedProtectionManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import app.minimise.R
import app.minimise.guard.GuardAdminReceiver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
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

    /** Lite protection: lets the app see which app came to the front without accessibility. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, null)
        } else {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** Lite protection: lets the pause screen open over another app from the background. */
    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun overlayIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null))

    /**
     * Android's Advanced Protection mode (Android 16+). On Android 17 it takes accessibility away from
     * apps that aren't accessibility tools, this one included.
     */
    fun isAdvancedProtectionOn(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        return runCatching { context.getSystemService(AdvancedProtectionManager::class.java)?.isAdvancedProtectionEnabled == true }
            .getOrDefault(false)
    }

    fun isIgnoringBatteryOptimizations(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun hasNotificationAccess(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun notificationAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun canPostNotifications(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun appNotificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun canReadCalendar(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun hasDoNotDisturbAccess(): Boolean = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun doNotDisturbIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun canWriteSecureSettings(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** Run once from a computer with USB debugging on. */
    fun grayscaleGrantCommand(): String = "adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    fun isDeviceAdmin(): Boolean =
        context.getSystemService(DevicePolicyManager::class.java).isAdminActive(GuardAdminReceiver.component(context))

    /** The system screen that turns on the uninstall guard. */
    fun deviceAdminIntent(): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, GuardAdminReceiver.component(context))
        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.admin_explanation))

    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    /** App info, where sideloaded installs find "Allow restricted settings" in the ⋮ menu. */
    fun appInfoIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun homeRoleIntent(): Intent = homeRole.requestIntent()

    fun homeSettingsIntent(): Intent = Intent(Settings.ACTION_HOME_SETTINGS)

    fun batteryOptimizationIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.fromParts("package", context.packageName, null))
}
