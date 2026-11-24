package app.minimise.core.time

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

/**
 * Shifts the wall clock in debug builds so day resets can be tested without changing the phone's time.
 * Always zero in release builds (see MinimiseApp).
 */
@Singleton
class DebugClockOffset @Inject constructor() {
    @Volatile
    var offset: Duration = Duration.ZERO
}
