package app.minimise.settings.domain

import app.minimise.apps.domain.AppKind
import app.minimise.core.time.WeeklyWindow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Direction { TIGHTEN, LOOSEN }

/**
 * A request to change a rule setting. Changes go through [SettingsGuard], never straight to storage.
 * Stored as JSON while pending, so keep field names stable.
 */
@Serializable
sealed interface SettingChange {
    /** Changes with the same key are the same setting: a new request replaces a pending one. */
    val key: String
}

@Serializable
@SerialName("set_app_kind")
data class SetAppKind(val packageName: String, val kind: AppKind) : SettingChange {
    override val key: String get() = "app_kind:$packageName"
}

@Serializable
@SerialName("set_budget")
data class SetBudget(val minutes: Int) : SettingChange {
    override val key: String get() = "budget"
}

/** A daily cap on opens of one app; null removes it. */
@Serializable
@SerialName("set_open_limit")
data class SetOpenLimit(val packageName: String, val limit: Int?) : SettingChange {
    override val key: String get() = "open_limit:$packageName"
}

/** A Breathe app's own pause in seconds; null goes back to the usual one. A longer pause is tightening. */
@Serializable
@SerialName("set_breathe_wait")
data class SetBreatheWait(val packageName: String, val seconds: Int?) : SettingChange {
    override val key: String get() = "breathe_wait:$packageName"
}

/** Adds (id 0) or edits a schedule. Times are minutes after midnight; days have Monday as bit 0. */
@Serializable
@SerialName("save_schedule")
data class SaveSchedule(val id: Long, val name: String, val days: Int, val startMinute: Int, val endMinute: Int) : SettingChange {
    override val key: String get() = "schedule:$id"
}

/** Shares its key with [SaveSchedule], so removing a schedule replaces a pending edit of it. */
@Serializable
@SerialName("remove_schedule")
data class RemoveSchedule(val id: Long) : SettingChange {
    override val key: String get() = "schedule:$id"
}

@Serializable
@SerialName("set_first_hour_rule")
data class SetFirstHourRule(val enabled: Boolean) : SettingChange {
    override val key: String get() = "first_hour_rule"
}

/** Pairs a physical key. [secretHash] is the SHA-256 of the sticker ID or the QR text. */
@Serializable
@SerialName("add_key")
data class AddKey(val type: String, val label: String, val secretHash: String) : SettingChange {
    override val key: String get() = "key:add:$secretHash"
}

@Serializable
@SerialName("remove_key")
data class RemoveKey(val id: Long) : SettingChange {
    override val key: String get() = "key:$id"
}

/**
 * Picks a mode by hand until [untilEpochMillis], when the schedules take over again; null goes back to
 * following them now. An end is part of the choice: the app always asks for one, and only changes made
 * before it existed (or restored from them) have none, which means the pick lasts until changed.
 */
@Serializable
@SerialName("set_manual_mode")
data class SetManualMode(val modeId: Long?, val untilEpochMillis: Long? = null) : SettingChange {
    override val key: String get() = "manual_mode"
}

/**
 * Adds (id 0, with a [name]) or edits a mode. A null allowlist keeps every app's usual kind;
 * scheduleDays 0 means no schedule. [waitStepsSeconds] null keeps the mode's waits (a new mode takes
 * them from the mode it started from). [name] null keeps the name; only the user's own modes are renamed.
 */
@Serializable
@SerialName("save_mode")
data class SaveMode(
    val id: Long,
    val budgetMinutes: Int,
    val allowlist: List<String>?,
    val scheduleDays: Int,
    val scheduleStartMinute: Int,
    val scheduleEndMinute: Int,
    val waitStepsSeconds: List<Long>? = null,
    val name: String? = null,
) : SettingChange {
    /** New modes are told apart by name, so adding two (from a backup) doesn't replace one with the other. */
    override val key: String get() = if (id == 0L) "mode:new:${name.orEmpty()}" else "mode:$id"
}

