package app.minimise.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.Defaults
import app.minimise.core.time.DayBoundary
import app.minimise.tasks.domain.TaskRepository
import app.minimise.usage.domain.AutoTighten
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Collections
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property drafts the day's tasks being edited, most important first. Home shows the top
 * [Defaults.HOME_TASKS] still to do.
 */
data class TodayUiState(
    val drafts: List<String> = List(Defaults.HOME_TASKS) { "" },
    val loaded: Boolean = false,
    val finished: Boolean = false,
    /** After three days over budget: a smaller budget on offer (minutes), until answered. */
    val tightenOffer: Int? = null,
) {
    val canAdd: Boolean get() = drafts.size < Defaults.MAX_TODAY_TASKS
}

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
                // At least the three home shows, so a new day starts with three empty lines.
                val count = maxOf(existing.size, Defaults.HOME_TASKS)
                state.copy(drafts = List(count) { existing.getOrElse(it) { "" } }, loaded = true)
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

    fun onAdd() {
        _uiState.update { state -> if (state.canAdd) state.copy(drafts = state.drafts + "") else state }
    }

    /** Swaps a task with the one above it ([up]) or below it. */
    fun onMove(index: Int, up: Boolean) {
        val target = if (up) index - 1 else index + 1
        _uiState.update { state ->
            if (index !in state.drafts.indices || target !in state.drafts.indices) return@update state
            state.copy(drafts = state.drafts.toMutableList().also { Collections.swap(it, index, target) })
        }
    }

    /** Removes a line. The last few stay as empty lines, so there's always somewhere to type. */
    fun onRemove(index: Int) {
        _uiState.update { state ->
            if (index !in state.drafts.indices) return@update state
            val left = state.drafts.toMutableList().also { it.removeAt(index) }
            state.copy(drafts = if (left.size < Defaults.HOME_TASKS) left + "" else left)
        }
    }

    fun onSave() {
        viewModelScope.launch {
            taskRepository.replaceDay(day, _uiState.value.drafts)
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun onCancel() {
        _uiState.update { it.copy(finished = true) }
    }
}
