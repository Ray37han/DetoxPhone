package dev.minimalist.launcher.detox.ui.dashboard

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.minimalist.launcher.detox.domain.model.DailyTelemetry
import dev.minimalist.launcher.detox.domain.model.MonitoredApp
import dev.minimalist.launcher.detox.domain.model.StrictModeConfig
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import dev.minimalist.launcher.detox.service.DetoxAccessibilityService
import dev.minimalist.launcher.detox.service.UsageStatsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DetoxDashboardUiState(
    val todayTelemetry: List<DailyTelemetry> = emptyList(),
    val monitoredApps: List<MonitoredApp> = emptyList(),
    val strictMode: StrictModeConfig? = null,
    val unreadNotificationCount: Int = 0,
    val totalImpulsesDeflected: Int = 0,
    val totalScreenTimeMs: Long = 0L,
    val estimatedScrollMeters: Double = 0.0,
    val isAccessibilityEnabled: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val isNotificationListenerEnabled: Boolean = false
)

@HiltViewModel
class DetoxDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detoxRepository: DetoxRepository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val _permissionsState = MutableStateFlow(Triple(false, false, false))

    val uiState: StateFlow<DetoxDashboardUiState> = combine(
        detoxRepository.getTodayTelemetry(),
        detoxRepository.getAllMonitoredApps(),
        detoxRepository.getStrictModeConfig(),
        detoxRepository.getUnreadCount(),
        _permissionsState
    ) { telemetry, apps, strict, unread, perms ->
        DetoxDashboardUiState(
            todayTelemetry = telemetry,
            monitoredApps = apps,
            strictMode = strict,
            unreadNotificationCount = unread,
            totalImpulsesDeflected = telemetry.sumOf { it.abortedOpens },
            totalScreenTimeMs = telemetry.sumOf { it.totalForegroundMs },
            estimatedScrollMeters = telemetry.sumOf { it.estimatedScrollMeters },
            isAccessibilityEnabled = perms.first,
            hasUsageAccess = perms.second,
            isNotificationListenerEnabled = perms.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetoxDashboardUiState())

    init {
        refreshPermissions()
        syncUsageMetrics()
    }

    fun refreshPermissions() {
        // 1. Accessibility
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val isA11y = enabledServices.contains(context.packageName) ||
                DetoxAccessibilityService.isConnected()

        // 2. Usage Stats
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        val hasUsage = mode == AppOpsManager.MODE_ALLOWED

        // 3. Notification Listener
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        val hasNotification = flat.contains(context.packageName)

        _permissionsState.value = Triple(isA11y, hasUsage, hasNotification)
    }

    fun syncUsageMetrics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startOfDay = UsageStatsEngine.startOfTodayMs()
                val now = System.currentTimeMillis()
                val summary = UsageStatsEngine.queryDailyMetrics(context, startOfDay, now)
                val today = dateFormat.format(Date())

                val monitored = detoxRepository.getAllMonitoredApps().first()
                for (app in monitored) {
                    val ms = summary.foregroundMsMap[app.packageName] ?: 0L
                    if (ms > 0L) {
                        detoxRepository.updateForegroundTime(today, app.packageName, ms)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateMonitoredApp(app: MonitoredApp) = viewModelScope.launch(Dispatchers.IO) {
        detoxRepository.upsertMonitoredApp(app)
        // Clear old sessions so new protection setting applies immediately without legacy cooldown locks
        detoxRepository.endSession(app.packageName, 0)
    }

    fun activateStrictMode(hours: Int) = viewModelScope.launch {
        detoxRepository.activateStrictMode(hours * 3600_000L, "TIMER_60S")
    }

    fun deactivateStrictMode() = viewModelScope.launch {
        detoxRepository.deactivateStrictMode()
    }

    fun removeMonitoredApp(packageName: String) = viewModelScope.launch(Dispatchers.IO) {
        detoxRepository.removeMonitoredApp(packageName)
        detoxRepository.endSession(packageName, 0)
    }
}
