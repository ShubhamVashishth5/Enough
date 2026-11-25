package app.minimise.core.prefs

import app.minimise.core.Defaults
import app.minimise.display.domain.GrayscaleMode
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration

/** Scalar settings kept in DataStore. Rule settings here change only through SettingsGuard. */
data class Settings(
    val dailyBudget: Duration = Defaults.DAILY_BUDGET,
    /** First-run setup has been completed. */
    val onboardingDone: Boolean = false,
    /** Set once the home slots have been filled for the first time, so they aren't refilled. */
    val homeSlotsChosen: Boolean = false,
    /** The usage day the Today screen last opened by itself, so it does so once a day. */
    val todayPromptedDay: LocalDate? = null,
    /** No Vault apps until [app.minimise.core.Defaults.FIRST_HOUR] after the day's first unlock. */
    val firstHourRule: Boolean = false,
    /** The mode picked by hand; null follows mode schedules. */
    val manualModeId: Long? = null,
    /** When the hand-picked mode hands back to the schedules; null for a pick that lasts until changed. */
    val manualModeUntil: Instant? = null,
    /** Apps that open during focus sessions; null means the Tool apps on the home screen. */
    val focusAllowlist: Set<String>? = null,
    /** Hold notifications for the digest. Off until the user turns it on. */
    val digestEnabled: Boolean = false,
    /** Apps whose notifications always come through live (the VIP list). */
    val digestExempt: Set<String> = emptySet(),
    val grayscaleMode: GrayscaleMode = GrayscaleMode.OFF,
    /** Feed rules (assets/feed_rules.json ids) that are switched on. */
    val blockedFeeds: Set<String> = emptySet(),
    /** The user's colour correction (enabled, mode) from before grayscale first went on; null when not saved. */
    val savedColourCorrection: List<Int>? = null,
    /** Taper's floor in minutes; null when taper is off. */
    val taperFloorMinutes: Int? = null,
    /** The usage week taper last ran in (or was switched on in). */
    val taperWeek: LocalDate? = null,
    /** Finishing tasks and focus sessions earns Vault minutes. Off by default. */
    val earnIt: Boolean = false,
    /** The day the auto-tighten offer was last answered, so it isn't repeated for the same run of days. */
    val tightenAnsweredDay: LocalDate? = null,
    /** Calendar events with "focus" in the title start focus sessions. */
    val calendarFocus: Boolean = false,
    /** The calendar event a session was last started for, so it isn't started twice. */
    val calendarFocusStartedKey: String? = null,
    /** While a lock period runs, nothing can be loosened; null when there never was one. */
    val lockedUntil: Instant? = null,
    /** Apps Enough put behind the system app lock, so it only ever takes off its own locks. */
    val appsLockedByEnough: Set<String> = emptySet(),
    /** Set once the Instead list has been filled with the built-in swaps, so deleting them sticks. */
    val swapsSeeded: Boolean = false,
    /** What the home screen shows and which gestures it answers. Not rules: changes apply at once. */
    val home: HomeOptions = HomeOptions(),
    /** Debug builds only: shifts the wall clock, for testing day resets. */
    val debugTimeOffset: Duration = Duration.ZERO,
    /** Debug builds only: loosening waits 1 minute instead of 24 hours. */
    val debugShortLoosenDelay: Boolean = false,
)

/**
 * The home screen's optional parts, each of which can be switched off. Weather and air quality are the
 * only things in the app that use the internet, so they start off.
 */
data class HomeOptions(
    /** A lock icon by the clock. */
    val lockButton: Boolean = true,
    /** Swiping down on home opens the notification shade. */
    val swipeDownShade: Boolean = true,
    /** The status bar is hidden while home is showing; a swipe from the top edge brings it back for a moment. */
    val hideStatusBar: Boolean = false,
    val battery: Boolean = true,
    /** The next alarm, when one is set. */
    val nextAlarm: Boolean = true,
    /** What's playing, with previous, play/pause and next. Needs notification access. */
    val music: Boolean = true,
    /** Temperature and sky from Open-Meteo, for the phone's rough location. */
    val weather: Boolean = false,
    /** US AQI from Open-Meteo, for the phone's rough location. */
    val airQuality: Boolean = false,
    val temperatureUnit: TemperatureChoice = TemperatureChoice.AUTOMATIC,
) {
    /** Both widgets that need the phone's location and the internet. */
    val usesLocation: Boolean get() = weather || airQuality
}

/** Stored by name, so never rename an entry. */
enum class TemperatureChoice {
    /** The phone's regional preference: Fahrenheit where that's the custom (or the phone says so), Celsius elsewhere. */
    AUTOMATIC,
    CELSIUS,
    FAHRENHEIT,
}
