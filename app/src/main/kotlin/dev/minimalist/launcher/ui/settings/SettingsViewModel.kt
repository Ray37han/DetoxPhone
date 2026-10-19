package dev.minimalist.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.minimalist.launcher.apps.domain.model.LauncherApp
import dev.minimalist.launcher.apps.domain.repository.AppRepository
import dev.minimalist.launcher.settings.domain.model.AppSettings
import dev.minimalist.launcher.settings.domain.model.ColorTheme
import dev.minimalist.launcher.settings.domain.model.FontFamily
import dev.minimalist.launcher.settings.domain.model.FontSize
import dev.minimalist.launcher.settings.domain.model.ThemeMode
import dev.minimalist.launcher.settings.domain.model.TimeFormatType
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val hiddenApps: List<LauncherApp> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getSettings(),
        appRepository.getAllApps()
    ) { settings, allApps ->
        SettingsUiState(
            settings = settings,
            hiddenApps = allApps.filter { it.isHidden },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.updateThemeMode(mode) }
    }

    fun setColorTheme(theme: ColorTheme) {
        viewModelScope.launch { settingsRepository.updateColorTheme(theme) }
    }

    fun setFontFamily(family: FontFamily) {
        viewModelScope.launch { settingsRepository.updateFontFamily(family) }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch { settingsRepository.updateFontSize(size) }
    }

    fun setShowClock(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowClock(show) }
    }

    fun setShowDate(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowDate(show) }
    }

    fun setTimeFormat(format: TimeFormatType) {
        viewModelScope.launch { settingsRepository.updateTimeFormat(format) }
    }

    fun setListItemSpacing(spacing: Int) {
        viewModelScope.launch { settingsRepository.updateListItemSpacing(spacing) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAnimationsEnabled(enabled) }
    }

    fun setScreenLockDoubleTap(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setScreenLockDoubleTap(enabled) }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { appRepository.setHidden(packageName, false) }
    }
}
