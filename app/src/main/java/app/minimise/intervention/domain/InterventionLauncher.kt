package app.minimise.intervention.domain

import app.minimise.apps.domain.AppEntry
import app.minimise.gate.domain.Decision

/** Turns a gate decision into a full-screen intervention. */
interface InterventionLauncher {
    /** @param launchAfter true when the app isn't open yet (launcher path): start it once the user proceeds. */
    fun showPause(app: AppEntry, pause: Decision.Pause, launchAfter: Boolean)

    fun showBlocked(app: AppEntry, block: Decision.Block)

    /** The time box for [packageName] ended while it was in front. */
    fun showTimeUp(packageName: String)
}
