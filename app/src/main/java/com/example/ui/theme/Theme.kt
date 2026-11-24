package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.GlassThemeId

val LocalTryAgainPalette = staticCompositionLocalOf { DuoEmeraldPalette }

private val TryAgainShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun TryAgainTheme(
    themeId: GlassThemeId = GlassThemeId.DUO_EMERALD,
    content: @Composable () -> Unit
) {
    val target = paletteForTheme(themeId)
    val spec = tween<Color>(durationMillis = 360)

    val canvasTop by animateColorAsState(target.canvasTop, spec, label = "canvasTop")
    val canvasBottom by animateColorAsState(target.canvasBottom, spec, label = "canvasBottom")
    val glassFill by animateColorAsState(target.glassFill, spec, label = "glassFill")
    val glassFillElevated by animateColorAsState(target.glassFillElevated, spec, label = "glassFillElev")
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val primaryLip by animateColorAsState(target.primaryLip, spec, label = "primaryLip")
    val primarySoft by animateColorAsState(target.primarySoft, spec, label = "primarySoft")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "textPrimary")
    val textSecondary by animateColorAsState(target.textSecondary, spec, label = "textSecondary")

    val animatedPalette = target.copy(
        canvasTop = canvasTop,
        canvasBottom = canvasBottom,
        glassFill = glassFill,
        glassFillElevated = glassFillElevated,
        primary = primary,
        primaryLip = primaryLip,
        primarySoft = primarySoft,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    )

    val colorScheme = if (animatedPalette.isDark) {
        darkColorScheme(
            primary = animatedPalette.primary,
            onPrimary = Color(0xFF0A1A06),
            primaryContainer = animatedPalette.primarySoft,
            onPrimaryContainer = animatedPalette.primary,
            secondary = animatedPalette.gemColor,
            onSecondary = Color.White,
            background = animatedPalette.canvasTop,
            onBackground = animatedPalette.textPrimary,
            surface = animatedPalette.glassFill,
            onSurface = animatedPalette.textPrimary,
            surfaceVariant = animatedPalette.chipUnselected,
            onSurfaceVariant = animatedPalette.textSecondary,
            outline = animatedPalette.glassBorderSubtle
        )
    } else {
        lightColorScheme(
            primary = animatedPalette.primary,
            onPrimary = Color.White,
            primaryContainer = animatedPalette.primarySoft,
            onPrimaryContainer = animatedPalette.primaryLip,
            secondary = animatedPalette.gemColor,
            onSecondary = Color.White,
            background = animatedPalette.canvasTop,
            onBackground = animatedPalette.textPrimary,
            surface = animatedPalette.glassFill,
            onSurface = animatedPalette.textPrimary,
            surfaceVariant = animatedPalette.chipUnselected,
            onSurfaceVariant = animatedPalette.textSecondary,
            outline = animatedPalette.glassBorderSubtle
        )
    }

    CompositionLocalProvider(LocalTryAgainPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TryAgainTypography,
            shapes = TryAgainShapes,
            content = content
        )
    }
}
