package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// =========================================================================
// SKEUOMORPHIC PALETTE & PHYSICAL LIGHTING TOKENS
// =========================================================================

// Metallic Studio Console & Chassis
val SkeuoChassisDeep = Color(0xFF0D0F14)
val SkeuoChassisMid = Color(0xFF161A22)
val SkeuoChassisElevated = Color(0xFF1F2430)
val SkeuoMetalFaceplate = Color(0xFF262C38)
val SkeuoMetalHighlight = Color(0xFF3B4456)
val SkeuoMetalPlateBottom = Color(0xFF131720)

// Inset display wells & bezels
val SkeuoWellDark = Color(0xFF08090D)
val SkeuoWellCavity = Color(0xFF0F1218)
val SkeuoVfdScreenBg = Color(0xFF05080E)

// Specular reflection & Ambient occlusion borders
val SkeuoHighlightTop = Color(0x55FFFFFF)
val SkeuoHighlightSubtle = Color(0x22FFFFFF)
val SkeuoShadowBottom = Color(0xCC000000)
val SkeuoShadowSubtle = Color(0x66000000)

// Jewel indicator lamps & vacuum tube glows
val SkeuoRubyLed = Color(0xFFFF2222)
val SkeuoRubyGlow = Color(0x80FF2222)
val SkeuoAmberGlow = Color(0xFFFF9E0B)
val SkeuoAmberDim = Color(0x40FF9E0B)
val SkeuoEmeraldLed = Color(0xFF00E676)
val SkeuoEmeraldGlow = Color(0x8000E676)
val SkeuoPhosphorCyan = Color(0xFF00E5FF)
val SkeuoPhosphorGlow = Color(0x8000E5FF)
val SkeuoTungstenWarm = Color(0xFFFFCC66)

// Brass / Gold & Knurled Aluminum
val SkeuoBrassGold = Color(0xFFD4AF37)
val SkeuoBrassDark = Color(0xFF7A5F18)
val SkeuoAluminumLight = Color(0xFFE2E8F0)
val SkeuoAluminumMid = Color(0xFF94A3B8)
val SkeuoAluminumDark = Color(0xFF475569)

// =========================================================================
// SKEUOMORPHIC GRADIENT BRUSHES
// =========================================================================

val SkeuoPlateBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2E3544),
        Color(0xFF202632),
        Color(0xFF181C26),
        Color(0xFF141820)
    )
)

val SkeuoRaisedButtonBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF3D4759),
        Color(0xFF2B3342),
        Color(0xFF1E2430)
    )
)

val SkeuoPressedButtonBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF141820),
        Color(0xFF1A202A),
        Color(0xFF222936)
    )
)

val SkeuoAccentButtonBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF00F0FF),
        Color(0xFF00A2C7),
        Color(0xFF007291)
    )
)

val SkeuoGoldPlateBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFBF2C4),
        Color(0xFFD4AF37),
        Color(0xFFA17F1A),
        Color(0xFFE3C766)
    )
)

val SkeuoRecessedWellBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF07090D),
        Color(0xFF0D1016),
        Color(0xFF12161E)
    )
)

// =========================================================================
// SKEUOMORPHIC MODIFIERS
// =========================================================================

/**
 * Renders a tactile 3D physical beveled panel with cast shadow,
 * top-left specular highlight edge, and bottom-right dark bevel shadow.
 */
