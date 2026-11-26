package app.minimise.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import app.minimise.launcher.domain.DefaultApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Finds the phone's own app for each [DefaultApp]: the default the user set, or else the first that fits. */
class ShortcutDefaults @Inject constructor(@ApplicationContext private val context: Context) {
    /** The package for each of [kinds] that this phone has an app for. */
    suspend fun resolve(kinds: Collection<DefaultApp>): Map<DefaultApp, String> = withContext(Dispatchers.IO) {
        kinds.toSet().mapNotNull { kind -> find(kind)?.let { kind to it } }.toMap()
    }

    private fun find(kind: DefaultApp): String? = runCatching {
        when (kind) {
            DefaultApp.DIALER -> context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
                ?: byIntent(Intent(Intent.ACTION_DIAL))
            DefaultApp.SMS -> Telephony.Sms.getDefaultSmsPackage(context)
                ?: byIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
            DefaultApp.CAMERA -> byIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            DefaultApp.BROWSER -> byIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE))
            DefaultApp.MUSIC -> byCategory(Intent.CATEGORY_APP_MUSIC)
            DefaultApp.MAPS -> byCategory(Intent.CATEGORY_APP_MAPS)
            DefaultApp.EMAIL -> byCategory(Intent.CATEGORY_APP_EMAIL)
            DefaultApp.CALENDAR -> byCategory(Intent.CATEGORY_APP_CALENDAR)
            DefaultApp.CONTACTS -> byCategory(Intent.CATEGORY_APP_CONTACTS)
            DefaultApp.GALLERY -> byCategory(Intent.CATEGORY_APP_GALLERY)
            DefaultApp.CALCULATOR -> byCategory(Intent.CATEGORY_APP_CALCULATOR)
            DefaultApp.FILES -> byCategory(Intent.CATEGORY_APP_FILES)
            DefaultApp.WEATHER -> byCategory(Intent.CATEGORY_APP_WEATHER)
            DefaultApp.FITNESS -> byCategory(Intent.CATEGORY_APP_FITNESS)
            DefaultApp.CLOCK -> byIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS))
            DefaultApp.SETTINGS -> byIntent(Intent(Settings.ACTION_SETTINGS))
        }
    }.onFailure { Timber.w(it, "No default for %s", kind) }.getOrNull()

    /** Apps that declare themselves as one kind, as the system's own app categories do. */
    private fun byCategory(category: String): String? = byIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category))

    /**
     * The app [intent] opens when the user has set a default; else the first that can handle it. Without
     * a default, Android resolves to its "Open with" chooser, which is none of the candidates.
     */
    private fun byIntent(intent: Intent): String? {
        val packages = context.packageManager
        val candidates = packages.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo.packageName }
        val chosen = packages.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return chosen?.takeIf { it in candidates } ?: candidates.firstOrNull()
    }
}