/** Removes one of the user's own modes. Shares its key with [SaveMode], so it replaces a pending edit. */
@Serializable
@SerialName("remove_mode")
data class RemoveMode(val id: Long) : SettingChange {
    override val key: String get() = "mode:$id"
}

/** Switches the block on one feed (assets/feed_rules.json) on or off. */
@Serializable
@SerialName("set_feed_blocked")
data class SetFeedBlocked(val feedId: String, val blocked: Boolean) : SettingChange {
    override val key: String get() = "feed:$feedId"
}

/** Starts (or extends) a lock period: until then nothing can be loosened. Shortening one isn't possible. */
@Serializable
@SerialName("start_lock")
data class StartLock(val untilEpochMillis: Long) : SettingChange {
    override val key: String get() = "lock"
}

/** Taper down to [floorMinutes] a day; null stops tapering. A lower floor is tightening, like a lower limit. */
@Serializable
@SerialName("set_taper")
data class SetTaper(val floorMinutes: Int?) : SettingChange {
    override val key: String get() = "taper"
}

/** Earn-it adds minutes to the budget, so switching it on is loosening and off is tightening. */
@Serializable
@SerialName("set_earn_it")
data class SetEarnIt(val enabled: Boolean) : SettingChange {
    override val key: String get() = "earn_it"
}

/** Calendar focus: events with "focus" in the title start sessions. On is tightening. */
@Serializable
@SerialName("set_calendar_focus")
data class SetCalendarFocus(val enabled: Boolean) : SettingChange {
    override val key: String get() = "calendar_focus"
}

/**
 * How strict each kind is. Vault apps get every rule and Breathe apps only the pause, so leaving the
 * Vault is always loosening, and so is going from Breathe to Tool or Hidden (no pause at all).
 */
private val AppKind.strictness: Int
    get() = when (this) {
        AppKind.TOOL -> 0
        AppKind.HIDDEN -> 1
        AppKind.BREATHE -> 2
        AppKind.VAULT -> 3
    }

/** Null when nothing would change. */
fun kindChangeDirection(from: AppKind, to: AppKind): Direction? = when {
    to == from -> null
    to.strictness > from.strictness -> Direction.TIGHTEN
    else -> Direction.LOOSEN
}

/** Adding a limit or lowering one is tightening; raising or removing one is loosening. */
fun openLimitChangeDirection(from: Int?, to: Int?): Direction? = when {
    to == from -> null
    from == null -> Direction.TIGHTEN
    to == null -> Direction.LOOSEN
    to < from -> Direction.TIGHTEN
    else -> Direction.LOOSEN
}

/**
 * A new schedule, or an edit whose window covers the old one, is tightening (a rename alone too).
 * Shrinking or moving a window is loosening. Null when nothing would change.
 */
fun scheduleChangeDirection(old: WeeklyWindow?, oldName: String?, new: WeeklyWindow, newName: String): Direction? = when {
    new.days.isEmpty() && old == null -> null
    old == null -> Direction.TIGHTEN
    old == new && oldName == newName -> null
    new.covers(old) -> Direction.TIGHTEN
    else -> Direction.LOOSEN
}

/**
 * A longer Breathe pause is tightening; a shorter one is loosening. Null (the usual pause) counts as
 * [usualSeconds]; going between it and the same number of seconds just records the choice.
 */
fun breatheWaitChangeDirection(from: Int?, to: Int?, usualSeconds: Int): Direction? {
    if (from == to) return null
    val before = from ?: usualSeconds
    val after = to ?: usualSeconds
    return if (after >= before) Direction.TIGHTEN else Direction.LOOSEN
}

/** Switching a rule on is tightening; off is loosening. */
fun switchDirection(from: Boolean, to: Boolean): Direction? = when {
    from == to -> null
    to -> Direction.TIGHTEN
    else -> Direction.LOOSEN
}

/** A smaller budget is tightening; a bigger one is loosening. Null when nothing would change. */
fun budgetChangeDirection(fromMinutes: Int, toMinutes: Int): Direction? = when {
    toMinutes == fromMinutes -> null
    toMinutes < fromMinutes -> Direction.TIGHTEN
    else -> Direction.LOOSEN
}
