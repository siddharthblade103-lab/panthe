package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AmbientLightingStyle
import com.example.service.TactileSoundEngine
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalTactileSound = compositionLocalOf<TactileSoundEngine?> { null }

/**
 * Alive, ultra-smooth Apple Liquid Glass canvas:
 * Soft drifting ambient light blooms + ultra-fine architectural crystalline micro-grain texture.
 * Minimal, serene, zero visual clutter.
 */
@Composable
fun MetamorphosisBackdrop(
    modifier: Modifier = Modifier,
    lightingStyle: AmbientLightingStyle = AmbientLightingStyle.AURORA_GLASS,
    lightingIntensity: Float = 0.72f,
    edgeGlowEnabled: Boolean = true,
    isFocusActive: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val infinite = rememberInfiniteTransition(label = "liquidBackdrop")
    val driftPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isFocusActive) 12500 else 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbDrift"
    )
    val edgeSweepAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isFocusActive) 6400 else 9500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "edgeSweep"
    )

    val (orbPrimary, orbCyan, orbViolet, orbAccent) = when (lightingStyle) {
        AmbientLightingStyle.AURORA_GLASS -> listOf(
            palette.primary, palette.prismCyan, palette.prismViolet, palette.secondary
        )
        AmbientLightingStyle.GALAXY_NEBULA -> listOf(
            Color(0xFF4F46E5), Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFF2563EB)
        )
        AmbientLightingStyle.SUNSET_AMBER -> listOf(
            Color(0xFFF59E0B), Color(0xFFFB7185), Color(0xFFF97316), Color(0xFFFBBF24)
        )
        AmbientLightingStyle.MIDNIGHT_PULSE -> listOf(
            Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF10B981)
        )
        AmbientLightingStyle.CALM_MINIMAL -> listOf(
            palette.primary.copy(alpha = 0.55f),
            palette.prismCyan.copy(alpha = 0.45f),
            palette.glassBorderSubtle,
            palette.secondary.copy(alpha = 0.45f)
        )
    }

    val clampedIntensity = lightingIntensity.coerceIn(0.15f, 1f)
    val styleGain = if (lightingStyle == AmbientLightingStyle.CALM_MINIMAL) 0.42f else 1.0f
    val totalGain = clampedIntensity * styleGain * (if (isFocusActive) 1.15f else 1.0f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.canvasTop, palette.canvasBottom)
                )
            )
            // Cache static crystalline micro-lattice once per size/theme for zero per-frame loop overhead
            .drawWithCache {
                val step = 32.dp.toPx()
                val radius = 0.85.dp.toPx()
                val cols = (size.width / step).toInt() + 1
                val rows = (size.height / step).toInt() + 1
                val dotColor = palette.textureDotColor
                val points = buildList(cols * rows) {
                    for (r in 0..rows) {
                        val y = r * step
                        val xOffset = if (r % 2 == 0) 0f else step * 0.5f
                        for (c in 0..cols) {
                            add(Offset(c * step + xOffset, y))
                        }
                    }
                }
                onDrawBehind {
                    points.forEach { pt ->
                        drawCircle(color = dotColor, radius = radius, center = pt)
                    }
                }
            }
            .drawBehind {
                val primaryAlpha = ((if (palette.isDark) 0.34f else 0.25f) * totalGain).coerceIn(0.04f, 0.48f)
                val secondaryAlpha = ((if (palette.isDark) 0.26f else 0.19f) * totalGain).coerceIn(0.03f, 0.38f)

                // Slowly drifting top-left primary sunlit aurora bloom
                val orb1 = Offset(
                    x = size.width * (0.22f + 0.16f * cos(driftPhase)),
                    y = size.height * (0.10f + 0.06f * sin(driftPhase))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbPrimary.copy(alpha = primaryAlpha), Color.Transparent),
                        center = orb1,
                        radius = size.width * 0.86f
                    ),
                    radius = size.width * 0.86f,
                    center = orb1
                )

                // Slowly drifting upper-right prismatic caustic
                val orb2 = Offset(
                    x = size.width * (0.84f + 0.12f * sin(driftPhase * 0.85f)),
                    y = size.height * (0.30f + 0.08f * cos(driftPhase * 0.85f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            orbCyan.copy(alpha = secondaryAlpha),
                            orbViolet.copy(alpha = secondaryAlpha * 0.50f),
                            Color.Transparent
                        ),
                        center = orb2,
                        radius = size.width * 0.76f
                    ),
                    radius = size.width * 0.76f,
                    center = orb2
                )

                // Delicate lavender/rose refraction pool mid-left
                val orbPrism = Offset(
                    x = size.width * (0.14f + 0.10f * sin(driftPhase * 0.65f + 2.1f)),
                    y = size.height * (0.58f + 0.07f * cos(driftPhase * 0.65f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            orbViolet.copy(alpha = secondaryAlpha * 0.78f),
                            orbAccent.copy(alpha = secondaryAlpha * 0.34f),
                            Color.Transparent
                        ),
                        center = orbPrism,
                        radius = size.width * 0.66f
                    ),
                    radius = size.width * 0.66f,
                    center = orbPrism
                )

                // Soft warm glow near bottom dock
                val orb3 = Offset(
                    x = size.width * (0.64f + 0.14f * sin(driftPhase + 1.4f)),
                    y = size.height * 0.88f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbAccent.copy(alpha = secondaryAlpha * 0.88f), Color.Transparent),
                        center = orb3,
                        radius = size.width * 0.70f
                    ),
                    radius = size.width * 0.70f,
                    center = orb3
                )

                // Subtle diagonal liquid-crystal optical caustic veil
                val bandShift = 0.12f * sin(driftPhase)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = (if (palette.isDark) 0.04f else 0.18f) * clampedIntensity),
                            orbCyan.copy(alpha = (if (palette.isDark) 0.055f else 0.09f) * totalGain),
                            Color.Transparent
                        ),
                        start = Offset(size.width * (0.05f + bandShift), 0f),
                        end = Offset(size.width * (0.95f + bandShift), size.height * 0.75f)
                    )
                )

                // 5 subtle drifting optical bokeh light motes for living liquid-glass depth
                if (lightingStyle != AmbientLightingStyle.CALM_MINIMAL) {
                    val moteAlpha = (if (palette.isDark) 0.18f else 0.30f) * clampedIntensity
                    for (i in 0 until 5) {
                        val angle = driftPhase * (0.7f + i * 0.12f) + i * 1.3f
                        val mx = size.width * (0.15f + (i * 0.18f) + 0.04f * cos(angle))
                        val my = size.height * (0.16f + ((i * 0.19f) % 0.72f) + 0.03f * sin(angle))
                        val mr = (3.5f + (i % 3) * 2.2f).dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = moteAlpha), Color.Transparent),
                                center = Offset(mx, my),
                                radius = mr * 2.4f
                            ),
                            radius = mr * 2.4f,
                            center = Offset(mx, my)
                        )
                    }
                }
            }
            .drawWithContent {
                drawContent()
                // Galaxy-style ambient gradient Edge Lighting perimeter glow
                if (edgeGlowEnabled && lightingStyle != AmbientLightingStyle.CALM_MINIMAL) {
                    val edgeAlpha = ((if (isFocusActive) 0.42f else 0.24f) * clampedIntensity).coerceIn(0.06f, 0.52f)
                    val sx = size.width * (0.5f + 0.5f * cos(edgeSweepAngle))
                    val sy = size.height * (0.5f + 0.5f * sin(edgeSweepAngle))
                    val edgeBrush = Brush.linearGradient(
                        colors = listOf(
                            orbPrimary.copy(alpha = edgeAlpha),
                            orbCyan.copy(alpha = edgeAlpha * 0.85f),
                            orbViolet.copy(alpha = edgeAlpha * 0.75f),
                            orbAccent.copy(alpha = edgeAlpha * 0.60f),
                            orbPrimary.copy(alpha = edgeAlpha)
                        ),
                        start = Offset(sx, 0f),
                        end = Offset(size.width - sx, sy)
                    )
                    drawRect(
                        brush = edgeBrush,
                        style = Stroke(width = if (isFocusActive) 4.dp.toPx() else 2.5.dp.toPx())
                    )
                }
            },
        content = content
    )
}

