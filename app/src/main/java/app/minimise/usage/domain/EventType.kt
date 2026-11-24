package app.minimise.usage.domain

/** Stored by name in the event log, so never rename an entry. Add new ones at the end. */
enum class EventType {
    SCREEN_UNLOCK,
    APP_SPAN,
    GATE_SHOWN,
    GATE_PROCEEDED,
    GATE_CANCELLED,
    TIME_UP,
    OVERRIDE_USED,
    SETTING_CHANGED,
    SWAP_TAKEN,
    FOCUS_SESSION,

    /** A blocked feed came on screen and was left. data: the feed's id. */
    FEED_BLOCKED,

    /** The Blocked screen was shown. data: the rule's reason ("daily_budget" is a slip). */
    BLOCK_SHOWN,

    /** "What pulled you in?" after a budget block. data: the answer. */
    TRIGGER,

    /** The stats screen was opened; it opens once per usage day. */
    STATS_VIEWED,

    /** Earn-it: minutes added to today's budget. data: minutes, source (task or focus), ref. */
    BUDGET_BONUS,

    /** The protection level changed (accessibility, usage access or overlay switched). data: level. */
    PROTECTION_CHANGED,
}
