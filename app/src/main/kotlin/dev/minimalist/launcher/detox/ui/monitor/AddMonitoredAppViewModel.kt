package dev.minimalist.launcher.detox.ui.monitor

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.minimalist.launcher.detox.domain.model.MonitoredApp
import dev.minimalist.launcher.detox.domain.model.ProtectionLevel
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppItem(
    val packageName: String,
    val displayName: String,
    val isMonitored: Boolean
)

data class AddMonitoredAppUiState(
    val allApps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class AddMonitoredAppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detoxRepository: DetoxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMonitoredAppUiState())
    val uiState: StateFlow<AddMonitoredAppUiState> = _uiState

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

            // Observe monitored apps and refresh the full list
            detoxRepository.getAllMonitoredApps().collect { monitoredList ->
                val monitoredPackages = monitoredList.map { it.packageName }.toSet()
                val appItems = installed.map { appInfo ->
                    AppItem(
                        packageName = appInfo.packageName,
                        displayName = pm.getApplicationLabel(appInfo).toString(),
                        isMonitored = appInfo.packageName in monitoredPackages
                    )
                }
                val query = _uiState.value.searchQuery
                val filtered = if (query.isBlank()) appItems
                else appItems.filter {
                    it.displayName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
                }
                _uiState.update {
                    it.copy(
                        allApps = appItems,
                        filteredApps = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearch(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) state.allApps
            else state.allApps.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
            state.copy(searchQuery = query, filteredApps = filtered)
        }
    }

    fun addApp(packageName: String, level: ProtectionLevel) {
        viewModelScope.launch {
            val isShortVideoApp = dev.minimalist.launcher.detox.service.ShortVideoSignatures.VIEW_ID_MAP.containsKey(packageName)
            val isFeedApp = isShortVideoApp ||
                packageName.contains("facebook", ignoreCase = true) ||
                packageName.contains("instagram", ignoreCase = true) ||
                packageName.contains("twitter", ignoreCase = true) ||
                packageName.contains("reddit", ignoreCase = true) ||
                packageName.contains("tiktok", ignoreCase = true)

            detoxRepository.upsertMonitoredApp(
                MonitoredApp(
                    packageName = packageName,
                    protectionLevel = level,
                    blockShortVideos = isShortVideoApp,
                    enableScrollDecay = isFeedApp,
                    forceGrayscale = false,
                    dailyBudgetMinutes = 30,
                    cooldownMinutes = 10,
                    batchNotifications = isFeedApp
                )
            )
        }
    }
}