/**
 * Choreographed staggered spring entrance tuned for crisp 60/120fps fluidity without scroll lag.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(14f) }
    val scale = remember { Animatable(0.985f) }

    LaunchedEffect(Unit) {
        delay((index * 32L).coerceAtMost(150L))
        launch { alpha.animateTo(1f, tween(210, easing = FastOutSlowInEasing)) }
        launch {
            offsetY.animateTo(
                0f,
                spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMedium)
            )
        }
        launch {
            scale.animateTo(
                1f,
                spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = offsetY.value
            this.scaleX = scale.value
            this.scaleY = scale.value
        }
    ) {
        content()
    }
}

/**
 * Tactile Liquid Crystal Glass surface with prismatic optical-dispersion borders,
 * hardware-blurred frosted refraction core, dual specular quartz bevel highlights,
 * optional animated refraction shimmer, and soft ambient shadow depth.
 */
@Composable
fun MorphismSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    elevated: Boolean = false,
    accentTint: Color? = null,
    shimmer: Boolean = false,
    frostedBlur: Boolean = true,
    blurRadius: Dp = 18.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val baseFill = if (frostedBlur) {
        if (palette.isDark) {
            if (elevated) palette.glassFillElevated.copy(alpha = 0.72f) else palette.glassFill.copy(alpha = 0.62f)
        } else {
            if (elevated) Color.White.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.72f)
        }
    } else if (elevated) {
        palette.glassFillElevated
    } else {
        palette.glassFill
    }

    val shimmerOffset = if (shimmer) {
        val infinite = rememberInfiniteTransition(label = "glassShimmer")
        infinite.animateFloat(
            initialValue = -0.6f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerX"
        ).value
    } else -2f

    // Animate caustic drift on elevated/shimmer hero panels; keep standard cards crisp & zero-overhead
    val frostDrift = if (frostedBlur && (elevated || shimmer)) {
        val infinite = rememberInfiniteTransition(label = "frostedRefraction")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "frostPhase"
        ).value
    } else 0.7f

    val surfaceBrush = remember(baseFill, accentTint, palette.isDark, frostedBlur, palette.prismCyan, palette.prismViolet) {
        val topTint = if (frostedBlur) {
            Color.White.copy(alpha = if (palette.isDark) 0.15f else 0.52f)
        } else {
            accentTint?.copy(alpha = if (palette.isDark) 0.16f else 0.08f)
                ?: Color.White.copy(alpha = if (palette.isDark) 0.07f else 0.50f)
        }
        val midTint = palette.prismCyan.copy(alpha = if (palette.isDark) 0.04f else 0.045f)
        val bottomTint = if (frostedBlur) {
            (accentTint ?: palette.prismViolet).copy(alpha = if (palette.isDark) 0.10f else 0.065f)
        } else {
            Color.Transparent
        }
        Brush.linearGradient(
            colors = listOf(topTint, midTint, bottomTint)
        )
    }

    // Prismatic Liquid Crystal optical dispersion border
    val borderBrush = remember(
        palette.glassBorderHighlight,
        palette.glassBorderSubtle,
        palette.prismCyan,
        palette.prismViolet,
        accentTint,
        frostedBlur,
        palette.isDark
    ) {
        Brush.linearGradient(
            colors = if (frostedBlur) {
                listOf(
                    Color.White.copy(alpha = if (palette.isDark) 0.50f else 0.96f),
                    palette.prismCyan.copy(alpha = if (palette.isDark) 0.30f else 0.34f),
                    (accentTint ?: palette.prismViolet).copy(alpha = if (palette.isDark) 0.28f else 0.28f),
                    Color.White.copy(alpha = if (palette.isDark) 0.22f else 0.72f)
                )
            } else {
                listOf(
                    accentTint?.copy(alpha = 0.45f) ?: palette.glassBorderHighlight,
                    palette.glassBorderSubtle,
                    palette.glassBorderSubtle.copy(alpha = 0.48f)
                )
            }
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 18.dp else 9.dp,
                shape = shape,
                ambientColor = palette.shadowAmbient,
                spotColor = palette.shadowAmbient
            )
            .clip(shape)
    ) {
        // Layer 1: GPU-friendly optical liquid-crystal refraction plane (reserves offscreen hardware blur for elevated shimmer hero panels for silky 120fps scrolling)
        if (frostedBlur) {
            val tintA = accentTint ?: palette.primary
            val tintB = palette.prismCyan
            val tintC = palette.prismViolet
            val useHardwareBlur = elevated && shimmer
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (useHardwareBlur) {
                            Modifier.blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                        } else {
                            Modifier
                        }
                    )
            ) {
                val glowAlpha = if (palette.isDark) 0.28f else 0.22f
                val secondaryGlowAlpha = if (palette.isDark) 0.22f else 0.16f

                val bloomA = Offset(
                    x = size.width * (0.20f + 0.09f * cos(frostDrift)),
                    y = size.height * (0.16f + 0.07f * sin(frostDrift))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tintA.copy(alpha = glowAlpha),
                            tintA.copy(alpha = glowAlpha * 0.36f),
                            Color.Transparent
                        ),
                        center = bloomA,
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = bloomA
                )

                val bloomB = Offset(
                    x = size.width * (0.84f + 0.09f * sin(frostDrift * 0.9f)),
                    y = size.height * (0.76f + 0.07f * cos(frostDrift * 0.9f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tintB.copy(alpha = secondaryGlowAlpha),
                            tintC.copy(alpha = secondaryGlowAlpha * 0.48f),
                            Color.Transparent
                        ),
                        center = bloomB,
                        radius = size.width * 0.68f
                    ),
                    radius = size.width * 0.68f,
                    center = bloomB
                )

                // Soft luminous center crystal quartz haze
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.12f else 0.56f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.46f, size.height * 0.32f),
                        radius = size.width * 0.62f
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.46f, size.height * 0.32f)
                )
            }
        }

        // Layer 2: Semi-transparent liquid-crystal glaze + dual specular bevels + crisp content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(baseFill)
                .background(surfaceBrush)
                .drawWithContent {
                    drawContent()
                    // Specular top inner edge highlight for liquid-crystal curvature
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (palette.isDark) 0.34f else 0.92f),
                                palette.prismCyan.copy(alpha = if (palette.isDark) 0.22f else 0.42f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.08f, 1.4.dp.toPx()),
                        end = Offset(size.width * 0.92f, 1.4.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    // Subtle lower-right optical refraction glint
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                palette.prismViolet.copy(alpha = if (palette.isDark) 0.18f else 0.26f),
                                Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.55f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.22f, size.height - 1.4.dp.toPx()),
                        end = Offset(size.width * 0.88f, size.height - 1.4.dp.toPx()),
                        strokeWidth = 1.1.dp.toPx()
                    )
                    if (shimmer) {
                        val bandStart = size.width * shimmerOffset
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    palette.prismCyan.copy(alpha = if (palette.isDark) 0.08f else 0.14f),
                                    Color.White.copy(alpha = if (palette.isDark) 0.10f else 0.32f),
                                    palette.prismViolet.copy(alpha = if (palette.isDark) 0.06f else 0.12f),
                                    Color.Transparent
                                ),
                                start = Offset(bandStart - size.width * 0.28f, 0f),
                                end = Offset(bandStart + size.width * 0.28f, size.height)
                            )
                        )
                    }
                }
                .border(
                    width = if (frostedBlur) 1.35.dp else 1.2.dp,
                    brush = borderBrush,
                    shape = shape
                )
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Duolingo-meets-Apple 3D tactile action button with physical bottom lip,
 * spring depression physics, subtle glossy top sheen, and Apple micro-click sound.
 */
