package dev.minimalist.launcher.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

data class AllAppsUiState(
    val allApps: List<LauncherApp> = emptyList(),
    val filteredApps: List<LauncherApp> = emptyList(),
    val searchQuery: String = "",
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AllAppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<AllAppsUiState> = combine(
        appRepository.getVisibleApps(),
        _searchQuery,
        settingsRepository.getSettings()
    ) { apps, query, settings ->
        val filtered = if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.displayName.contains(query, ignoreCase = true)
            }
        }
        AllAppsUiState(
            allApps = apps,
            filteredApps = filtered,
            searchQuery = query,
            settings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AllAppsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setFavorite(packageName: String, favorite: Boolean) {
        viewModelScope.launch { appRepository.setFavorite(packageName, favorite) }
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch { appRepository.setHidden(packageName, hidden) }
    }

    fun renameApp(packageName: String, newName: String?) {
        viewModelScope.launch { appRepository.setCustomName(packageName, newName) }
    }
}
