package app.minimise.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.Defaults
import app.minimise.core.time.DayBoundary
import app.minimise.tasks.domain.TaskRepository
import app.minimise.usage.domain.AutoTighten
import app.minimise.usage.domain.EarnIt
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Collections
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One line on the Today screen. [done] can be unticked here, even once the task has left home. */
data class TaskDraft(val text: String = "", val done: Boolean = false)

/**
 * @property drafts the day's tasks being edited, most important first. Home shows the top
 * [Defaults.HOME_TASKS] still to do.
 */
data class TodayUiState(
    val drafts: List<TaskDraft> = List(Defaults.HOME_TASKS) { TaskDraft() },
    val loaded: Boolean = false,
    val finished: Boolean = false,
    /** After three days over budget: a smaller budget on offer (minutes), until answered. */
    val tightenOffer: Int? = null,
    /** Yesterday's tasks left undone, in their order. Only yesterday's: older ones are let go. */
    val yesterday: List<String> = emptyList(),
) {
    val canAdd: Boolean get() = drafts.size < Defaults.MAX_TODAY_TASKS

    /** What's left of [yesterday] once the ones already carried over (or typed again) are taken out. */
    val leftovers: List<String> get() = yesterday.filter { text -> drafts.none { it.text.trim() == text } }

    /** A carried task fills the first empty line, or a new one while there's room. */
    val canCarryOver: Boolean get() = canAdd || drafts.any { it.text.isBlank() }

    /** [text] brought into today as a new task, or this state unchanged when today is full. */
    fun carriedOver(text: String): TodayUiState {
        val blank = drafts.indexOfFirst { it.text.isBlank() }
        return when {
            blank >= 0 -> copy(drafts = drafts.toMutableList().also { it[blank] = TaskDraft(text) })
            canAdd -> copy(drafts = drafts + TaskDraft(text))
            else -> this
        }
    }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val autoTighten: AutoTighten,
    private val earnIt: EarnIt,
    dayBoundary: DayBoundary,
) : ViewModel() {
    private val day = dayBoundary.today()
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = taskRepository.tasksFor(day).map { TaskDraft(it.text, it.done) }
            val leftUndone = taskRepository.tasksFor(day.minusDays(1)).filter { !it.done }.map { it.text }
            _uiState.update { state ->
                // At least the three home shows, so a new day starts with three empty lines.
                val count = maxOf(existing.size, Defaults.HOME_TASKS)
                state.copy(drafts = List(count) { existing.getOrElse(it) { TaskDraft() } }, yesterday = leftUndone, loaded = true)
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

    fun onDraftChange(index: Int, text: String) = updateDraft(index) { it.copy(text = text) }

    /** Ticks a task off or back on. Saved with the rest. */
    fun onDoneToggled(index: Int) = updateDraft(index) { it.copy(done = !it.done) }

    fun onAdd() {
        _uiState.update { state -> if (state.canAdd) state.copy(drafts = state.drafts + TaskDraft()) else state }
    }

    /**
     * Brings one of yesterday's undone tasks into today, as a new task. Yesterday's stays as it was, so
     * the day's record doesn't change.
     */
    fun onCarryOver(text: String) {
        _uiState.update { it.carriedOver(text) }
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
            state.copy(drafts = if (left.size < Defaults.HOME_TASKS) left + TaskDraft() else left)
        }
    }

    /**
     * Saves the texts and order, then the ticks: the saved tasks line up with the non-blank lines, in
     * order. A task ticked off here earns its minutes as it would on home.
     */
    fun onSave() {
        viewModelScope.launch {
            val lines = _uiState.value.drafts.filter { it.text.isNotBlank() }
            taskRepository.replaceDay(day, lines.map { it.text })
            taskRepository.tasksFor(day).zip(lines).forEach { (task, line) ->
                if (task.done != line.done) {
                    taskRepository.setDone(task.id, line.done)
                    if (line.done) earnIt.onTaskDone(task.id)
                }
            }
            _uiState.update { it.copy(finished = true) }
        }
    }

    private fun updateDraft(index: Int, change: (TaskDraft) -> TaskDraft) {
        _uiState.update { state ->
            if (index !in state.drafts.indices) return@update state
            state.copy(drafts = state.drafts.toMutableList().also { it[index] = change(it[index]) })
        }
    }

    fun onCancel() {
        _uiState.update { it.copy(finished = true) }
    }
}
