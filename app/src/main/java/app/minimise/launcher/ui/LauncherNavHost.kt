package app.minimise.launcher.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.minimise.apps.ui.AppsRoute
import app.minimise.settings.permissions.PermissionsRoute
import app.minimise.settings.ui.PendingChangesRoute
import app.minimise.usage.ui.DebugUsageRoute
import app.minimise.settings.ui.SettingsRoute
import kotlinx.coroutines.flow.Flow

@Composable
fun LauncherNavHost(homePresses: Flow<Unit>) {
    val navController = rememberNavController()

    // The Home button always lands on the home screen itself.
    LaunchedEffect(homePresses) {
        homePresses.collect { navController.popBackStack(Destination.Home, inclusive = false) }
    }

    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<Destination.Home> {
            HomeRoute(
                homePresses = homePresses,
                onOpenSearch = { navController.navigate(Destination.Search) { launchSingleTop = true } },
                onOpenSettings = { navController.navigate(Destination.Settings) { launchSingleTop = true } },
                onOpenPermissions = { navController.navigate(Destination.Permissions) { launchSingleTop = true } },
            )
        }
        composable<Destination.Search> {
            SearchRoute(onClose = { navController.popBackStack(Destination.Home, inclusive = false) })
        }
        composable<Destination.Settings> {
            SettingsRoute(
                onOpenApps = { navController.navigate(Destination.Apps) },
                onOpenPendingChanges = { navController.navigate(Destination.PendingChanges) },
                onOpenPermissions = { navController.navigate(Destination.Permissions) },
                onOpenDebugUsage = { navController.navigate(Destination.DebugUsage) },
            )
        }
        composable<Destination.Apps> {
            AppsRoute()
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
