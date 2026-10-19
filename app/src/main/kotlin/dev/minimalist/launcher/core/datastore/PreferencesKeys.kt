package dev.minimalist.launcher.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * All DataStore preference keys in one place.
 *
 * Key names are persisted on-device; changing them creates a new key
 * and the old value becomes orphaned. Treat key names as stable API.
 */
object PreferencesKeys {

    // ── Appearance ────────────────────────────────────────────────────────────

    /** "LIGHT" | "DARK" | "SYSTEM" */
    val THEME_MODE = stringPreferencesKey("theme_mode")

    /** "DEFAULT" | "CHARCOAL" | "MIDNIGHT" | ... */
    val COLOR_THEME = stringPreferencesKey("color_theme")

    /** "SYSTEM" | "SANS_SERIF" | "SERIF" | "MONOSPACE" */
    val FONT_FAMILY = stringPreferencesKey("font_family")

    /** "SMALL" | "MEDIUM" | "LARGE" | "EXTRA_LARGE" */
    val FONT_SIZE = stringPreferencesKey("font_size")

    val SHOW_CLOCK = booleanPreferencesKey("show_clock")
    val SHOW_DATE = booleanPreferencesKey("show_date")

    /** "SYSTEM" | "HOUR_12" | "HOUR_24" */
    val TIME_FORMAT = stringPreferencesKey("time_format")

    /** sp multiplier, e.g. 1.0f = default, 1.2f = large */
    val FONT_SCALE = floatPreferencesKey("font_scale")

    /** dp between list items */
    val LIST_ITEM_SPACING = intPreferencesKey("list_item_spacing")

    val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")

    // ── Launcher behaviour ────────────────────────────────────────────────────

    /** Whether onboarding has been completed */
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

    /** Whether this app is currently the default launcher */
    val IS_DEFAULT_LAUNCHER = booleanPreferencesKey("is_default_launcher")

    // ── Wellbeing: Precision Mode ─────────────────────────────────────────────

    val PRECISION_MODE_ENABLED = booleanPreferencesKey("precision_mode_enabled")
    val PRECISION_MODE_CONSENT = booleanPreferencesKey("precision_mode_consent")

    // ── Wellbeing: Monochrome ─────────────────────────────────────────────────

    val MONOCHROME_MODE_ACTIVE = booleanPreferencesKey("monochrome_mode_active")

    // ── Wellbeing: App Timer Defaults ─────────────────────────────────────────

    val DEFAULT_COOLDOWN_WHEN_OPENING = booleanPreferencesKey("default_cooldown_when_opening")

    /** "ONLY_REMIND" | "COOLDOWN_PERIOD" | "EXIT_APP" */
    val DEFAULT_TIMER_BEHAVIOUR = stringPreferencesKey("default_timer_behaviour")

    // ── Wellbeing: Reels Blocking ─────────────────────────────────────────────

    val REELS_BLOCKING_ENABLED = booleanPreferencesKey("reels_blocking_enabled")

    // ── Wellbeing: Blocking Challenge ─────────────────────────────────────────

    val BLOCKING_CHALLENGE_ACTIVE = booleanPreferencesKey("blocking_challenge_active")
    val BLOCKING_CHALLENGE_START_MS = longPreferencesKey("blocking_challenge_start_ms")

    // ── Wellbeing: Screen Lock ────────────────────────────────────────────────

    val SCREEN_LOCK_DOUBLE_TAP = booleanPreferencesKey("screen_lock_double_tap")

    // ── Wellbeing: Schedule Change Delays ─────────────────────────────────────

    /** Timestamp (epoch ms) when user last requested a schedule change */
    val SCHEDULE_CHANGE_REQUESTED_AT = longPreferencesKey("schedule_change_requested_at")
}
