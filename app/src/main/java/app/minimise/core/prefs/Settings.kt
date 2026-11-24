package app.minimise.core.prefs

import app.minimise.core.Defaults
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
    /** Set once the Instead list has been filled with the built-in swaps, so deleting them sticks. */
    val swapsSeeded: Boolean = false,
    /** Debug builds only: shifts the wall clock, for testing day resets. */
    val debugTimeOffset: Duration = Duration.ZERO,
    /** Debug builds only: loosening waits 1 minute instead of 24 hours. */
    val debugShortLoosenDelay: Boolean = false,
)
