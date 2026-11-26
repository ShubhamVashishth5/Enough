package app.minimise.core.ui.theme

import android.content.Context
import app.minimise.core.di.ApplicationScope
import app.minimise.core.prefs.Appearance
import app.minimise.core.prefs.SettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The look, kept for the life of the process so that every screen after the first (the pause screen
 * above all, which opens over another app) starts in the right font at once. Null only until settings
 * are first read.
 */
@Singleton
class AppearanceSource @Inject constructor(
    settingsStore: SettingsStore,
    @ApplicationScope scope: CoroutineScope,
) {
    val appearance: StateFlow<Appearance?> = settingsStore.settings
        .map { it.appearance }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Access {
        fun appearanceSource(): AppearanceSource
    }

    companion object {
        fun from(context: Context): AppearanceSource =
            EntryPointAccessors.fromApplication(context.applicationContext, Access::class.java).appearanceSource()
    }
}
