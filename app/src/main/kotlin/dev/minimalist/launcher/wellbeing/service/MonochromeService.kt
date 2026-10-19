package dev.minimalist.launcher.wellbeing.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service that toggles the system display daltonizer to grayscale.
 *
 * Uses Settings.Secure:
 * - accessibility_display_daltonizer_enabled (0/1)
 * - accessibility_display_daltonizer (0 = grayscale)
 *
 * Requires WRITE_SECURE_SETTINGS granted via ADB:
 * adb shell pm grant dev.minimalist.launcher android.permission.WRITE_SECURE_SETTINGS
 */
@AndroidEntryPoint
class MonochromeService : Service() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val ACTION_ENABLE = "action_enable"
        private const val ACTION_DISABLE = "action_disable"
        private const val DALTONIZER_GRAYSCALE = 0

        fun enable(context: Context) {
            val intent = Intent(context, MonochromeService::class.java).apply {
                action = ACTION_ENABLE
            }
            context.startService(intent)
        }

        fun disable(context: Context) {
            val intent = Intent(context, MonochromeService::class.java).apply {
                action = ACTION_DISABLE
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE -> setGrayscale(true)
            ACTION_DISABLE -> setGrayscale(false)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun setGrayscale(enabled: Boolean) {
        try {
            val cr = contentResolver
            if (enabled) {
                Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", 1)
                Settings.Secure.putInt(cr, "accessibility_display_daltonizer", DALTONIZER_GRAYSCALE)
            } else {
                Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", 0)
            }
        } catch (e: SecurityException) {
            // WRITE_SECURE_SETTINGS not granted
            android.util.Log.w("MonochromeService", "WRITE_SECURE_SETTINGS not granted", e)
        }
    }
}
