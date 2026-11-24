package app.minimise.focus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusSettingsUiState(val apps: List<AppEntry> = emptyList(), val allowed: Set<String> = emptySet())

/** The focus allowlist. It isn't a rule setting (sessions are opt-in), so changes apply at once. */
@HiltViewModel
class FocusSettingsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val uiState: StateFlow<FocusSettingsUiState> = combine(appRepository.apps, settingsStore.settings) { apps, settings ->
        val visible = apps.filter { it.kind != AppKind.HIDDEN }
        FocusSettingsUiState(
            apps = visible,
            allowed = settings.focusAllowlist ?: defaultAllowlist(visible),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusSettingsUiState())

    fun onToggled(app: AppEntry) {
        val current = uiState.value.allowed
        val next = if (app.packageName in current) current - app.packageName else current + app.packageName
        viewModelScope.launch { settingsStore.update { it.copy(focusAllowlist = next) } }
    }

    private fun defaultAllowlist(apps: List<AppEntry>): Set<String> =
        apps.filter { it.homeSlot != null && it.kind == AppKind.TOOL }.map { it.packageName }.toSet()
}

@Composable
fun FocusSettingsRoute(viewModel: FocusSettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.focus_settings_title)) {
        Text(
            text = stringResource(R.string.focus_settings_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn {
            items(state.apps, key = { it.packageName }) { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.onToggled(app) },
                ) {
                    Checkbox(checked = app.packageName in state.allowed, onCheckedChange = { viewModel.onToggled(app) })
                    Text(app.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
