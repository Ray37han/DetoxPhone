package dev.minimalist.launcher.detox.ui.digest

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.minimalist.launcher.detox.domain.model.VaultedNotification
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DigestDrawerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detoxRepository: DetoxRepository
) : ViewModel() {

    val unreadNotifications: StateFlow<List<VaultedNotification>> =
        detoxRepository.getUnreadNotifications()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> =
        detoxRepository.getUnreadCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markRead(id: Long) = viewModelScope.launch {
        detoxRepository.markNotificationRead(id)
    }

    fun markAllRead() = viewModelScope.launch {
        detoxRepository.markAllNotificationsRead()
    }

    /**
     * Launch the app that sent the notification and mark it read.
     * Fallback: if no PendingIntent is stored, fall back to the app's launch intent
     * via PackageManager (Section 7.4 — ephemeral action fallback).
     */
    fun launchApp(notification: VaultedNotification) = viewModelScope.launch {
        detoxRepository.markNotificationRead(notification.id)
        val launched = context.packageManager
            .getLaunchIntentForPackage(notification.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?.let {
                runCatching { context.startActivity(it) }.isSuccess
            } ?: false
        // PendingIntent path not applicable here (notifications are stored as text
        // without a live PendingIntent). getLaunchIntentForPackage is the safe fallback.
    }
}
