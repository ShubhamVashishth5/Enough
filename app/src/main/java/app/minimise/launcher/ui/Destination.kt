package app.minimise.launcher.ui

import kotlinx.serialization.Serializable

/** Screens inside HomeActivity. Interventions (pause, time's up) are separate activities. */
sealed interface Destination {
    @Serializable
    data object Onboarding : Destination

    @Serializable
    data object Home : Destination

    @Serializable
    data object Search : Destination

    @Serializable
    data object Today : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object Apps : Destination

    @Serializable
    data object Budget : Destination

    @Serializable
    data object Swaps : Destination

    @Serializable
    data object Schedules : Destination

    @Serializable
    data object Keys : Destination

    @Serializable
    data object Focus : Destination

    @Serializable
    data object Modes : Destination

    @Serializable
    data class ModeEdit(val id: Long) : Destination

    @Serializable
    data object Digest : Destination

    @Serializable
    data object DigestSettings : Destination

    @Serializable
    data object Capture : Destination

    @Serializable
    data object Inbox : Destination

    @Serializable
    data object Feeds : Destination

    @Serializable
    data object Stats : Destination

    @Serializable
    data object Lock : Destination

    @Serializable
    data object FriendKey : Destination

    @Serializable
    data class ApplyNow(val pendingId: Long) : Destination

    @Serializable
    data object PendingChanges : Destination

    @Serializable
    data object Permissions : Destination

    @Serializable
    data object DebugUsage : Destination
}
