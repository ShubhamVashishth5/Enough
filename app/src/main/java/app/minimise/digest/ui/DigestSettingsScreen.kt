package app.minimise.digest.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.digest.domain.DigestScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DigestSettingsUiState(val enabled: Boolean = false, val apps: List<AppEntry> = emptyList(), val exempt: Set<String> = emptySet())

/** Notifications aren't a gate rule, so these settings apply at once. */
@HiltViewModel
class DigestSettingsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val scheduler: DigestScheduler,
) : ViewModel() {
    val uiState: StateFlow<DigestSettingsUiState> = combine(appRepository.apps, settingsStore.settings) { apps, settings ->
        DigestSettingsUiState(enabled = settings.digestEnabled, apps = apps, exempt = settings.digestExempt)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DigestSettingsUiState())

    fun onEnabledToggled() {
        viewModelScope.launch {
            settingsStore.update { it.copy(digestEnabled = !it.digestEnabled) }
            scheduler.scheduleNext()
        }
    }

    fun onExemptToggled(app: AppEntry) {
        viewModelScope.launch {
            settingsStore.update {
                val exempt = it.digestExempt
                it.copy(digestExempt = if (app.packageName in exempt) exempt - app.packageName else exempt + app.packageName)
            }
        }
    }
}

@Composable
fun DigestSettingsRoute(onOpenPermissions: () -> Unit, viewModel: DigestSettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.digest_settings_title)) {
        Text(
            text = stringResource(R.string.digest_settings_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsRow(
            title = stringResource(R.string.digest_settings_enabled),
            summary = stringResource(if (state.enabled) R.string.debug_on else R.string.debug_off),
            onClick = viewModel::onEnabledToggled,
        )
        SettingsRow(
            title = stringResource(R.string.settings_permissions),
            summary = stringResource(R.string.digest_settings_permissions),
            onClick = onOpenPermissions,
        )
        Text(stringResource(R.string.digest_settings_exempt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        LazyColumn {
            items(state.apps, key = { it.packageName }) { app ->
                CheckRow(label = app.label, checked = app.packageName in state.exempt, onToggle = { viewModel.onExemptToggled(app) })
            }
        }
    }
}
