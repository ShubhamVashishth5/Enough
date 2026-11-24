package app.minimise.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.focus.ui.FocusSection
import kotlinx.coroutines.flow.Flow

/** Connects [HomeScreen] to its ViewModel and to the activity (role request, Back, Home presses). */
@Composable
fun HomeRoute(
    homePresses: Flow<Unit>,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenToday: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onResume()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LaunchedEffect(homePresses) { homePresses.collect { viewModel.onHomePressed() } }
    LaunchedEffect(state.promptToday) {
        if (state.promptToday) {
            viewModel.onTodayPromptShown()
            onOpenToday()
        }
    }
    // Back does nothing on the home screen.
    BackHandler {}

    HomeScreen(
        state = state,
        onSlotTapped = viewModel::onSlotTapped,
        onSlotLongPressed = viewModel::onSlotLongPressed,
        onMakeDefaultHome = { roleRequest.launch(viewModel.homeRoleRequestIntent()) },
        onSlotAppPicked = viewModel::onSlotAppPicked,
        onSlotCleared = viewModel::onSlotCleared,
        onPickerDismissed = viewModel::onPickerDismissed,
        onSwipeUp = onOpenSearch,
        onLongPressBackground = onOpenSettings,
        onFixProtection = onOpenPermissions,
        onTaskTapped = viewModel::onTaskTapped,
        onEditTasks = onOpenToday,
        focusSection = { FocusSection() },
    )
}