@Composable
fun MorphismActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color? = null,
    lipColor: Color? = null,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    cornerRadius: Dp = 18.dp,
    hapticEvent: SystemHapticEvent = SystemHapticEvent.BUTTON_TAP,
    testTag: String? = null
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current
    val bgColor = containerColor ?: palette.primary
    val shadowLipColor = lipColor ?: palette.primaryLip

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) 3.8.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "btnPressOffset"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "btnScale"
    )

    val lipDepth = 4.dp
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveAlpha = if (enabled) 1f else 0.46f

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .defaultMinSize(minHeight = height + lipDepth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = effectiveAlpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                systemHaptics.perform(hapticEvent)
                tactile?.playTap()
                onClick()
            }
    ) {
        // 3D tactile bottom lip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(shadowLipColor)
        )

        // Top glossy tactile cap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = pressOffset)
                .clip(shape)
                .background(bgColor)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.38f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.4.sp
                    ),
                    color = contentColor
                )
            }
        }
    }
}

/**
 * Apple iOS-style fluid sliding liquid-glass segmented switcher!
 * The active frosted glass capsule glides smoothly with spring physics between tabs.
 */
@Composable
fun SlidingGlassSegmentedBar(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tagPrefix: String = "segment"
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current
    val count = labels.size.coerceAtLeast(1)
    val safeIndex = selectedIndex.coerceIn(0, count - 1)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.chipUnselected)
            .border(1.dp, palette.glassBorderSubtle, RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / count
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * safeIndex,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "segmentIndicatorX"
        )

        // Gliding frosted liquid-glass pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(4.dp, RoundedCornerShape(50), ambientColor = palette.shadowAmbient)
                .clip(RoundedCornerShape(50))
                .background(palette.glassFillElevated)
                .border(1.dp, palette.glassBorderHighlight, RoundedCornerShape(50))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            labels.forEachIndexed { idx, label ->
                val isSelected = idx == safeIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) palette.textPrimary else palette.textMuted,
                    animationSpec = tween(200),
                    label = "segTextColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("${tagPrefix}_$idx")
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = enabled
                        ) {
                            if (!isSelected) {
                                systemHaptics.tick()
                                tactile?.playTap()
                                onSelect(idx)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Clean interactive frosted glass pill chip with spring press & tactile toggle pop.
 */
@Composable
fun MorphismChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selectedColor: Color? = null,
    testTag: String? = null
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current
    val activeColor = selectedColor ?: palette.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "chipScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) activeColor else palette.chipUnselected,
        animationSpec = tween(220),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else palette.textPrimary,
        animationSpec = tween(220),
        label = "chipText"
    )

    Surface(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .defaultMinSize(minHeight = 40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null) {
                systemHaptics.toggle(on = !selected)
                tactile?.playToggle(on = !selected)
                onClick()
            },
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color.White.copy(alpha = 0.4f) else palette.glassBorderSubtle
        ),
        shadowElevation = if (selected) 5.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Smooth rolling animated number display for Gems & Streaks.
 */
@Composable
fun AnimatedCounterText(
    count: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = count,
        modifier = modifier,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { -it } + fadeIn(tween(180)))
                    .togetherWith(slideOutVertically { it } + fadeOut(tween(140)))
            } else {
                (slideInVertically { it } + fadeIn(tween(180)))
                    .togetherWith(slideOutVertically { -it } + fadeOut(tween(140)))
            }
        },
        label = "rollingCounter"
    ) { value ->
        Text(text = "$value", style = style, color = color)
    }
}

