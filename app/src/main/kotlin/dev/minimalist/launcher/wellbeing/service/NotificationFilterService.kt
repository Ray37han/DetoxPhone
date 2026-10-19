package dev.minimalist.launcher.wellbeing.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.detox.domain.model.VaultedNotification
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import dev.minimalist.launcher.wellbeing.domain.model.NotificationFilterMode
import dev.minimalist.launcher.wellbeing.domain.repository.WellbeingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * DetoxNotificationListener (Phase 5) — NotificationListenerService that:
 *
 * 1. VIP / Critical Pass-Through: calls, alarms, ongoing services, OTP/2FA codes
 *    are NEVER intercepted (plan Section 6.5.1).
 * 2. Per-App Batch Interception: If monitored_apps.batch_notifications == true,
 *    the notification is silently cancelled and stored in notification_vault.
 * 3. Legacy wellbeing filter rules (ALLOW / BATCH / BLOCK) from older system.
 * 4. Scheduled digest delivery handled by DigestWorker (WorkManager).
 *
 * 100% on-device — no notification data ever transmitted over the network.
 */
@AndroidEntryPoint
class NotificationFilterService : NotificationListenerService() {

    @Inject lateinit var wellbeingRepository: WellbeingRepository
    @Inject lateinit var detoxRepository: DetoxRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val batchWindowFormat = SimpleDateFormat("yyyy-MM-dd_HH:00", Locale.US)

    /**
     * OTP / 2FA pass-through regex per plan Section 6.5.1.
     * Matches patterns like "Your OTP is 123456" or "472819 is your verification code".
     */
    private val otpRegex = Regex(
        """(?i)\b(\d{4,8})\b.*(otp|code|verification|passcode|login|auth|bank)|(otp|code|verification|passcode).*?\b(\d{4,8})\b"""
    )

    /** VIP notification categories that always pass through immediately. */
    private val vipCategories = setOf(
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_ALARM,
        "car_emergency",
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_SYSTEM,
        Notification.CATEGORY_NAVIGATION,
        Notification.CATEGORY_TRANSPORT
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return

        // Never filter our own app's notifications
        if (pkg == packageName) return

        val extras = notification.notification?.extras
        val title = extras?.getString("android.title") ?: ""
        val body = extras?.getCharSequence("android.text")?.toString() ?: ""

        serviceScope.launch {
            try {
                // ── VIP Pass-Through checks ──────────────────────────────────

                // 1. Ongoing (calls, navigation, music player)
                if (notification.isOngoing) return@launch

                // 2. VIP category
                val category = notification.notification?.category
                if (category != null && category in vipCategories) return@launch

                // 3. OTP / 2FA — never batch (plan explicit rule)
                if (otpRegex.containsMatchIn(body) || otpRegex.containsMatchIn(title)) return@launch

                // ── Detox per-app batch check (new, primary path) ─────────────
                val monitoredApp = detoxRepository.getMonitoredApp(pkg)
                if (monitoredApp?.batchNotifications == true) {
                    cancelNotification(notification.key)
                    val batchWindow = batchWindowFormat.format(Date())
                    detoxRepository.vaultNotification(
                        VaultedNotification(
                            packageName = pkg,
                            senderTitle = title.ifBlank { pkg },
                            messageBody = body,
                            postedEpochMs = notification.postTime,
                            batchWindowId = batchWindow
                        )
                    )
                    return@launch
                }

                // ── Legacy wellbeing filter rules (fallback) ─────────────────
                val filter = wellbeingRepository.getNotificationFilter(pkg)
                when (filter?.filterMode) {
                    NotificationFilterMode.BLOCK -> {
                        cancelNotification(notification.key)
                    }
                    NotificationFilterMode.BATCH -> {
                        cancelNotification(notification.key)
                        val batchWindow = batchWindowFormat.format(Date())
                        detoxRepository.vaultNotification(
                            VaultedNotification(
                                packageName = pkg,
                                senderTitle = title.ifBlank { pkg },
                                messageBody = body,
                                postedEpochMs = notification.postTime,
                                batchWindowId = batchWindow
                            )
                        )
                    }
                    else -> { /* ALLOW or no filter — pass through */ }
                }
            } catch (_: Exception) { /* graceful degradation */ }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
