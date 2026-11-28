package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AmbientSoundPreset
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlin.math.roundToInt

/**
 * Shared icon and signature color metadata for every ambient soundscape preset.
 */
fun ambientPresetVisual(preset: AmbientSoundPreset): Pair<ImageVector, Color> = when (preset) {
    AmbientSoundPreset.OFF -> Icons.Rounded.VolumeOff to Color(0xFF64748B)
    AmbientSoundPreset.RAIN -> Icons.Rounded.WaterDrop to Color(0xFF1CB0F6)
    AmbientSoundPreset.CAFE_NOISE -> Icons.Rounded.LocalCafe to Color(0xFFD97706)
    AmbientSoundPreset.WHITE_NOISE -> Icons.Rounded.Air to Color(0xFF6366F1)
    AmbientSoundPreset.FOREST_WIND -> Icons.Rounded.Park to Color(0xFF28B463)
    AmbientSoundPreset.OCEAN_WAVES -> Icons.Rounded.Waves to Color(0xFF0EA5E9)
    AmbientSoundPreset.BROWN_NOISE -> Icons.Rounded.GraphicEq to Color(0xFFA16207)
    AmbientSoundPreset.ALPHA_WAVES -> Icons.Rounded.SelfImprovement to Color(0xFF8B5CF6)
    AmbientSoundPreset.FIREPLACE -> Icons.Rounded.LocalFireDepartment to Color(0xFFF97316)
}

/**
 * Compact Liquid-Glass launcher bar embedded inside the hero focus timer card.
 * Keeps background sound switching 1 tap away both before and during active focus sessions.
 */
