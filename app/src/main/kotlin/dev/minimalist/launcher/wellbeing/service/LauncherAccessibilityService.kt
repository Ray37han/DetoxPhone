package dev.minimalist.launcher.wellbeing.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import dev.minimalist.launcher.wellbeing.domain.repository.WellbeingRepository
import dev.minimalist.launcher.wellbeing.ui.blocking.AppBlockedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accessibility Service providing:
 * 1. Real-time foreground package detection (Precision Mode)
 * 2. Short video / Reels detection and blocking via view hierarchy inspection
 *
 * This service is only active when Precision Mode is enabled in settings.
 */
@AndroidEntryPoint
class LauncherAccessibilityService : AccessibilityService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var wellbeingRepository: WellbeingRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastForegroundPackage: String? = null

    // Known short video tab identifiers per platform
    private val reelsIndicators = mapOf(
        "com.instagram.android" to listOf("reels_tab", "clips_tab", "reel_viewer"),
        "com.google.android.youtube" to listOf("shorts", "reel_player", "shorts_pivot_button"),
        "com.zhiliaoapp.musically" to listOf("com.ss.android.ugc.aweme") // TikTok is all shorts
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 200
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val event = event ?: return
        val packageName = event.packageName?.toString() ?: return

        serviceScope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                if (!settings.precisionModeEnabled) return@launch

                // Track foreground package changes
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    if (packageName != lastForegroundPackage) {
                        lastForegroundPackage = packageName
                        onForegroundAppChanged(packageName)
                    }
                }

                // Reels/Shorts blocking via node inspection
                if (settings.reelsBlockingEnabled) {
                    checkForShortVideoContent(event, packageName)
                }
            } catch (_: Exception) { /* Graceful degradation */ }
        }
    }

    override fun onInterrupt() {
        // Required override — nothing to clean up
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun onForegroundAppChanged(packageName: String) {
        // The blocking service handles timer/schedule enforcement.
        // This callback can be used for additional precision tracking.
    }

    private fun checkForShortVideoContent(event: AccessibilityEvent, packageName: String) {
        val indicators = reelsIndicators[packageName] ?: return
        val rootNode = rootInActiveWindow ?: return

        try {
            if (containsShortVideoIndicator(rootNode, indicators)) {
                blockReelsContent(packageName)
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun containsShortVideoIndicator(
        node: AccessibilityNodeInfo,
        indicators: List<String>
    ): Boolean {
        // Check view ID
        val viewId = node.viewIdResourceName
        if (viewId != null && indicators.any { viewId.contains(it, ignoreCase = true) }) {
            return true
        }

        // Check content description
        val contentDesc = node.contentDescription?.toString()
        if (contentDesc != null && indicators.any { contentDesc.contains(it, ignoreCase = true) }) {
            return true
        }

        // Recurse children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (containsShortVideoIndicator(child, indicators)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }

    private fun blockReelsContent(packageName: String) {
        val intent = Intent(this, AppBlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AppBlockedActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AppBlockedActivity.EXTRA_BLOCK_REASON, "Short videos blocked")
        }
        startActivity(intent)
    }
}
