package dev.minimalist.launcher.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.minimalist.launcher.ui.apps.AllAppsScreen
import dev.minimalist.launcher.ui.home.HomeScreen
import dev.minimalist.launcher.ui.settings.SettingsScreen
import dev.minimalist.launcher.ui.settings.HiddenAppsScreen
import dev.minimalist.launcher.ui.settings.AboutScreen
import dev.minimalist.launcher.ui.wellbeing.WellbeingScreen
import dev.minimalist.launcher.ui.wellbeing.TimerSettingsScreen
import dev.minimalist.launcher.ui.wellbeing.BlockScheduleScreen
import dev.minimalist.launcher.ui.wellbeing.NotificationSettingsScreen
import dev.minimalist.launcher.ui.wellbeing.MonochromeSettingsScreen

import dev.minimalist.launcher.ui.onboarding.OnboardingScreen
import dev.minimalist.launcher.detox.ui.dashboard.DetoxDashboardScreen
import dev.minimalist.launcher.detox.ui.monitor.AddMonitoredAppScreen

private const val NAV_DURATION = 220

private val enterHoriz = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = tween(NAV_DURATION, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(NAV_DURATION))

private val exitHoriz = slideOutHorizontally(
    targetOffsetX = { -it / 4 },
    animationSpec = tween(NAV_DURATION, easing = FastOutSlowInEasing)
) + fadeOut(animationSpec = tween(NAV_DURATION))

private val popEnterHoriz = slideInHorizontally(
    initialOffsetX = { -it / 4 },
    animationSpec = tween(NAV_DURATION, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(NAV_DURATION))

private val popExitHoriz = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(NAV_DURATION, easing = FastOutSlowInEasing)
) + fadeOut(animationSpec = tween(NAV_DURATION))

/**
 * Central navigation graph for the launcher.
 * All screen-level composables are wired here.
 */
@Composable
fun LauncherNavGraph(
    navController: NavHostController,
    startDestination: NavDestination = NavDestination.Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(NAV_DURATION)) },
        exitTransition = { fadeOut(tween(NAV_DURATION)) }
    ) {
        // ── Onboarding ──
        composable<NavDestination.Onboarding>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            OnboardingScreen(
                onOnboardingFinished = {
                    navController.navigate(NavDestination.Home) {
                        popUpTo<NavDestination.Onboarding> { inclusive = true }
                    }
                }
            )
        }

        // ── Home ──
        composable<NavDestination.Home>(
            enterTransition = { fadeIn(tween(NAV_DURATION)) },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { fadeOut(tween(NAV_DURATION)) }
        ) {
            HomeScreen(
                onNavigateToAllApps = {
                    navController.navigate(NavDestination.AllApps)
                },
                onNavigateToSettings = {
                    navController.navigate(NavDestination.Settings)
                },
                onNavigateToDetox = {
                    navController.navigate(NavDestination.DetoxDashboard)
                }
            )
        }

        // ── All Apps ──
        composable<NavDestination.AllApps>(
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(NAV_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(NAV_DURATION))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(NAV_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(NAV_DURATION))
            },
            popEnterTransition = { fadeIn(tween(NAV_DURATION)) },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(NAV_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(NAV_DURATION))
            }
        ) {
            AllAppsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetox = {
                    navController.navigate(NavDestination.DetoxDashboard)
                }
            )
        }

        // ── Settings ──
        composable<NavDestination.Settings>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHiddenApps = {
                    navController.navigate(NavDestination.HiddenApps)
                },
                onNavigateToWellbeing = {
                    navController.navigate(NavDestination.Wellbeing)
                },
                onNavigateToAbout = {
                    navController.navigate(NavDestination.About)
                }
            )
        }

        // ── Hidden Apps ──
        composable<NavDestination.HiddenApps>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            HiddenAppsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Wellbeing Dashboard ──
        composable<NavDestination.Wellbeing>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            WellbeingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTimers = {
                    navController.navigate(NavDestination.TimerSettings)
                },
                onNavigateToSchedules = {
                    navController.navigate(NavDestination.BlockSchedules)
                },
                onNavigateToNotifications = {
                    navController.navigate(NavDestination.NotificationSettings)
                },
                onNavigateToMonochrome = {
                    navController.navigate(NavDestination.MonochromeSettings)
                },
                onNavigateToDetox = {
                    navController.navigate(NavDestination.DetoxDashboard)
                }
            )
        }

        // ── Timer Settings ──
        composable<NavDestination.TimerSettings>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            TimerSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Block Schedules ──
        composable<NavDestination.BlockSchedules>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            BlockScheduleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Notification Settings ──
        composable<NavDestination.NotificationSettings>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Monochrome Settings ──
        composable<NavDestination.MonochromeSettings>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            MonochromeSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── About ──
        composable<NavDestination.About>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Detox Dashboard ──
        composable<NavDestination.DetoxDashboard>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            DetoxDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddApp = { navController.navigate(NavDestination.AddMonitoredApp) }
            )
        }

        // ── Add Monitored App ──
        composable<NavDestination.AddMonitoredApp>(
            enterTransition = { enterHoriz },
            exitTransition = { exitHoriz },
            popEnterTransition = { popEnterHoriz },
            popExitTransition = { popExitHoriz }
        ) {
            AddMonitoredAppScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
