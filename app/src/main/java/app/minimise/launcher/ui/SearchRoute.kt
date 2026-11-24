package app.minimise.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.core.ui.AppActionsDialog

/** Connects [SearchScreen] to its ViewModel. [onClose] returns to home once an app has been opened. */
@Composable
fun SearchRoute(
    onClose: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onClose() }
    SearchScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onAppSelected = viewModel::onAppSelected,
        onSubmit = viewModel::onSubmit,
        onAppLongPressed = viewModel::onAppLongPressed,
    )
    state.actionsFor?.let { app ->
        AppActionsDialog(
            label = app.label,
            onAppInfo = viewModel::onAppInfo,
            onUninstall = viewModel::onUninstall,
            onDismiss = viewModel::onActionsDismissed,
        )
    }
}
