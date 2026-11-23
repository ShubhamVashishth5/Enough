package app.minimise

import android.app.Application
import app.minimise.core.di.ApplicationScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.DebugClockOffset
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
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            followDebugClockOffset()
        }
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
