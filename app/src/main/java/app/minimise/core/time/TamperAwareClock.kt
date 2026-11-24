package app.minimise.core.time

import app.minimise.core.Defaults
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** A wall-clock reading the app trusts, paired with the monotonic clock at that moment. */
data class ClockAnchor(val wallMillis: Long, val elapsedMillis: Long, val bootCount: Int)

/** Keeps the last anchor across process restarts. Reads and writes must be quick: [TamperAwareClock.now] calls them. */
interface ClockAnchorStore {
    fun load(): ClockAnchor?

    fun save(anchor: ClockAnchor)
}

/**
 * Wall-clock time that ignores the user moving the phone's clock. It keeps an anchor of wall time
 * against time since boot (which can't be changed). If the wall clock jumps more than
 * [Defaults.CLOCK_TAMPER_TOLERANCE] away from where the anchor says it should be, the anchor wins.
 *
 * Network time (automatic date and time) is always trusted, so a phone that was simply wrong gets
 * corrected. After a restart the anchor's monotonic half is gone: the clock is trusted again, except
 * that it can't go back before the last trusted time plus the time since boot.
 */
class TamperAwareClock(
    private val wall: () -> Instant,
    private val elapsed: () -> Duration,
    private val bootCount: () -> Int,
    private val networkTime: () -> Boolean,
    private val store: ClockAnchorStore,
    private val tolerance: Duration = Defaults.CLOCK_TAMPER_TOLERANCE,
    private val saveEvery: Duration = Defaults.CLOCK_ANCHOR_SAVE_INTERVAL,
) {
    private var anchor: ClockAnchor? = null
    private var loaded = false
    private var savedAtElapsed: Duration? = null

    /** True when the last reading ignored the wall clock. */
    var tampered: Boolean = false
        private set

    @Synchronized
    fun now(): Instant {
        val wallNow = wall()
        val elapsedNow = elapsed()
        val boot = bootCount()
        if (!loaded) {
            anchor = store.load()
            loaded = true
        }
        val last = anchor
        val trusted = when {
            last == null -> wallNow
            last.bootCount != boot -> maxOf(wallNow, Instant.ofEpochMilli(last.wallMillis) + elapsedNow)
            else -> {
                val expected = Instant.ofEpochMilli(last.wallMillis) + (elapsedNow - last.elapsedMillis.milliseconds)
                val drift = durationBetween(expected, wallNow).absoluteValue
                if (drift <= tolerance || networkTime()) wallNow else expected
            }
        }
        val wasTampered = tampered
        tampered = trusted != wallNow
        val next = ClockAnchor(trusted.toEpochMilli(), elapsedNow.inWholeMilliseconds, boot)
        anchor = next
        val lastSave = savedAtElapsed
        if (lastSave == null || last?.bootCount != boot || wasTampered != tampered || elapsedNow - lastSave >= saveEvery) {
            store.save(next)
            savedAtElapsed = elapsedNow
        }
        return trusted
    }
}
