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

/** Picks a mode by hand; null goes back to following mode schedules. */
@Serializable
@SerialName("set_manual_mode")
data class SetManualMode(val modeId: Long?) : SettingChange {
    override val key: String get() = "manual_mode"
}

/** Edits a mode. A null allowlist keeps every app's usual kind; scheduleDays 0 means no schedule. */
@Serializable
@SerialName("save_mode")
data class SaveMode(
    val id: Long,
    val budgetMinutes: Int,
    val allowlist: List<String>?,
    val scheduleDays: Int,
    val scheduleStartMinute: Int,
    val scheduleEndMinute: Int,
) : SettingChange {
    override val key: String get() = "mode:$id"
}

/** Switches the block on one feed (assets/feed_rules.json) on or off. */
@Serializable
@SerialName("set_feed_blocked")
data class SetFeedBlocked(val feedId: String, val blocked: Boolean) : SettingChange {
    override val key: String get() = "feed:$feedId"
}

/** How strict each kind is. Only Vault apps are gated, so leaving the Vault is always loosening. */
private val AppKind.strictness: Int
    get() = when (this) {
        AppKind.TOOL -> 0
        AppKind.HIDDEN -> 1
        AppKind.VAULT -> 2
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
