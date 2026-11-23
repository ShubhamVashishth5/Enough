package app.minimise.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsStore {
    private val dataStore = context.dataStore

    override val settings: Flow<Settings> = dataStore.data
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            dailyBudget = this[Keys.DAILY_BUDGET_SECONDS]?.seconds ?: defaults.dailyBudget,
            homeSlotsChosen = this[Keys.HOME_SLOTS_CHOSEN] ?: defaults.homeSlotsChosen,
            debugTimeOffset = this[Keys.DEBUG_TIME_OFFSET_SECONDS]?.seconds ?: defaults.debugTimeOffset,
        )
    }

    private fun MutablePreferences.write(settings: Settings) {
        this[Keys.DAILY_BUDGET_SECONDS] = settings.dailyBudget.inWholeSeconds
        this[Keys.HOME_SLOTS_CHOSEN] = settings.homeSlotsChosen
        this[Keys.DEBUG_TIME_OFFSET_SECONDS] = settings.debugTimeOffset.inWholeSeconds
    }

    private object Keys {
        val DAILY_BUDGET_SECONDS = longPreferencesKey("daily_budget_seconds")
        val HOME_SLOTS_CHOSEN = booleanPreferencesKey("home_slots_chosen")
        val DEBUG_TIME_OFFSET_SECONDS = longPreferencesKey("debug_time_offset_seconds")
    }
}
