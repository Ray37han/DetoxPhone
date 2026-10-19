package dev.minimalist.launcher.ui.home

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.minimalist.launcher.apps.domain.model.LauncherApp
import dev.minimalist.launcher.apps.domain.repository.AppRepository
import dev.minimalist.launcher.settings.domain.model.AppSettings
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val currentTime: String = "",
    val currentDate: String = "",
    val favoriteApps: List<LauncherApp> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _timeState = MutableStateFlow(formatTime())
    private val _dateState = MutableStateFlow(formatDate())

    val uiState: StateFlow<HomeUiState> = combine(
        _timeState,
        _dateState,
        appRepository.getFavoriteApps(),
        settingsRepository.getSettings()
    ) { time, date, favorites, settings ->
        HomeUiState(
            currentTime = time,
            currentDate = date,
            favoriteApps = favorites,
            settings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = HomeUiState()
    )

    init {
        startClockUpdater()
    }

    private fun startClockUpdater() {
        viewModelScope.launch {
            while (true) {
                _timeState.value = formatTime()
                _dateState.value = formatDate()
                // Update every 30 seconds
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    private fun formatTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun formatDate(): String {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        return sdf.format(Date())
    }

    fun launchApp(app: LauncherApp) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (_: Exception) {
            // App may have been uninstalled
        }
    }

    fun lockScreen() {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.lockNow()
        } catch (_: SecurityException) {
            // Device admin not enabled
        }
    }

    fun setFavorite(packageName: String, favorite: Boolean) {
        viewModelScope.launch {
            appRepository.setFavorite(packageName, favorite)
        }
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch {
            appRepository.setHidden(packageName, hidden)
        }
    }

    fun renameApp(packageName: String, newName: String?) {
        viewModelScope.launch {
            appRepository.setCustomName(packageName, newName)
        }
    }
}
