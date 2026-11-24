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
import app.minimise.swaps.ui.SwapsRoute
import app.minimise.schedules.ui.SchedulesRoute
import app.minimise.keys.ui.KeysRoute
import app.minimise.focus.ui.FocusSettingsRoute
import app.minimise.tasks.ui.TodayRoute
import kotlinx.coroutines.flow.Flow

@Composable
fun LauncherNavHost(
    homePresses: Flow<Unit>,
    todayRequests: Flow<Unit>,
    onTodayRequestHandled: () -> Unit,
    launcherViewModel: LauncherViewModel = hiltViewModel(),
) {
    val onboardingDone by launcherViewModel.onboardingDone.collectAsStateWithLifecycle()
    // Wait for settings, then fix the start destination once: finishing onboarding navigates away itself.
    val done = onboardingDone ?: return Box(Modifier.fillMaxSize())
    val startDestination: Destination = remember { if (done) Destination.Home else Destination.Onboarding }
    val navController = rememberNavController()
    val openToday = { navController.navigate(Destination.Today) { launchSingleTop = true } }

    // The Home button always lands on the home screen itself.
    LaunchedEffect(homePresses) {
        homePresses.collect { navController.popBackStack(Destination.Home, inclusive = false) }
    }
    LaunchedEffect(todayRequests) {
        todayRequests.collect {
            onTodayRequestHandled()
            navController.popBackStack(Destination.Home, inclusive = false)
            openToday()
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
                onOpenBudget = { navController.navigate(Destination.Budget) },
                onOpenSwaps = { navController.navigate(Destination.Swaps) },
                onOpenSchedules = { navController.navigate(Destination.Schedules) },
                onOpenKeys = { navController.navigate(Destination.Keys) },
                onOpenFocus = { navController.navigate(Destination.Focus) },
                onOpenPendingChanges = { navController.navigate(Destination.PendingChanges) },
                onOpenPermissions = { navController.navigate(Destination.Permissions) },
                onOpenDebugUsage = { navController.navigate(Destination.DebugUsage) },
            )
        }
        composable<Destination.Apps> {
            AppsRoute()
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
            FocusSettingsRoute()
        }
        composable<Destination.PendingChanges> {
            PendingChangesRoute()
        }
        composable<Destination.Permissions> {
            PermissionsRoute()
        }
        composable<Destination.DebugUsage> {
            DebugUsageRoute()
        }
    }
}
