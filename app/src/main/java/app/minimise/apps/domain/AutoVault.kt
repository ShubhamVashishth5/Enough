package app.minimise.apps.domain

import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.core.time.plus
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SettingsGuard
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/** What the system says an app is. Only the categories that matter here. */
enum class AppCategory { SOCIAL, VIDEO, GAME, NEWS, OTHER }

/** Tells the user an app went into the Vault on its own. */
interface AutoVaultNotice {
    fun show(appLabel: String)
}

/**
 * A newly installed social, video, game or news app goes straight into the Vault. That's tightening,
 * so it applies at once, through SettingsGuard like any change.
 */
class AutoVault @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsGuard: SettingsGuard,
    private val settingsStore: SettingsStore,
    private val eventLog: EventLog,
    private val timeSource: TimeSource,
    private val notice: AutoVaultNotice,
) {
    suspend fun onInstalled(packageName: String, label: String, category: AppCategory) {
        if (!settingsStore.current().onboardingDone || !isDistracting(category)) return
        if (appRepository.kindOf(packageName) != AppKind.TOOL) return
        // A reinstalled app the user once chose a kind for keeps that choice.
        // The end is exclusive, so reach a little past now.
        val choseBefore = eventLog.between(Instant.EPOCH, timeSource.now() + 1.minutes, EventType.SETTING_CHANGED)
            .any { it.packageName == packageName }
        if (choseBefore) return
        if (settingsGuard.request(SetAppKind(packageName, AppKind.VAULT)) == GuardResult.AppliedNow) notice.show(label)
    }

    companion object {
        fun isDistracting(category: AppCategory): Boolean = category != AppCategory.OTHER
    }
}
