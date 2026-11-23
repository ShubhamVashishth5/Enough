package app.minimise.core.prefs

import app.minimise.core.Defaults
import kotlin.time.Duration

/** Scalar settings kept in DataStore. Rule settings here change only through SettingsGuard. */
data class Settings(
    val dailyBudget: Duration = Defaults.DAILY_BUDGET,
    /** Debug builds only: shifts the wall clock, for testing day resets. */
    val debugTimeOffset: Duration = Duration.ZERO,
)
