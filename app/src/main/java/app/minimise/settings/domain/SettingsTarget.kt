package app.minimise.settings.domain

import kotlin.time.Duration

/** Reads and writes the real settings. Only [SettingsGuard] should call [apply]. */
interface SettingsTarget {
    /** Which way [change] moves the setting from its current value, or null if it's already that value. */
    suspend fun direction(change: SettingChange): Direction?

    suspend fun apply(change: SettingChange)
}

/** How long a loosening change waits. */
interface LoosenDelay {
    suspend fun current(): Duration
}