/**
 * Metamorphosis morphing container for silky spring glass transitions.
 */
@Composable
fun <T> MetamorphosisContainer(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "metamorphosis",
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(260, easing = FastOutSlowInEasing)) + scaleIn(
                initialScale = 0.96f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
            ))
                .togetherWith(
                    fadeOut(tween(170, easing = FastOutSlowInEasing)) + scaleOut(
                        targetScale = 0.98f,
                        animationSpec = tween(170)
                    )
                )
                .using(SizeTransform(clip = false))
        },
        label = label
    ) { state ->
        content(state)
    }
}

/**
 * Directional liquid-glass screen transition that glides smoothly along the tab axis
 * with spring scale and optical fade so navigating across tabs feels fluid and effortless.
 */
@Composable
fun TryAgainTabTransition(
    selectedTab: TryAgainTab,
    modifier: Modifier = Modifier,
    content: @Composable (TryAgainTab) -> Unit
) {
    AnimatedContent(
        targetState = selectedTab,
        modifier = modifier,
        transitionSpec = {
            val movingForward = targetState.ordinal >= initialState.ordinal
            val slideDistance = { width: Int -> (width * 0.055f).toInt() }
            val enter = slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium),
                initialOffsetX = { if (movingForward) slideDistance(it) else -slideDistance(it) }
            ) + fadeIn(tween(220, easing = FastOutSlowInEasing)) + scaleIn(
                initialScale = 0.982f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
            )
            val exit = slideOutHorizontally(
                animationSpec = tween(150, easing = FastOutSlowInEasing),
                targetOffsetX = { if (movingForward) -slideDistance(it) else slideDistance(it) }
            ) + fadeOut(tween(140, easing = FastOutSlowInEasing)) + scaleOut(
                targetScale = 0.99f,
                animationSpec = tween(140)
            )
            enter.togetherWith(exit).using(SizeTransform(clip = false))
        },
        label = "liquidTabTransition"
    ) { tab ->
        content(tab)
    }
}

