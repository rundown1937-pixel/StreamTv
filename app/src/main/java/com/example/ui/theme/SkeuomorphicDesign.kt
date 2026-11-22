@file:Suppress("unused")

package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =========================================================================
// SIMPLE CLEAN MODERN THEME TOKENS
// =========================================================================

val SkeuoRubyLed = Color(0xFF9E9E9E)

// =========================================================================
// SIMPLE FLAT SURFACES & CONTAINERS
// =========================================================================

/**
 * Clean simple panel with subtle border.
 */
fun Modifier.skeuomorphicPanel(
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 0.dp,
    shadowAlpha: Float = 0f,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor ?: Color(0xFF1E1E1E))
    .border(
        width = 1.dp,
        color = borderColor ?: Color(0xFF383838),
        shape = RoundedCornerShape(cornerRadius),
    )

/**
 * Simple clean inset container for inputs and search bars.
 */
fun Modifier.skeuomorphicWell(
    cornerRadius: Dp = 10.dp,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor ?: Color(0xFF282828))
    .border(
        width = 1.dp,
        color = borderColor ?: Color(0xFF383838),
        shape = RoundedCornerShape(cornerRadius),
    )

/**
 * Simple TV screen frame with minimal border.
 */
fun Modifier.tvScreenBezel(
    cornerRadius: Dp = 12.dp,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .border(
        width = 1.dp,
        color = Color(0xFF383838),
        shape = RoundedCornerShape(cornerRadius),
    )

// =========================================================================
// CLEAN MINIMALIST COMPONENTS
// =========================================================================

/**
 * Simple clean status indicator dot with optional label.
 */
@Composable
fun SkeuoJewelLed(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    color: Color = SkeuoRubyLed,
    label: String? = "ON AIR",
    size: Dp = 7.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (isActive) color else Color(0xFF616161)),
        )

        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) color else Color(0xFF9E9E9E),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/**
 * Simple clean button with solid background, subtle press state, and minimal border.
 */
@Composable
fun SkeuoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    isActive: Boolean = false,
    cornerRadius: Dp = 10.dp,
    containerColor: Color? = null,
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val defaultBgColor = when {
        isPressed -> androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        isFocused -> androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
        isAccent -> androidx.compose.material3.MaterialTheme.colorScheme.primary
        isActive -> androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
        else -> androidx.compose.material3.MaterialTheme.colorScheme.surface
    }
    val bgColor = containerColor ?: defaultBgColor

    val defaultBorderColor = if (isFocused) {
        androidx.compose.material3.MaterialTheme.colorScheme.primary
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.outline
    }
    val strokeColor = borderColor ?: defaultBorderColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = strokeColor,
                shape = RoundedCornerShape(cornerRadius),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
