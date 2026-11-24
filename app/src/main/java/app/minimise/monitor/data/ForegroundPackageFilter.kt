package app.minimise.monitor.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides what a foreground change means. Keyboards, system UI and popups that aren't activities are
 * ignored; the home screen and this app's own screens mean "no tracked app in front". Shared by both
 * watchers (the accessibility service and the usage access poller).
 */
@Singleton
class ForegroundPackageFilter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface Verdict {
        /** Not an app change (keyboard, notification shade, a popup window). */
        data object Ignore : Verdict

        /** Home or one of this app's screens. */
        data object NoApp : Verdict

        data class App(val packageName: String) : Verdict
    }

    private val activityCache = ConcurrentHashMap<String, Boolean>()

    @Volatile
    private var homePackages: Set<String> = queryHomePackages()

    @Volatile
    private var keyboardPackages: Set<String> = queryKeyboardPackages()

    /** A window change from the accessibility service: only an activity's window counts. */
    fun classify(packageName: String, className: String?): Verdict {
        val verdict = classifyResumed(packageName)
        if (verdict !is Verdict.App) return verdict
        if (className == null || !isActivity(packageName, className)) return Verdict.Ignore
        return verdict
    }

    /** An activity that came to the front, as usage access reports it. */
    fun classifyResumed(packageName: String): Verdict {
        if (packageName in IGNORED_PACKAGES || packageName in keyboardPackages) return Verdict.Ignore
        if (packageName == context.packageName || packageName in homePackages) return Verdict.NoApp
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
