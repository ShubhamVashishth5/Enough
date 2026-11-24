package app.minimise.protection.domain

/** How much the gate can see. Stored by name in the event log, so never rename an entry. */
enum class ProtectionLevel {
    /** The accessibility service: every open is gated, and screens can be read where a rule needs it. */
    FULL,

    /** Usage access plus "display over other apps": every open is gated, but no screen is read. */
    LITE,

    /** Only opens from home and search are gated. */
    LAUNCHER_ONLY,
    ;

    /** Feed blocking, website rules and the lock guard read other apps' screens, so they need [FULL]. */
    val readsScreens: Boolean get() = this == FULL

    companion object {
        /** Accessibility wins whenever it's on; Lite needs both of its permissions. */
        fun from(accessibility: Boolean, usageAccess: Boolean, overlay: Boolean): ProtectionLevel = when {
            accessibility -> FULL
            usageAccess && overlay -> LITE
            else -> LAUNCHER_ONLY
        }
    }
}
