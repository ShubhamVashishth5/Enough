package app.minimise.core.time

import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration

/**
 * The only source of time in the app. Never call System.currentTimeMillis() or Instant.now() directly.
 */
interface TimeSource {
    /** Wall clock: display, day boundary, schedules. */
    fun now(): Instant

    /** Monotonic time since boot: timers and waits. Unaffected by clock changes. */
    fun elapsed(): Duration

    /** The time zone used to turn [now] into local dates and times. */
    fun zone(): ZoneId
}
