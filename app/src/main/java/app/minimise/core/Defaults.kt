package app.minimise.core

import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Every tunable number in the app lives here. */
object Defaults {
    /** Usage days start at this local time, not midnight. */
    val DAY_RESET: LocalTime = LocalTime.of(4, 0)

    /** Shared daily allowance across all Vault apps. */
    val DAILY_BUDGET: Duration = 45.minutes

    /** Pause length for the 1st, 2nd, 3rd... Vault open of the day. The last step repeats. */
    val WAIT_STEPS: List<Duration> = listOf(10.seconds, 20.seconds, 40.seconds, 60.seconds, 120.seconds)

    /** Rows of apps on the home screen. */
    const val HOME_SLOT_COUNT: Int = 6

    /** How long a loosening settings change waits before it applies. */
    val LOOSEN_DELAY: Duration = 24.hours

    /** Debug builds only: the loosen delay when the hidden debug switch is on. */
    val DEBUG_LOOSEN_DELAY: Duration = 1.minutes

    /** Tasks asked for on the Today screen. */
    const val MAX_TODAY_TASKS: Int = 3

    /** Choices offered after a pause. */
    val TIME_BOX_OPTIONS: List<Duration> = listOf(2.minutes, 5.minutes, 10.minutes)

    /** Emergency passes per usage week (Monday 04:00 to Monday 04:00). Each skips one block. */
    const val EMERGENCY_PASSES_PER_WEEK: Int = 3

    /** With the first-hour rule on, Vault apps stay blocked this long after the day's first unlock. */
    val FIRST_HOUR: Duration = 60.minutes

    /** Focus session lengths offered on home. */
    val FOCUS_LENGTHS: List<Duration> = listOf(25.minutes, 50.minutes, 90.minutes)

    /** The wait that gets past an unlocked focus session for one app. */
    val FOCUS_WAIT: Duration = 120.seconds

    /** How long home shows "Focus done" after a session ends. */
    val FOCUS_SUMMARY_SHOWN_FOR: Duration = 30.minutes
}
