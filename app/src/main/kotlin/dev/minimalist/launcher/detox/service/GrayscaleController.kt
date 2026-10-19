package dev.minimalist.launcher.detox.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * GrayscaleController — Hardware Per-App Grayscale via Daltonizer.
 *
 * Primary path (requires WRITE_SECURE_SETTINGS via ADB):
 *   Sets accessibility_display_daltonizer_enabled + accessibility_display_daltonizer=0 (Monochromacy)
 *
 * Fallback: caller should enable the desaturation tint layer in OverlayController.
 *
 * Plan Section 6.6.1.
 */
object GrayscaleController {

    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER_MODE    = "accessibility_display_daltonizer"

    /** Returns true if WRITE_SECURE_SETTINGS is granted (via ADB grant). */
    fun hasSecureSettingsPermission(context: Context): Boolean {
        return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Enable or disable hardware monochrome grayscale.
     * @return true if the secure setting was written successfully, false if permission denied.
     */
    fun setHardwareGrayscale(context: Context, enable: Boolean): Boolean {
        if (!hasSecureSettingsPermission(context)) return false
        return try {
            Settings.Secure.putInt(
                context.contentResolver,
                DALTONIZER_ENABLED,
                if (enable) 1 else 0
            )
            if (enable) {
                Settings.Secure.putInt(
                    context.contentResolver,
                    DALTONIZER_MODE,
                    0 // 0 = Monochromacy (full grayscale)
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
