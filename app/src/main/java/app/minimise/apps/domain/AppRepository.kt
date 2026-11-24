package app.minimise.apps.domain

import kotlinx.coroutines.flow.Flow

interface AppRepository {
    /** Installed, launchable apps (all profiles) with their stored kind, sorted by label. */
    val apps: Flow<List<AppEntry>>

    /** Stored kind of every package ever seen, installed or not. */
    val kinds: Flow<Map<String, AppKind>>

    /** One app, without listing all of them. Null if it isn't installed and launchable. */
    suspend fun entry(packageName: String): AppEntry?

    /** The stored kind, whether or not the app is installed right now. Unknown packages are TOOL. */
    suspend fun kindOf(packageName: String): AppKind

    /** Writes the kind directly. Only SettingsGuard should call this. */
    suspend fun setKind(packageName: String, kind: AppKind)

    /** The stored daily open limit, or null for none. */
    suspend fun openLimitOf(packageName: String): Int?

    /** Writes the open limit directly. Only SettingsGuard should call this. */
    suspend fun setOpenLimit(packageName: String, limit: Int?)

    /** Puts [packageName] in [slot], moving out whichever app was there. */
    suspend fun setHomeSlot(packageName: String, slot: Int)

    suspend fun clearHomeSlot(slot: Int)
}
