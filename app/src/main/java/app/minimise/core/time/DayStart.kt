package app.minimise.core.time

import android.content.Context
import app.minimise.core.Defaults
import app.minimise.core.di.ApplicationScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/** The local time a new day starts at, read synchronously wherever a day is worked out. */
fun interface DayStartSource {
    fun dayStart(): LocalTime
}

/** A day start that never moves. For tests, and for anything built without settings. */
class FixedDayStart(private val at: LocalTime = Defaults.DAY_RESET) : DayStartSource {
    override fun dayStart(): LocalTime = at
}

/**
 * The day start the user chose. Kept in memory for the life of the process, since days are worked out
 * everywhere and synchronously; only a read in the first moments after the process starts, before
 * settings are loaded, waits for them.
 */
@Singleton
class SettingsDayStart @Inject constructor(
    private val settingsStore: SettingsStore,
    @ApplicationScope scope: CoroutineScope,
) : DayStartSource {
    private val state: StateFlow<LocalTime?> = settingsStore.settings
        .map { it.dayStart }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** The day start now and whenever it changes. */
    val changes: Flow<LocalTime> = state.filterNotNull()

    // DataStore reads on its own thread, so this can't deadlock even on the main thread.
    override fun dayStart(): LocalTime = state.value ?: runBlocking { settingsStore.current().dayStart }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Access {
        fun dayStart(): SettingsDayStart
    }

    companion object {
        /** For screens outside the usual view models, such as the ones shown over other apps. */
        fun from(context: Context): SettingsDayStart =
            EntryPointAccessors.fromApplication(context.applicationContext, Access::class.java).dayStart()
    }
}
