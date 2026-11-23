package app.minimise.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Connects [SearchScreen] to its ViewModel. [onClose] returns to home once an app has been opened. */
@Composable
fun SearchRoute(
    onClose: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onQueryChange = { if (viewModel.onQueryChange(it)) onClose() },
        onAppSelected = { if (viewModel.onAppSelected(it)) onClose() },
        onSubmit = { if (viewModel.onSubmit()) onClose() },
    )
}
