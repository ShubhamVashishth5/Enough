package app.minimise.launcher.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.minimise.R
import app.minimise.widgets.ui.HomeWidgets
import app.minimise.launcher.QuickTools
import app.minimise.calendar.ui.NextEventLine
import app.minimise.digest.ui.DigestHomeLine
import app.minimise.focus.ui.FocusSection
import app.minimise.protection.ui.AppLockHomeLine
import kotlinx.coroutines.flow.Flow

/** Connects [HomeScreen] to its ViewModel and to the activity (role request, Back, Home presses). */
@Composable
fun HomeRoute(
    homePresses: Flow<Unit>,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenDigest: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenInbox: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
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
    HideStatusBar(hidden = state.options.hideStatusBar)

    HomeScreen(
        state = state,
        onSlotTapped = viewModel::onSlotTapped,
        onSlotLongPressed = viewModel::onSlotLongPressed,
        onMakeDefaultHome = { roleRequest.launch(viewModel.homeRoleRequestIntent()) },
        onSlotAppPicked = viewModel::onSlotAppPicked,
        onSlotCleared = viewModel::onSlotCleared,
        onPickerDismissed = viewModel::onPickerDismissed,
        onSwipeUp = onOpenSearch,
        onSwipeDown = { if (!viewModel.onSwipeDown()) toast(context, R.string.home_shade_unavailable) },
        onSwipeLeft = onOpenCapture,
        onSwipeRight = { QuickTools.openCamera(context) },
        onLock = { if (!viewModel.onLock()) toast(context, R.string.home_lock_unavailable) },
        onClockLongPress = { QuickTools.openTimer(context) },
        onLongPressBackground = onOpenSettings,
        onFixProtection = onOpenPermissions,
        onTaskTapped = viewModel::onTaskTapped,
        onEditTasks = onOpenToday,
        onShortcutTapped = viewModel::onShortcutTapped,
        onShortcutLongPressed = viewModel::onShortcutLongPressed,
        onShortcutAppPicked = viewModel::onShortcutAppPicked,
        onShortcutRemoved = viewModel::onShortcutRemoved,
        onShortcutPickerDismissed = viewModel::onShortcutPickerDismissed,
        focusSection = { FocusSection(onOpenInbox = onOpenInbox) },
        digestLine = { DigestHomeLine(onOpenDigest = onOpenDigest) },
        eventLine = { NextEventLine() },
        appLockLine = { AppLockHomeLine() },
        widgets = { HomeWidgets() },
    )
}

/**
 * While home is on screen (and only then), hides the status bar. A swipe in from the top edge shows it
 * for a moment, as in a full-screen video.
 */
@Composable
private fun HideStatusBar(hidden: Boolean) {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    DisposableEffect(window, view, hidden) {
        val controller = WindowCompat.getInsetsController(window, view)
        if (hidden) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose { if (hidden) controller.show(WindowInsetsCompat.Type.statusBars()) }
    }
}

private fun toast(context: Context, text: Int) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
