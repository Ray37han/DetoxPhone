package dev.minimalist.launcher.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of all navigation destinations in the launcher.
 * Uses Kotlin Serialization for type-safe Navigation Compose routes.
 */
@Serializable
sealed class NavDestination {

    @Serializable
    data object Home : NavDestination()

    @Serializable
    data object AllApps : NavDestination()

    @Serializable
    data object Settings : NavDestination()

    @Serializable
    data object Onboarding : NavDestination()

    @Serializable
    data object Wellbeing : NavDestination()

    @Serializable
    data object HiddenApps : NavDestination()

    @Serializable
    data object TimerSettings : NavDestination()

    @Serializable
    data object BlockSchedules : NavDestination()

    @Serializable
    data object NotificationSettings : NavDestination()

    @Serializable
    data object MonochromeSettings : NavDestination()

    @Serializable
    data object About : NavDestination()

    @Serializable
    data class AppDetail(val packageName: String) : NavDestination()

    @Serializable
    data class FolderDetail(val folderId: Long) : NavDestination()

    @Serializable
    data object DetoxDashboard : NavDestination()

    @Serializable
    data object AddMonitoredApp : NavDestination()
}
