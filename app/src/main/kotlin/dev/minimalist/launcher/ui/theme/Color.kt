package dev.minimalist.launcher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Launcher color palette.
 *
 * Design direction: monochrome-first, high contrast, minimal colour.
 * Accent colours are intentionally muted — this is a minimalist launcher,
 * not a branded consumer app.
 *
 * Naming convention: semantic names (used in Theme.kt) over descriptive
 * names where possible, to keep theming intent clear.
 *
 * Do not reference these directly in UI code — use MaterialTheme.colorScheme instead.
 */
internal object LauncherColors {

    // ── Neutral scale ─────────────────────────────────────────────────────────
    val Black       = Color(0xFF000000)
    val White       = Color(0xFFFFFFFF)

    val Neutral05   = Color(0xFF0D0D0D)
    val Neutral10   = Color(0xFF1A1A1A)
    val Neutral15   = Color(0xFF262626)
    val Neutral20   = Color(0xFF333333)
    val Neutral30   = Color(0xFF4D4D4D)
    val Neutral50   = Color(0xFF808080)
    val Neutral70   = Color(0xFFB3B3B3)
    val Neutral85   = Color(0xFFD9D9D9)
    val Neutral92   = Color(0xFFEBEBEB)
    val Neutral95   = Color(0xFFF2F2F2)
    val Neutral98   = Color(0xFFFAFAFA)

    // ── Accent — single muted blue for interactive elements ───────────────────
    val AccentLight = Color(0xFF4A90D9)  // links, focused items
    val AccentDark  = Color(0xFF78B4F0)  // same intent, dark theme

    // ── Error ─────────────────────────────────────────────────────────────────
    val ErrorLight  = Color(0xFFBA1A1A)
    val ErrorDark   = Color(0xFFFFB4AB)
    val OnErrorLight = Color(0xFFFFFFFF)
    val OnErrorDark  = Color(0xFF690005)
}
