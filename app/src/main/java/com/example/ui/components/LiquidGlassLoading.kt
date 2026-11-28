package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTryAgainPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Liquid-Crystal Orbital Prism Loader:
 * Dual counter-rotating luminous arcs with specular orbiting crystal nodes
 * and a softly breathing center core. Smooth 60/120fps pure Canvas drawing.
 */
@Composable
fun LiquidPrismOrbLoader(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    primaryColor: Color? = null,
    secondaryColor: Color? = null
) {
    val palette = LocalTryAgainPalette.current
    val main = primaryColor ?: palette.primary
    val accent = secondaryColor ?: palette.prismCyan
    val violet = palette.prismViolet

    val infinite = rememberInfiniteTransition(label = "liquidPrismLoader")
    val outerAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerOrbit"
    )
    val innerAngle by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerOrbit"
    )
    val coreBreath by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreBreath"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokeOuter = (this.size.minDimension * 0.11f).coerceAtLeast(2.2.dp.toPx())
        val strokeInner = (strokeOuter * 0.72f).coerceAtLeast(1.6.dp.toPx())
        val padOuter = strokeOuter
        val padInner = strokeOuter * 2.4f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Subtle recessed glass track
        drawCircle(
            color = palette.glassBorderSubtle.copy(alpha = 0.35f),
            radius = (this.size.minDimension / 2f) - padOuter,
            center = center,
            style = Stroke(width = strokeOuter * 0.65f)
        )

        // Outer luminous prism arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(Color.Transparent, main, accent, Color.White),
                center = center
            ),
            startAngle = outerAngle,
            sweepAngle = 210f,
            useCenter = false,
            topLeft = Offset(padOuter, padOuter),
            size = Size(this.size.width - padOuter * 2, this.size.height - padOuter * 2),
            style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
        )

        // Counter-rotating inner crystal arc
        if (this.size.minDimension > padInner * 2.4f) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, violet, accent),
                    center = center
                ),
                startAngle = innerAngle,
                sweepAngle = 155f,
                useCenter = false,
                topLeft = Offset(padInner, padInner),
                size = Size(this.size.width - padInner * 2, this.size.height - padInner * 2),
                style = Stroke(width = strokeInner, cap = StrokeCap.Round)
            )
        }

        // Orbiting bright specular bead at arc leading tip
        val tipRad = ((outerAngle + 210f) * PI / 180.0)
        val outerR = (this.size.minDimension / 2f) - padOuter
        val tipPos = Offset(
            x = center.x + (outerR * cos(tipRad)).toFloat(),
            y = center.y + (outerR * sin(tipRad)).toFloat()
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = strokeOuter * 0.56f,
            center = tipPos
        )

        // Breathing luminous crystal core
        val coreR = (this.size.minDimension * 0.13f) * (0.8f + 0.25f * coreBreath)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    main.copy(alpha = 0.55f * coreBreath),
                    Color.Transparent
                ),
                center = center,
                radius = coreR * 2.1f
            ),
            radius = coreR * 2.1f,
            center = center
        )
        drawCircle(
            color = accent.copy(alpha = 0.85f),
            radius = coreR * 0.65f,
            center = center
        )
    }
}

/**
 * Optical Liquid-Glass shimmer skeleton bar for ultra-smooth perceived loading.
 */
@Composable
fun LiquidShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    accent: Color? = null
) {
    val palette = LocalTryAgainPalette.current
    val highlight = accent ?: palette.prismCyan
    val infinite = rememberInfiniteTransition(label = "liquidShimmerBox")
    val shift by infinite.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerShift"
    )

    val baseColor = if (palette.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        palette.textMuted.copy(alpha = 0.12f)
    }
    val glowColor = if (palette.isDark) {
        highlight.copy(alpha = 0.20f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseColor)
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = if (palette.isDark) 0.10f else 0.42f),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val x = size.width * shift
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        glowColor,
                        highlight.copy(alpha = if (palette.isDark) 0.16f else 0.18f),
                        Color.Transparent
                    ),
                    start = Offset(x - size.width * 0.45f, 0f),
                    end = Offset(x + size.width * 0.45f, size.height)
                )
            )
        }
    }
}

/**
 * Sleek luminous Liquid-Glass horizontal loading bar with travelling crystal beam.
 */
