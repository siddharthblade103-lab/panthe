package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.data.model.GlassThemeId

/**
 * Tactile Duolingo-meets-Apple liquid glass palette tokens.
 * Clean, airy surfaces with crisp directional highlights, subtle shading, and vibrant accents.
 */
@Immutable
data class TryAgainPalette(
    val themeId: GlassThemeId,
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasBottom: Color,
    val glassFill: Color,
    val glassFillElevated: Color,
    val glassBorderHighlight: Color,
    val glassBorderSubtle: Color,
    val shadowAmbient: Color,
    val primary: Color,
    val primaryLip: Color,
    val primarySoft: Color,
    val secondary: Color,
    val secondaryLip: Color,
    val gemColor: Color,
    val gemLip: Color,
    val streakColor: Color,
    val streakLip: Color,
    val coralAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val chipUnselected: Color,
    val navDockFill: Color,
    val navDockBorder: Color,
    val textureDotColor: Color
)

// Signature Duolingo-inspired crisp vibrancy
val DuoFeatherGreen = Color(0xFF58CC02)
val DuoSplashGreen = Color(0xFF65C502) // Exact sampled corner hex of jumping monkey artwork
val DuoGreenLip = Color(0xFF46A302)
val DuoGemBlue = Color(0xFF1CB0F6)
val DuoGemLip = Color(0xFF148ECA)
val DuoStreakGold = Color(0xFFFF9600)
val DuoStreakLip = Color(0xFFD97D00)
val DuoCoral = Color(0xFFFF4B4B)

val DuoEmeraldPalette = TryAgainPalette(
    themeId = GlassThemeId.DUO_EMERALD,
    isDark = false,
    canvasTop = Color(0xFFF2F9EE),
    canvasBottom = Color(0xFFE1F2DA),
    glassFill = Color(0xADFFFFFF),
    glassFillElevated = Color(0xBDFFFFFF),
    glassBorderHighlight = Color(0xF2FFFFFF),
    glassBorderSubtle = Color(0xAAC7DFBE),
    shadowAmbient = Color(0x1C1B3A12),
    primary = DuoFeatherGreen,
    primaryLip = DuoGreenLip,
    primarySoft = Color(0xCCE6F8D8),
    secondary = Color(0xFF28B463),
    secondaryLip = Color(0xFF1E8B4C),
    gemColor = DuoGemBlue,
    gemLip = DuoGemLip,
    streakColor = DuoStreakGold,
    streakLip = DuoStreakLip,
    coralAccent = DuoCoral,
    textPrimary = Color(0xFF152214),
    textSecondary = Color(0xFF425441),
    textMuted = Color(0xFF6E816D),
    chipUnselected = Color(0x88EBF5E5),
    navDockFill = Color(0xCCFFFFFF),
    navDockBorder = Color(0xC0FFFFFF),
    textureDotColor = Color(0x1258CC02)
)

val SapphireSkyPalette = TryAgainPalette(
    themeId = GlassThemeId.SAPPHIRE_SKY,
    isDark = false,
    canvasTop = Color(0xFFEEF6FE),
    canvasBottom = Color(0xFFD9ECFC),
    glassFill = Color(0xAAFFFFFF),
    glassFillElevated = Color(0xBCFFFFFF),
    glassBorderHighlight = Color(0xF2FFFFFF),
    glassBorderSubtle = Color(0xAABEDAF6),
    shadowAmbient = Color(0x1C0C2D52),
    primary = Color(0xFF1CB0F6),
    primaryLip = Color(0xFF1489C4),
    primarySoft = Color(0xCCDCF2FE),
    secondary = Color(0xFF58CC02),
    secondaryLip = Color(0xFF46A302),
    gemColor = Color(0xFF0A84FF),
    gemLip = Color(0xFF0064CC),
    streakColor = DuoStreakGold,
    streakLip = DuoStreakLip,
    coralAccent = DuoCoral,
    textPrimary = Color(0xFF112032),
    textSecondary = Color(0xFF40566E),
    textMuted = Color(0xFF6B8199),
    chipUnselected = Color(0x88E3F0FC),
    navDockFill = Color(0xCCFFFFFF),
    navDockBorder = Color(0xC0FFFFFF),
    textureDotColor = Color(0x121CB0F6)
)

