package dev.minimalist.launcher.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.minimalist.launcher.settings.domain.model.ThemeMode

// ── Static color schemes ──────────────────────────────────────────────────────

private val LauncherLightColorScheme = lightColorScheme(
    primary         = LauncherColors.Black,
    onPrimary       = LauncherColors.White,
    secondary       = LauncherColors.Neutral20,
    onSecondary     = LauncherColors.White,
    tertiary        = LauncherColors.AccentLight,
    onTertiary      = LauncherColors.White,
    background      = LauncherColors.White,
    onBackground    = LauncherColors.Black,
    surface         = LauncherColors.White,
    onSurface       = LauncherColors.Black,
    surfaceVariant  = LauncherColors.Neutral95,
    onSurfaceVariant = LauncherColors.Neutral20,
    outline         = LauncherColors.Neutral50,
    error           = LauncherColors.ErrorLight,
    onError         = LauncherColors.OnErrorLight,
)

private val LauncherDarkColorScheme = darkColorScheme(
    primary         = LauncherColors.White,
    onPrimary       = LauncherColors.Black,
    secondary       = LauncherColors.Neutral92,
    onSecondary     = LauncherColors.Black,
    tertiary        = LauncherColors.AccentDark,
    onTertiary      = LauncherColors.Black,
    background      = LauncherColors.Black,
    onBackground    = LauncherColors.White,
    surface         = LauncherColors.Black,
    onSurface       = LauncherColors.White,
    surfaceVariant  = LauncherColors.Neutral15,
    onSurfaceVariant = LauncherColors.Neutral85,
    outline         = LauncherColors.Neutral70,
    error           = LauncherColors.ErrorDark,
    onError         = LauncherColors.OnErrorDark,
)

// ── CompositionLocal for launcher-specific extras ─────────────────────────────

/**
 * Whether animations are enabled — surfaced as a CompositionLocal so any
 * Composable can conditionally animate without passing state through the tree.
 */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

// ── Theme composable ──────────────────────────────────────────────────────────

/**
 * Root theme composable.
 *
 * Dynamic color (Material You / Android 12+):
 * - Disabled by default. The launcher's identity is intentional monochrome.
 * - Can be enabled per-user in Settings (Stage 3) if desired.
 *
 * @param themeMode       Explicit override; defaults to [ThemeMode.SYSTEM].
 * @param dynamicColor    Enable Material You dynamic color (API 31+).
 * @param animationsEnabled Injected from [AppSettings].
 * @param content         Composable content.
 */
@Composable
fun LauncherTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    animationsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        useDarkTheme -> LauncherDarkColorScheme
        else         -> LauncherLightColorScheme
    }

    // Respect system "Remove animations" / Developer Options animator scale (Section 16.3).
    val context = LocalContext.current
    val systemAnimationsEnabled = remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale > 0.05f // treat near-zero as disabled
    }

    CompositionLocalProvider(
        LocalAnimationsEnabled provides (animationsEnabled && systemAnimationsEnabled)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = LauncherTypography,
            content     = content
        )
    }
}