fun Modifier.skeuomorphicPanel(
    cornerRadius: Dp = 14.dp,
    elevation: Dp = 6.dp,
    highlightAlpha: Float = 0.28f,
    shadowAlpha: Float = 0.85f,
    backgroundColor: Color? = null
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = RoundedCornerShape(cornerRadius),
        clip = false,
        ambientColor = Color.Black,
        spotColor = Color.Black
    )
    .clip(RoundedCornerShape(cornerRadius))
    .then(
        if (backgroundColor != null) Modifier.background(backgroundColor)
        else Modifier.background(SkeuoPlateBrush)
    )
    .drawBehind {
        val strokeWidth = 1.2.dp.toPx()
        // Top highlight line (light from above)
        drawLine(
            color = Color.White.copy(alpha = highlightAlpha),
            start = Offset(cornerRadius.toPx(), strokeWidth / 2),
            end = Offset(size.width - cornerRadius.toPx(), strokeWidth / 2),
            strokeWidth = strokeWidth
        )
        // Bottom shadow line (shadow underneath)
        drawLine(
            color = Color.Black.copy(alpha = shadowAlpha),
            start = Offset(cornerRadius.toPx(), size.height - strokeWidth / 2),
            end = Offset(size.width - cornerRadius.toPx(), size.height - strokeWidth / 2),
            strokeWidth = strokeWidth * 1.5f
        )
    }
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = highlightAlpha),
            0.5f to Color.White.copy(alpha = 0.05f),
            1.0f to Color.Black.copy(alpha = shadowAlpha)
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Renders a debossed/recessed well (groove cut into the metal plate
 * for screens, displays, or input bars).
 */
fun Modifier.skeuomorphicWell(
    cornerRadius: Dp = 10.dp,
    depth: Dp = 2.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(SkeuoRecessedWellBrush)
    .drawBehind {
        val depthPx = depth.toPx()
        // Dark inner drop-shadow at the top
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.9f),
                    Color.Black.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = depthPx * 4
            )
        )
        // Bottom specular reflection edge
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(cornerRadius.toPx(), size.height - 1f),
            end = Offset(size.width - cornerRadius.toPx(), size.height - 1f),
            strokeWidth = 1.2.dp.toPx()
        )
    }
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            0.0f to Color.Black.copy(alpha = 0.9f),
            0.6f to Color(0xFF1E2430),
            1.0f to Color.White.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * TV Screen Bezel with authentic monitor rim and glass glare reflection
 */
fun Modifier.tvScreenBezel(
    cornerRadius: Dp = 16.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .border(
        width = 2.5.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF4A5568),
                Color(0xFF2D3748),
                Color(0xFF1A202C)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .drawBehind {
        // Diagonal glass reflection across the top-right
        val path = Path().apply {
            moveTo(size.width * 0.35f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height * 0.45f)
            lineTo(size.width * 0.65f, size.height * 0.65f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.015f),
                    Color.Transparent
                ),
                start = Offset(size.width * 0.5f, 0f),
                end = Offset(size.width, size.height * 0.5f)
            )
        )
    }

// =========================================================================
// SKEUOMORPHIC HARDWARE COMPONENTS
// =========================================================================

/**
 * Authentic metallic screw with slotted drive head and cast shadow.
 */
