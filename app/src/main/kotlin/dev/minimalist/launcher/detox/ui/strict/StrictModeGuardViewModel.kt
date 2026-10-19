package dev.minimalist.launcher.detox.ui.strict

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.minimalist.launcher.detox.domain.model.StrictModeConfig
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StrictModeGuardViewModel @Inject constructor(
    private val detoxRepository: DetoxRepository
) : ViewModel() {

    val strictModeConfig: StateFlow<StrictModeConfig?> = detoxRepository.getStrictModeConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deactivateStrictMode() {
        viewModelScope.launch {
            detoxRepository.deactivateStrictMode()
        }
    }
}
