package app.minimise.modes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.time.minuteOfDay
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.WindowFields
import app.minimise.launcher.ui.Destination
import app.minimise.modes.domain.ModeRepository
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

data class ModeEditUiState(
    val name: String = "",
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
                budgetText = mode.values.budget.inWholeMinutes.toString(),
                schedule = mode.schedule,
                allowlist = mode.values.allowlist,
                apps = appRepository.apps.first().filter { it.kind != AppKind.HIDDEN },
                loaded = true,
            )
        }
    }

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
        viewModelScope.launch {
            val result = settingsGuard.request(
                SaveMode(
                    id = modeId,
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
    ScreenScaffold(title = state.name) {
        if (!state.loaded) return@ScreenScaffold
        LazyColumn {
            item {
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
                items(state.apps, key = { it.packageName }) { app ->
                    CheckRow(app.label, app.packageName in allowed, { viewModel.onAppToggled(app) }, indent = true)
                }
            }
            item {
                if (state.result is GuardResult.Scheduled) {
                    Text(stringResource(R.string.mode_saved_later), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                }
                OutlinedButton(onClick = viewModel::onSave, enabled = state.budgetText.isNotEmpty(), modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(stringResource(R.string.swap_save))
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
