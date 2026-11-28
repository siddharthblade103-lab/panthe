package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppIconShapeId
import com.example.data.model.CustomAppIconId
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SilkyEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Silky Liquid-Glass Opening Animation showcasing the user's customized app icon:
 * - Multi-layered refractive crystal ripple waves & orbital specular halo
 * - Fluid damped spring bloom of the user's chosen CustomAppIconEmblem
 * - Staggered typographic glide + shimmering glass progress pill
 * - Seamless zoom-dissolve into Focus Hub (strictly silent on open & close)
 */
@Composable
fun TryAgainSplashOverlay(
    onFinished: () -> Unit,
    customIconId: CustomAppIconId = CustomAppIconId.MONKEY_CLASSIC,
    customIconShapeId: AppIconShapeId = AppIconShapeId.SQUIRCLE,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current

    val iconScale = remember { Animatable(0.62f) }
    val iconOffsetY = remember { Animatable(48f) }
    val iconAlpha = remember { Animatable(0f) }
    val rippleProgress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(24f) }
    val pillAlpha = remember { Animatable(0f) }
    val loadProgress = remember { Animatable(0.06f) }
    val overlayAlpha = remember { Animatable(1f) }
    val overlayScale = remember { Animatable(1f) }

    val infinite = rememberInfiniteTransition(label = "liquidSplashAmbient")
    val haloOrbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing)
        ),
        label = "splashHaloOrbit"
    )
    val specularSweep by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
        ),
        label = "splashSpecularSweep"
    )

    val splashGradientColors = remember(palette.isDark, palette.primary, palette.prismCyan, palette.prismViolet) {
        if (palette.isDark) {
            listOf(
                Color(0xFF07131E),
                palette.primary.copy(alpha = 0.42f),
                Color(0xFF091B29)
            )
        } else {
            listOf(
                palette.primary,
                Color(0xFF2CB67D),
                Color(0xFF1CB0F6)
            )
        }
    }

    LaunchedEffect(Unit) {
        // Continuous buttery progress fill
        launch {
            loadProgress.animateTo(1f, tween(durationMillis = 1220, easing = SilkyEaseOut))
        }

        // Phase 1: Silky icon bloom + glide into center
        launch {
            iconAlpha.animateTo(1f, tween(340, easing = SilkyEaseOut))
        }
        launch {
            iconOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // Phase 2: Expanding concentric crystal ripples & wordmark glide
        delay(120)
        launch {
            rippleProgress.animateTo(1f, tween(980, easing = SilkyEaseOut))
        }
        launch {
            titleAlpha.animateTo(1f, tween(380, easing = SilkyEaseOut))
        }
        launch {
            titleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(440, easing = SilkyEaseOut)
            )
        }

        delay(160)
        launch {
            pillAlpha.animateTo(1f, tween(340, easing = SilkyEaseOut))
        }

        // Brief peaceful hold, then silky glass zoom-dissolve into Focus Hub
        delay(860)
        launch {
            overlayScale.animateTo(1.07f, tween(380, easing = SilkyEaseOut))
        }
        overlayAlpha.animateTo(0f, tween(360, easing = SilkyEaseOut))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("try_again_splash")
            .graphicsLayer {
                alpha = overlayAlpha.value
                scaleX = overlayScale.value
                scaleY = overlayScale.value
            }
            .background(
                Brush.linearGradient(
                    colors = splashGradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // Multi-layered refractive glass waves, soft caustic aurora & orbital sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.43f)
            val rp = rippleProgress.value

            // Ambient caustic aurora glow behind the custom emblem
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f * iconAlpha.value),
                        Color(0xFF8BE9FD).copy(alpha = 0.16f * iconAlpha.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.56f
                ),
                radius = size.minDimension * 0.56f,
                center = center
            )

            // 3 Concentric Liquid-Glass Ripple Waves expanding smoothly
            if (rp > 0.01f) {
                listOf(0f, 0.22f, 0.44f).forEach { stagger ->
                    val wave = ((rp - stagger) / (1f - stagger)).coerceIn(0f, 1f)
                    if (wave > 0f) {
                        val waveRadius = (66.dp.toPx()) + (size.minDimension * 0.42f * wave)
                        val waveAlpha = (1f - wave) * 0.34f
                        drawCircle(
                            color = Color.White.copy(alpha = waveAlpha),
                            radius = waveRadius,
                            center = center,
                            style = Stroke(width = (2.2f - 1.2f * wave).dp.toPx())
                        )
                    }
                }
            }

            // Orbiting refractive glass rim & specular droplet around the icon
            val ringRadius = 78.dp.toPx()
            val ringAlpha = 0.52f * iconAlpha.value
            if (ringAlpha > 0.02f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = ringAlpha),
                            Color(0xFFE0F2FE).copy(alpha = ringAlpha * 0.85f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = haloOrbit,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                val orbRad = ((haloOrbit + 210f) * PI / 180.0)
                drawCircle(
                    color = Color.White.copy(alpha = (ringAlpha * 1.6f).coerceAtMost(0.95f)),
                    radius = 4.dp.toPx(),
                    center = Offset(
                        x = center.x + (ringRadius * cos(orbRad)).toFloat(),
                        y = center.y + (ringRadius * sin(orbRad)).toFloat()
                    )
                )
            }

            // Subtle diagonal specular glass light sweep across the backdrop
            val sweepX = size.width * specularSweep
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.14f * iconAlpha.value),
                        Color.Transparent
                    ),
                    start = Offset(sweepX - 160f, 0f),
                    end = Offset(sweepX + 160f, size.height)
                ),
                start = Offset(sweepX - 140f, 0f),
                end = Offset(sweepX + 140f, size.height),
                strokeWidth = 96.dp.toPx()
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Frosted Glass Pedestal Framing the User's Custom App Icon
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        alpha = iconAlpha.value
                        translationY = iconOffsetY.value
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
                    .clip(appIconShape(customIconShapeId, 132.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.88f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        ),
                        shape = appIconShape(customIconShapeId, 132.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CustomAppIconEmblem(
                    iconId = customIconId,
                    shapeId = customIconShapeId,
                    size = 112.dp,
                    contentDescription = "Try Again custom splash icon"
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Smooth gliding wordmark
            Text(
                text = "Try Again",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = NunitoDisplayFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 42.sp,
                    letterSpacing = (-0.8).sp
                ),
                color = Color.White,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .offset(y = titleOffsetY.value.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Liquid-Glass Tagline Capsule
            Box(
                modifier = Modifier
                    .alpha(pillAlpha.value)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.20f))
                    .border(1.1.dp, Color.White.copy(alpha = 0.48f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Focus • Earn Gems • Study Together",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Silky Liquid-Crystal Opening Loading Bar
            LiquidLoadingBar(
                progress = loadProgress.value,
                modifier = Modifier
                    .width(172.dp)
                    .alpha(pillAlpha.value)
                    .testTag("splash_loading_bar"),
                height = 6.dp,
                trackColor = Color.White.copy(alpha = 0.24f),
                fillColors = listOf(Color.White, Color(0xFFE0F2FE))
            )
        }
    }
}
