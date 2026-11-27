package app.minimise.modes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.minimise.R
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppFilter
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.time.minuteOfDay
import app.minimise.core.ui.AppFilterEmpty
import app.minimise.core.ui.appFilterHeader
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.WindowFields
import app.minimise.launcher.ui.Destination
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.settings.domain.RemoveMode
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.SaveMode
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property custom one of the user's own modes, which can be renamed ([nameText]) and removed.
 * @property removeScheduled removing it waits 24 hours, since it's on or has hours that tighten.
 */
data class ModeEditUiState(
    val name: String = "",
    val custom: Boolean = false,
    val nameText: String = "",
    val nameTaken: Boolean = false,
    val removeScheduled: Boolean = false,
    val budgetText: String = "",
    val schedule: WeeklyWindow? = null,
    /** Null keeps every app's usual kind. */
    val allowlist: Set<String>? = null,
    val apps: List<AppEntry> = emptyList(),
    val loaded: Boolean = false,
    val result: GuardResult? = null,
)

@HiltViewModel
class ModeEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val modeRepository: ModeRepository,
    private val appRepository: AppRepository,
    private val settingsGuard: SettingsGuard,
) : ViewModel() {
    private val modeId = savedState.toRoute<Destination.ModeEdit>().id
    private val _uiState = MutableStateFlow(ModeEditUiState())
    val uiState: StateFlow<ModeEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val mode = modeRepository.get(modeId) ?: return@launch
            _uiState.value = ModeEditUiState(
                name = mode.name,
                custom = mode.isCustom,
                nameText = mode.name,
                budgetText = mode.values.budget.inWholeMinutes.toString(),
                schedule = mode.schedule,
                allowlist = mode.values.allowlist,
                apps = appRepository.apps.first().filter { it.kind != AppKind.HIDDEN },
                loaded = true,
            )
        }
    }

    fun onNameChange(text: String) = _uiState.update { it.copy(nameText = text.take(Mode.MAX_NAME_LENGTH), nameTaken = false, result = null) }

    fun onBudgetChange(text: String) = _uiState.update { it.copy(budgetText = text.filter(Char::isDigit).take(3), result = null) }

    fun onScheduleToggled() = _uiState.update { it.copy(schedule = if (it.schedule == null) DEFAULT_WINDOW else null, result = null) }

    fun onScheduleChange(window: WeeklyWindow) = _uiState.update { it.copy(schedule = window, result = null) }

    fun onAllowlistToggled() = _uiState.update { it.copy(allowlist = if (it.allowlist == null) emptySet() else null, result = null) }

    fun onAppToggled(app: AppEntry) = _uiState.update { state ->
        val list = state.allowlist ?: return@update state
        state.copy(allowlist = if (app.packageName in list) list - app.packageName else list + app.packageName, result = null)
    }

    fun onSave() {
        val state = _uiState.value
        val budget = state.budgetText.toIntOrNull() ?: return
        val schedule = state.schedule?.takeIf { it.days.isNotEmpty() }
        val name = state.nameText.trim().takeIf { state.custom }
        if (name != null && name.isEmpty()) return
        viewModelScope.launch {
            if (name != null && modeRepository.all().any { it.id != modeId && it.name.equals(name, ignoreCase = true) }) {
                _uiState.update { it.copy(nameTaken = true) }
                return@launch
            }
            val result = settingsGuard.request(
                SaveMode(
                    id = modeId,
                    name = name,
                    budgetMinutes = budget,
                    allowlist = state.allowlist?.sorted(),
                    scheduleDays = schedule?.daysMask() ?: 0,
                    scheduleStartMinute = schedule?.start?.minuteOfDay ?: 0,
                    scheduleEndMinute = schedule?.end?.minuteOfDay ?: 0,
                ),
            )
            _uiState.update { it.copy(result = result) }
        }
    }

    /** One of the user's own modes. Gone at once if nothing loosens; otherwise it waits like any loosen. */
    fun onRemove() {
        if (!_uiState.value.custom) return
        viewModelScope.launch {
            val result = settingsGuard.request(RemoveMode(modeId))
            _uiState.update { it.copy(result = result.takeIf { r -> r == GuardResult.AppliedNow }, removeScheduled = result is GuardResult.Scheduled) }
        }
    }

    private companion object {
        val DEFAULT_WINDOW = WeeklyWindow(
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            start = LocalTime.of(10, 0),
            end = LocalTime.of(18, 0),
        )
    }
}

@Composable
fun ModeEditRoute(onSaved: () -> Unit, viewModel: ModeEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.result) { if (state.result == GuardResult.AppliedNow) onSaved() }
    var query by rememberSaveable { mutableStateOf("") }
    val apps = remember(state.apps, query) { AppFilter.filter(state.apps, query) { it.label } }
    val searching = query.isNotBlank() && state.allowlist != null
    val listState = rememberLazyListState()
    ScreenScaffold(title = state.name) {
        if (!state.loaded) return@ScreenScaffold
        LazyColumn(state = listState) {
            // The rest of the form steps aside while searching, so matches sit right under the box.
            if (!searching) item(key = "form") {
                if (state.custom) {
                    OutlinedTextField(
                        value = state.nameText,
                        onValueChange = viewModel::onNameChange,
                        singleLine = true,
                        isError = state.nameTaken,
                        supportingText = if (state.nameTaken) {
                            { Text(stringResource(R.string.mode_name_taken)) }
                        } else {
                            null
                        },
                        label = { Text(stringResource(R.string.mode_name_label)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = state.budgetText,
                    onValueChange = viewModel::onBudgetChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.mode_budget_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                CheckRow(stringResource(R.string.mode_on_schedule), state.schedule != null, viewModel::onScheduleToggled)
                state.schedule?.let { WindowFields(window = it, onChange = viewModel::onScheduleChange) }
                CheckRow(stringResource(R.string.mode_only_apps), state.allowlist != null, viewModel::onAllowlistToggled)
            }
            state.allowlist?.let { allowed ->
                appFilterHeader(query = query, onQueryChange = { query = it }, listState = listState)
                items(apps, key = { it.packageName }) { app ->
                    CheckRow(app.label, app.packageName in allowed, { viewModel.onAppToggled(app) }, indent = true)
                }
                if (apps.isEmpty() && query.isNotBlank()) item(key = "none") { AppFilterEmpty(query, modifier = Modifier.padding(start = 16.dp)) }
            }
            item {
                if (state.result is GuardResult.Scheduled) {
                    Text(stringResource(R.string.mode_saved_later), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                }
                OutlinedButton(
                    onClick = viewModel::onSave,
                    enabled = state.budgetText.isNotEmpty() && (!state.custom || state.nameText.isNotBlank()),
                    modifier = Modifier.padding(vertical = 16.dp),
                ) {
                    Text(stringResource(R.string.swap_save))
                }
                if (state.custom) {
                    TextButton(onClick = viewModel::onRemove) { Text(stringResource(R.string.mode_remove)) }
                    if (state.removeScheduled) {
                        Text(stringResource(R.string.mode_removed_later), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit, indent: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = if (indent) 16.dp else 0.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
