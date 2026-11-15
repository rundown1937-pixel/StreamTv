package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// ONLY NECESSARY APPLICATION COLORS
// Minimalist Monochrome Palette & Functional Status Indicators
// =========================================================================

// Functional Status Colors (Strictly Live and Offline)
val LiveGreen = Color(0xFF22C55E)        // Emerald Green for LIVE streams & active indicators
val LiveGreenText = Color(0xFF4ADE80)    // High-contrast green text on dark backgrounds
val OfflineRed = Color(0xFFEF4444)       // Vivid Red for OFFLINE streams & error indicators
val OfflineRedText = Color(0xFFF87171)   // High-contrast red text on dark backgrounds

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
val WhiteSurfaceVariant = Color(0xFFF8F9FA)
val WhiteOnBackground = Color(0xFF1E1E1E)
val WhiteOnSurface = Color(0xFF1E1E1E)
val WhiteOnSurfaceVariant = Color(0xFF424242)
val WhiteBorder = Color(0xFFE5E7EB)

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
