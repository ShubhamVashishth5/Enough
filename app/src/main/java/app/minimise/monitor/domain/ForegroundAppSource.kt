package app.minimise.monitor.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A change of the app in front.
 *
 * @property packageName null when no tracked app is in front: the home screen, one of this app's own
 * screens (such as the pause screen), or the screen is off.
 */
data class ForegroundEvent(val packageName: String?, val at: Instant)

interface ForegroundAppSource {
    /** Only real changes: the same package twice in a row is never emitted. */
    val events: Flow<ForegroundEvent>

    /** The latest event, or null before the first one. */
    val current: StateFlow<ForegroundEvent?>
}
