package app.minimise.widgets.data

import app.minimise.core.time.TimeSource
import app.minimise.widgets.domain.OpenMeteo
import app.minimise.widgets.domain.WeatherNow
import app.minimise.widgets.domain.WeatherRefresh
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex

enum class WeatherProblem {
    /** Location isn't allowed. */
    NO_PERMISSION,

    /** Allowed, but the phone couldn't say where it is (location off, no fix). */
    NO_LOCATION,

    /** Open-Meteo couldn't be reached, or answered with something unexpected. */
    OFFLINE,
}

/**
 * @property problem why the last refresh didn't get everything. Home still shows an older reading if it
 * has one, and the problem only when it has nothing to show.
 */
data class WeatherState(
    val weather: WeatherNow? = null,
    val usAqi: Int? = null,
    val problem: WeatherProblem? = null,
)

/** Home's weather and air quality, kept in memory and asked for again when stale. */
@Singleton
class WeatherRepository @Inject constructor(
    private val location: DeviceLocation,
    private val client: OpenMeteoClient,
    private val timeSource: TimeSource,
) {
    private val _state = MutableStateFlow(WeatherState())
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    private val refreshing = Mutex()
    private var lastAttempt: Duration? = null
    private var lastSuccess: Duration? = null

    /** Asks only for what's switched on, and only when [WeatherRefresh] says it's time. */
    suspend fun refresh(weather: Boolean, airQuality: Boolean, forced: Boolean = false) {
        if (!weather && !airQuality) return
        if (!location.hasPermission()) {
            _state.value = WeatherState(problem = WeatherProblem.NO_PERMISSION)
            return
        }
        if (!refreshing.tryLock()) return
        try {
            fetch(weather, airQuality, forced)
        } finally {
            refreshing.unlock()
        }
    }

    private suspend fun fetch(weather: Boolean, airQuality: Boolean, forced: Boolean) {
        val before = _state.value
        val missing = (weather && before.weather == null) || (airQuality && before.usAqi == null)
        val now = timeSource.elapsed()
        if (!WeatherRefresh.due(now, lastAttempt, lastSuccess, missing, forced)) return
        lastAttempt = now

        val place = location.current()
        if (place == null) {
            _state.update { it.copy(problem = WeatherProblem.NO_LOCATION) }
            return
        }
        val (newWeather, newAqi) = coroutineScope {
            val forecast = async {
                if (weather) client.get(OpenMeteo.forecastUrl(place.latitude, place.longitude))?.let(OpenMeteo::parseWeather) else null
            }
            val air = async {
                if (airQuality) client.get(OpenMeteo.airQualityUrl(place.latitude, place.longitude))?.let(OpenMeteo::parseUsAqi) else null
            }
            forecast.await() to air.await()
        }
        val complete = (!weather || newWeather != null) && (!airQuality || newAqi != null)
        if (complete) lastSuccess = now
        _state.value = WeatherState(
            weather = newWeather ?: before.weather,
            usAqi = newAqi ?: before.usAqi,
            problem = if (complete) null else WeatherProblem.OFFLINE,
        )
    }
}
