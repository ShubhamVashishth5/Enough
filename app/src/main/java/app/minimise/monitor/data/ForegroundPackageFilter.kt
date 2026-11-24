package app.minimise.monitor.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager

/**
 * Decides what a window-state event means. Keyboards, system UI and popups that aren't activities are
 * ignored; the home screen and this app's own screens mean "no tracked app in front".
 */
class ForegroundPackageFilter(private val context: Context) {
    sealed interface Verdict {
        /** Not an app change (keyboard, notification shade, a popup window). */
        data object Ignore : Verdict

        /** Home or one of this app's screens. */
        data object NoApp : Verdict

        data class App(val packageName: String) : Verdict
    }

    private val activityCache = HashMap<String, Boolean>()
    private var homePackages: Set<String> = queryHomePackages()
    private var keyboardPackages: Set<String> = queryKeyboardPackages()

    fun classify(packageName: String, className: String?): Verdict {
        if (packageName in IGNORED_PACKAGES || packageName in keyboardPackages) return Verdict.Ignore
        if (packageName == context.packageName || packageName in homePackages) return Verdict.NoApp
        if (className == null || !isActivity(packageName, className)) return Verdict.Ignore
        return Verdict.App(packageName)
    }

    /** Call when packages or keyboards may have changed. */
    fun refresh() {
        activityCache.clear()
        homePackages = queryHomePackages()
        keyboardPackages = queryKeyboardPackages()
    }

    private fun isActivity(packageName: String, className: String): Boolean =
        activityCache.getOrPut("$packageName/$className") {
            runCatching {
                context.packageManager.getActivityInfo(ComponentName(packageName, className), PackageManager.MATCH_ALL)
            }.isSuccess
        }

    private fun queryHomePackages(): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.queryIntentActivities(home, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    private fun queryKeyboardPackages(): Set<String> =
        context.getSystemService(InputMethodManager::class.java).enabledInputMethodList
            .map { it.packageName }
            .toSet()

    private companion object {
        val IGNORED_PACKAGES = setOf("com.android.systemui", "android")
    }
}
