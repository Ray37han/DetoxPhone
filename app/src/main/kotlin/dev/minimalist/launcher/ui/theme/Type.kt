package dev.minimalist.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Launcher typography system.
 *
 * Design intent:
 * - System font (FontFamily.Default) to avoid large font asset downloads and
 *   to respect user's system font preferences on Android.
 * - Generous line heights and letter spacing for readability on all screen sizes.
 * - Text-first interface: the type scale must feel calm and legible, not decorative.
 *
 * The [fontScale] setting in [dev.minimalist.launcher.settings.domain.model.AppSettings]
 * is applied at the call site via LocalDensity, NOT here.
 *
 * Scale follows Material 3 naming: displayLarge → labelSmall.
 * We override only the roles relevant to a launcher interface.
 */
internal val LauncherTypography = Typography(
    // Clock display on home screen
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    // Date line on home screen
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // App list item labels
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Search field, secondary labels
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // Folder labels, caption text
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
