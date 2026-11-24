package app.minimise.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.Defaults
import app.minimise.core.time.DayBoundary
import app.minimise.tasks.domain.TaskRepository
import app.minimise.usage.domain.AutoTighten
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUiState(
    val drafts: List<String> = List(Defaults.MAX_TODAY_TASKS) { "" },
    val loaded: Boolean = false,
    val finished: Boolean = false,
    /** After three days over budget: a smaller budget on offer (minutes), until answered. */
    val tightenOffer: Int? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val autoTighten: AutoTighten,
    dayBoundary: DayBoundary,
) : ViewModel() {
    private val day = dayBoundary.today()
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = taskRepository.tasksFor(day).map { it.text }
            _uiState.update { state ->
                state.copy(
                    drafts = List(Defaults.MAX_TODAY_TASKS) { existing.getOrElse(it) { "" } },
                    loaded = true,
                )
            }
            val offer = autoTighten.offer()
            _uiState.update { it.copy(tightenOffer = offer) }
        }
    }

    /** Only ever offered: the smaller budget applies only if the user says so. */
    fun onTightenAccepted() {
        val minutes = _uiState.value.tightenOffer ?: return
        _uiState.update { it.copy(tightenOffer = null) }
        viewModelScope.launch { autoTighten.accept(minutes) }
    }

    fun onTightenDeclined() {
        _uiState.update { it.copy(tightenOffer = null) }
        viewModelScope.launch { autoTighten.decline() }
    }

    fun onDraftChange(index: Int, text: String) {
        _uiState.update { state -> state.copy(drafts = state.drafts.toMutableList().also { it[index] = text }) }
    }

    fun onSave() {
        viewModelScope.launch {
            taskRepository.replaceDay(day, _uiState.value.drafts)
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun onSkip() {
        _uiState.update { it.copy(finished = true) }
    }
}
