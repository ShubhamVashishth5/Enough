package app.minimise.widgets.domain

import app.minimise.core.Defaults
import kotlin.time.Duration

/** When home's weather line asks Open-Meteo again. Times are monotonic (time since boot). */
object WeatherRefresh {
    /**
     * @param lastAttempt the last request, whether or not it worked
     * @param lastSuccess the last request that came back with everything asked for
     * @param missing something switched on has no reading yet (air quality just turned on, say)
     * @param forced the user tapped the line
     */
    fun due(
        now: Duration,
        lastAttempt: Duration?,
        lastSuccess: Duration?,
        missing: Boolean,
        forced: Boolean,
        staleAfter: Duration = Defaults.WEATHER_STALE_AFTER,
        minGap: Duration = Defaults.WEATHER_MIN_GAP,
    ): Boolean = when {
        lastAttempt != null && now - lastAttempt < minGap -> false
        forced || missing || lastSuccess == null -> true
        else -> now - lastSuccess >= staleAfter
    }
}
