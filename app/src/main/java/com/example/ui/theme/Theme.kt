package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String, val description: String) {
    DARK("Simple Dark", "Clean minimalist dark theme"),
    WHITE("Simple Light", "Clean minimalist light theme"),
    SLATE("Simple Slate", "Neutral minimalist slate theme"),
}

data class AppThemeColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val socketBackground: Color,
    val socketBorder: Color,
    val buttonBackground: Color,
    val isDark: Boolean,
    val statusLiveText: Color = if (isDark) LiveGreenTextDark else LiveGreenTextLight,
    val statusOfflineText: Color = if (isDark) OfflineRedTextDark else OfflineRedTextLight,
)

val DarkAppThemeColors = AppThemeColors(
    cardBackground = DarkSurface,
    cardBorder = DarkBorder,
    socketBackground = DarkSurfaceVariant,
    socketBorder = Color(0xFF383838),
    buttonBackground = DarkSurfaceElevated,
    isDark = true,
)

val WhiteAppThemeColors = AppThemeColors(
    cardBackground = WhiteSurface,
    cardBorder = WhiteBorder,
    socketBackground = WhiteSurfaceVariant,
    socketBorder = Color(0xFFCBD5E1),
    buttonBackground = Color(0xFFE2E8F0),
    isDark = false,
)

val SlateAppThemeColors = AppThemeColors(
    cardBackground = SlateSurface,
    cardBorder = SlateBorder,
    socketBackground = SlateSurfaceVariant,
    socketBorder = Color(0xFF3E4349),
    buttonBackground = SlateSurfaceElevated,
    isDark = true,
)

val LocalAppThemeColors = staticCompositionLocalOf { SlateAppThemeColors }

val CoralRed = Color(0xFFEF4444)
val AmberPending = Color(0xFFF59E0B)
val BlueScanning = Color(0xFF3B82F6)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFF2C2C2C),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF383838),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFF9E9E9E),
    onTertiary = Color(0xFF121212),
    error = Color(0xFFCF6679),
    onError = Color(0xFF121212),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder,
)

private val WhiteColorScheme = lightColorScheme(
    primary = Color(0xFF0F172A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF334155),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF475569),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    background = WhiteBackground,
    onBackground = WhiteOnBackground,
    surface = WhiteSurface,
    onSurface = WhiteOnSurface,
    surfaceVariant = WhiteSurfaceVariant,
    onSurfaceVariant = WhiteOnSurfaceVariant,
    outline = WhiteBorder,
)

private val SlateColorScheme = darkColorScheme(
    primary = SlatePrimary,
    onPrimary = Color(0xFF16181A),
    primaryContainer = Color(0xFF2B2E32),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFA0A5AA),
    onSecondary = Color(0xFF16181A),
    secondaryContainer = Color(0xFF373B40),
    onSecondaryContainer = Color(0xFFEEEEEE),
    tertiary = Color(0xFF888D94),
    onTertiary = Color(0xFF16181A),
    error = Color(0xFFCF6679),
    onError = Color(0xFF16181A),
    background = SlateBackground,
    onBackground = SlateOnBackground,
    surface = SlateSurface,
    onSurface = SlateOnSurface,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceVariant,
    outline = SlateBorder,
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SLATE,
    content: @Composable () -> Unit,
) {
    val (colorScheme, appColors) = when (themeMode) {
        AppThemeMode.DARK -> DarkColorScheme to DarkAppThemeColors
        AppThemeMode.WHITE -> WhiteColorScheme to WhiteAppThemeColors
        AppThemeMode.SLATE -> SlateColorScheme to SlateAppThemeColors
    }

    CompositionLocalProvider(LocalAppThemeColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
