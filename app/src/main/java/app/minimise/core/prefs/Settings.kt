package app.minimise.core.prefs

import app.minimise.core.Defaults
import kotlin.time.Duration

/** Scalar settings kept in DataStore. Rule settings here change only through SettingsGuard. */
data class Settings(
    val dailyBudget: Duration = Defaults.DAILY_BUDGET,
    /** Set once the home slots have been filled for the first time, so they aren't refilled. */
    val homeSlotsChosen: Boolean = false,
    /** Debug builds only: shifts the wall clock, for testing day resets. */
    val debugTimeOffset: Duration = Duration.ZERO,
)
