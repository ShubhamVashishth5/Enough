package app.minimise.focus.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.minimise.core.ui.CheckRow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.minimise.R
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.durationText
import app.minimise.calendar.domain.CalendarFocusScheduler
import app.minimise.calendar.domain.CalendarSource
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.settings.domain.SetCalendarFocus
import app.minimise.settings.domain.SettingsGuard
import kotlin.time.Duration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @property calendarFocus events with "focus" in the title start sessions; [calendarOffIn] while switching off waits.
 * @property calendarAllowed calendar access is granted (calendar focus needs it).
 */
data class FocusSettingsUiState(
    val apps: List<AppEntry> = emptyList(),
    val allowed: Set<String> = emptySet(),
    val calendarFocus: Boolean = false,
    val calendarOffIn: Duration? = null,
    val calendarAllowed: Boolean = false,
)

/** The focus allowlist. It isn't a rule setting (sessions are opt-in), so changes apply at once. */
@HiltViewModel
class FocusSettingsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val settingsGuard: SettingsGuard,
    private val calendar: CalendarSource,
    private val calendarFocusScheduler: CalendarFocusScheduler,
    private val timeSource: TimeSource,
) : ViewModel() {
    val uiState: StateFlow<FocusSettingsUiState> = combine(appRepository.apps, settingsStore.settings, settingsGuard.pending) { apps, settings, pending ->
        val visible = apps.filter { it.kind != AppKind.HIDDEN }
        FocusSettingsUiState(
            apps = visible,
            allowed = settings.focusAllowlist ?: defaultAllowlist(visible),
            calendarFocus = settings.calendarFocus,
            calendarOffIn = pending.firstOrNull { it.change == SetCalendarFocus(false) }?.let { durationBetween(timeSource.now(), it.applyAt) },
            calendarAllowed = calendar.isAvailable(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusSettingsUiState())

    /** On applies at once; off waits like any loosening. Tapping while off waits keeps it on. */
    fun onCalendarFocusTapped() {
        val state = uiState.value
        val enable = !state.calendarFocus || state.calendarOffIn != null
        viewModelScope.launch {
            settingsGuard.request(SetCalendarFocus(enable))
            calendarFocusScheduler.scheduleNext()
        }
    }

    fun onToggled(app: AppEntry) {
        val current = uiState.value.allowed
        val next = if (app.packageName in current) current - app.packageName else current + app.packageName
        viewModelScope.launch { settingsStore.update { it.copy(focusAllowlist = next) } }
    }

    private fun defaultAllowlist(apps: List<AppEntry>): Set<String> =
        apps.filter { it.homeSlot != null && it.kind == AppKind.TOOL }.map { it.packageName }.toSet()
}

@Composable
fun FocusSettingsRoute(onOpenPermissions: () -> Unit, viewModel: FocusSettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.focus_settings_title)) {
        Text(
            text = stringResource(R.string.focus_settings_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        val offIn = state.calendarOffIn
        SettingsRow(
            title = stringResource(R.string.calendar_focus_title),
            summary = when {
                !state.calendarAllowed -> stringResource(R.string.calendar_focus_needs_calendar)
                offIn != null -> stringResource(R.string.calendar_focus_off_in, durationText(offIn))
                state.calendarFocus -> stringResource(R.string.calendar_focus_on)
                else -> stringResource(R.string.calendar_focus_off)
            },
            onClick = if (state.calendarAllowed) viewModel::onCalendarFocusTapped else onOpenPermissions,
        )
        LazyColumn {
            items(state.apps, key = { it.packageName }) { app ->
                CheckRow(label = app.label, checked = app.packageName in state.allowed, onToggle = { viewModel.onToggled(app) })
            }
        }
    }
}
