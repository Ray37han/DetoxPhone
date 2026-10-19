package dev.minimalist.launcher.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized semantic haptics controller — Section 16.5.
 *
 * All vibration/haptic feedback in the app must go through this class.
 * Callers use semantic methods; the implementation maps to the correct
 * platform API and respects system haptic preferences.
 *
 * Do NOT call Vibrator/performHapticFeedback from individual composables.
 */
@Singleton
class HapticsController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // ── Public semantic API ───────────────────────────────────────────────────

    /** Light tap — app list item or button press. */
    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Navigation gesture accepted (drawer open, settings open, etc.). */
    fun navigationAccepted(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** Session started successfully. One medium confirmation. */
    fun sessionStarted() {
        vibratePrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scale = 0.7f, delayMs = 0)
    }

    /** Session paused or resumed — light confirmation. */
    fun sessionPausedOrResumed() {
        vibratePrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale = 0.5f, delayMs = 0)
    }

    /** Countdown reached zero / session expired — restrained single confirmation. */
    fun sessionExpired() {
        vibratePrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scale = 1.0f, delayMs = 0)
    }

    /** Wind-down warning (≤30 s remaining). */
    fun windDown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createOneShot(40L, 60))
        }
    }

    /** Budget exhausted / gate required — distinct but non-alarming. */
    fun cooldownAlert() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 50L, 80L, 50L),
                    intArrayOf(0, 80, 0, 60),
                    -1
                )
            )
        }
    }

    /** Invalid/rejected action — optional light. */
    fun reject(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /** Slider value changed tick — only when value actually changes. */
    fun selectionTick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun vibratePrimitive(primitive: Int, scale: Float, delayMs: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(primitive)) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(primitive, scale, delayMs)
                .compose()
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Fallback for API 29–30
            vibrator.vibrate(VibrationEffect.createOneShot(30L, (scale * 128).toInt().coerceIn(1, 255)))
        }
        // Below API 29: rely on view.performHapticFeedback; skip vibrator API.
    }
}
