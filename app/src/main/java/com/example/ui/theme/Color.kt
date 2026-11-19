package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// ONLY NECESSARY APPLICATION COLORS
// Minimalist Monochrome Palette & Functional Status Indicators
// =========================================================================

// Functional Status Colors (Strictly Live and Offline)
val LiveGreen = Color(0xFF22C55E)        // Emerald Green for LIVE streams & active indicators
val LiveGreenTextDark = Color(0xFF4ADE80)    // High-contrast green text on dark backgrounds
val LiveGreenTextLight = Color(0xFF15803D)   // High-contrast green text on light backgrounds (WCAG AA 4.5:1+)
val LiveGreenText = LiveGreenTextDark

val OfflineRed = Color(0xFFEF4444)       // Vivid Red for OFFLINE streams & error indicators
val OfflineRedTextDark = Color(0xFFF87171)   // High-contrast red text on dark backgrounds
val OfflineRedTextLight = Color(0xFFB91C1C)  // High-contrast red text on light backgrounds (WCAG AA 4.5:1+)
val OfflineRedText = OfflineRedTextDark

// Backward-compatible semantic aliases
val EmeraldSuccess = LiveGreen
val LiveRed = OfflineRed

// =========================================================================
// DARK THEME (CLEAN MONOCHROME DARK)
// =========================================================================
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF282828)
val DarkSurfaceElevated = Color(0xFF333333)
val DarkOnBackground = Color(0xFFFFFFFF)
val DarkOnSurface = Color(0xFFEEEEEE)
val DarkOnSurfaceVariant = Color(0xFF9E9E9E)
val DarkBorder = Color(0xFF383838)

// =========================================================================
// WHITE THEME (CLEAN MATERIAL 3 HIGH-CONTRAST LIGHT)
// =========================================================================
val WhiteBackground = Color(0xFFFFFFFF)
val WhiteSurface = Color(0xFFFFFFFF)
val WhiteSurfaceVariant = Color(0xFFF1F5F9)
val WhiteOnBackground = Color(0xFF0F172A)
val WhiteOnSurface = Color(0xFF0F172A)
val WhiteOnSurfaceVariant = Color(0xFF334155)
val WhiteBorder = Color(0xFFCBD5E1)

// =========================================================================
// SLATE THEME (CLEAN MINIMALIST SLATE)
// =========================================================================
val SlateBackground = Color(0xFF16181A)
val SlateSurface = Color(0xFF202326)
val SlateSurfaceVariant = Color(0xFF2B2E32)
val SlateSurfaceElevated = Color(0xFF373B40)
val SlateOnBackground = Color(0xFFFFFFFF)
val SlateOnSurface = Color(0xFFEEEEEE)
val SlateOnSurfaceVariant = Color(0xFFA0A5AA)
val SlateBorder = Color(0xFF3E4349)
val SlatePrimary = Color(0xFFE0E0E0)
