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
    data object PendingChanges : Destination

    @Serializable
    data object Permissions : Destination

    @Serializable
    data object DebugUsage : Destination
}
