package app.minimise.protection

import android.content.IntentSender
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.protection.domain.AppLockPlan
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Keeps Vault apps behind the system app lock while the gate can't see everything, and takes the lock
 * off again once it can (or once an app leaves the Vault). Each change needs the user's confirmation on
 * a system screen, so home offers it and the user goes through one screen per app.
 */
class AppLockSync @Inject constructor(
    private val appLocker: AppLocker,
    private val appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val protectionStatus: ProtectionStatus,
) {
    /** One system screen to show: lock or unlock [packageName]. */
    data class Request(val packageName: String, val lock: Boolean, val intentSender: IntentSender)

    /** What to ask for now. Also forgets locks the user took off in system settings. */
    suspend fun plan(): AppLockPlan = withContext(Dispatchers.IO) {
        if (!appLocker.isAvailable()) return@withContext AppLockPlan(emptyList(), emptyList())
        val vault = appRepository.kinds.first().filterValues { it == AppKind.VAULT }.keys
        val lockedByEnough = settingsStore.current().appsLockedByEnough
        val states = appLocker.lockStates(vault + lockedByEnough)
        val stillLocked = AppLockPlan.stillLocked(lockedByEnough, states)
        if (stillLocked != lockedByEnough) settingsStore.update { it.copy(appsLockedByEnough = stillLocked) }
        AppLockPlan.of(protectionStatus.current(), vault, states, stillLocked)
    }

    /** The first change still to make, locks before unlocks, or null when there's none. */
    suspend fun next(): Request? {
        val plan = plan()
        val changes = plan.toLock.map { it to true } + plan.toUnlock.map { it to false }
        return withContext(Dispatchers.IO) {
            changes.firstNotNullOfOrNull { (packageName, lock) ->
                appLocker.request(packageName, lock)?.let { Request(packageName, lock, it) }
            }
        }
    }

    /** After the system screen closes: remember a lock Enough made, forget one it took off. */
    suspend fun onDone(request: Request) {
        val locked = withContext(Dispatchers.IO) { appLocker.lockStates(listOf(request.packageName))[request.packageName] }
        settingsStore.update {
            when {
                request.lock && locked == true -> it.copy(appsLockedByEnough = it.appsLockedByEnough + request.packageName)
                !request.lock && locked == false -> it.copy(appsLockedByEnough = it.appsLockedByEnough - request.packageName)
                else -> it
            }
        }
    }
}
