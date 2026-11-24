package app.minimise.settings.domain

import java.time.Instant

sealed interface GuardResult {
    /** Tightening (or no change at all): in effect already. */
    data object AppliedNow : GuardResult

    /** Loosening: waits until [applyAt]. */
    data class Scheduled(val applyAt: Instant) : GuardResult

    /** Not allowed at all (used by lock periods from Phase 3). */
    data class Refused(val reason: String) : GuardResult
}