/**
 * Subtle animated liquid-crystal glowing status dot for real-time sync and live presence.
 * - Softly breathes with a radial crystal glow while active.
 * - Emits a delicate expanding sync halo ring whenever `isSyncing` is true OR whenever
 *   `syncPulseKey` increments on a real-time Firestore snapshot update.
 */
@Composable
fun LiquidCrystalSyncDot(
    color: Color,
    isLivePulse: Boolean,
    isSyncing: Boolean,
    syncPulseKey: Int = 0,
    dotSize: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "crystalSyncDot")
    val breathe by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSyncing) 900 else 1850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotBreathe"
    )
    val continuousWave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSyncing) 1150 else 2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotWave"
    )
    val snapshotRipple = remember { Animatable(1f) }
    LaunchedEffect(syncPulseKey) {
        if (syncPulseKey > 0) {
            snapshotRipple.snapTo(0f)
            snapshotRipple.animateTo(1f, tween(durationMillis = 820, easing = FastOutSlowInEasing))
        }
    }

    val canvasSize = dotSize * 2.4f
    Canvas(modifier = modifier.size(canvasSize)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val coreRadius = dotSize.toPx() / 2f

        // Delicate expanding ring on active sync or live focus state
        if (isLivePulse || isSyncing) {
            val ringRadius = coreRadius * (1.05f + 1.35f * continuousWave)
            val ringAlpha = (1f - continuousWave) * if (isSyncing) 0.60f else 0.36f
            drawCircle(
                color = color.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx())
            )
        }

        // Crisp one-shot real-time Firestore update flare whenever a fresh snapshot lands
        if (snapshotRipple.value < 0.99f) {
            val p = snapshotRipple.value
            drawCircle(
                color = Color.White.copy(alpha = (1f - p) * 0.72f),
                radius = coreRadius * (1.1f + 1.55f * p),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx())
            )
        }

        // Soft glowing radial aura
        val glowAlpha = if (isLivePulse || isSyncing) 0.44f * breathe else 0.22f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
                center = center,
                radius = coreRadius * 2.15f
            ),
            radius = coreRadius * 2.15f,
            center = center
        )

        // Luminous liquid-crystal core dot + tiny specular pinpoint
        drawCircle(
            color = color,
            radius = coreRadius * (if (isSyncing) 0.92f + 0.14f * breathe else 1f),
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.78f),
            radius = coreRadius * 0.36f,
            center = Offset(center.x - coreRadius * 0.24f, center.y - coreRadius * 0.24f)
        )
    }
}

