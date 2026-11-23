package app.minimise.apps.domain

import kotlinx.coroutines.flow.Flow

interface AppRepository {
    /** Installed, launchable apps (all profiles) with their stored kind, sorted by label. */
    val apps: Flow<List<AppEntry>>

    /** The stored kind, whether or not the app is installed right now. Unknown packages are TOOL. */
    suspend fun kindOf(packageName: String): AppKind

    /** Writes the kind directly. Only SettingsGuard should call this. */
    suspend fun setKind(packageName: String, kind: AppKind)

    /** Puts [packageName] in [slot], moving out whichever app was there. */
    suspend fun setHomeSlot(packageName: String, slot: Int)

    suspend fun clearHomeSlot(slot: Int)
}
