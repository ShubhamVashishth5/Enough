package app.minimise.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppRepository
import app.minimise.launcher.AppOpener
import app.minimise.launcher.domain.AppSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    appRepository: AppRepository,
    private val appOpener: AppOpener,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val finished = MutableStateFlow(false)
    private var latestApps: List<AppEntry> = emptyList()

    val uiState: StateFlow<SearchUiState> = combine(appRepository.apps, query, finished) { apps, text, done ->
        latestApps = apps
        SearchUiState(query = text, results = AppSearch.search(text, apps).matches, finished = done)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(text: String) {
        query.value = text
        AppSearch.search(text, latestApps).autoLaunch?.let(::open)
    }

    fun onAppSelected(app: AppEntry) = open(app)

    /** The keyboard's Go key opens the top result. */
    fun onSubmit() {
        uiState.value.results.firstOrNull()?.let(::open)
    }

    private fun open(app: AppEntry) {
        viewModelScope.launch {
            if (appOpener.open(app)) {
                query.value = ""
                finished.value = true
            }
        }
    }
}
