package app.minimise.widgets.ui

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.timeText
import app.minimise.core.ui.dayName
import app.minimise.launcher.QuickTools
import app.minimise.widgets.data.BatteryState
import app.minimise.widgets.data.Track
import app.minimise.widgets.data.WeatherProblem
import app.minimise.widgets.domain.AqiLevel
import app.minimise.widgets.domain.Sky
import java.time.LocalDateTime

/**
 * Home's small lines under the clock, all words: weather, air quality, the next alarm and the battery on
 * one line, and what's playing under it. Each part can be switched off in Settings → Home screen.
 */
@Composable
fun HomeWidgets(viewModel: HomeWidgetsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onLocationPermissionResult()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    val options = state.options ?: return

    val items = buildList {
        if (options.usesLocation) {
            val weather = state.weather
            if (options.weather) weather.weather?.let { add(Item(weatherText(it.temperature, it.sky), viewModel::onWeatherTapped)) }
            if (options.airQuality) weather.usAqi?.let { add(Item(aqiText(it), viewModel::onWeatherTapped)) }
            val nothingToShow = (!options.weather || weather.weather == null) && (!options.airQuality || weather.usAqi == null)
            if (nothingToShow) {
                when (weather.problem) {
                    WeatherProblem.NO_PERMISSION -> add(
                        Item(stringResource(R.string.home_weather_needs_location)) {
                            locationRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        },
                    )
                    WeatherProblem.NO_LOCATION -> add(Item(stringResource(R.string.home_weather_no_location), viewModel::onWeatherTapped))
                    WeatherProblem.OFFLINE -> add(Item(stringResource(R.string.home_weather_offline), viewModel::onWeatherTapped))
                    null -> Unit
                }
            }
        }
        if (options.nextAlarm) state.nextAlarm?.let { add(Item(alarmText(it, state.now)) { QuickTools.openAlarms(context) }) }
        if (options.battery) state.battery?.let { add(Item(batteryText(it)) { QuickTools.openBatteryUsage(context) }) }
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        if (items.isNotEmpty()) StatusLine(items)
        if (options.music) {
            state.track?.let {
                MusicLine(
                    track = it,
                    onOpen = viewModel::onTrackTapped,
                    onPrevious = viewModel::onPrevious,
                    onPlayPause = viewModel::onPlayPause,
                    onNext = viewModel::onNext,
                )
            }
        }
    }
}

private data class Item(val text: String, val onClick: () -> Unit)

@Composable
private fun StatusLine(items: List<Item>) {
    FlowRow(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Text(
                text = if (index < items.lastIndex) item.text + SEPARATOR else item.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(role = Role.Button, onClick = item.onClick).padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MusicLine(track: Track, onOpen: () -> Unit, onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = track.artist?.let { stringResource(R.string.home_music_track, track.title, it) } ?: track.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.a11y_open_player), onClick = onOpen)
                .padding(vertical = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Control(stringResource(R.string.home_music_previous), onPrevious)
            Control(stringResource(if (track.playing) R.string.home_music_pause else R.string.home_music_play), onPlayPause)
            Control(stringResource(R.string.home_music_next), onNext)
        }
    }
}

@Composable
private fun Control(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
    )
}

@Composable
private fun weatherText(temperature: Int, sky: Sky): String =
    sky.labelRes()?.let { stringResource(R.string.home_weather, temperature, stringResource(it)) }
        ?: stringResource(R.string.home_weather_temperature_only, temperature)

@Composable
private fun aqiText(aqi: Int): String = stringResource(R.string.home_aqi, aqi, stringResource(AqiLevel.ofUsAqi(aqi).labelRes()))

@Composable
private fun batteryText(battery: BatteryState): String =
    stringResource(if (battery.charging) R.string.home_battery_charging else R.string.home_battery, battery.percent)

/** "Alarm 07:00" within the next day, "Alarm Thu 07:00" further out. */
@Composable
private fun alarmText(at: LocalDateTime, now: LocalDateTime?): String {
    val time = timeText(at.toLocalTime())
    val soon = now != null && at.isBefore(now.plusHours(HOURS_SHOWN_AS_TIME_ONLY))
    return stringResource(R.string.home_alarm, if (soon) time else "${dayName(at.dayOfWeek)} $time")
}

/** Null for a code Open-Meteo added after this was written: the temperature then shows alone. */
fun Sky.labelRes(): Int? = when (this) {
    Sky.CLEAR -> R.string.sky_clear
    Sky.MAINLY_CLEAR -> R.string.sky_mainly_clear
    Sky.PARTLY_CLOUDY -> R.string.sky_partly_cloudy
    Sky.OVERCAST -> R.string.sky_overcast
    Sky.FOG -> R.string.sky_fog
    Sky.DRIZZLE -> R.string.sky_drizzle
    Sky.FREEZING_DRIZZLE -> R.string.sky_freezing_drizzle
    Sky.RAIN -> R.string.sky_rain
    Sky.FREEZING_RAIN -> R.string.sky_freezing_rain
    Sky.SNOW -> R.string.sky_snow
    Sky.SNOW_GRAINS -> R.string.sky_snow_grains
    Sky.RAIN_SHOWERS -> R.string.sky_rain_showers
    Sky.SNOW_SHOWERS -> R.string.sky_snow_showers
    Sky.THUNDERSTORM -> R.string.sky_thunderstorm
    Sky.THUNDERSTORM_HAIL -> R.string.sky_thunderstorm_hail
    Sky.UNKNOWN -> null
}

fun AqiLevel.labelRes(): Int = when (this) {
    AqiLevel.GOOD -> R.string.aqi_good
    AqiLevel.MODERATE -> R.string.aqi_moderate
    AqiLevel.UNHEALTHY_FOR_SENSITIVE -> R.string.aqi_sensitive
    AqiLevel.UNHEALTHY -> R.string.aqi_unhealthy
    AqiLevel.VERY_UNHEALTHY -> R.string.aqi_very_unhealthy
    AqiLevel.HAZARDOUS -> R.string.aqi_hazardous
}

private const val SEPARATOR = " · "
private const val HOURS_SHOWN_AS_TIME_ONLY = 20L
