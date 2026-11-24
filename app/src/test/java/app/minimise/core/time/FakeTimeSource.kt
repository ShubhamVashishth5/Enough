package app.minimise.core.time

import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration

/** A clock that only moves when a test tells it to. */
class FakeTimeSource(
    var now: Instant = Instant.parse("2026-01-15T12:00:00Z"),
    var zone: ZoneId = ZoneId.of("UTC"),
    var elapsed: Duration = Duration.ZERO,
) : TimeSource {
    override fun now(): Instant = now

    override fun elapsed(): Duration = elapsed

    override fun zone(): ZoneId = zone

    fun advance(by: Duration) {
        now += by
        elapsed += by
    }
}
