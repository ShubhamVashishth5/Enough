package app.minimise.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.minimise.display.domain.GrayscaleMode
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
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
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: defaults.onboardingDone,
            homeSlotsChosen = this[Keys.HOME_SLOTS_CHOSEN] ?: defaults.homeSlotsChosen,
            todayPromptedDay = this[Keys.TODAY_PROMPTED_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
            swapsSeeded = this[Keys.SWAPS_SEEDED] ?: defaults.swapsSeeded,
            firstHourRule = this[Keys.FIRST_HOUR_RULE] ?: defaults.firstHourRule,
            manualModeId = this[Keys.MANUAL_MODE_ID],
            focusAllowlist = this[Keys.FOCUS_ALLOWLIST],
            digestEnabled = this[Keys.DIGEST_ENABLED] ?: defaults.digestEnabled,
            digestExempt = this[Keys.DIGEST_EXEMPT] ?: defaults.digestExempt,
            grayscaleMode = this[Keys.GRAYSCALE_MODE]?.let { name -> GrayscaleMode.entries.firstOrNull { it.name == name } }
                ?: defaults.grayscaleMode,
            blockedFeeds = this[Keys.BLOCKED_FEEDS] ?: defaults.blockedFeeds,
            lockedUntil = this[Keys.LOCKED_UNTIL_MILLIS]?.let(Instant::ofEpochMilli),
            savedColourCorrection = this[Keys.SAVED_COLOUR_CORRECTION]?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 },
            debugTimeOffset = this[Keys.DEBUG_TIME_OFFSET_SECONDS]?.seconds ?: defaults.debugTimeOffset,
            debugShortLoosenDelay = this[Keys.DEBUG_SHORT_LOOSEN_DELAY] ?: defaults.debugShortLoosenDelay,
        )
    }

    private fun MutablePreferences.write(settings: Settings) {
        this[Keys.DAILY_BUDGET_SECONDS] = settings.dailyBudget.inWholeSeconds
        this[Keys.ONBOARDING_DONE] = settings.onboardingDone
        this[Keys.HOME_SLOTS_CHOSEN] = settings.homeSlotsChosen
        settings.todayPromptedDay?.let { this[Keys.TODAY_PROMPTED_EPOCH_DAY] = it.toEpochDay() }
            ?: remove(Keys.TODAY_PROMPTED_EPOCH_DAY)
        this[Keys.SWAPS_SEEDED] = settings.swapsSeeded
        this[Keys.FIRST_HOUR_RULE] = settings.firstHourRule
        this[Keys.GRAYSCALE_MODE] = settings.grayscaleMode.name
        this[Keys.DIGEST_ENABLED] = settings.digestEnabled
        this[Keys.DIGEST_EXEMPT] = settings.digestExempt
        this[Keys.BLOCKED_FEEDS] = settings.blockedFeeds
        settings.lockedUntil?.let { this[Keys.LOCKED_UNTIL_MILLIS] = it.toEpochMilli() } ?: remove(Keys.LOCKED_UNTIL_MILLIS)
        settings.manualModeId?.let { this[Keys.MANUAL_MODE_ID] = it } ?: remove(Keys.MANUAL_MODE_ID)
        settings.focusAllowlist?.let { this[Keys.FOCUS_ALLOWLIST] = it } ?: remove(Keys.FOCUS_ALLOWLIST)
        settings.savedColourCorrection?.let { this[Keys.SAVED_COLOUR_CORRECTION] = it.joinToString(",") }
            ?: remove(Keys.SAVED_COLOUR_CORRECTION)
        this[Keys.DEBUG_TIME_OFFSET_SECONDS] = settings.debugTimeOffset.inWholeSeconds
        this[Keys.DEBUG_SHORT_LOOSEN_DELAY] = settings.debugShortLoosenDelay
    }

    private object Keys {
        val DAILY_BUDGET_SECONDS = longPreferencesKey("daily_budget_seconds")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val HOME_SLOTS_CHOSEN = booleanPreferencesKey("home_slots_chosen")
        val TODAY_PROMPTED_EPOCH_DAY = longPreferencesKey("today_prompted_epoch_day")
        val SWAPS_SEEDED = booleanPreferencesKey("swaps_seeded")
        val FIRST_HOUR_RULE = booleanPreferencesKey("first_hour_rule")
        val GRAYSCALE_MODE = stringPreferencesKey("grayscale_mode")
        val FOCUS_ALLOWLIST = stringSetPreferencesKey("focus_allowlist")
        val MANUAL_MODE_ID = longPreferencesKey("manual_mode_id")
        val DIGEST_ENABLED = booleanPreferencesKey("digest_enabled")
        val DIGEST_EXEMPT = stringSetPreferencesKey("digest_exempt")
        val BLOCKED_FEEDS = stringSetPreferencesKey("blocked_feeds")
        val LOCKED_UNTIL_MILLIS = longPreferencesKey("locked_until_millis")
        val SAVED_COLOUR_CORRECTION = stringPreferencesKey("saved_colour_correction")
        val DEBUG_TIME_OFFSET_SECONDS = longPreferencesKey("debug_time_offset_seconds")
        val DEBUG_SHORT_LOOSEN_DELAY = booleanPreferencesKey("debug_short_loosen_delay")
    }
}
