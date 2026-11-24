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
    /** While a lock period runs, nothing can be loosened; null when there never was one. */
    val lockedUntil: Instant? = null,
    /** Set once the Instead list has been filled with the built-in swaps, so deleting them sticks. */
    val swapsSeeded: Boolean = false,
    /** Debug builds only: shifts the wall clock, for testing day resets. */
    val debugTimeOffset: Duration = Duration.ZERO,
    /** Debug builds only: loosening waits 1 minute instead of 24 hours. */
    val debugShortLoosenDelay: Boolean = false,
)
