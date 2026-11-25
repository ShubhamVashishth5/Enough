package app.minimise.launcher.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.minimise.apps.ui.AppsRoute
import app.minimise.onboarding.ui.OnboardingRoute
import app.minimise.settings.permissions.PermissionsRoute
import app.minimise.settings.ui.BudgetRoute
import app.minimise.settings.ui.PendingChangesRoute
import app.minimise.usage.ui.DebugUsageRoute
import app.minimise.settings.ui.SettingsRoute
import app.minimise.settings.ui.HomeSettingsRoute
import app.minimise.swaps.ui.SwapsRoute
import app.minimise.schedules.ui.SchedulesRoute
import app.minimise.keys.ui.KeysRoute
import app.minimise.focus.ui.FocusSettingsRoute
import app.minimise.modes.ui.ModeEditRoute
import app.minimise.modes.ui.ModesRoute
import app.minimise.digest.ui.DigestRoute
import app.minimise.digest.ui.DigestSettingsRoute
import app.minimise.capture.ui.CaptureRoute
import app.minimise.capture.ui.InboxLink
import app.minimise.capture.ui.InboxRoute
import app.minimise.tasks.ui.TodayRoute
import app.minimise.feeds.ui.FeedsRoute
import app.minimise.settings.ui.ApplyNowRoute
import app.minimise.settings.ui.LockRoute
import app.minimise.stats.ui.StatsRoute
import app.minimise.modes.ui.WindDownRoute
import app.minimise.apps.ui.PrivateSpaceRoute
import app.minimise.unlock.ui.FriendKeyRoute
import app.minimise.core.ui.dateTimeText
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import app.minimise.R
import app.minimise.BuildConfig
import kotlinx.coroutines.flow.Flow

