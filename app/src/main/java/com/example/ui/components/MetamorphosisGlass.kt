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
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Explore
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val infinite = rememberInfiniteTransition(label = "liquidBackdrop")
    val driftPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbDrift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.canvasTop, palette.canvasBottom)
                )
            )
            .drawBehind {
                val primaryAlpha = if (palette.isDark) 0.26f else 0.20f
                val secondaryAlpha = if (palette.isDark) 0.20f else 0.15f

                // Slowly drifting top-left emerald/primary sunlit aurora bloom
                val orb1 = Offset(
                    x = size.width * (0.24f + 0.16f * cos(driftPhase)),
                    y = size.height * (0.10f + 0.06f * sin(driftPhase))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.primary.copy(alpha = primaryAlpha), Color.Transparent),
                        center = orb1,
                        radius = size.width * 0.82f
                    ),
                    radius = size.width * 0.82f,
                    center = orb1
                )

                // Slowly drifting upper-right sapphire/gem luminous mist behind the hero study card
                val orb2 = Offset(
                    x = size.width * (0.82f + 0.12f * sin(driftPhase * 0.85f)),
                    y = size.height * (0.34f + 0.08f * cos(driftPhase * 0.85f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.gemColor.copy(alpha = secondaryAlpha), Color.Transparent),
                        center = orb2,
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = orb2
                )

                // Soft warm glow near bottom dock
                val orb3 = Offset(
                    x = size.width * (0.32f + 0.12f * sin(driftPhase + 1.4f)),
                    y = size.height * 0.86f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.secondary.copy(alpha = secondaryAlpha * 0.85f), Color.Transparent),
                        center = orb3,
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = orb3
                )

                // Ultra-fine morphism crystalline micro-grain dot texture
                val step = 30.dp.toPx()
                val radius = 0.9.dp.toPx()
                val cols = (size.width / step).toInt() + 1
                val rows = (size.height / step).toInt() + 1
                for (r in 0..rows) {
                    val y = r * step
                    val xOffset = if (r % 2 == 0) 0f else step * 0.5f
                    for (c in 0..cols) {
                        drawCircle(
                            color = palette.textureDotColor,
                            radius = radius,
                            center = Offset(c * step + xOffset, y)
                        )
                    }
                }
            },
        content = content
    )
}

/**
 * Choreographed staggered spring entrance for cards so every screen feels silky and alive.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }
    val scale = remember { Animatable(0.97f) }

    LaunchedEffect(Unit) {
        delay((index * 55L).coerceAtMost(320L))
        launch { alpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
        launch {
            offsetY.animateTo(
                0f,
                spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch {
            scale.animateTo(
                1f,
                spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
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
 * Tactile Apple liquid-glass + soft morphism card with directional top-left specular bevel,
 * frosted inner sheen, optional real blurred backdrop frost layer (`frostedBlur = true`),
 * optional animated subtle glass shimmer, and soft ambient shadow depth.
 */
