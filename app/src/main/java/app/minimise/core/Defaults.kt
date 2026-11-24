package app.minimise.core

import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class ModePreset(val key: String, val budget: Duration, val waitSteps: List<Duration>)

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

    /** When held notifications come out together. */
    val DIGEST_TIMES: List<LocalTime> = listOf(LocalTime.of(9, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))

    /** How late a digest may come, so the system can batch the alarm. */
    val DIGEST_WINDOW: Duration = 10.minutes

    /** Reading another app's screen: how deep and how many nodes at most, so a huge screen can't stall the service. */
    const val SCREEN_MAX_DEPTH: Int = 60
    const val SCREEN_MAX_NODES: Int = 3000

    /** At most one screen read per app this often, however fast its content changes. */
    val SCREEN_CHECK_INTERVAL: Duration = 300.milliseconds

    /** After stepping out of a blocked feed, give the app this long to leave it before acting again. */
    val FEED_ACTION_COOLDOWN: Duration = 1500.milliseconds

    /** Lock period lengths on offer. */
    val LOCK_LENGTHS: List<Duration> = listOf(7.days, 30.days, 90.days)

    /** Friend key: one-time codes made at a time. */
    const val FRIEND_CODE_COUNT: Int = 10

    /** Typing penance: passage length, and the most characters one keystroke may add (so no paste or swipe). */
    const val TYPING_PASSAGE_LENGTH: Int = 300
    const val TYPING_MAX_INSERT: Int = 2

    /** A wall-clock jump bigger than this, against time since boot, is ignored as tampering. */
    val CLOCK_TAMPER_TOLERANCE: Duration = 5.minutes

    /** How often the trusted clock reading is written to disk. */
    val CLOCK_ANCHOR_SAVE_INTERVAL: Duration = 1.minutes

    /** The weekly report's notification: Sunday evening. The system may delay it by up to the window. */
    val WEEKLY_REPORT_DAY: DayOfWeek = DayOfWeek.SUNDAY
    val WEEKLY_REPORT_TIME: LocalTime = LocalTime.of(20, 0)
    val WEEKLY_REPORT_WINDOW: Duration = 10.minutes

    /** Taper: the budget drops this much each week, down to the floor the user sets. */
    const val TAPER_PERCENT: Int = 5

    /** Auto-tighten: after this many days in a row over budget, offer a budget this much smaller. */
    const val AUTO_TIGHTEN_DAYS: Int = 3
    const val AUTO_TIGHTEN_PERCENT: Int = 10

    /** Earn-it: minutes for a finished task or a full focus session, and the most a day can earn. */
    const val EARN_IT_TASK_MINUTES: Int = 5
    const val EARN_IT_FOCUS_MINUTES: Int = 10
    const val EARN_IT_DAILY_CAP: Int = 30

    /** How far back earn-it looks for a task or session that already earned. */
    const val EARN_IT_LOOKBACK_DAYS: Long = 7

    /** Wind-down starts this long before bedtime. */
    val WIND_DOWN_LEAD: Duration = 30.minutes

    /** A calendar event with this in its title starts a focus session. */
    const val CALENDAR_FOCUS_WORD: String = "focus"

    /** Calendar focus looks this far ahead for the next event, and re-arms at least this often. */
    val CALENDAR_FOCUS_LOOKAHEAD: Duration = 24.hours

    /** Lite protection: how often usage access is read while the screen is on. */
    val LITE_POLL_INTERVAL: Duration = 1.seconds

    /** Each read starts this far before the last one ended, since the system can write an event late. */
    val LITE_POLL_OVERLAP: Duration = 2.seconds

    /** When Lite protection starts, how far back to look for the app already in front. */
    val LITE_LOOK_BACK: Duration = 1.hours

    /** After the accessibility service stops, wait this long for the system's settings to catch up before checking the level again. */
    val ACCESSIBILITY_SETTLE: Duration = 1.seconds

    /** The modes the app ships with. None has a schedule until the user gives it one. */
    val MODES: List<ModePreset> = listOf(
        ModePreset("work", budget = 20.minutes, waitSteps = listOf(20, 40, 60, 120, 180).map { it.seconds }),
        ModePreset("study", budget = 15.minutes, waitSteps = listOf(30, 60, 120, 180, 300).map { it.seconds }),
        ModePreset("home", budget = DAILY_BUDGET, waitSteps = WAIT_STEPS),
        ModePreset("sleep", budget = 0.minutes, waitSteps = listOf(60, 120, 300).map { it.seconds }),
    )
}
