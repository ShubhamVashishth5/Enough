package app.minimise.apps.domain

import kotlinx.coroutines.flow.Flow

interface AppRepository {
    /** Installed, launchable apps (all profiles) with their stored kind, sorted by label. */
    val apps: Flow<List<AppEntry>>

    /** Websites the user added ("web:instagram.com"), labelled by domain, sorted. Never on home or in search. */
    val webTargets: Flow<List<AppEntry>>

    /** Stored kind of every package ever seen, installed or not. */
    val kinds: Flow<Map<String, AppKind>>

    /** One app, without listing all of them. Null if it isn't installed and launchable (or, for a website, never added). */
    suspend fun entry(packageName: String): AppEntry?

    /** The stored kind, whether or not the app is installed right now. Unknown packages are TOOL. */
    suspend fun kindOf(packageName: String): AppKind

    /** Writes the kind directly. Only SettingsGuard should call this. */
    suspend fun setKind(packageName: String, kind: AppKind)

    /** The stored daily open limit, or null for none. */
    suspend fun openLimitOf(packageName: String): Int?

    /** Every stored open limit, by package, installed or not. */
    suspend fun openLimits(): Map<String, Int>

    /** Writes the open limit directly. Only SettingsGuard should call this. */
    suspend fun setOpenLimit(packageName: String, limit: Int?)

    /** The stored Breathe pause in seconds, or null for the usual one. */
    suspend fun breatheWaitOf(packageName: String): Int?

    /** Every stored Breathe pause in seconds, by package, installed or not. */
    suspend fun breatheWaits(): Map<String, Int>

    /** Writes the Breathe pause directly. Only SettingsGuard should call this. */
    suspend fun setBreatheWait(packageName: String, seconds: Int?)

    /** Puts [packageName] in [slot], moving out whichever app was there. */
    suspend fun setHomeSlot(packageName: String, slot: Int)

    suspend fun clearHomeSlot(slot: Int)
}
