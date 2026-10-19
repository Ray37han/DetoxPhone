package dev.minimalist.launcher.settings.domain.repository

import dev.minimalist.launcher.settings.domain.model.AppSettings
import dev.minimalist.launcher.settings.domain.model.ColorTheme
import dev.minimalist.launcher.settings.domain.model.FontFamily
import dev.minimalist.launcher.settings.domain.model.FontSize
import dev.minimalist.launcher.settings.domain.model.ThemeMode
import dev.minimalist.launcher.settings.domain.model.TimeFormatType
import dev.minimalist.launcher.settings.domain.model.TimerBehaviour
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reading and writing launcher settings.
 * Backed by DataStore Preferences.
 */
interface SettingsRepository {

    /** Observe all settings as a reactive stream. */
    fun getSettings(): Flow<AppSettings>

    // ── Appearance ──
    suspend fun updateThemeMode(mode: ThemeMode)
    suspend fun updateColorTheme(theme: ColorTheme)
    suspend fun updateFontFamily(family: FontFamily)
    suspend fun updateFontSize(size: FontSize)
    suspend fun updateShowClock(show: Boolean)
    suspend fun updateShowDate(show: Boolean)
    suspend fun updateTimeFormat(format: TimeFormatType)
    suspend fun updateListItemSpacing(spacing: Int)
    suspend fun updateAnimationsEnabled(enabled: Boolean)

    // ── Launcher behaviour ──
    suspend fun setOnboardingComplete(complete: Boolean)

    // ── Wellbeing toggles ──
    suspend fun setPrecisionModeEnabled(enabled: Boolean)
    suspend fun setPrecisionModeConsent(given: Boolean)
    suspend fun setMonochromeModeActive(active: Boolean)
    suspend fun setReelsBlockingEnabled(enabled: Boolean)
    suspend fun setBlockingChallengeActive(active: Boolean)
    suspend fun setScreenLockDoubleTap(enabled: Boolean)
    suspend fun setDefaultTimerBehaviour(behaviour: TimerBehaviour)
}
