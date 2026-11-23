package app.minimise.apps.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Finds the phone's own Phone, Messages, Maps, Camera, Calendar and Notes apps for the first home screen. */
class DefaultHomeApps @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun packages(): List<String> {
        val byIntent = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
        ).mapNotNull(::defaultPackageFor)
        val notes = KNOWN_NOTES_APPS.firstOrNull(::isInstalled)
        return (byIntent + listOfNotNull(notes)).distinct()
    }

    private fun defaultPackageFor(intent: Intent): String? {
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val packageName = info?.activityInfo?.packageName ?: return null
        // "android" is the chooser, shown when there is no default.
        return packageName.takeIf { it != "android" }
    }

    private fun isInstalled(packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    private companion object {
        // There is no standard intent for a notes app, so try the common ones.
        val KNOWN_NOTES_APPS = listOf(
            "com.google.android.keep",
            "com.samsung.android.app.notes",
            "com.miui.notes",
            "com.oneplus.note",
            "com.coloros.note",
            "org.fossify.notes",
        )
    }
}
