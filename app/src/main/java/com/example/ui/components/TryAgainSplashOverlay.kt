package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DuoSplashGreen
import com.example.ui.theme.NunitoDisplayFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Silent Duolingo + Swiggy style opening animation:
 * Vibrant feather-green canvas with a joyful bouncing monkey mascot (squash-and-stretch leap),
 * crisp kinetic "Try Again" wordmark reveal, and smooth curtain metamorphosis into the app.
 * Strictly silent on both opening and closing per user request.
 */
@Composable
fun TryAgainSplashOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monkeyOffsetY = remember { Animatable(240f) }
    val monkeyScaleX = remember { Animatable(0.75f) }
    val monkeyScaleY = remember { Animatable(1.25f) }
    val monkeyRotation = remember { Animatable(-8f) }
    val sparkleProgress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(32f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }
    val overlayScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: Monkey leaps up into frame like Duolingo mascot pop
        launch {
            monkeyOffsetY.animateTo(
                targetValue = -26f,
                animationSpec = spring(
                    dampingRatio = 0.56f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            // Land with a second joyful mini-hop
            monkeyOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.45f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            monkeyScaleX.animateTo(1.12f, tween(200, easing = FastOutSlowInEasing))
            monkeyScaleX.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
        launch {
            monkeyScaleY.animateTo(0.90f, tween(200, easing = FastOutSlowInEasing))
            monkeyScaleY.animateTo(1.06f, tween(160, easing = FastOutSlowInEasing))
            monkeyScaleY.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
        launch {
            monkeyRotation.animateTo(5f, tween(260, easing = FastOutSlowInEasing))
            monkeyRotation.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMedium))
        }

        // Phase 2: Sparkle ring bursts & Swiggy-crisp wordmark slides up
        delay(220)
        launch {
            sparkleProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            titleAlpha.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        }
        launch {
            titleOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
            )
        }

        delay(260)
        launch {
            subtitleAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }

        // Hold cleanly for readability then morph away silently
        delay(1150)
        launch {
            overlayScale.animateTo(1.08f, tween(360, easing = FastOutSlowInEasing))
        }
        overlayAlpha.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
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
            .background(DuoSplashGreen)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Silent instant tap-to-enter
                onFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // Soft sunlit radial halo + crisp Duolingo-style sparkle bursts
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f * sparkleProgress.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.58f
                ),
                radius = size.width * 0.58f,
                center = center
            )

            // 6 subtle crisp white 4-point sparkles radiating outward as the monkey leaps
            val p = sparkleProgress.value
            if (p > 0.05f) {
                val starAlpha = (1f - (p - 0.5f).coerceAtLeast(0f) * 1.4f).coerceIn(0f, 0.9f)
                val offsets = listOf(
                    Offset(-120.dp.toPx(), -110.dp.toPx()),
                    Offset(126.dp.toPx(), -96.dp.toPx()),
                    Offset(-142.dp.toPx(), 16.dp.toPx()),
                    Offset(138.dp.toPx(), 24.dp.toPx()),
                    Offset(-78.dp.toPx(), -152.dp.toPx()),
                    Offset(82.dp.toPx(), -145.dp.toPx())
                )
                offsets.forEachIndexed { idx, delta ->
                    val pos = center + delta * (0.45f + 0.55f * p)
                    val r = (if (idx % 2 == 0) 9.dp else 6.dp).toPx() * (0.5f + 0.5f * p)
                    val path = Path().apply {
                        moveTo(pos.x, pos.y - r)
                        quadraticTo(pos.x, pos.y, pos.x + r, pos.y)
                        quadraticTo(pos.x, pos.y, pos.x, pos.y + r)
                        quadraticTo(pos.x, pos.y, pos.x - r, pos.y)
                        quadraticTo(pos.x, pos.y, pos.x, pos.y - r)
                        close()
                    }
                    drawPath(path = path, color = Color.White.copy(alpha = starAlpha))
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Bouncing Monkey Mascot (blends seamlessly into #65C502 green canvas!)
            Image(
                painter = painterResource(id = R.drawable.img_tryagain_monkey_1795336082990),
                contentDescription = "Try Again bouncing monkey mascot",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(216.dp)
                    .graphicsLayer {
                        translationY = monkeyOffsetY.value
                        scaleX = monkeyScaleX.value
                        scaleY = monkeyScaleY.value
                        rotationZ = monkeyRotation.value
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Swiggy-crisp kinetic wordmark
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

            Box(
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.20f))
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Focus • Earn Gems • Level Up",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.4.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}