@Composable
fun LauncherNavHost(
    homePresses: Flow<Unit>,
    openRequests: Flow<Destination>,
    onOpenRequestHandled: () -> Unit,
    launcherViewModel: LauncherViewModel = hiltViewModel(),
) {
    val onboardingDone by launcherViewModel.onboardingDone.collectAsStateWithLifecycle()
    // Wait for settings, then fix the start destination once: finishing onboarding navigates away itself.
    val done = onboardingDone ?: return Box(Modifier.fillMaxSize())
    val startDestination: Destination = remember { if (done) Destination.Home else Destination.Onboarding }
    val navController = rememberNavController()
    val openToday = { navController.navigate(Destination.Today) { launchSingleTop = true } }

    // Whatever screen asked for it, say why a loosening change did nothing.
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        launcherViewModel.refusals.collect { until ->
            Toast.makeText(context, context.getString(R.string.lock_refused, dateTimeText(context, until)), Toast.LENGTH_LONG).show()
        }
    }

    // The Home button always lands on the home screen itself.
    LaunchedEffect(homePresses) {
        homePresses.collect { navController.popBackStack(Destination.Home, inclusive = false) }
    }
    LaunchedEffect(openRequests) {
        openRequests.collect { destination ->
            onOpenRequestHandled()
            navController.popBackStack(Destination.Home, inclusive = false)
            navController.navigate(destination) { launchSingleTop = true }
        }
    }

    // Opening a screen is instant. Going back slides the screen off to the right, over the one beneath,
    // which stays still: with the Back gesture it follows the finger.
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { width -> width }) },
    ) {
        screen<Destination.Onboarding> {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(Destination.Home) { popUpTo<Destination.Onboarding> { inclusive = true } }
                },
            )
        }
        screen<Destination.Home> {
            HomeRoute(
                homePresses = homePresses,
                onOpenSearch = { navController.navigate(Destination.Search) { launchSingleTop = true } },
                onOpenSettings = { navController.navigate(Destination.Settings) { launchSingleTop = true } },
                onOpenPermissions = { navController.navigate(Destination.Permissions) { launchSingleTop = true } },
                onOpenToday = openToday,
                onOpenDigest = { navController.navigate(Destination.Digest) { launchSingleTop = true } },
                onOpenCapture = { navController.navigate(Destination.Capture) { launchSingleTop = true } },
                onOpenInbox = { navController.navigate(Destination.Inbox) { launchSingleTop = true } },
            )
        }
        screen<Destination.Today> {
            TodayRoute(onClose = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        screen<Destination.Search> {
            SearchRoute(onClose = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        screen<Destination.Settings> {
            SettingsRoute(
                onOpenApps = { navController.navigate(Destination.Apps) },
                onOpenHome = { navController.navigate(Destination.HomeSettings) },
                onOpenBudget = { navController.navigate(Destination.Budget) },
                onOpenSwaps = { navController.navigate(Destination.Swaps) },
                onOpenSchedules = { navController.navigate(Destination.Schedules) },
                onOpenKeys = { navController.navigate(Destination.Keys) },
                onOpenFocus = { navController.navigate(Destination.Focus) },
                onOpenModes = { navController.navigate(Destination.Modes) },
                onOpenDigest = { navController.navigate(Destination.DigestSettings) },
                onOpenFeeds = { navController.navigate(Destination.Feeds) },
                onOpenLock = { navController.navigate(Destination.Lock) },
                onOpenStats = { navController.navigate(Destination.Stats) },
                onOpenWindDown = { navController.navigate(Destination.WindDown) },
                onOpenPrivateSpace = { navController.navigate(Destination.PrivateSpace) },
                onOpenFriendKey = { navController.navigate(Destination.FriendKey) },
                onOpenPendingChanges = { navController.navigate(Destination.PendingChanges) },
                onOpenPermissions = { navController.navigate(Destination.Permissions) },
                onOpenDebugUsage = { navController.navigate(Destination.DebugUsage) },
            )
        }
        screen<Destination.Apps> {
            AppsRoute()
        }
        screen<Destination.HomeSettings> {
            HomeSettingsRoute(onOpenPermissions = { navController.navigate(Destination.Permissions) })
        }
        screen<Destination.Budget> {
            BudgetRoute()
        }
        screen<Destination.Swaps> {
            SwapsRoute()
        }
        screen<Destination.Schedules> {
            SchedulesRoute()
        }
        screen<Destination.Keys> {
            KeysRoute()
        }
        screen<Destination.Focus> {
            FocusSettingsRoute(onOpenPermissions = { navController.navigate(Destination.Permissions) })
        }
        screen<Destination.Modes> {
            ModesRoute(onEdit = { navController.navigate(Destination.ModeEdit(it)) })
        }
        screen<Destination.Digest> {
            DigestRoute(extra = { InboxLink(onOpenInbox = { navController.navigate(Destination.Inbox) }) })
        }
        screen<Destination.Capture> {
            CaptureRoute(onDone = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        screen<Destination.Inbox> {
            InboxRoute()
        }
        screen<Destination.DigestSettings> {
            DigestSettingsRoute(onOpenPermissions = { navController.navigate(Destination.Permissions) })
        }
        screen<Destination.ModeEdit> {
            ModeEditRoute(onSaved = { navController.popBackStack() })
        }
        screen<Destination.Feeds> {
            FeedsRoute()
        }
        screen<Destination.PrivateSpace> {
            PrivateSpaceRoute()
        }
        screen<Destination.WindDown> {
            WindDownRoute()
        }
        screen<Destination.Stats> {
            StatsRoute()
        }
        screen<Destination.Lock> {
            LockRoute()
        }
        screen<Destination.FriendKey> {
            FriendKeyRoute()
        }
        screen<Destination.ApplyNow> {
            ApplyNowRoute(onDone = { navController.popBackStack() })
        }
        screen<Destination.PendingChanges> {
            PendingChangesRoute(onApplyNow = { navController.navigate(Destination.ApplyNow(it)) })
        }
        screen<Destination.Permissions> {
            PermissionsRoute()
        }
        // Debug screens aren't registered at all in release builds, so R8 strips them.
        if (BuildConfig.DEBUG) {
            screen<Destination.DebugUsage> {
                DebugUsageRoute()
            }
        }
    }
}

/**
 * A destination that paints its own background. Screens used to be see-through over the activity's,
 * so while one was leaving (above all during the Back gesture) its text showed on top of the screen
 * beneath it.
 */
private inline fun <reified T : Any> NavGraphBuilder.screen(noinline content: @Composable (NavBackStackEntry) -> Unit) {
    composable<T> { entry ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content(entry) }
    }
}
