package app.minimise.protection.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.minimise.R
import app.minimise.protection.AppLockSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property toLock Vault apps to put behind the system app lock while the gate can't see everything.
 * @property toUnlock apps Enough locked that no longer need it.
 * @property request the system screen to show next, if the user started going through them.
 */
data class AppLockUiState(
    val toLock: Int = 0,
    val toUnlock: Int = 0,
    val request: AppLockSync.Request? = null,
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val sync: AppLockSync,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    private var showing: AppLockSync.Request? = null

    fun onResume() {
        viewModelScope.launch {
            val plan = sync.plan()
            _uiState.update { it.copy(toLock = plan.toLock.size, toUnlock = plan.toUnlock.size) }
        }
    }

    /** Goes through the system screens one app at a time. */
    fun onTapped() {
        viewModelScope.launch { _uiState.update { it.copy(request = sync.next()) } }
    }

    fun onRequestShown(request: AppLockSync.Request) {
        showing = request
        _uiState.update { it.copy(request = null) }
    }

    /** A confirmed change moves on to the next app; backing out stops there. */
    fun onResult(confirmed: Boolean) {
        val request = showing ?: return
        showing = null
        viewModelScope.launch {
            sync.onDone(request)
            if (confirmed) onTapped()
            onResume()
        }
    }
}

/** "Put 3 Vault apps behind the phone's app lock" while protection is below Full, where Android has one. */
@Composable
fun AppLockHomeLine(viewModel: AppLockViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onResult(confirmed = result.resultCode == Activity.RESULT_OK)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LaunchedEffect(state.request) {
        val request = state.request ?: return@LaunchedEffect
        viewModel.onRequestShown(request)
        launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
    }
    val text = when {
        state.toLock > 0 -> pluralStringResource(R.plurals.home_app_lock_add, state.toLock, state.toLock)
        state.toUnlock > 0 -> pluralStringResource(R.plurals.home_app_lock_remove, state.toUnlock, state.toUnlock)
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = viewModel::onTapped)
            .padding(vertical = 6.dp),
    )
}