@Composable
fun AmbientSoundDrawerTriggerBar(
    activeAmbient: AmbientSoundPreset,
    isPlaying: Boolean,
    waveAmplitude: Float,
    onOpenDrawer: () -> Unit,
    onQuickPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val (presetIcon, presetTint) = remember(activeAmbient) { ambientPresetVisual(activeAmbient) }
    val activeTint = if (activeAmbient == AmbientSoundPreset.OFF) palette.textSecondary else presetTint

    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) activeTint.copy(alpha = 0.50f) else palette.glassBorderSubtle,
        animationSpec = tween(260),
        label = "ambientTriggerBorder"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .testTag("open_ambient_drawer_button")
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                tactile?.playTap()
                onOpenDrawer()
            },
        shape = RoundedCornerShape(20.dp),
        color = if (isPlaying) activeTint.copy(alpha = if (palette.isDark) 0.16f else 0.11f) else palette.glassFillElevated,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = activeTint.copy(alpha = if (isPlaying) 0.22f else 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = presetIcon,
                            contentDescription = activeAmbient.displayName,
                            tint = activeTint,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeAmbient == AmbientSoundPreset.OFF) {
                            stringResource(R.string.ambient_drawer_open_cta)
                        } else {
                            activeAmbient.displayName
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isPlaying -> stringResource(R.string.ambient_drawer_playing_status)
                            activeAmbient != AmbientSoundPreset.OFF -> stringResource(R.string.ambient_drawer_paused_status)
                            else -> stringResource(R.string.ambient_drawer_silent_status)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPlaying) activeTint else palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isPlaying) {
                    AmbientWaveBars(
                        color = activeTint,
                        amplitude = waveAmplitude,
                        barCount = 4,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                if (activeAmbient != AmbientSoundPreset.OFF) {
                    IconButton(
                        onClick = {
                            tactile?.playTap()
                            onQuickPlayPause()
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("ambient_trigger_quick_toggle")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = activeTint.copy(alpha = 0.18f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause ambient sound" else "Resume ambient sound",
                                    tint = activeTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.chipUnselected,
                    border = BorderStroke(1.dp, palette.glassBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Open sound selection drawer",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Liquid-Glass Bottom Selection Drawer for toggling ambient background soundscapes
 * (Rainfall, Cafe Noise, White Noise, Forest Breeze, Ocean Tide, Deep Brown, Alpha 10Hz, Cozy Hearth)
 * during focus sessions, with live waveform feedback, volume mixer, and auto-play preference.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientSoundSelectionDrawer(
    activeAmbient: AmbientSoundPreset,
    isPlaying: Boolean,
    waveAmplitude: Float,
    volume: Float,
    autoPlayWithTimer: Boolean,
    unlockedSounds: List<String>,
    userGems: Int,
    onSelectOrToggleSound: (AmbientSoundPreset) -> Unit,
    onUnlockSound: (AmbientSoundPreset) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleAutoPlay: (Boolean) -> Unit,
    onOpenShop: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val essentials = remember {
        listOf(
            AmbientSoundPreset.OFF,
            AmbientSoundPreset.RAIN,
            AmbientSoundPreset.CAFE_NOISE,
            AmbientSoundPreset.WHITE_NOISE
        )
    }
    val atmospheres = remember {
        listOf(
            AmbientSoundPreset.FOREST_WIND,
            AmbientSoundPreset.OCEAN_WAVES,
            AmbientSoundPreset.BROWN_NOISE,
            AmbientSoundPreset.ALPHA_WAVES,
            AmbientSoundPreset.FIREPLACE
        )
    }

    val (_, activeAccent) = remember(activeAmbient) { ambientPresetVisual(activeAmbient) }
    val drawerTint = if (activeAmbient == AmbientSoundPreset.OFF) palette.primary else activeAccent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = if (palette.isDark) Color(0xFF101F18) else Color(0xFFF6FBF8),
        contentColor = palette.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.48f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = palette.textMuted.copy(alpha = 0.45f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("ambient_sound_selection_drawer"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Drawer Header
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
                        color = drawerTint.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, drawerTint.copy(alpha = 0.35f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = drawerTint,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.ambient_drawer_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.ambient_drawer_subtitle),
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
                        .testTag("ambient_drawer_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.ambient_drawer_close_desc),
                        tint = palette.textSecondary
                    )
                }
            }

            // 2. Live Acoustic Status, Volume Mixer & Timer Auto-Play Card
            LiquidGlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                elevated = isPlaying,
                accentTint = drawerTint,
                animatedCaustics = isPlaying,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (nowIcon, nowColor) = ambientPresetVisual(activeAmbient)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = nowColor.copy(alpha = 0.18f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = nowIcon,
                                        contentDescription = null,
                                        tint = nowColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = activeAmbient.displayName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = palette.textPrimary
                                    )
                                    if (isPlaying) {
                                        AmbientWaveBars(
                                            color = nowColor,
                                            amplitude = waveAmplitude,
                                            barCount = 5
                                        )
                                    }
                                }
                                Text(
                                    text = activeAmbient.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (activeAmbient != AmbientSoundPreset.OFF) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .testTag("ambient_drawer_play_pause_button")
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            tactile?.playTap()
                                            onSelectOrToggleSound(activeAmbient)
                                        },
                                    shape = RoundedCornerShape(50),
                                    color = drawerTint.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, drawerTint.copy(alpha = 0.40f))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause sound" else "Play sound",
                                            tint = drawerTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .testTag("ambient_drawer_mute_button")
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            onSelectOrToggleSound(AmbientSoundPreset.OFF)
                                        },
                                    shape = RoundedCornerShape(50),
                                    color = palette.chipUnselected,
                                    border = BorderStroke(1.dp, palette.glassBorderSubtle)
                                ) {
                                    Text(
                                        text = stringResource(R.string.ambient_drawer_mute_cta),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Volume Slider Row
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VolumeUp,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.ambient_drawer_volume_label),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textSecondary
                                )
                            }
                            Text(
                                text = "${(volume * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = drawerTint
                            )
                        }
                        Slider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            valueRange = 0.05f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = drawerTint,
                                activeTrackColor = drawerTint,
                                inactiveTrackColor = palette.glassBorderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ambient_drawer_volume_slider")
                        )
                    }

                    // Auto-play with Focus Timer Switch
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.glassFillElevated.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, palette.glassBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ambient_drawer_autoplay_title),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.ambient_drawer_autoplay_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = autoPlayWithTimer,
                                onCheckedChange = { enabled ->
                                    tactile?.playToggle(enabled)
                                    onToggleAutoPlay(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.primary
                                ),
                                modifier = Modifier.testTag("ambient_drawer_autoplay_switch")
                            )
                        }
                    }
                }
            }

            // 3. Focus Essentials 2x2 Grid (Silent, Rainfall, Cafe Noise, White Noise)
            Text(
                text = stringResource(R.string.ambient_drawer_essentials_section),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                ),
                color = palette.textSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                essentials.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = if (preset == AmbientSoundPreset.OFF) {
                                activeAmbient == AmbientSoundPreset.OFF || !isPlaying
                            } else {
                                activeAmbient == preset && isPlaying
                            }
                            AmbientEssentialTile(
                                preset = preset,
                                isActive = isSelected,
                                isPausedSelection = preset != AmbientSoundPreset.OFF && activeAmbient == preset && !isPlaying,
                                waveAmplitude = waveAmplitude,
                                onClick = { onSelectOrToggleSound(preset) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Atmospheres & Deep Focus Library
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ambient_drawer_atmospheres_section),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    ),
                    color = palette.textSecondary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.gemColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, palette.gemColor.copy(alpha = 0.30f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Diamond,
                            contentDescription = null,
                            tint = palette.gemColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$userGems Gems",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.gemColor
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                atmospheres.forEach { preset ->
                    val unlocked = preset.gemCost == 0 || preset.name in unlockedSounds
                    val active = activeAmbient == preset && isPlaying
                    val pausedSelected = activeAmbient == preset && !isPlaying
                    val canAfford = userGems >= preset.gemCost

                    AmbientAtmosphereRowCard(
                        preset = preset,
                        unlocked = unlocked,
                        active = active,
                        pausedSelected = pausedSelected,
                        canAfford = canAfford,
                        waveAmplitude = waveAmplitude,
                        onClick = {
                            when {
                                unlocked -> onSelectOrToggleSound(preset)
                                canAfford -> onUnlockSound(preset)
                                else -> {
                                    onDismiss()
                                    onOpenShop()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientEssentialTile(
    preset: AmbientSoundPreset,
    isActive: Boolean,
    isPausedSelection: Boolean,
    waveAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val (icon, tint) = remember(preset) { ambientPresetVisual(preset) }

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "essentialTileScale_${preset.name}"
    )

    val containerBg = if (isActive) {
        tint.copy(alpha = if (palette.isDark) 0.22f else 0.14f)
    } else {
        palette.glassFillElevated
    }
    val borderColor = when {
        isActive -> tint.copy(alpha = 0.65f)
        isPausedSelection -> tint.copy(alpha = 0.35f)
        else -> palette.glassBorderSubtle
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 112.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("ambient_drawer_item_${preset.name.lowercase()}")
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = containerBg,
        border = BorderStroke(if (isActive) 1.8.dp else 1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isActive) {
                        Brush.linearGradient(
                            colors = listOf(
                                tint.copy(alpha = if (palette.isDark) 0.20f else 0.12f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = tint.copy(alpha = if (isActive) 0.26f else 0.14f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = preset.displayName,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (isActive && preset != AmbientSoundPreset.OFF) {
                        AmbientWaveBars(
                            color = tint,
                            amplitude = waveAmplitude,
                            barCount = 3
                        )
                    } else if (isActive && preset == AmbientSoundPreset.OFF) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = tint.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "ON",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = tint,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (isPausedSelection) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Resume ${preset.displayName}",
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = preset.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientAtmosphereRowCard(
    preset: AmbientSoundPreset,
    unlocked: Boolean,
    active: Boolean,
    pausedSelected: Boolean,
    canAfford: Boolean,
    waveAmplitude: Float,
    onClick: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val (icon, tint) = remember(preset) { ambientPresetVisual(preset) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .testTag("ambient_drawer_item_${preset.name.lowercase()}")
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (active) tint.copy(alpha = if (palette.isDark) 0.18f else 0.12f) else palette.glassFillElevated,
        border = BorderStroke(
            if (active) 1.6.dp else 1.dp,
            if (active) tint.copy(alpha = 0.58f) else palette.glassBorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = tint.copy(alpha = if (unlocked) 0.18f else 0.10f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = preset.displayName,
                            tint = if (unlocked) tint else palette.textMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = preset.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            when {
                active -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AmbientWaveBars(
                            color = tint,
                            amplitude = waveAmplitude,
                            barCount = 4
                        )
                        Surface(
                            shape = CircleShape,
                            color = tint.copy(alpha = 0.22f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = "Pause ${preset.displayName}",
                                    tint = tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                unlocked -> {
                    Surface(
                        shape = CircleShape,
                        color = if (pausedSelected) tint.copy(alpha = 0.20f) else palette.chipUnselected,
                        border = BorderStroke(
                            1.dp,
                            if (pausedSelected) tint.copy(alpha = 0.45f) else palette.glassBorderSubtle
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play ${preset.displayName}",
                                tint = if (pausedSelected) tint else palette.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                else -> {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (canAfford) palette.gemColor.copy(alpha = 0.18f) else palette.chipUnselected,
                        border = BorderStroke(
                            1.dp,
                            if (canAfford) palette.gemColor.copy(alpha = 0.45f) else palette.glassBorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (canAfford) Icons.Rounded.Diamond else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (canAfford) palette.gemColor else palette.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (canAfford) {
                                    "${stringResource(R.string.ambient_drawer_unlock_cta)} • ${preset.gemCost}"
                                } else {
                                    "${preset.gemCost} • ${stringResource(R.string.ambient_drawer_shop_cta)}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (canAfford) palette.gemColor else palette.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmbientWaveBars(
    color: Color,
    amplitude: Float,
    barCount: Int = 4,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "ambientEqBars")
    val pulse by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqPulse"
    )
    val energy = (0.55f + amplitude.coerceIn(0f, 1f) * 0.65f).coerceIn(0.45f, 1.15f)
    val multipliers = remember(barCount) {
        when (barCount) {
            3 -> listOf(0.65f, 1f, 0.78f)
            5 -> listOf(0.50f, 0.88f, 1f, 0.74f, 0.60f)
            else -> listOf(0.58f, 1f, 0.72f, 0.88f)
        }
    }

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        multipliers.forEachIndexed { idx, factor ->
            val phase = if (idx % 2 == 0) pulse else (1.28f - pulse).coerceIn(0.24f, 1f)
            val barHeight = (4f + 13f * factor * phase * energy).coerceIn(4f, 18f).dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
