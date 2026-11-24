package app.minimise.focus

import app.minimise.focus.domain.FocusSessions
import javax.inject.Inject

/** Runs in ProtectionRuntime: finishes a session the moment it ends, so Do Not Disturb comes back off on time. */
class FocusController @Inject constructor(
    private val focusSessions: FocusSessions,
) {
    suspend fun run() {
        focusSessions.active.collect { if (it == null) focusSessions.finishIfDue() }
    }
}