@Composable
fun LiquidLoadingBar(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = Color.White.copy(alpha = 0.24f),
    fillColors: List<Color> = listOf(Color.White, Color(0xFFD9F99D))
) {
    val infinite = rememberInfiniteTransition(label = "liquidBarBeam")
    val beam by infinite.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "barBeam"
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .border(0.8.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(50))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val fillWidth = if (progress != null) {
                (size.width * progress.coerceIn(0f, 1f))
            } else {
                size.width * 0.42f
            }
            val startX = if (progress != null) 0f else (size.width * beam - fillWidth * 0.5f)
            if (fillWidth > 1f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = fillColors,
                        startX = startX.coerceAtLeast(0f),
                        endX = (startX + fillWidth).coerceAtLeast(1f)
                    ),
                    topLeft = Offset(startX.coerceAtLeast(0f), 0f),
                    size = Size(fillWidth, size.height)
                )
            }
            // Specular highlight beam sweeping across the filled bar
            if (progress != null && fillWidth > 8f) {
                val glintX = fillWidth * beam
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.72f),
                            Color.Transparent
                        ),
                        startX = glintX - size.width * 0.16f,
                        endX = glintX + size.width * 0.16f
                    ),
                    size = Size(fillWidth, size.height)
                )
            }
        }
    }
}

/**
 * Rich shimmering Liquid-Glass skeleton for Gemini Insights loading state.
 */
@Composable
fun LiquidInsightsSkeleton(
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.glassFillElevated,
        border = BorderStroke(1.dp, palette.prismViolet.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiquidPrismOrbLoader(
                    size = 32.dp,
                    primaryColor = palette.prismViolet,
                    secondaryColor = palette.prismCyan
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    LiquidLoadingBar(
                        progress = null,
                        modifier = Modifier.fillMaxWidth(0.72f),
                        height = 4.dp,
                        trackColor = palette.prismViolet.copy(alpha = 0.16f),
                        fillColors = listOf(palette.prismViolet, palette.prismCyan)
                    )
                }
            }

            LiquidShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                cornerRadius = 8.dp,
                accent = palette.prismViolet
            )
            LiquidShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .height(14.dp),
                cornerRadius = 8.dp,
                accent = palette.prismCyan
            )
            LiquidShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                cornerRadius = 14.dp,
                accent = palette.primary
            )
        }
    }
}

/**
 * Compact shimmering skeleton for Gemini AI Coach inside the Focus Session Summary Dialog.
 */
@Composable
fun LiquidCoachSkeleton(
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidPrismOrbLoader(
                size = 24.dp,
                primaryColor = palette.prismViolet,
                secondaryColor = palette.prismCyan
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textSecondary
            )
        }
        LiquidShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            cornerRadius = 6.dp,
            accent = palette.prismViolet
        )
        LiquidShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .height(12.dp),
            cornerRadius = 6.dp,
            accent = palette.prismCyan
        )
    }
}

/**
 * Shimmering Liquid-Glass podium & rows skeleton for the Global Streaks Leaderboard.
 */
@Composable
fun LiquidLeaderboardSkeleton(
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header pill with Orbital Prism Loader
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            LiquidPrismOrbLoader(
                size = 26.dp,
                primaryColor = palette.primary,
                secondaryColor = palette.prismCyan
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.textSecondary
            )
        }

        // Podium skeleton pillars (#2, #1, #3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(64.dp, 86.dp, 56.dp).forEachIndexed { idx, pillarHeight ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidShimmerBox(
                        modifier = Modifier.size(if (idx == 1) 44.dp else 36.dp),
                        cornerRadius = 50.dp,
                        accent = if (idx == 1) palette.streakColor else palette.prismCyan
                    )
                    LiquidShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pillarHeight),
                        cornerRadius = 16.dp,
                        accent = if (idx == 1) palette.streakColor else palette.prismViolet
                    )
                }
            }
        }

        // 3 ranked list row skeletons
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.glassFillElevated.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidShimmerBox(
                    modifier = Modifier.size(34.dp),
                    cornerRadius = 50.dp,
                    accent = palette.primary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(12.dp),
                        cornerRadius = 6.dp
                    )
                    LiquidShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(10.dp),
                        cornerRadius = 6.dp
                    )
                }
                LiquidShimmerBox(
                    modifier = Modifier
                        .width(48.dp)
                        .height(22.dp),
                    cornerRadius = 50.dp,
                    accent = palette.streakColor
                )
            }
        }
    }
}
