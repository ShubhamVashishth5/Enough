package app.minimise.settings.domain

import java.time.Instant
import kotlin.time.Duration

/** Reads and writes the real settings. Only [SettingsGuard] should call [apply]. */
interface SettingsTarget {
    /** Which way [change] moves the setting from its current value, or null if it's already that value. */
    suspend fun direction(change: SettingChange): Direction?

    suspend fun apply(change: SettingChange)
}

/** A lock period: until it ends, nothing can be loosened at all. */
interface LockPeriod {
    /** The end of the current or last lock, or null if there never was one. */
    suspend fun lockedUntil(): Instant?
}

/** How long a loosening change waits. */
interface LoosenDelay {
    suspend fun current(): Duration
}