@Composable
fun SkeuoScrew(
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    rotationDegrees: Float = 42f,
    isBrass: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(radius, radius)

        // Drop shadow under screw head
        drawCircle(
            color = Color.Black.copy(alpha = 0.6f),
            radius = radius,
            center = center + Offset(0.8f, 1.2f)
        )

        // Screw head metallic rim
        drawCircle(
            brush = if (isBrass) {
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF6E08A), Color(0xFFC49826), Color(0xFF7A5812)),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color(0xFF7E8B9B), Color(0xFF384352), Color(0xFF1E2530)),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height)
                )
            },
            radius = radius * 0.95f,
            center = center
        )

        // Inset slot groove
        val angleRad = Math.toRadians(rotationDegrees.toDouble())
        val slotLength = radius * 1.2f
        val dx = (slotLength / 2f * cos(angleRad)).toFloat()
        val dy = (slotLength / 2f * sin(angleRad)).toFloat()

        // Shadow inside slot
        drawLine(
            color = Color.Black.copy(alpha = 0.9f),
            start = Offset(center.x - dx, center.y - dy),
            end = Offset(center.x + dx, center.y + dy),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Highlight rim on slot edge
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(center.x - dx + 0.5f, center.y - dy + 0.8f),
            end = Offset(center.x + dx + 0.5f, center.y + dy + 0.8f),
            strokeWidth = 0.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Realistic Studio Jewel Lamp with faceted reflection and outer glow.
 */
@Composable
fun SkeuoJewelLed(
    isActive: Boolean,
    color: Color = SkeuoRubyLed,
    label: String? = null,
    size: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size + 6.dp)) {
            Canvas(modifier = Modifier.size(size + 6.dp)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val ledRadius = size.toPx() / 2f

                // Outer diffuse radial glow when active
                if (isActive) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.7f),
                                color.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = ledRadius * 2.2f
                        ),
                        radius = ledRadius * 2.2f,
                        center = center
                    )
                }

                // Chrome/Bezel mounting ring
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF8895A5), Color(0xFF262D37), Color(0xFF101318)),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height)
                    ),
                    radius = ledRadius + 1.5.dp.toPx(),
                    center = center
                )

                // Lens body
                val lensBrush = if (isActive) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            color,
                            color.copy(alpha = 0.85f)
                        ),
                        center = center - Offset(ledRadius * 0.25f, ledRadius * 0.25f),
                        radius = ledRadius
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.25f),
                            Color(0xFF1A1D24)
                        ),
                        center = center,
                        radius = ledRadius
                    )
                }

                drawCircle(
                    brush = lensBrush,
                    radius = ledRadius,
                    center = center
                )

                // Specular glint on glass dome
                if (isActive) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = ledRadius * 0.3f,
                        center = center - Offset(ledRadius * 0.3f, ledRadius * 0.3f)
                    )
                }
            }
        }

        if (!label.isNullOrEmpty()) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) color else Color(0xFF6B7280),
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Tactile 3D Push Button with real pressed down travel, convex surface,
 * and illuminated highlight.
 */
