package dev.minimalist.launcher.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Central motion tokens — Section 16.3.
 * Use these instead of inline magic numbers for durations/distances.
 */
object MotionTokens {

    // ── Durations (ms) ────────────────────────────────────────────────────────
    /** Instant tap-feedback and micro-scale responses. */
    const val DurationFastMs: Int = 120

    /** Dialog/sheet open/close, state transitions. */
    const val DurationStandardMs: Int = 220

    /** Full-page navigation (drawer, settings, search). */
    const val DurationNavigationMs: Int = 260

    /** Reserved for deliberate slow reveals; use sparingly. */
    const val DurationSlowMs: Int = 340

    /** Per-item stagger for small lists only. Never stagger large lists. */
    const val DurationItemStaggerMs: Int = 20

    // ── Distances (dp) ────────────────────────────────────────────────────────
    /** Vertical translation for dialogs/sheets. */
    const val DistanceSmallDp: Int = 8

    /** Vertical translation for page-level enter/exit. */
    const val DistanceMediumDp: Int = 16

    // ── Scale ─────────────────────────────────────────────────────────────────
    /** Pressed scale for tap targets. */
    const val ScalePressed: Float = 0.98f

    // ── Alpha ─────────────────────────────────────────────────────────────────
    const val AlphaHidden: Float = 0f
    const val AlphaVisible: Float = 1f

    // ── Easing ────────────────────────────────────────────────────────────────
    /** Standard entrance: fast-out/slow-in. */
    val EasingEnter: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Exit: slightly faster. */
    val EasingExit: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** Emphasized deceleration (Material 3). */
    val EasingEmphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
}
