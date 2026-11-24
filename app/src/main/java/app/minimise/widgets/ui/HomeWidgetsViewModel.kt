package app.minimise.widgets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.HomeOptions
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.TimeSource
import app.minimise.launcher.AppOpener
import app.minimise.widgets.data.BatteryMonitor
import app.minimise.widgets.data.BatteryState
import app.minimise.widgets.data.NextAlarmSource
import app.minimise.widgets.data.NowPlaying
import app.minimise.widgets.data.Track
import app.minimise.widgets.data.WeatherRepository
import app.minimise.widgets.data.WeatherState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Each part is null while it's switched off or has nothing to show. Null [options] until settings load. */
data class HomeWidgetsUiState(
    val options: HomeOptions? = null,
    val battery: BatteryState? = null,
    val nextAlarm: LocalDateTime? = null,
    val now: LocalDateTime? = null,
    val track: Track? = null,
    val weather: WeatherState = WeatherState(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeWidgetsViewModel @Inject constructor(
    settingsStore: SettingsStore,
    batteryMonitor: BatteryMonitor,
    nextAlarmSource: NextAlarmSource,
    private val nowPlaying: NowPlaying,
    private val weatherRepository: WeatherRepository,
    private val appRepository: AppRepository,
    private val appOpener: AppOpener,
    private val timeSource: TimeSource,
) : ViewModel() {
    /** Bumped on resume: notification access may have been allowed meanwhile, so media sessions are asked again. */
    private val resumes = MutableStateFlow(0)

    private val options: Flow<HomeOptions> = settingsStore.settings.map { it.home }.distinctUntilChanged()

    private val battery = options.whenOn({ it.battery }) { batteryMonitor.state }

    private val nextAlarm = options.whenOn({ it.nextAlarm }) {
        nextAlarmSource.next.map { at -> at?.atZone(timeSource.zone())?.toLocalDateTime() }
    }

    private val track = combine(options.map { it.music }.distinctUntilChanged(), resumes) { on, _ -> on }
        .flatMapLatest { on -> if (on) nowPlaying.track else flowOf(null) }

    private val parts = combine(battery, nextAlarm, track) { battery, alarm, track -> Triple(battery, alarm, track) }

    val uiState: StateFlow<HomeWidgetsUiState> =
        combine(options, parts, weatherRepository.state, resumes) { options, (battery, alarm, track), weather, _ ->
            HomeWidgetsUiState(
                options = options,
                battery = battery,
                nextAlarm = alarm,
                now = timeSource.now().atZone(timeSource.zone()).toLocalDateTime(),
                track = track,
                weather = weather,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeWidgetsUiState())

    init {
        // Switching weather or air quality on fetches at once, rather than at the next visit to home.
        options.map { it.weather to it.airQuality }
            .distinctUntilChanged()
            .onEach { (weather, airQuality) -> weatherRepository.refresh(weather, airQuality) }
            .launchIn(viewModelScope)
    }

    fun onResume() {
        resumes.value++
        refreshWeather(forced = false)
    }

    /** Tapping the weather asks again (at most once a minute), and is the way to retry after a failure. */
    fun onWeatherTapped() = refreshWeather(forced = true)

    fun onLocationPermissionResult() = refreshWeather(forced = true)

    /** The player opens through the gate like any other app, so a Vault player still pauses first. */
    fun onTrackTapped() {
        val packageName = uiState.value.track?.packageName ?: return
        viewModelScope.launch { appRepository.entry(packageName)?.let { appOpener.open(it) } }
    }

    fun onPlayPause() {
        uiState.value.track?.let { nowPlaying.playPause(it.packageName) }
    }

    fun onNext() {
        uiState.value.track?.let { nowPlaying.next(it.packageName) }
    }

    fun onPrevious() {
        uiState.value.track?.let { nowPlaying.previous(it.packageName) }
    }

    private fun refreshWeather(forced: Boolean) {
        viewModelScope.launch {
            val home = options.first()
            weatherRepository.refresh(home.weather, home.airQuality, forced)
        }
    }

    /** [source] while [isOn], null otherwise, so nothing is watched for a part that's switched off. */
    private fun <T> Flow<HomeOptions>.whenOn(isOn: (HomeOptions) -> Boolean, source: () -> Flow<T?>): Flow<T?> =
        map(isOn).distinctUntilChanged().flatMapLatest { on -> if (on) source() else flowOf(null) }
}
