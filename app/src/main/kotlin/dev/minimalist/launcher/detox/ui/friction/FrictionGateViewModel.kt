package dev.minimalist.launcher.detox.ui.friction

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.minimalist.launcher.detox.domain.model.ProtectionLevel
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class FrictionGateUiState(
    val packageName: String = "",
    val appDisplayName: String = "",
    val reason: String = "",
    val cooldownRemainingMs: Long = 0L,
    val dailyBudgetMinutes: Int = 30,
    val cooldownMinutes: Int = 10,
    val protectionLevel: ProtectionLevel = ProtectionLevel.FRICTION_TIMEBOX,
    /** Number of completed opens today — drives progressive delay formula. */
    val todayOpens: Int = 0,
    /** Package name of configured habit substitute, or null if none set. */
    val substitutePackage: String? = null
)

@HiltViewModel
class FrictionGateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detoxRepository: DetoxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrictionGateUiState())
    val uiState: StateFlow<FrictionGateUiState> = _uiState

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun init(
        packageName: String,
        reason: String,
        cooldownRemainingMs: Long,
        dailyBudgetMinutes: Int,
        cooldownMinutes: Int,
        protectionLevel: ProtectionLevel = ProtectionLevel.FRICTION_TIMEBOX
    ) {
        val appName = getAppName(packageName)
        _uiState.update {
            it.copy(
                packageName = packageName,
                appDisplayName = appName,
                reason = reason,
                cooldownRemainingMs = cooldownRemainingMs,
                dailyBudgetMinutes = dailyBudgetMinutes,
                cooldownMinutes = cooldownMinutes,
                protectionLevel = protectionLevel
            )
        }

        viewModelScope.launch {
            val monitored = detoxRepository.getMonitoredApp(packageName)
            val today = dateFormat.format(Date())
            val telemetry = detoxRepository.getTodayTelemetryForApp(today, packageName)
            val todayCompletedOpens = telemetry?.completedOpens ?: 0

            _uiState.update {
                it.copy(
                    // Custom alias overrides display name if set
                    appDisplayName = monitored?.customAlias ?: it.appDisplayName,
                    protectionLevel = monitored?.protectionLevel ?: it.protectionLevel,
                    todayOpens = todayCompletedOpens,
                    substitutePackage = monitored?.substitutePackage
                )
            }
        }
    }

    fun onProceed(intentReason: String, allocatedDurationMs: Long, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val pkg = _uiState.value.packageName
            val today = dateFormat.format(Date())
            detoxRepository.startSession(pkg, allocatedDurationMs, intentReason)
            detoxRepository.recordCompletedOpen(today, pkg)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            val pkg = _uiState.value.packageName
            val today = dateFormat.format(Date())
            detoxRepository.recordAbortedOpen(today, pkg)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
