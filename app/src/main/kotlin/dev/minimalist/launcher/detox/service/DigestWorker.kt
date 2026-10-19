package dev.minimalist.launcher.detox.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.minimalist.launcher.MainActivity
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * DigestWorker — WorkManager periodic job for Notification Batching Vault.
 *
 * Fires at each user-configured digest window (plan Section 6.5, Phase 5).
 * Queries unread notification count from the vault and posts a single calm
 * summary notification on channel `detox_digest_channel`.
 *
 * Scheduling: two periodic workers are scheduled at app startup:
 *   - DIGEST_TAG_1PM  fires every 24h anchored to 13:00
 *   - DIGEST_TAG_8PM  fires every 24h anchored to 20:00
 *
 * Default digest times per plan: 13:00 and 20:00.
 * 100% on-device — no notification content transmitted over network.
 */
@HiltWorker
class DigestWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val detoxRepository: DetoxRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "detox_digest_channel"
        const val NOTIFICATION_ID = 7001
        const val DIGEST_TAG_1PM = "detox_digest_13h"
        const val DIGEST_TAG_8PM = "detox_digest_20h"

        /**
         * Schedule both daily digest workers.
         * Call from Application.onCreate() or after permission is granted.
         * Uses [ExistingPeriodicWorkPolicy.KEEP] so repeated calls are safe.
         */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // 1 PM digest (runs every 24h; initial delay will naturally drift to ~1PM)
            val request1pm = PeriodicWorkRequestBuilder<DigestWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayUntilHour(13), TimeUnit.MILLISECONDS)
                .addTag(DIGEST_TAG_1PM)
                .build()

            // 8 PM digest
            val request8pm = PeriodicWorkRequestBuilder<DigestWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayUntilHour(20), TimeUnit.MILLISECONDS)
                .addTag(DIGEST_TAG_8PM)
                .build()

            workManager.enqueueUniquePeriodicWork(
                DIGEST_TAG_1PM, ExistingPeriodicWorkPolicy.KEEP, request1pm
            )
            workManager.enqueueUniquePeriodicWork(
                DIGEST_TAG_8PM, ExistingPeriodicWorkPolicy.KEEP, request8pm
            )
        }

        /** Computes ms until the next occurrence of [hour]:00 local time. */
        private fun initialDelayUntilHour(hour: Int): Long {
            val cal = java.util.Calendar.getInstance()
            val now = cal.timeInMillis
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= now) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis - now
        }
    }

    override suspend fun doWork(): Result {
        try {
            val unreadCount = detoxRepository.getUnreadCount().first()
            if (unreadCount <= 0) return Result.success()

            // Count distinct apps in vault
            val notifications = detoxRepository.getUnreadNotifications().first()
            val appCount = notifications.map { it.packageName }.distinct().size

            ensureChannel()
            postDigestNotification(unreadCount, appCount)
        } catch (_: Exception) {
            // Graceful degradation — never crash
        }
        return Result.success()
    }

    private fun ensureChannel() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quiet Digest",
                NotificationManager.IMPORTANCE_LOW // silent by plan spec
            ).apply {
                description = "Batched notification summaries — no interruption"
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun postDigestNotification(unreadCount: Int, appCount: Int) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_digest", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📦 Quiet Digest Ready")
            .setContentText("$unreadCount notification${if (unreadCount > 1) "s" else ""} from $appCount app${if (appCount > 1) "s" else ""} waiting in your vault.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$unreadCount notifications from $appCount apps are waiting in your Quiet Digest vault. Tap to review them calmly."
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}
