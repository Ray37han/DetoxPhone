package dev.minimalist.launcher.detox.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * OverlayController — Dual-Mode Window Host.
 *
 * Attaches overlays to WindowManager via TYPE_ACCESSIBILITY_OVERLAY (no SYSTEM_ALERT_WINDOW
 * permission needed — displays instantly over any app).
 *
 * Two overlay types:
 *  - Blocking Modal: full-screen interactive overlay (friction gate, cooldown screens)
 *  - Pass-Through HUD: non-interactive pill at the top showing session countdown,
 *    scroll distance meter, and sensory decay dim overlay.
 */
class OverlayController(private val service: AccessibilityService) {

    private val windowManager: WindowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var modalView: View? = null
    private var hudView: TextView? = null
    private var decayOverlayView: View? = null

    // ── Blocking Modal ──────────────────────────────────────────────────────

    fun showBlockingModal(contentView: View) {
        removeBlockingModal()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        modalView = contentView
        runOnMain {
            try {
                windowManager.addView(modalView, params)
            } catch (_: Exception) {}
        }
    }

    fun removeBlockingModal() {
        modalView?.let {
            val v = it
            modalView = null
            runOnMain {
                try {
                    if (v.isAttachedToWindow) windowManager.removeView(v)
                } catch (_: Exception) {}
            }
        }
    }

    // ── Pass-Through HUD ────────────────────────────────────────────────────

    fun showHudPill(initialText: String = "Session active") {
        runOnMain {
            try {
                if (hudView == null) {
                    val density = service.resources.displayMetrics.density
                    val padH = (14 * density).toInt()
                    val padV = (6 * density).toInt()

                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 24 * density
                        setColor(Color.argb(220, 18, 18, 18))
                        setStroke((1 * density).toInt(), Color.argb(80, 255, 255, 255))
                    }

                    val tv = TextView(service).apply {
                        text = initialText
                        setTextColor(Color.WHITE)
                        textSize = 12f
                        setPadding(padH, padV, padH, padV)
                        background = bgDrawable
                        elevation = 8 * density
                    }

                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.END
                        x = (16 * density).toInt()
                        y = (48 * density).toInt()
                    }

                    hudView = tv
                    windowManager.addView(tv, params)
                } else {
                    hudView?.text = initialText
                }
            } catch (_: Exception) {}
        }
    }

    fun updateHudText(text: String) {
        runOnMain {
            try {
                hudView?.text = text
            } catch (_: Exception) {}
        }
    }

    fun removePassThroughHud() {
        hudView?.let {
            val v = it
            hudView = null
            runOnMain {
                try {
                    if (v.isAttachedToWindow) windowManager.removeView(v)
                } catch (_: Exception) {}
            }
        }
    }

    // ── Scroll Decay Dim Overlay ─────────────────────────────────────────────

    /**
     * Updates (or creates) the pass-through decay overlay with the given alpha [0.0, 0.78].
     * When alpha <= 0, the overlay is removed.
     */
    fun setDecayOverlayAlpha(alpha: Float) {
        if (alpha <= 0.01f) {
            removeDecayOverlay()
            return
        }
        val argb = Color.argb((alpha * 255).toInt().coerceIn(0, 255), 10, 10, 10)
        runOnMain {
            try {
                if (decayOverlayView == null) {
                    val view = View(service).apply {
                        setBackgroundColor(argb)
                    }
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    )
                    decayOverlayView = view
                    windowManager.addView(view, params)
                } else {
                    decayOverlayView?.setBackgroundColor(argb)
                }
            } catch (_: Exception) {}
        }
    }

    fun removeDecayOverlay() {
        decayOverlayView?.let {
            val v = it
            decayOverlayView = null
            runOnMain {
                try {
                    if (v.isAttachedToWindow) windowManager.removeView(v)
                } catch (_: Exception) {}
            }
        }
    }

    fun removeAll() {
        removeBlockingModal()
        removePassThroughHud()
        removeDecayOverlay()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