/**
 * Duolingo-style celebratory floating sparkle & gem confetti burst on session/task/chest rewards.
 */
@Composable
fun GemCelebrationBurst(
    triggerKey: Long,
    modifier: Modifier = Modifier
) {
    val progress = remember(triggerKey) { Animatable(0f) }
    val palette = LocalTryAgainPalette.current

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1500, easing = FastOutSlowInEasing))
    }

    if (progress.value < 0.99f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val p = progress.value
            val alpha = (1f - (p - 0.55f).coerceAtLeast(0f) * 2.2f).coerceIn(0f, 0.95f)
            val origin = Offset(size.width * 0.5f, size.height * 0.14f)
            val colors = listOf(
                palette.primary,
                palette.gemColor,
                palette.streakColor,
                Color(0xFF89E219),
                Color(0xFF38BDF8),
                Color(0xFFFFC800)
            )
            for (i in 0 until 20) {
                val angle = -PI * 0.1 + (PI * 1.2 * i / 19.0)
                val speed = (120f + (i % 5) * 48f).dp.toPx()
                val gravity = 220.dp.toPx() * p * p
                val x = origin.x + (cos(angle) * speed * p).toFloat()
                val y = origin.y + (sin(angle) * speed * p).toFloat() + gravity
                val r = (4.5f + (i % 3) * 2f).dp.toPx() * (1f - p * 0.35f)
                val c = colors[i % colors.size].copy(alpha = alpha)
                if (i % 2 == 0) {
                    val diamond = Path().apply {
                        moveTo(x, y - r)
                        lineTo(x + r * 0.78f, y)
                        lineTo(x, y + r)
                        lineTo(x - r * 0.78f, y)
                        close()
                    }
                    drawPath(diamond, color = c)
                } else {
                    drawCircle(color = c, radius = r * 0.65f, center = Offset(x, y))
                }
            }
        }
    }
}

@Composable
fun StreakPillBadge(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = palette.glassFillElevated,
        border = BorderStroke(1.dp, palette.streakColor.copy(alpha = 0.38f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = "Daily focus streak",
                tint = palette.streakColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedCounterText(
                count = streakDays,
                style = MaterialTheme.typography.labelLarge,
                color = palette.streakColor
            )
        }
    }
}

