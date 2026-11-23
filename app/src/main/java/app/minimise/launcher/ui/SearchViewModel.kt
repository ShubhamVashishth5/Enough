package app.minimise.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.data.AppLauncher
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppRepository
import app.minimise.launcher.domain.AppSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appLauncher: AppLauncher,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private var latestApps: List<AppEntry> = emptyList()

    val uiState: StateFlow<SearchUiState> = combine(appRepository.apps, query) { apps, text ->
        latestApps = apps
        SearchUiState(query = text, results = AppSearch.search(text, apps).matches)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    /** Returns true when the query picked out a single Tool app and it was opened. */
    fun onQueryChange(text: String): Boolean {
        query.value = text
        val autoLaunch = AppSearch.search(text, latestApps).autoLaunch ?: return false
        return open(autoLaunch)
    }

    /** Returns true when the app was opened. */
    fun onAppSelected(app: AppEntry): Boolean = open(app)

    /** The keyboard's Go key opens the top result. */
    fun onSubmit(): Boolean {
        val first = uiState.value.results.firstOrNull() ?: return false
        return open(first)
    }

    private fun open(app: AppEntry): Boolean {
        val opened = appLauncher.launch(app.packageName)
        if (opened) query.value = ""
        return opened
    }
}