val GoldenAmberPalette = TryAgainPalette(
    themeId = GlassThemeId.GOLDEN_AMBER,
    isDark = false,
    canvasTop = Color(0xFFFDF7EC),
    canvasBottom = Color(0xFFF5E6CA),
    glassFill = Color(0xADFFFDF8),
    glassFillElevated = Color(0xBDFFFFFF),
    glassBorderHighlight = Color(0xF2FFFFFF),
    glassBorderSubtle = Color(0xAAE7D1A8),
    shadowAmbient = Color(0x1C472F07),
    primary = Color(0xFFF59E0B),
    primaryLip = Color(0xFFD18104),
    primarySoft = Color(0xCCFEF1D2),
    secondary = DuoFeatherGreen,
    secondaryLip = DuoGreenLip,
    gemColor = DuoGemBlue,
    gemLip = DuoGemLip,
    streakColor = Color(0xFFFF7A00),
    streakLip = Color(0xFFD46300),
    coralAccent = DuoCoral,
    textPrimary = Color(0xFF281D0E),
    textSecondary = Color(0xFF5E4B30),
    textMuted = Color(0xFF8A7558),
    chipUnselected = Color(0x88F5EBD7),
    navDockFill = Color(0xCCFFFFFF),
    navDockBorder = Color(0xC0FFFFFF),
    textureDotColor = Color(0x14F59E0B)
)

val MidnightForestPalette = TryAgainPalette(
    themeId = GlassThemeId.MIDNIGHT_FOREST,
    isDark = true,
    canvasTop = Color(0xFF0B1610),
    canvasBottom = Color(0xFF12261C),
    glassFill = Color(0x8C182B21),
    glassFillElevated = Color(0xA620382C),
    glassBorderHighlight = Color(0x4DFFFFFF),
    glassBorderSubtle = Color(0x2E85E85A),
    shadowAmbient = Color(0x59000000),
    primary = Color(0xFF6EE014),
    primaryLip = Color(0xFF4CA808),
    primarySoft = Color(0x336EE014),
    secondary = Color(0xFF34D399),
    secondaryLip = Color(0xFF10B981),
    gemColor = Color(0xFF38BDF8),
    gemLip = Color(0xFF0284C7),
    streakColor = Color(0xFFFFB020),
    streakLip = Color(0xFFD98B00),
    coralAccent = Color(0xFFFF6B6B),
    textPrimary = Color(0xFFF4FCF5),
    textSecondary = Color(0xFFBCD0C1),
    textMuted = Color(0xFF839D8A),
    chipUnselected = Color(0x781E3429),
    navDockFill = Color(0xBA15261D),
    navDockBorder = Color(0x40FFFFFF),
    textureDotColor = Color(0x166EE014)
)

val SakuraQuartzPalette = TryAgainPalette(
    themeId = GlassThemeId.SAKURA_QUARTZ,
    isDark = false,
    canvasTop = Color(0xFFFDF2F6),
    canvasBottom = Color(0xFFF7E0EA),
    glassFill = Color(0xADFFFBFD),
    glassFillElevated = Color(0xBDFFFFFF),
    glassBorderHighlight = Color(0xF2FFFFFF),
    glassBorderSubtle = Color(0xAAECC5D4),
    shadowAmbient = Color(0x1C471227),
    primary = Color(0xFFEC4899),
    primaryLip = Color(0xFFBE185D),
    primarySoft = Color(0xCCFDE2F0),
    secondary = DuoFeatherGreen,
    secondaryLip = DuoGreenLip,
    gemColor = DuoGemBlue,
    gemLip = DuoGemLip,
    streakColor = DuoStreakGold,
    streakLip = DuoStreakLip,
    coralAccent = DuoCoral,
    textPrimary = Color(0xFF2B1420),
    textSecondary = Color(0xFF644254),
    textMuted = Color(0xFF936D80),
    chipUnselected = Color(0x88F8E5ED),
    navDockFill = Color(0xCCFFFFFF),
    navDockBorder = Color(0xC0FFFFFF),
    textureDotColor = Color(0x12EC4899)
)

fun paletteForTheme(themeId: GlassThemeId): TryAgainPalette = when (themeId) {
    GlassThemeId.DUO_EMERALD -> DuoEmeraldPalette
    GlassThemeId.SAPPHIRE_SKY -> SapphireSkyPalette
    GlassThemeId.GOLDEN_AMBER -> GoldenAmberPalette
    GlassThemeId.MIDNIGHT_FOREST -> MidnightForestPalette
    GlassThemeId.SAKURA_QUARTZ -> SakuraQuartzPalette
}