@Composable
fun GemPillBadge(
    gems: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "gemPillScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null) {
                        systemHaptics.buttonTap()
                        tactile?.playTap()
                        onClick()
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(50),
        color = palette.glassFillElevated,
        border = BorderStroke(1.dp, palette.gemColor.copy(alpha = 0.42f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Diamond,
                contentDescription = "Gem balance",
                tint = palette.gemColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedCounterText(
                count = gems,
                style = MaterialTheme.typography.labelLarge,
                color = palette.gemColor
            )
        }
    }
}

enum class TryAgainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    FOCUS("Focus", Icons.Rounded.Timer, Icons.Outlined.Timer, "nav_tab_focus"),
    SOCIAL("Social", Icons.Rounded.Groups, Icons.Outlined.Groups, "nav_tab_social"),
    SHOP("Shop", Icons.Rounded.Storefront, Icons.Outlined.Storefront, "nav_tab_shop"),
    PROFILE("Profile", Icons.Rounded.Person, Icons.Outlined.Person, "nav_tab_profile")
}

/**
 * Liquid Crystal floating bottom dock with a prismatic refractive hairline border,
 * spring-gliding active crystal capsule, and bouncy icon choreography,
 * strictly respecting system navigation bar insets.
 */
@Composable
fun TryAgainFloatingDock(
    selectedTab: TryAgainTab,
    onSelectTab: (TryAgainTab) -> Unit,
    showSocialTab: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current
    val dockShape: Shape = RoundedCornerShape(30.dp)
    val tabs = remember(showSocialTab) {
        if (showSocialTab) TryAgainTab.entries.toList()
        else TryAgainTab.entries.filter { it != TryAgainTab.SOCIAL }
    }
    val selectedIdx = tabs.indexOf(selectedTab).coerceAtLeast(0)

    val prismaticDockBorder = remember(palette.isDark, palette.prismCyan, palette.prismViolet, palette.navDockBorder) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (palette.isDark) 0.45f else 0.96f),
                palette.prismCyan.copy(alpha = if (palette.isDark) 0.28f else 0.38f),
                palette.prismViolet.copy(alpha = if (palette.isDark) 0.24f else 0.28f),
                palette.navDockBorder
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = dockShape,
                    ambientColor = palette.shadowAmbient,
                    spotColor = palette.shadowAmbient
                ),
            shape = dockShape,
            color = palette.navDockFill,
            border = BorderStroke(1.3.dp, prismaticDockBorder)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Top specular crystal edge glint
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (palette.isDark) 0.28f else 0.88f),
                                    palette.prismCyan.copy(alpha = if (palette.isDark) 0.16f else 0.34f),
                                    Color.Transparent
                                )
                            ),
                            start = Offset(size.width * 0.10f, 1.5.dp.toPx()),
                            end = Offset(size.width * 0.90f, 1.5.dp.toPx()),
                            strokeWidth = 1.3.dp.toPx()
                        )
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val tabWidth = maxWidth / tabs.size
                val pillOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIdx,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "dockSlidingPill"
                )

                // Gliding liquid-crystal active glass capsule with luminous bottom refraction glow
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(tabWidth)
                        .height(50.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        palette.primary.copy(alpha = if (palette.isDark) 0.34f else 0.24f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2f, size.height * 0.72f),
                                    radius = size.width * 0.78f
                                ),
                                radius = size.width * 0.78f,
                                center = Offset(size.width / 2f, size.height * 0.72f)
                            )
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    palette.primarySoft,
                                    palette.prismCyan.copy(alpha = if (palette.isDark) 0.18f else 0.20f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (palette.isDark) 0.38f else 0.90f),
                                    palette.primary.copy(alpha = 0.38f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab == selectedTab
                        val tint by animateColorAsState(
                            targetValue = if (isSelected) palette.primaryLip else palette.textMuted,
                            animationSpec = tween(220),
                            label = "dockIconTint"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.13f else 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dockIconScale"
                        )
                        val iconLift by animateDpAsState(
                            targetValue = if (isSelected) (-1.5).dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dockIconLift"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag(tab.testTag)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        systemHaptics.tick()
                                        tactile?.playTabGlide()
                                        onSelectTab(tab)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = tint,
                                    modifier = Modifier
                                        .size(21.dp)
                                        .offset(y = iconLift)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    ),
                                    color = tint,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
