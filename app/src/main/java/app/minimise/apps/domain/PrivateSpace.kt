package app.minimise.apps.domain

import kotlinx.coroutines.flow.Flow

/**
 * Android 15's Private space: a separate profile the system locks behind the phone's own credential.
 * Vault apps installed there are hidden everywhere while it's locked, an OS-level lock nothing in this
 * app can loosen. Everything here is a no-op below Android 15 or when no private space is set up.
 */
interface PrivateSpace {
    fun exists(): Boolean

    fun isLocked(): Boolean

    /** Asks the system to lock or unlock it; unlocking shows the system's own prompt. False when refused. */
    fun setLocked(locked: Boolean): Boolean

    /** Emits whenever a profile is locked, unlocked, added or removed. */
    val changes: Flow<Unit>
}

object PrivateSpaceApps {
    /** A profile's apps show unless it's the private space and that's locked. */
    fun showsProfile(isPrivate: Boolean, locked: Boolean): Boolean = !isPrivate || !locked
}
