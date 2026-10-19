package dev.minimalist.launcher.wellbeing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.R
import dev.minimalist.launcher.settings.domain.model.TimerBehaviour
import dev.minimalist.launcher.wellbeing.domain.model.AppTimer
import dev.minimalist.launcher.wellbeing.domain.model.BlockSchedule
import dev.minimalist.launcher.wellbeing.domain.model.DayOfWeek
import dev.minimalist.launcher.wellbeing.domain.repository.WellbeingRepository
import dev.minimalist.launcher.wellbeing.ui.blocking.AppBlockedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Foreground service that monitors app usage and enforces:
 * - Per-app daily usage timers (remind / cooldown / exit)
 * - Time-based blocking schedules
 *
 * Polls the foreground app every 2 seconds via UsageStatsManager.
 */
@AndroidEntryPoint
class BlockingAndReminderService : Service() {

    @Inject lateinit var wellbeingRepository: WellbeingRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "blocking_service"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 5000L

        fun start(context: Context) {
            val intent = Intent(context, BlockingAndReminderService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BlockingAndReminderService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        monitorJob = serviceScope.launch {
            while (true) {
                try {
                    checkForegroundApp()
                } catch (_: Exception) { /* Graceful degradation */ }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkForegroundApp() {
        val foregroundPkg = getForegroundPackage() ?: return
        if (foregroundPkg == packageName) return // Don't block ourselves

        // Check block schedules first
        val schedules = wellbeingRepository.getEnabledSchedules().first()
        if (isBlockedBySchedule(foregroundPkg, schedules)) {
            blockApp(foregroundPkg, "Blocked by schedule")
            return
        }

        // Check app timers
        val timer = wellbeingRepository.getTimerForApp(foregroundPkg) ?: return
        if (!timer.isEnabled) return

        val usageResult = wellbeingRepository.getDailyUsageMs(foregroundPkg)
        val usageMs = (usageResult as? dev.minimalist.launcher.core.common.Result.Success)?.data ?: return
        val limitMs = timer.dailyLimitMinutes * 60_000L

        if (limitMs > 0 && usageMs >= limitMs) {
            when (timer.timerBehaviour) {
                TimerBehaviour.ONLY_REMIND -> {
                    // Show a notification reminder
                    showTimerReminder(foregroundPkg, timer)
                }
                TimerBehaviour.COOLDOWN_PERIOD -> {
                    blockApp(foregroundPkg, "Cooldown: ${timer.cooldownSeconds}s")
                }
                TimerBehaviour.EXIT_APP -> {
                    blockApp(foregroundPkg, "Daily limit reached")
                }
            }
        }
    }

    private fun isBlockedBySchedule(packageName: String, schedules: List<BlockSchedule>): Boolean {
        val cal = Calendar.getInstance()
        val currentMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dayIndex = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday
        val currentDay = DayOfWeek.entries.getOrNull(dayIndex - 1) ?: return false

        return schedules.any { schedule ->
            packageName in schedule.packageNames &&
                    schedule.isActiveNow(currentMinute, currentDay)
        }
    }

    private fun blockApp(packageName: String, reason: String) {
        val intent = Intent(this, AppBlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AppBlockedActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AppBlockedActivity.EXTRA_BLOCK_REASON, reason)
        }
        startActivity(intent)
    }

    private fun showTimerReminder(packageName: String, timer: AppTimer) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time's up")
            .setContentText("You've reached your daily limit for ${packageName.substringAfterLast('.')}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(packageName.hashCode(), notification)
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 5000, now)
        var lastPkg: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wellbeing Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors app usage for timers and blocking"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Detox Phone")
            .setContentText("Monitoring app usage")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}
