package app.minimise.swaps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.swaps.domain.Swap
import app.minimise.swaps.domain.SwapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SwapsUiState(
    val rows: List<SwapRow> = emptyList(),
    /** Choices for a swap's app. */
    val toolApps: List<AppEntry> = emptyList(),
    /** The swap being added or edited; null when no editor is open. */
    val editor: SwapEditor? = null,
    val loaded: Boolean = false,
)

/** @property appLabel the Tool app the swap opens; null means it goes home. */
data class SwapRow(val swap: Swap, val appLabel: String?)

/** @property id 0 for a new swap. */
data class SwapEditor(
    val id: Long,
    val text: String,
    val packageName: String?,
    val appLabel: String?,
    val pickingApp: Boolean,
) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = text.isNotBlank()
}

/** The Instead list isn't a rule (Open anyway is always there), so edits apply at once, without SettingsGuard. */
@HiltViewModel
class SwapsViewModel @Inject constructor(
    private val swapRepository: SwapRepository,
    appRepository: AppRepository,
) : ViewModel() {
    private val draft = MutableStateFlow<Draft?>(null)

    val uiState: StateFlow<SwapsUiState> = combine(swapRepository.swaps, appRepository.apps, draft) { swaps, apps, draft ->
        val tools = apps.filter { it.kind == AppKind.TOOL }
        val labelOf = { packageName: String? -> tools.firstOrNull { it.packageName == packageName }?.label }
        SwapsUiState(
            rows = swaps.map { SwapRow(it, labelOf(it.packageName)) },
            toolApps = tools,
            editor = draft?.let { SwapEditor(it.id, it.text, it.packageName, labelOf(it.packageName), it.pickingApp) },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwapsUiState())

    fun onAdd() {
        draft.value = Draft(id = 0, text = "", packageName = null)
    }

    fun onEdit(row: SwapRow) {
        draft.value = Draft(id = row.swap.id, text = row.swap.text, packageName = row.swap.packageName)
    }

    fun onTextChange(text: String) = draft.update { it?.copy(text = text) }

    fun onPickApp() = draft.update { it?.copy(pickingApp = true) }

    fun onAppChosen(app: AppEntry) = draft.update { it?.copy(packageName = app.packageName, pickingApp = false) }

    fun onAppCleared() = draft.update { it?.copy(packageName = null, pickingApp = false) }

    fun onPickerDismissed() = draft.update { it?.copy(pickingApp = false) }

    fun onSave() {
        val current = draft.value ?: return
        if (current.text.isBlank()) return
        draft.value = null
        viewModelScope.launch { swapRepository.save(Swap(current.id, current.text, current.packageName)) }
    }

    fun onDelete() {
        val current = draft.value ?: return
        draft.value = null
        if (current.id != 0L) viewModelScope.launch { swapRepository.delete(current.id) }
    }

    fun onEditorDismissed() {
        draft.value = null
    }

    private data class Draft(val id: Long, val text: String, val packageName: String?, val pickingApp: Boolean = false)
}
