package app.minimise

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import app.minimise.core.di.ApplicationScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.DebugClockOffset
import app.minimise.settings.domain.SettingsGuard
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
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            followDebugClockOffset()
        }
        appScope.launch { settingsGuard.applyDue() }
        applyDueChangesOnUnlock()
    }

    // TODO(question): moves into GateAccessibilityService with the SCREEN_UNLOCK event in step 1.5.
    private fun applyDueChangesOnUnlock() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                appScope.launch { settingsGuard.applyDue() }
            }
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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