@Composable
fun SkeuoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    isActive: Boolean = false,
    cornerRadius: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translateY by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = tween(durationMillis = 80),
        label = "btn_depth_anim"
    )

    Box(
        modifier = modifier
            .offset(y = translateY.dp)
            .shadow(
                elevation = if (isPressed) 1.dp else 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                when {
                    isPressed -> SkeuoPressedButtonBrush
                    isAccent -> SkeuoAccentButtonBrush
                    isActive -> Brush.verticalGradient(listOf(Color(0xFF004455), Color(0xFF002733)))
                    else -> SkeuoRaisedButtonBrush
                }
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    0.0f to if (isPressed) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f),
                    0.6f to if (isAccent) Color(0xFF00E5FF) else Color(0xFF333D4F),
                    1.0f to Color.Black.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Backlit Analog VU / Signal Level Meter with vintage curved dial arc
 * and calibrated needle.
 */
@Composable
fun SkeuoVuMeter(
    level: Float, // 0.0f to 1.0f
    title: String = "SIGNAL VU",
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350),
        label = "vu_meter_needle"
    )

    Box(
        modifier = modifier
            .skeuomorphicWell(cornerRadius = 8.dp)
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            // Backlit vintage warm yellow/amber glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3A2B10),
                        Color(0xFF1F180A),
                        Color(0xFF0B0D12)
                    ),
                    center = Offset(width / 2f, height * 1.1f),
                    radius = width * 0.8f
                )
            )

            // Calibrated Arc Scale (-20 to +3 dB)
            val pivot = Offset(width / 2f, height * 1.15f)
            val arcRadius = height * 0.9f

            // Green/Amber/Red tick zones
            val startAngle = -140f
            val sweepTotal = 100f

            for (i in 0..10) {
                val frac = i / 10f
                val currentAngle = startAngle + (frac * sweepTotal)
                val rad = Math.toRadians(currentAngle.toDouble())
                val isRedZone = frac > 0.8f

                val tickInner = arcRadius - (if (i % 2 == 0) 10f else 6f)
                val tickOuter = arcRadius

                val start = Offset(
                    (pivot.x + tickInner * cos(rad)).toFloat(),
                    (pivot.y + tickInner * sin(rad)).toFloat()
                )
                val end = Offset(
                    (pivot.x + tickOuter * cos(rad)).toFloat(),
                    (pivot.y + tickOuter * sin(rad)).toFloat()
                )

                drawLine(
                    color = if (isRedZone) SkeuoRubyLed else Color(0xFFFFCC66).copy(alpha = 0.85f),
                    start = start,
                    end = end,
                    strokeWidth = if (i % 2 == 0) 1.5f else 1f
                )
            }

            // Meter Needle
            val needleAngle = startAngle + (animatedLevel * sweepTotal)
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val needleLength = arcRadius * 0.95f
            val needleTip = Offset(
                (pivot.x + needleLength * cos(needleRad)).toFloat(),
                (pivot.y + needleLength * sin(needleRad)).toFloat()
            )

            // Needle shadow
            drawLine(
                color = Color.Black.copy(alpha = 0.7f),
                start = pivot + Offset(2f, 2f),
                end = needleTip + Offset(2f, 2f),
                strokeWidth = 2f
            )

            // Needle body
            drawLine(
                color = SkeuoRubyLed,
                start = pivot,
                end = needleTip,
                strokeWidth = 1.8f,
                cap = StrokeCap.Round
            )

            // Pivot hub
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF475569), Color(0xFF0F172A)),
                    center = pivot,
                    radius = 12f
                ),
                radius = 12f,
                center = pivot
            )
        }

        // Title and readout labels
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFCC66).copy(alpha = 0.8f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Vacuum Fluorescent Display (VFD) style segmented digital readout.
 */
@Composable
fun SkeuoVfdDisplay(
    text: String,
    label: String = "STATUS",
    glowColor: Color = SkeuoPhosphorCyan,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .skeuomorphicWell(cornerRadius = 6.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = glowColor,
            letterSpacing = 1.5.sp,
            modifier = Modifier.drawBehind {
                // Subtle glow under text
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.15f), Color.Transparent),
                        center = center,
                        radius = size.width / 2f
                    )
                )
            }
        )
    }
}

/**
 * Tactile Rotary Dial Knob with milled aluminum ring, indicator tick,
 * and drag gesture rotation.
 */
@Composable
fun SkeuoRotaryKnob(
    value: Float, // 0.0f to 1.0f
    onValueChange: (Float) -> Unit,
    label: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(4.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += -dragAmount.y + dragAmount.x
                            val delta = dragAccumulator / 180f
                            onValueChange((value + delta).coerceIn(0f, 1f))
                            dragAccumulator = 0f
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val radius = this.size.minDimension / 2f
                val center = Offset(radius, radius)

                // Outer knurled ridge ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF333C4A),
                            Color(0xFF6B788C),
                            Color(0xFF1E2430),
                            Color(0xFF6B788C),
                            Color(0xFF333C4A)
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center
                )

                // Inner beveled aluminum cap
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF556377), Color(0xFF2B3340), Color(0xFF151922)),
                        center = center - Offset(radius * 0.2f, radius * 0.2f),
                        radius = radius * 0.82f
                    ),
                    radius = radius * 0.82f,
                    center = center
                )

                // Indicator tick notch (-135 deg to +135 deg)
                val angleDeg = -135f + (value * 270f)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val tickStart = radius * 0.35f
                val tickEnd = radius * 0.78f

                val start = Offset(
                    (center.x + tickStart * cos(angleRad)).toFloat(),
                    (center.y + tickStart * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + tickEnd * cos(angleRad)).toFloat(),
                    (center.y + tickEnd * sin(angleRad)).toFloat()
                )

                // Tick glow line
                drawLine(
                    color = SkeuoPhosphorCyan,
                    start = start,
                    end = end,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp
        )
    }
}
