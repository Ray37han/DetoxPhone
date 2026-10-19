package dev.minimalist.launcher.ui.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.minimalist.launcher.apps.domain.model.LauncherApp
import dev.minimalist.launcher.apps.domain.repository.AppRepository
import dev.minimalist.launcher.core.launcher.DefaultLauncherHelper
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 4,
    val installedApps: List<LauncherApp> = emptyList(),
    val favoritePackageNames: Set<String> = emptySet(),
    val hasUsageStatsPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val isCompleted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    private val _permissionsState = MutableStateFlow(checkPermissions())

    val uiState: StateFlow<OnboardingUiState> = combine(
        _currentStep,
        appRepository.getVisibleApps(),
        appRepository.getFavoriteApps(),
        _permissionsState
    ) { step: Int, allApps: List<LauncherApp>, favorites: List<LauncherApp>, permissions: Permissions ->
        OnboardingUiState(
            currentStep = step,
            installedApps = allApps.sortedBy { it.displayName },
            favoritePackageNames = favorites.map { it.packageName }.toSet(),
            hasUsageStatsPermission = permissions.usageStats,
            hasAccessibilityPermission = permissions.accessibility,
            hasNotificationPermission = permissions.notification,
            isIgnoringBatteryOptimizations = permissions.battery,
            isDefaultLauncher = DefaultLauncherHelper.isDefaultLauncher(context)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingUiState()
    )

    fun nextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value += 1
        }
    }

    fun prevStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun toggleFavorite(app: LauncherApp) {
        viewModelScope.launch {
            val isFav = uiState.value.favoritePackageNames.contains(app.packageName)
            appRepository.setFavorite(app.packageName, !isFav)
        }
    }

    fun refreshPermissions() {
        _permissionsState.value = checkPermissions()
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete(true)
            onFinished()
        }
    }

    private data class Permissions(
        val usageStats: Boolean,
        val accessibility: Boolean,
        val notification: Boolean,
        val battery: Boolean
    )

    private fun checkPermissions(): Permissions {
        // Usage stats
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps?.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        val usageStats = mode == AppOpsManager.MODE_ALLOWED

        // Accessibility
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val accessibility = accessibilityEnabled && enabledServices.contains(context.packageName)

        // Notification listener
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        val notification = flat.contains(context.packageName)

        // Battery optimization
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val battery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        return Permissions(usageStats, accessibility, notification, battery)
    }
}
