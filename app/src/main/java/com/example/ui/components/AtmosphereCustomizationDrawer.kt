package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AmbientLightingStyle
import com.example.data.model.AppPreferencesState
import com.example.data.model.CompletionSoundPreset
import com.example.data.model.HapticIntensityLevel
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlin.math.roundToInt

/**
 * Unified Liquid-Glass Studio Drawer for customizing:
 * - Ambient Gradient Lighting Style & Glow Intensity
 * - Galaxy Edge Lighting perimeter aura
 * - Tactile Vibration / Haptic strength with live feel preview
 * - UI Glass Click Sounds & Completion Chime
 * - Quick links to Ambient Soundscapes & Icon Studio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtmosphereCustomizationDrawer(
    preferences: AppPreferencesState,
    onSelectLightingStyle: (AmbientLightingStyle) -> Unit,
    onLightingIntensityChange: (Float) -> Unit,
    onToggleEdgeGlow: (Boolean) -> Unit,
    onSelectHapticIntensity: (HapticIntensityLevel) -> Unit,
    onToggleUiSounds: (Boolean) -> Unit,
    onSelectCompletionSound: (CompletionSoundPreset) -> Unit,
    onOpenAmbientDrawer: () -> Unit,
    onOpenIconStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = if (palette.isDark) Color(0xFF0F1E17) else Color(0xFFF6FBF8),
        contentColor = palette.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.46f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.textMuted.copy(alpha = 0.45f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("atmosphere_studio_drawer"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.prismViolet.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, palette.prismViolet.copy(alpha = 0.36f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = palette.prismViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.atmosphere_drawer_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.atmosphere_drawer_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        tactile?.playTap()
                        onDismiss()
                    },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("atmosphere_drawer_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close atmosphere studio",
                        tint = palette.textSecondary
                    )
                }
            }

            // 1. Ambient Gradient Lighting Swatches
            LiquidGlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                elevated = true,
                accentTint = palette.prismCyan,
                animatedCaustics = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.atmosphere_lighting_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AmbientLightingStyle.entries) { style ->
                            val selected = preferences.ambientLightingStyle == style
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.04f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "lightingSwatch_${style.name}"
                            )
                            val swatchColors = lightingStyleSwatchColors(style)
                            Surface(
                                modifier = Modifier
                                    .width(118.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .testTag("lighting_style_${style.name.lowercase()}")
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onSelectLightingStyle(style) },
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) swatchColors.first().copy(alpha = 0.16f) else palette.glassFillElevated,
                                border = BorderStroke(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) swatchColors.first() else palette.glassBorderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(swatchColors))
                                    )
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Glow Intensity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.atmosphere_glow_intensity),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.textSecondary
                            )
                            Text(
                                text = "${(preferences.ambientLightingIntensity * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.primary
                            )
                        }
                        Slider(
                            value = preferences.ambientLightingIntensity,
                            onValueChange = onLightingIntensityChange,
                            valueRange = 0.15f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.primary,
                                activeTrackColor = palette.primary,
                                inactiveTrackColor = palette.glassBorderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("atmosphere_intensity_slider")
                        )
                    }

                    // Galaxy Edge Lighting Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.atmosphere_edge_glow_title),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.atmosphere_edge_glow_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = preferences.ambientEdgeGlowEnabled,
                            onCheckedChange = onToggleEdgeGlow,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.primary
                            ),
                            modifier = Modifier.testTag("atmosphere_edge_glow_switch")
                        )
                    }
                }
            }

            // 2. Vibration & Tactile Haptics Card
            LiquidGlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                elevated = false,
                accentTint = palette.primary,
                animatedCaustics = false,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = stringResource(R.string.atmosphere_haptics_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.textPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HapticIntensityLevel.entries.forEach { level ->
                            val selected = preferences.hapticIntensity == level
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .testTag("haptic_level_${level.name.lowercase()}")
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectHapticIntensity(level) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) palette.primary else palette.chipUnselected,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) palette.primaryLip else palette.glassBorderSubtle
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = level.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        ),
                                        color = if (selected) Color.White else palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // UI Click Sounds Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.atmosphere_ui_sounds_title),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.atmosphere_ui_sounds_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Switch(
                            checked = preferences.uiClickSoundsEnabled,
                            onCheckedChange = onToggleUiSounds,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.primary
                            ),
                            modifier = Modifier.testTag("atmosphere_ui_sounds_switch")
                        )
                    }

                    // Completion Chime Quick Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = palette.gemColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.atmosphere_chime_title),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textSecondary
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CompletionSoundPreset.entries) { preset ->
                            MorphismChip(
                                label = preset.displayName,
                                selected = preferences.completionSound == preset,
                                onClick = { onSelectCompletionSound(preset) },
                                testTag = "atmosphere_chime_${preset.name.lowercase()}"
                            )
                        }
                    }
                }
            }

            // 3. Quick Jumps to Soundscapes & Custom App Icon Studio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("atmosphere_open_sounds_button")
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            tactile?.playTap()
                            onDismiss()
                            onOpenAmbientDrawer()
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = palette.primarySoft,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = palette.primaryLip,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.ambient_drawer_open_cta),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.primaryLip
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("atmosphere_open_icon_studio_button")
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            tactile?.playTap()
                            onDismiss()
                            onOpenIconStudio()
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = palette.gemColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, palette.gemColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = palette.gemColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.custom_icon_studio_title),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.gemColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun lightingStyleSwatchColors(style: AmbientLightingStyle): List<Color> = when (style) {
    AmbientLightingStyle.AURORA_GLASS -> listOf(Color(0xFF58CC02), Color(0xFF38BDF8), Color(0xFFA78BFA))
    AmbientLightingStyle.GALAXY_NEBULA -> listOf(Color(0xFF4F46E5), Color(0xFF38BDF8), Color(0xFFA855F7))
    AmbientLightingStyle.SUNSET_AMBER -> listOf(Color(0xFFF59E0B), Color(0xFFFB7185), Color(0xFFF97316))
    AmbientLightingStyle.MIDNIGHT_PULSE -> listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFEC4899))
    AmbientLightingStyle.CALM_MINIMAL -> listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFE2E8F0))
}
