package dev.minimalist.launcher.ui.wellbeing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.minimalist.launcher.core.common.getOrDefault
import dev.minimalist.launcher.settings.domain.model.AppSettings
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import dev.minimalist.launcher.wellbeing.domain.model.AppTimer
import dev.minimalist.launcher.wellbeing.domain.model.BlockSchedule
import dev.minimalist.launcher.wellbeing.domain.repository.WellbeingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUsageInfo(
    val packageName: String,
    val usageMs: Long
)

data class WellbeingUiState(
    val settings: AppSettings = AppSettings(),
    val timers: List<AppTimer> = emptyList(),
    val schedules: List<BlockSchedule> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val totalScreenTimeMs: Long = 0,
    val topAppsUsage: List<AppUsageInfo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WellbeingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wellbeingRepository: WellbeingRepository
) : ViewModel() {

    private val _screenTimeUsage = MutableStateFlow<Map<String, Long>>(emptyMap())

    init {
        refreshUsage()
    }

    fun refreshUsage() {
        viewModelScope.launch {
            val usageResult = wellbeingRepository.getAllDailyUsageMs()
            if (usageResult is dev.minimalist.launcher.core.common.Result.Success) {
                _screenTimeUsage.value = usageResult.data
            }
        }
    }

    val uiState: StateFlow<WellbeingUiState> = combine(
        settingsRepository.getSettings(),
        wellbeingRepository.getAllTimers(),
        wellbeingRepository.getAllSchedules(),
        wellbeingRepository.isUsageAccessGranted(),
        _screenTimeUsage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val settings = args[0] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val timers = args[1] as List<AppTimer>
        @Suppress("UNCHECKED_CAST")
        val schedules = args[2] as List<BlockSchedule>
        val hasAccess = args[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val usageMap = args[4] as Map<String, Long>

        val totalMs = usageMap.values.sum()
        val topApps = usageMap.entries
            .filter { it.value > 60_000 }
            .sortedByDescending { it.value }
            .take(5)
            .map { AppUsageInfo(it.key, it.value) }

        WellbeingUiState(
            settings = settings,
            timers = timers,
            schedules = schedules,
            hasUsageAccess = hasAccess,
            totalScreenTimeMs = totalMs,
            topAppsUsage = topApps,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WellbeingUiState()
    )

    fun setPrecisionMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPrecisionModeEnabled(enabled) }
    }

    fun setMonochromeMode(active: Boolean) {
        viewModelScope.launch { settingsRepository.setMonochromeModeActive(active) }
    }

    fun setReelsBlocking(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReelsBlockingEnabled(enabled) }
    }

    fun setBlockingChallenge(active: Boolean) {
        viewModelScope.launch { settingsRepository.setBlockingChallengeActive(active) }
    }

    fun deleteTimer(packageName: String) {
        viewModelScope.launch { wellbeingRepository.deleteTimer(packageName) }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch { wellbeingRepository.deleteSchedule(id) }
    }

    fun toggleSchedule(id: Long, enabled: Boolean) {
        viewModelScope.launch { wellbeingRepository.setScheduleEnabled(id, enabled) }
    }
}
