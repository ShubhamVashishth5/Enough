package app.minimise.protection.domain

import app.minimise.apps.domain.WebTarget

/**
 * Which apps to put behind the system app lock, and which to take back out. Every change goes
 * through the system's own confirmation, so the user is asked each time.
 */
data class AppLockPlan(val toLock: List<String>, val toUnlock: List<String>) {
    val isEmpty: Boolean get() = toLock.isEmpty() && toUnlock.isEmpty()

    companion object {
        /**
         * While the gate can't see everything ([level] below Full), every Vault app should be locked.
         * Back at Full, or once an app leaves the Vault, Enough unlocks the apps it locked itself
         * ([lockedByEnough]), never one the user locked on their own.
         *
         * @param locked the lock state of each app that supports the system app lock; others are absent.
         */
        fun of(level: ProtectionLevel, vault: Set<String>, locked: Map<String, Boolean>, lockedByEnough: Set<String>): AppLockPlan {
            val toLock = if (level == ProtectionLevel.FULL) {
                emptyList()
            } else {
                vault.filter { !WebTarget.isWebTarget(it) && locked[it] == false }.sorted()
            }
            val toUnlock = lockedByEnough
                .filter { locked[it] == true && (level == ProtectionLevel.FULL || it !in vault) }
                .sorted()
            return AppLockPlan(toLock, toUnlock)
        }

        /** Forgets the apps that aren't locked any more (unlocked in system settings, or uninstalled). */
        fun stillLocked(lockedByEnough: Set<String>, locked: Map<String, Boolean>): Set<String> =
            lockedByEnough.filterTo(HashSet()) { locked[it] == true }
    }
}
