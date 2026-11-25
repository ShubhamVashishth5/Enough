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
            manualModeUntil = this[Keys.MANUAL_MODE_UNTIL_MILLIS]?.let(Instant::ofEpochMilli),
            focusAllowlist = this[Keys.FOCUS_ALLOWLIST],
            digestEnabled = this[Keys.DIGEST_ENABLED] ?: defaults.digestEnabled,
            digestExempt = this[Keys.DIGEST_EXEMPT] ?: defaults.digestExempt,
            grayscaleMode = this[Keys.GRAYSCALE_MODE]?.let { name -> GrayscaleMode.entries.firstOrNull { it.name == name } }
                ?: defaults.grayscaleMode,
            blockedFeeds = this[Keys.BLOCKED_FEEDS] ?: defaults.blockedFeeds,
            appsLockedByEnough = this[Keys.APPS_LOCKED_BY_ENOUGH] ?: defaults.appsLockedByEnough,
            lockedUntil = this[Keys.LOCKED_UNTIL_MILLIS]?.let(Instant::ofEpochMilli),
            taperFloorMinutes = this[Keys.TAPER_FLOOR_MINUTES]?.toInt(),
            taperWeek = this[Keys.TAPER_WEEK_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
            earnIt = this[Keys.EARN_IT] ?: defaults.earnIt,
            calendarFocus = this[Keys.CALENDAR_FOCUS] ?: defaults.calendarFocus,
            calendarFocusStartedKey = this[Keys.CALENDAR_FOCUS_STARTED],
            tightenAnsweredDay = this[Keys.TIGHTEN_ANSWERED_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
            savedColourCorrection = this[Keys.SAVED_COLOUR_CORRECTION]?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 },
            home = HomeOptions(
                lockButton = this[Keys.HOME_LOCK_BUTTON] ?: defaults.home.lockButton,
                swipeDownShade = this[Keys.HOME_SWIPE_DOWN_SHADE] ?: defaults.home.swipeDownShade,
                hideStatusBar = this[Keys.HOME_HIDE_STATUS_BAR] ?: defaults.home.hideStatusBar,
                battery = this[Keys.HOME_BATTERY] ?: defaults.home.battery,
                nextAlarm = this[Keys.HOME_NEXT_ALARM] ?: defaults.home.nextAlarm,
                music = this[Keys.HOME_MUSIC] ?: defaults.home.music,
                weather = this[Keys.HOME_WEATHER] ?: defaults.home.weather,
                airQuality = this[Keys.HOME_AIR_QUALITY] ?: defaults.home.airQuality,
                temperatureUnit = this[Keys.HOME_TEMPERATURE_UNIT]?.let { name -> TemperatureChoice.entries.firstOrNull { it.name == name } }
                    ?: defaults.home.temperatureUnit,
            ),
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
        this[Keys.APPS_LOCKED_BY_ENOUGH] = settings.appsLockedByEnough
        settings.lockedUntil?.let { this[Keys.LOCKED_UNTIL_MILLIS] = it.toEpochMilli() } ?: remove(Keys.LOCKED_UNTIL_MILLIS)
        settings.taperFloorMinutes?.let { this[Keys.TAPER_FLOOR_MINUTES] = it.toLong() } ?: remove(Keys.TAPER_FLOOR_MINUTES)
        settings.taperWeek?.let { this[Keys.TAPER_WEEK_EPOCH_DAY] = it.toEpochDay() } ?: remove(Keys.TAPER_WEEK_EPOCH_DAY)
        this[Keys.EARN_IT] = settings.earnIt
        this[Keys.CALENDAR_FOCUS] = settings.calendarFocus
        settings.calendarFocusStartedKey?.let { this[Keys.CALENDAR_FOCUS_STARTED] = it } ?: remove(Keys.CALENDAR_FOCUS_STARTED)
        settings.tightenAnsweredDay?.let { this[Keys.TIGHTEN_ANSWERED_EPOCH_DAY] = it.toEpochDay() } ?: remove(Keys.TIGHTEN_ANSWERED_EPOCH_DAY)
        settings.manualModeId?.let { this[Keys.MANUAL_MODE_ID] = it } ?: remove(Keys.MANUAL_MODE_ID)
        settings.manualModeUntil?.let { this[Keys.MANUAL_MODE_UNTIL_MILLIS] = it.toEpochMilli() } ?: remove(Keys.MANUAL_MODE_UNTIL_MILLIS)
        settings.focusAllowlist?.let { this[Keys.FOCUS_ALLOWLIST] = it } ?: remove(Keys.FOCUS_ALLOWLIST)
        settings.savedColourCorrection?.let { this[Keys.SAVED_COLOUR_CORRECTION] = it.joinToString(",") }
            ?: remove(Keys.SAVED_COLOUR_CORRECTION)
        this[Keys.HOME_LOCK_BUTTON] = settings.home.lockButton
        this[Keys.HOME_SWIPE_DOWN_SHADE] = settings.home.swipeDownShade
        this[Keys.HOME_HIDE_STATUS_BAR] = settings.home.hideStatusBar
        this[Keys.HOME_BATTERY] = settings.home.battery
        this[Keys.HOME_NEXT_ALARM] = settings.home.nextAlarm
        this[Keys.HOME_MUSIC] = settings.home.music
        this[Keys.HOME_WEATHER] = settings.home.weather
        this[Keys.HOME_AIR_QUALITY] = settings.home.airQuality
        this[Keys.HOME_TEMPERATURE_UNIT] = settings.home.temperatureUnit.name
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
        val MANUAL_MODE_UNTIL_MILLIS = longPreferencesKey("manual_mode_until_millis")
        val DIGEST_ENABLED = booleanPreferencesKey("digest_enabled")
        val DIGEST_EXEMPT = stringSetPreferencesKey("digest_exempt")
        val BLOCKED_FEEDS = stringSetPreferencesKey("blocked_feeds")
        val APPS_LOCKED_BY_ENOUGH = stringSetPreferencesKey("apps_locked_by_enough")
        val LOCKED_UNTIL_MILLIS = longPreferencesKey("locked_until_millis")
        val TAPER_FLOOR_MINUTES = longPreferencesKey("taper_floor_minutes")
        val TAPER_WEEK_EPOCH_DAY = longPreferencesKey("taper_week_epoch_day")
        val EARN_IT = booleanPreferencesKey("earn_it")
        val CALENDAR_FOCUS = booleanPreferencesKey("calendar_focus")
        val CALENDAR_FOCUS_STARTED = stringPreferencesKey("calendar_focus_started")
        val TIGHTEN_ANSWERED_EPOCH_DAY = longPreferencesKey("tighten_answered_epoch_day")
        val SAVED_COLOUR_CORRECTION = stringPreferencesKey("saved_colour_correction")
        val HOME_LOCK_BUTTON = booleanPreferencesKey("home_lock_button")
        val HOME_SWIPE_DOWN_SHADE = booleanPreferencesKey("home_swipe_down_shade")
        val HOME_HIDE_STATUS_BAR = booleanPreferencesKey("home_hide_status_bar")
        val HOME_BATTERY = booleanPreferencesKey("home_battery")
        val HOME_NEXT_ALARM = booleanPreferencesKey("home_next_alarm")
        val HOME_MUSIC = booleanPreferencesKey("home_music")
        val HOME_WEATHER = booleanPreferencesKey("home_weather")
        val HOME_AIR_QUALITY = booleanPreferencesKey("home_air_quality")
        val HOME_TEMPERATURE_UNIT = stringPreferencesKey("home_temperature_unit")
        val DEBUG_TIME_OFFSET_SECONDS = longPreferencesKey("debug_time_offset_seconds")
        val DEBUG_SHORT_LOOSEN_DELAY = booleanPreferencesKey("debug_short_loosen_delay")
    }
}
