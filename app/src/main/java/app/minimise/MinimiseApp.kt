package app.minimise

import android.app.Application
import app.minimise.apps.data.AutoVaultWatcher
import app.minimise.core.di.ApplicationScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.DebugClockOffset
import app.minimise.digest.domain.DigestScheduler
import app.minimise.stats.domain.WeeklyReportScheduler
import app.minimise.focus.domain.FocusSessions
import app.minimise.settings.domain.SettingsGuard
import app.minimise.swaps.domain.SwapRepository
import app.minimise.usage.domain.Taper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class MinimiseApp : Application() {
    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var debugClockOffset: DebugClockOffset
    @Inject lateinit var settingsGuard: SettingsGuard
    @Inject lateinit var swapRepository: SwapRepository
    @Inject lateinit var focusSessions: FocusSessions
    @Inject lateinit var digestScheduler: DigestScheduler
    @Inject lateinit var weeklyReportScheduler: WeeklyReportScheduler
    @Inject lateinit var autoVaultWatcher: AutoVaultWatcher
    @Inject lateinit var taper: Taper
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            followDebugClockOffset()
        }
        // Also runs on every screen unlock, from GateAccessibilityService.
        appScope.launch {
            settingsGuard.applyDue()
            taper.applyIfDue()
        }
        appScope.launch { swapRepository.seedDefaultsOnce() }
        appScope.launch { focusSessions.finishIfDue() }
        digestScheduler.scheduleNext()
        weeklyReportScheduler.scheduleNext()
        autoVaultWatcher.start()
    }

    private fun followDebugClockOffset() {
        appScope.launch {
            settingsStore.settings
                .map { it.debugTimeOffset }
                .distinctUntilChanged()
                .collect { debugClockOffset.offset = it }
        }
    }
}
