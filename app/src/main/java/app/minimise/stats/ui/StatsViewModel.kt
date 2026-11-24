package app.minimise.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppRepository
import app.minimise.apps.domain.WebTarget
import app.minimise.stats.domain.StatsAccess
import app.minimise.stats.domain.WeeklyReport
import app.minimise.stats.domain.WeeklyReportProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StatsUiState {
    data object Loading : StatsUiState

    /** Already opened once today. */
    data object ComeBackTomorrow : StatsUiState

    /** @property sinks top time sinks with their labels. */
    data class Ready(val report: WeeklyReport, val sinks: List<Pair<String, Duration>>) : StatsUiState
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    access: StatsAccess,
    provider: WeeklyReportProvider,
    appRepository: AppRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = if (access.claim()) {
                val report = provider.build()
                val sinks = report.topSinks.map { sink ->
                    val label = appRepository.entry(sink.packageName)?.label
                        ?: if (WebTarget.isWebTarget(sink.packageName)) WebTarget.domainOf(sink.packageName) else sink.packageName
                    label to sink.time
                }
                StatsUiState.Ready(report, sinks)
            } else {
                StatsUiState.ComeBackTomorrow
            }
        }
    }
}
