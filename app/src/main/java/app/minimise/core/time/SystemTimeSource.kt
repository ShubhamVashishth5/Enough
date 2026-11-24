package app.minimise.core.time

import android.os.SystemClock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class SystemTimeSource @Inject constructor(
    private val debugClockOffset: DebugClockOffset,
) : TimeSource {
    override fun now(): Instant = Instant.now() + debugClockOffset.offset

    override fun elapsed(): Duration = SystemClock.elapsedRealtime().milliseconds

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
