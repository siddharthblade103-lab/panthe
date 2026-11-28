package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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
    val spec = tween<Color>(durationMillis = 440, easing = FastOutSlowInEasing)

    val canvasTop by animateColorAsState(target.canvasTop, spec, label = "canvasTop")
    val canvasBottom by animateColorAsState(target.canvasBottom, spec, label = "canvasBottom")
    val glassFill by animateColorAsState(target.glassFill, spec, label = "glassFill")
    val glassFillElevated by animateColorAsState(target.glassFillElevated, spec, label = "glassFillElev")
    val glassBorderHighlight by animateColorAsState(target.glassBorderHighlight, spec, label = "glassBorderHi")
    val glassBorderSubtle by animateColorAsState(target.glassBorderSubtle, spec, label = "glassBorderSub")
    val prismCyan by animateColorAsState(target.prismCyan, spec, label = "prismCyan")
    val prismViolet by animateColorAsState(target.prismViolet, spec, label = "prismViolet")
    val prismRose by animateColorAsState(target.prismRose, spec, label = "prismRose")
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val primaryLip by animateColorAsState(target.primaryLip, spec, label = "primaryLip")
    val primarySoft by animateColorAsState(target.primarySoft, spec, label = "primarySoft")
    val secondary by animateColorAsState(target.secondary, spec, label = "secondary")
    val gemColor by animateColorAsState(target.gemColor, spec, label = "gemColor")
    val streakColor by animateColorAsState(target.streakColor, spec, label = "streakColor")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "textPrimary")
    val textSecondary by animateColorAsState(target.textSecondary, spec, label = "textSecondary")
    val textMuted by animateColorAsState(target.textMuted, spec, label = "textMuted")
    val chipUnselected by animateColorAsState(target.chipUnselected, spec, label = "chipUnselected")
    val navDockFill by animateColorAsState(target.navDockFill, spec, label = "navDockFill")
    val navDockBorder by animateColorAsState(target.navDockBorder, spec, label = "navDockBorder")

    val animatedPalette = target.copy(
        canvasTop = canvasTop,
        canvasBottom = canvasBottom,
        glassFill = glassFill,
        glassFillElevated = glassFillElevated,
        glassBorderHighlight = glassBorderHighlight,
        glassBorderSubtle = glassBorderSubtle,
        prismCyan = prismCyan,
        prismViolet = prismViolet,
        prismRose = prismRose,
        primary = primary,
        primaryLip = primaryLip,
        primarySoft = primarySoft,
        secondary = secondary,
        gemColor = gemColor,
        streakColor = streakColor,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textMuted = textMuted,
        chipUnselected = chipUnselected,
        navDockFill = navDockFill,
        navDockBorder = navDockBorder
    )

    // Keep system status bar & navigation bar icon contrast synced seamlessly with dark/light glass themes
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !target.isDark
            insetsController.isAppearanceLightNavigationBars = !target.isDark
        }
    }

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
