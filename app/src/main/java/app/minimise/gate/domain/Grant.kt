package app.minimise.gate.domain

import java.time.Instant

/** A running time box: the app may be in front until [endAt] without going through the gate again. */
data class Grant(val packageName: String, val startAt: Instant, val endAt: Instant) {
    fun isActive(now: Instant): Boolean = !now.isBefore(startAt) && now.isBefore(endAt)
}

interface GrantStore {
    suspend fun get(packageName: String): Grant?

    /** Replaces any earlier grant for the same app. */
    suspend fun put(grant: Grant)

    suspend fun remove(packageName: String)
}
