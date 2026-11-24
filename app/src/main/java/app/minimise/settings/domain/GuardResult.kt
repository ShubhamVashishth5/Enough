package app.minimise.settings.domain

import java.time.Instant

sealed interface GuardResult {
    /** Tightening (or no change at all): in effect already. */
    data object AppliedNow : GuardResult

    /** Loosening: waits until [applyAt]. */
    data class Scheduled(val applyAt: Instant) : GuardResult

    /** Not allowed at all: a lock period is on until [until]. */
    data class Refused(val reason: String, val until: Instant? = null) : GuardResult

    companion object {
        /** [Refused.reason] during a lock period. */
        const val LOCKED = "locked"
    }
}
