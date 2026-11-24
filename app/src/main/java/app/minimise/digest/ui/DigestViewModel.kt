package app.minimise.digest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppRepository
import app.minimise.digest.domain.DigestRepository
import app.minimise.digest.domain.HeldNotification
import app.minimise.launcher.AppOpener
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DigestUiState(val groups: List<DigestGroup> = emptyList(), val waiting: Int = 0, val loaded: Boolean = false)

data class DigestGroup(val packageName: String, val appLabel: String, val items: List<HeldNotification>)

@HiltViewModel
class DigestViewModel @Inject constructor(
    private val repository: DigestRepository,
    private val appRepository: AppRepository,
    private val appOpener: AppOpener,
) : ViewModel() {
    val uiState: StateFlow<DigestUiState> = combine(repository.released, repository.waitingCount, appRepository.apps) { released, waiting, apps ->
        val labels = apps.associate { it.packageName to it.label }
        DigestUiState(
            groups = released.groupBy { it.packageName }.map { (pkg, items) -> DigestGroup(pkg, labels[pkg] ?: pkg, items) },
            waiting = waiting,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DigestUiState())

    /** Opens the app the update came from, through the gate if it's in the Vault. */
    fun onOpen(item: HeldNotification) {
        viewModelScope.launch {
            val app = appRepository.entry(item.packageName) ?: return@launch
            repository.remove(item.id)
            appOpener.open(app)
        }
    }

    fun onClear() {
        viewModelScope.launch { repository.clearReleased() }
    }
}
