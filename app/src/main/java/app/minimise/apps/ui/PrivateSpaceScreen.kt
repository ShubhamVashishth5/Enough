package app.minimise.apps.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.minimise.R
import app.minimise.apps.domain.PrivateSpace
import app.minimise.core.ui.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** @property exists Android 15+ with a Private space set up. */
data class PrivateSpaceUiState(val exists: Boolean = false, val locked: Boolean = true)

@HiltViewModel
class PrivateSpaceViewModel @Inject constructor(
    private val privateSpace: PrivateSpace,
) : ViewModel() {
    private val _uiState = MutableStateFlow(read())
    val uiState: StateFlow<PrivateSpaceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { privateSpace.changes.collect { _uiState.value = read() } }
    }

    fun onResume() {
        _uiState.value = read()
    }

    /** Locking applies at once; unlocking goes through the system's own prompt. */
    fun onToggle() {
        privateSpace.setLocked(!_uiState.value.locked)
        _uiState.value = read()
    }

    private fun read() = PrivateSpaceUiState(exists = privateSpace.exists(), locked = privateSpace.isLocked())
}

@Composable
fun PrivateSpaceRoute(viewModel: PrivateSpaceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    ScreenScaffold(title = stringResource(R.string.private_space_title)) {
        Text(
            text = stringResource(R.string.private_space_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        if (!state.exists) {
            Text(stringResource(R.string.private_space_none), style = MaterialTheme.typography.bodyLarge)
            return@ScreenScaffold
        }
        Text(
            text = stringResource(if (state.locked) R.string.private_space_locked else R.string.private_space_unlocked),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedButton(onClick = viewModel::onToggle, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(if (state.locked) R.string.private_space_unlock else R.string.private_space_lock))
        }
    }
}
