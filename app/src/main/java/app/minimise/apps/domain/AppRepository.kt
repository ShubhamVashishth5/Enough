package app.minimise.apps.domain

import kotlinx.coroutines.flow.Flow

interface AppRepository {
    /** Installed, launchable apps (all profiles) with their stored kind, sorted by label. */
    val apps: Flow<List<AppEntry>>

    /** Puts [packageName] in [slot], moving out whichever app was there. */
    suspend fun setHomeSlot(packageName: String, slot: Int)

    suspend fun clearHomeSlot(slot: Int)
}
