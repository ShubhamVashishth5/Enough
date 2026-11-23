package app.minimise.apps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppsUiState(val apps: List<AppEntry> = emptyList())

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {
    val uiState: StateFlow<AppsUiState> = appRepository.apps
        .map { AppsUiState(apps = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    fun onKindChosen(app: AppEntry, kind: AppKind) {
        if (app.kind == kind) return
        viewModelScope.launch { appRepository.setKind(app.packageName, kind) }
    }
}
