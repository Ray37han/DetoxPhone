package dev.minimalist.launcher.wellbeing.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device Admin Receiver enabling the screen lock feature.
 *
 * Only requests the `force-lock` policy (see res/xml/device_admin_policies.xml).
 * Used for double-tap-to-lock on the home screen.
 */
class ScreenLockDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        // Device admin enabled — screen lock is now available
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Device admin disabled — screen lock no longer available
    }
}
