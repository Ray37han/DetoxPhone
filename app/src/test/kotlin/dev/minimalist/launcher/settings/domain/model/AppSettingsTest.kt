package dev.minimalist.launcher.settings.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [ThemeMode] and [AppSettings] defaults.
 */
class AppSettingsTest {

    @Test
    fun `ThemeMode fromString returns correct enum for valid values`() {
        assertThat(ThemeMode.fromString("LIGHT")).isEqualTo(ThemeMode.LIGHT)
        assertThat(ThemeMode.fromString("DARK")).isEqualTo(ThemeMode.DARK)
        assertThat(ThemeMode.fromString("SYSTEM")).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `ThemeMode fromString returns SYSTEM for null`() {
        assertThat(ThemeMode.fromString(null)).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `ThemeMode fromString returns SYSTEM for unknown value`() {
        assertThat(ThemeMode.fromString("UNKNOWN")).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `AppSettings default values are sane`() {
        val settings = AppSettings()
        assertThat(settings.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(settings.showClock).isTrue()
        assertThat(settings.showDate).isTrue()
        assertThat(settings.fontScale).isEqualTo(1.0f)
        assertThat(settings.listItemSpacing).isEqualTo(4)
        assertThat(settings.animationsEnabled).isTrue()
        assertThat(settings.onboardingComplete).isFalse()
    }
}
