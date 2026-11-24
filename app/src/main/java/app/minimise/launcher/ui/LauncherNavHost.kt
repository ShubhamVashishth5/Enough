package app.minimise.launcher.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<Destination.Onboarding> {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(Destination.Home) { popUpTo<Destination.Onboarding> { inclusive = true } }
                },
            )
        }
        composable<Destination.Home> {
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
        composable<Destination.Today> {
            TodayRoute(onClose = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        composable<Destination.Search> {
            SearchRoute(onClose = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        composable<Destination.Settings> {
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
        composable<Destination.Apps> {
            AppsRoute()
        }
        composable<Destination.HomeSettings> {
            HomeSettingsRoute(onOpenPermissions = { navController.navigate(Destination.Permissions) })
        }
        composable<Destination.Budget> {
            BudgetRoute()
        }
        composable<Destination.Swaps> {
            SwapsRoute()
        }
        composable<Destination.Schedules> {
            SchedulesRoute()
        }
        composable<Destination.Keys> {
            KeysRoute()
        }
        composable<Destination.Focus> {
            FocusSettingsRoute(onOpenPermissions = { navController.navigate(Destination.Permissions) })
        }
        composable<Destination.Modes> {
            ModesRoute(onEdit = { navController.navigate(Destination.ModeEdit(it)) })
        }
        composable<Destination.Digest> {
            DigestRoute(extra = { InboxLink(onOpenInbox = { navController.navigate(Destination.Inbox) }) })
        }
        composable<Destination.Capture> {
            CaptureRoute(onDone = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        composable<Destination.Inbox> {
            InboxRoute()
        }
        composable<Destination.DigestSettings> {
            DigestSettingsRoute(onOpenPermissions = { navController.navigate(Destination.Permissions) })
        }
        composable<Destination.ModeEdit> {
            ModeEditRoute(onSaved = { navController.popBackStack() })
        }
        composable<Destination.Feeds> {
            FeedsRoute()
        }
        composable<Destination.PrivateSpace> {
            PrivateSpaceRoute()
        }
        composable<Destination.WindDown> {
            WindDownRoute()
        }
        composable<Destination.Stats> {
            StatsRoute()
        }
        composable<Destination.Lock> {
            LockRoute()
        }
        composable<Destination.FriendKey> {
            FriendKeyRoute()
        }
        composable<Destination.ApplyNow> {
            ApplyNowRoute(onDone = { navController.popBackStack() })
        }
        composable<Destination.PendingChanges> {
            PendingChangesRoute(onApplyNow = { navController.navigate(Destination.ApplyNow(it)) })
        }
        composable<Destination.Permissions> {
            PermissionsRoute()
        }
        // Debug screens aren't registered at all in release builds, so R8 strips them.
        if (BuildConfig.DEBUG) {
            composable<Destination.DebugUsage> {
                DebugUsageRoute()
            }
        }
    }
}
