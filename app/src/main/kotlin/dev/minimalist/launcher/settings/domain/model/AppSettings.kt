package dev.minimalist.launcher.settings.domain.model

/**
 * Domain model for all user-configurable launcher settings.
 *
 * Stored via DataStore (see [dev.minimalist.launcher.core.datastore.PreferencesKeys]).
 * This is the single representation of settings state consumed by ViewModels and
 * Compose UI — no raw DataStore Preferences objects leak past the repository layer.
 */
data class AppSettings(
    // ── Appearance ──
    val themeMode: ThemeMode = ThemeMode.DARK,
    val colorTheme: ColorTheme = ColorTheme.DEFAULT,
    val fontFamily: FontFamily = FontFamily.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val fontScale: Float = 1.0f,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val timeFormat: TimeFormatType = TimeFormatType.SYSTEM,
    val listItemSpacing: Int = 4,
    val animationsEnabled: Boolean = true,

    // ── Launcher behaviour ──
    val onboardingComplete: Boolean = false,
    val isDefaultLauncher: Boolean = false,

    // ── Wellbeing toggles ──
    val precisionModeEnabled: Boolean = false,
    val precisionModeConsentGiven: Boolean = false,
    val monochromeModeActive: Boolean = false,
    val defaultCooldownWhenOpeningActive: Boolean = false,
    val defaultTimerBehaviour: TimerBehaviour = TimerBehaviour.ONLY_REMIND,
    val reelsBlockingEnabled: Boolean = false,
    val blockingChallengeActive: Boolean = false,
    val screenLockDoubleTap: Boolean = false
)

/** Available theme modes. Stored as String in DataStore. */
enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromString(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Predefined color themes for the launcher. */
enum class ColorTheme {
    DEFAULT, CHARCOAL, MIDNIGHT, SNOW, OCEAN, FOREST, SUNSET, ROSE;

    companion object {
        fun fromString(value: String?): ColorTheme =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

/** Font families available in the launcher. */
enum class FontFamily {
    SYSTEM, SANS_SERIF, SERIF, MONOSPACE;

    companion object {
        fun fromString(value: String?): FontFamily =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Font size presets. */
enum class FontSize {
    SMALL, MEDIUM, LARGE, EXTRA_LARGE;

    val multiplier: Float
        get() = when (this) {
            SMALL -> 0.85f
            MEDIUM -> 1.0f
            LARGE -> 1.2f
            EXTRA_LARGE -> 1.4f
        }

    companion object {
        fun fromString(value: String?): FontSize =
            entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}

/** Time display format. */
enum class TimeFormatType {
    SYSTEM, HOUR_12, HOUR_24;

    companion object {
        fun fromString(value: String?): TimeFormatType =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** What happens when an app timer expires. */
enum class TimerBehaviour {
    /** Show a non-blocking reminder overlay. */
    ONLY_REMIND,
    /** Force a cooldown period before reopening. */
    COOLDOWN_PERIOD,
    /** Exit the app immediately. */
    EXIT_APP;

    companion object {
        fun fromString(value: String?): TimerBehaviour =
            entries.firstOrNull { it.name == value } ?: ONLY_REMIND
    }
}
