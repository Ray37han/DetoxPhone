package dev.minimalist.launcher.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dev.minimalist.launcher.core.datastore.PreferencesKeys
import dev.minimalist.launcher.settings.domain.model.AppSettings
import dev.minimalist.launcher.settings.domain.model.ColorTheme
import dev.minimalist.launcher.settings.domain.model.FontFamily
import dev.minimalist.launcher.settings.domain.model.FontSize
import dev.minimalist.launcher.settings.domain.model.ThemeMode
import dev.minimalist.launcher.settings.domain.model.TimeFormatType
import dev.minimalist.launcher.settings.domain.model.TimerBehaviour
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed implementation of [SettingsRepository].
 *
 * Every setting is stored as an individual preference key.
 * The repository maps raw Preferences to the typed [AppSettings] domain model.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromString(prefs[PreferencesKeys.THEME_MODE]),
            colorTheme = ColorTheme.fromString(prefs[PreferencesKeys.COLOR_THEME]),
            fontFamily = FontFamily.fromString(prefs[PreferencesKeys.FONT_FAMILY]),
            fontSize = FontSize.fromString(prefs[PreferencesKeys.FONT_SIZE]),
            fontScale = prefs[PreferencesKeys.FONT_SCALE] ?: 1.0f,
            showClock = prefs[PreferencesKeys.SHOW_CLOCK] ?: true,
            showDate = prefs[PreferencesKeys.SHOW_DATE] ?: true,
            timeFormat = TimeFormatType.fromString(prefs[PreferencesKeys.TIME_FORMAT]),
            listItemSpacing = prefs[PreferencesKeys.LIST_ITEM_SPACING] ?: 4,
            animationsEnabled = prefs[PreferencesKeys.ANIMATIONS_ENABLED] ?: true,
            onboardingComplete = prefs[PreferencesKeys.ONBOARDING_COMPLETE] ?: false,
            isDefaultLauncher = prefs[PreferencesKeys.IS_DEFAULT_LAUNCHER] ?: false,
            precisionModeEnabled = prefs[PreferencesKeys.PRECISION_MODE_ENABLED] ?: false,
            precisionModeConsentGiven = prefs[PreferencesKeys.PRECISION_MODE_CONSENT] ?: false,
            monochromeModeActive = prefs[PreferencesKeys.MONOCHROME_MODE_ACTIVE] ?: false,
            defaultCooldownWhenOpeningActive = prefs[PreferencesKeys.DEFAULT_COOLDOWN_WHEN_OPENING] ?: false,
            defaultTimerBehaviour = TimerBehaviour.fromString(prefs[PreferencesKeys.DEFAULT_TIMER_BEHAVIOUR]),
            reelsBlockingEnabled = prefs[PreferencesKeys.REELS_BLOCKING_ENABLED] ?: false,
            blockingChallengeActive = prefs[PreferencesKeys.BLOCKING_CHALLENGE_ACTIVE] ?: false,
            screenLockDoubleTap = prefs[PreferencesKeys.SCREEN_LOCK_DOUBLE_TAP] ?: false,
        )
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    override suspend fun updateColorTheme(theme: ColorTheme) {
        dataStore.edit { it[PreferencesKeys.COLOR_THEME] = theme.name }
    }

    override suspend fun updateFontFamily(family: FontFamily) {
        dataStore.edit { it[PreferencesKeys.FONT_FAMILY] = family.name }
    }

    override suspend fun updateFontSize(size: FontSize) {
        dataStore.edit { it[PreferencesKeys.FONT_SIZE] = size.name }
    }

    override suspend fun updateShowClock(show: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_CLOCK] = show }
    }

    override suspend fun updateShowDate(show: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_DATE] = show }
    }

    override suspend fun updateTimeFormat(format: TimeFormatType) {
        dataStore.edit { it[PreferencesKeys.TIME_FORMAT] = format.name }
    }

    override suspend fun updateListItemSpacing(spacing: Int) {
        dataStore.edit { it[PreferencesKeys.LIST_ITEM_SPACING] = spacing }
    }

    override suspend fun updateAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ANIMATIONS_ENABLED] = enabled }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_COMPLETE] = complete }
    }

    override suspend fun setPrecisionModeEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.PRECISION_MODE_ENABLED] = enabled }
    }

    override suspend fun setPrecisionModeConsent(given: Boolean) {
        dataStore.edit { it[PreferencesKeys.PRECISION_MODE_CONSENT] = given }
    }

    override suspend fun setMonochromeModeActive(active: Boolean) {
        dataStore.edit { it[PreferencesKeys.MONOCHROME_MODE_ACTIVE] = active }
    }

    override suspend fun setReelsBlockingEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.REELS_BLOCKING_ENABLED] = enabled }
    }

    override suspend fun setBlockingChallengeActive(active: Boolean) {
        dataStore.edit { it[PreferencesKeys.BLOCKING_CHALLENGE_ACTIVE] = active }
    }

    override suspend fun setScreenLockDoubleTap(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SCREEN_LOCK_DOUBLE_TAP] = enabled }
    }

    override suspend fun setDefaultTimerBehaviour(behaviour: TimerBehaviour) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_TIMER_BEHAVIOUR] = behaviour.name }
    }
}
