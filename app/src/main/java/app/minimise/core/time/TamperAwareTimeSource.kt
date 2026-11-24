package app.minimise.core.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The app's [TimeSource]: wall-clock time that moving the phone's clock doesn't fool (see
 * [TamperAwareClock]), plus the debug-only offset for testing day resets.
 */
@Singleton
class TamperAwareTimeSource @Inject constructor(
    @ApplicationContext context: Context,
    private val debugClockOffset: DebugClockOffset,
) : TimeSource {
    private val resolver = context.contentResolver
    private val bootCount: Int by lazy { Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, 0) }

    private val clock = TamperAwareClock(
        wall = Instant::now,
        elapsed = ::elapsed,
        bootCount = { bootCount },
        networkTime = { Settings.Global.getInt(resolver, Settings.Global.AUTO_TIME, 1) == 1 },
        store = PrefsClockAnchorStore(context),
    )

    override fun now(): Instant = clock.now() + debugClockOffset.offset

    override fun elapsed(): Duration = SystemClock.elapsedRealtime().milliseconds

    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** SharedPreferences, not DataStore: the clock is read synchronously, everywhere. */
private class PrefsClockAnchorStore(context: Context) : ClockAnchorStore {
    private val prefs = context.getSharedPreferences("clock_anchor", Context.MODE_PRIVATE)

    override fun load(): ClockAnchor? {
        if (!prefs.contains(WALL)) return null
        return ClockAnchor(prefs.getLong(WALL, 0), prefs.getLong(ELAPSED, 0), prefs.getInt(BOOT, 0))
    }

    override fun save(anchor: ClockAnchor) {
        prefs.edit {
            putLong(WALL, anchor.wallMillis)
            putLong(ELAPSED, anchor.elapsedMillis)
            putInt(BOOT, anchor.bootCount)
        }
    }

    private companion object {
        const val WALL = "wall_millis"
        const val ELAPSED = "elapsed_millis"
        const val BOOT = "boot_count"
    }
}