@Composable
fun MorphismSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    elevated: Boolean = false,
    accentTint: Color? = null,
    shimmer: Boolean = false,
    frostedBlur: Boolean = false,
    blurRadius: Dp = 22.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val baseFill = if (frostedBlur) {
        if (palette.isDark) {
            palette.glassFillElevated.copy(alpha = 0.44f)
        } else {
            Color.White.copy(alpha = 0.54f)
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
                animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerX"
        ).value
    } else -2f

    val frostDrift = if (frostedBlur) {
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
    } else 0f

    val surfaceBrush = remember(baseFill, accentTint, palette.isDark, frostedBlur) {
        val topTint = if (frostedBlur) {
            Color.White.copy(alpha = if (palette.isDark) 0.14f else 0.46f)
        } else {
            accentTint?.copy(alpha = if (palette.isDark) 0.16f else 0.08f)
                ?: Color.White.copy(alpha = if (palette.isDark) 0.07f else 0.50f)
        }
        val bottomTint = if (frostedBlur) {
            (accentTint ?: palette.primary).copy(alpha = if (palette.isDark) 0.08f else 0.06f)
        } else {
            Color.Transparent
        }
        Brush.linearGradient(
            colors = listOf(topTint, Color.Transparent, bottomTint)
        )
    }

    val borderBrush = remember(palette.glassBorderHighlight, palette.glassBorderSubtle, accentTint, frostedBlur, palette.isDark) {
        Brush.linearGradient(
            colors = if (frostedBlur) {
                listOf(
                    Color.White.copy(alpha = if (palette.isDark) 0.42f else 0.92f),
                    (accentTint ?: palette.primary).copy(alpha = if (palette.isDark) 0.24f else 0.20f),
                    Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.58f)
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
        // Layer 1: Dedicated hardware-blurred background refraction plane (strictly behind crisp content)
        if (frostedBlur) {
            val tintA = accentTint ?: palette.primary
            val tintB = palette.gemColor
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
            ) {
                val glowAlpha = if (palette.isDark) 0.30f else 0.24f
                val secondaryGlowAlpha = if (palette.isDark) 0.22f else 0.18f

                val bloomA = Offset(
                    x = size.width * (0.22f + 0.10f * cos(frostDrift)),
                    y = size.height * (0.18f + 0.08f * sin(frostDrift))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tintA.copy(alpha = glowAlpha), Color.Transparent),
                        center = bloomA,
                        radius = size.width * 0.68f
                    ),
                    radius = size.width * 0.68f,
                    center = bloomA
                )

                val bloomB = Offset(
                    x = size.width * (0.82f + 0.10f * sin(frostDrift * 0.9f)),
                    y = size.height * (0.76f + 0.08f * cos(frostDrift * 0.9f))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tintB.copy(alpha = secondaryGlowAlpha), Color.Transparent),
                        center = bloomB,
                        radius = size.width * 0.62f
                    ),
                    radius = size.width * 0.62f,
                    center = bloomB
                )

                // Soft luminous center frost haze
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.12f else 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.42f),
                        radius = size.width * 0.60f
                    ),
                    radius = size.width * 0.60f,
                    center = Offset(size.width * 0.5f, size.height * 0.42f)
                )
            }
        }

        // Layer 2: Semi-transparent frosted glass glaze + specular highlights + crisp content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(baseFill)
                .background(surfaceBrush)
                .drawWithContent {
                    drawContent()
                    // Specular top inner edge highlight for crisp liquid-glass curvature
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (palette.isDark) 0.24f else 0.85f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.10f, 1.5.dp.toPx()),
                        end = Offset(size.width * 0.90f, 1.5.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    if (shimmer) {
                        val bandStart = size.width * shimmerOffset
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (palette.isDark) 0.07f else 0.24f),
                                    Color.Transparent
                                ),
                                start = Offset(bandStart - size.width * 0.25f, 0f),
                                end = Offset(bandStart + size.width * 0.25f, size.height)
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
    testTag: String? = null
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
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
 * Metamorphosis morphing container for smooth state/tab transitions.
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
            (fadeIn(tween(240)) + scaleIn(
                initialScale = 0.97f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            ))
                .togetherWith(fadeOut(tween(160)))
                .using(SizeTransform(clip = false))
        },
        label = label
    ) { state ->
        content(state)
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
    JOURNEY("Path", Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_tab_journey"),
    SHOP("Shop", Icons.Rounded.Storefront, Icons.Outlined.Storefront, "nav_tab_shop"),
    PROFILE("Profile", Icons.Rounded.Person, Icons.Outlined.Person, "nav_tab_profile")
}

/**
 * Apple liquid-glass floating bottom dock with a spring-gliding active glass capsule
 * and bouncy icon choreography, strictly respecting system navigation bar insets.
 */
@Composable
fun TryAgainFloatingDock(
    selectedTab: TryAgainTab,
    onSelectTab: (TryAgainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val dockShape: Shape = RoundedCornerShape(30.dp)
    val tabs = TryAgainTab.entries
    val selectedIdx = tabs.indexOf(selectedTab).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
            border = BorderStroke(1.2.dp, palette.navDockBorder)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp)
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

                // Gliding frosted active glass capsule
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(tabWidth)
                        .height(50.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.primarySoft)
                        .border(
                            width = 1.dp,
                            color = palette.primary.copy(alpha = 0.24f),
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
                            targetValue = if (isSelected) 1.14f else 0.96f,
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
                                        tactile?.playTap()
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
                                        .size(22.dp)
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
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    ),
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
