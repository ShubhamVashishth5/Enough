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
}
