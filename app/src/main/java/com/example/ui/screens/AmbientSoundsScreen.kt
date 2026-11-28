package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.PomodoroPhase
import com.example.data.model.TimerUiState
import com.example.ui.components.AmbientWaveBars
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.components.LocalSystemHaptics
import com.example.ui.components.LocalTactileSound
import com.example.ui.components.ambientPresetVisual
import com.example.ui.components.formatClock
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlin.math.roundToInt

/**
 * Full-screen Liquid-Glass Ambient Sounds Selection Screen powered by the local
 * on-device audio player (AmbientSoundEngine + AmbientAudioService).
 *
 * Users can toggle, preview, switch, mute, and adjust volume of ambient sounds
 * (Rain, White Noise, Cafe Noise, Forest, Ocean, Deep Brown, Alpha 10Hz, Hearth)
 * at any time — including while a Pomodoro focus timer is actively counting down.
 */
@Composable
fun AmbientSoundsScreen(
    timerState: TimerUiState,
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
    onStartOrResumeTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onOpenShop: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current

    val (activeIcon, activeColor) = remember(activeAmbient) { ambientPresetVisual(activeAmbient) }
    val heroTint = if (activeAmbient == AmbientSoundPreset.OFF) palette.prismCyan else activeColor
    val volumePct = (volume * 100f).roundToInt().coerceIn(5, 100)

    val essentials = remember {
        listOf(
            AmbientSoundPreset.RAIN,
            AmbientSoundPreset.WHITE_NOISE,
            AmbientSoundPreset.CAFE_NOISE,
            AmbientSoundPreset.OFF
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

    val backdropGradient = remember(palette.isDark, heroTint, palette.canvasTop, palette.canvasBottom) {
        Brush.verticalGradient(
            colors = listOf(
                palette.canvasTop,
                if (palette.isDark) heroTint.copy(alpha = 0.14f) else heroTint.copy(alpha = 0.08f),
                palette.canvasBottom
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvasBottom)
            .background(backdropGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("ambient_sounds_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Navigation Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    systemHaptics.buttonTap()
                                    tactile?.playTap()
                                    onBack()
                                }
                                .testTag("ambient_sounds_back_button"),
                            shape = CircleShape,
                            color = palette.glassFillElevated,
                            border = BorderStroke(1.dp, palette.glassBorderHighlight)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.ambient_screen_back_desc),
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ambient_screen_title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.ambient_screen_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (activeAmbient != AmbientSoundPreset.OFF) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    systemHaptics.toggle(false)
                                    tactile?.playToggle(false)
                                    onSelectOrToggleSound(AmbientSoundPreset.OFF)
                                }
                                .testTag("ambient_sounds_quick_mute_button"),
                            shape = RoundedCornerShape(50),
                            color = palette.coralAccent.copy(alpha = if (palette.isDark) 0.22f else 0.13f),
                            border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.42f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VolumeOff,
                                    contentDescription = stringResource(R.string.ambient_drawer_mute_cta),
                                    tint = palette.coralAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.ambient_drawer_mute_cta),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = palette.coralAccent
                                )
                            }
                        }
                    }
                }
            }

            // 2. Live Focus Timer Mini-Bar (always visible when focus/break timer is running or paused)
            item {
                AnimatedVisibility(visible = timerState.isRunning || timerState.isPaused) {
                    LiveFocusSessionMiniBar(
                        timerState = timerState,
                        onStartOrResume = {
                            systemHaptics.sessionStart()
                            onStartOrResumeTimer()
                        },
                        onPause = {
                            systemHaptics.sessionPause()
                            onPauseTimer()
                        }
                    )
                }
            }

            // 3. Now Playing & Local Audio Player Mixer Hero Card
            item {
                LiquidGlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ambient_screen_now_playing_card"),
                    cornerRadius = 28.dp,
                    elevated = true,
                    accentTint = heroTint,
                    animatedCaustics = isPlaying,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    heroTint.copy(alpha = if (isPlaying) 0.34f else 0.18f),
                                                    heroTint.copy(alpha = if (isPlaying) 0.14f else 0.08f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = activeIcon,
                                        contentDescription = null,
                                        tint = heroTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                activeAmbient == AmbientSoundPreset.OFF ->
                                                    stringResource(R.string.ambient_drawer_silent_status)
                                                isPlaying ->
                                                    stringResource(R.string.ambient_screen_local_playing)
                                                else ->
                                                    stringResource(R.string.ambient_drawer_paused_status)
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.4.sp
                                            ),
                                            color = if (isPlaying) heroTint else palette.textSecondary
                                        )
                                        if (isPlaying) {
                                            AmbientWaveBars(
                                                color = heroTint,
                                                amplitude = waveAmplitude,
                                                barCount = 4
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeAmbient.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = NunitoDisplayFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = activeAmbient.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Play / Pause Master Button (starts Rainfall quickly if currently Silent)
                            Surface(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val target = if (activeAmbient == AmbientSoundPreset.OFF) {
                                            AmbientSoundPreset.RAIN
                                        } else {
                                            activeAmbient
                                        }
                                        systemHaptics.toggle(!isPlaying)
                                        onSelectOrToggleSound(target)
                                    }
                                    .testTag("ambient_screen_master_play_pause"),
                                shape = CircleShape,
                                color = if (isPlaying) heroTint else heroTint.copy(alpha = 0.22f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isPlaying) Color.White.copy(alpha = 0.55f) else heroTint.copy(alpha = 0.55f)
                                ),
                                shadowElevation = if (isPlaying) 8.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) {
                                            stringResource(R.string.ambient_action_pause)
                                        } else {
                                            stringResource(R.string.ambient_action_play)
                                        },
                                        tint = if (isPlaying) Color.White else heroTint,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        // Volume Slider
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
                                        tint = heroTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.ambient_drawer_volume_label),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textPrimary
                                    )
                                }
                                Text(
                                    text = "$volumePct%",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = heroTint
                                )
                            }
                            Slider(
                                value = volume.coerceIn(0.05f, 1f),
                                onValueChange = onVolumeChange,
                                valueRange = 0.05f..1f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ambient_screen_volume_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = heroTint,
                                    activeTrackColor = heroTint,
                                    inactiveTrackColor = palette.chipUnselected
                                )
                            )
                        }

                        // Auto-play with Focus Timer Toggle
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.chipUnselected.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, palette.glassBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.ambient_drawer_autoplay_title),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = stringResource(R.string.ambient_drawer_autoplay_desc),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = autoPlayWithTimer,
                                    onCheckedChange = { enabled ->
                                        systemHaptics.toggle(enabled)
                                        onToggleAutoPlay(enabled)
                                    },
                                    modifier = Modifier.testTag("ambient_screen_autoplay_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = palette.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. Focus Essentials Section (2x2 Grid: Rain, White Noise, Cafe Noise, Silent)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.ambient_drawer_essentials_section),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.ambient_screen_instant_toggle_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    essentials.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                val isSelected = activeAmbient == preset
                                val isPresetPlaying = isSelected && isPlaying && preset != AmbientSoundPreset.OFF
                                AmbientEssentialGridCard(
                                    preset = preset,
                                    isSelected = isSelected,
                                    isPlaying = isPresetPlaying,
                                    waveAmplitude = waveAmplitude,
                                    onClick = {
                                        systemHaptics.toggle(!isPresetPlaying && preset != AmbientSoundPreset.OFF)
                                        onSelectOrToggleSound(preset)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Atmospheres & Deep Focus List
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.ambient_drawer_atmospheres_section),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.gemColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.gemColor.copy(alpha = 0.35f))
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
                                text = "$userGems",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.gemColor
                            )
                        }
                    }
                }
            }

            items(atmospheres, key = { it.name }) { preset ->
                val unlocked = preset.gemCost == 0 || preset.name in unlockedSounds
                val isSelected = activeAmbient == preset
                val isPresetPlaying = isSelected && isPlaying
                AmbientSoundListTile(
                    preset = preset,
                    unlocked = unlocked,
                    isSelected = isSelected,
                    isPlaying = isPresetPlaying,
                    waveAmplitude = waveAmplitude,
                    userGems = userGems,
                    onClick = {
                        when {
                            unlocked -> {
                                systemHaptics.toggle(!isPresetPlaying)
                                onSelectOrToggleSound(preset)
                            }
                            userGems >= preset.gemCost -> {
                                systemHaptics.confirm()
                                onUnlockSound(preset)
                            }
                            else -> {
                                systemHaptics.buttonTap()
                                onBack()
                                onOpenShop()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveFocusSessionMiniBar(
    timerState: TimerUiState,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val pulseTransition = rememberInfiniteTransition(label = "focusTimerPulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    val accent = when {
        timerState.isPaused -> palette.streakColor
        timerState.phase == PomodoroPhase.FOCUS -> palette.primary
        else -> palette.gemColor
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ambient_screen_live_timer_bar"),
        shape = RoundedCornerShape(22.dp),
        color = palette.glassFillElevated,
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = if (timerState.isRunning) dotAlpha else 1f))
                        )
                        Text(
                            text = if (timerState.isRunning) {
                                "${timerState.phase.displayName} Running · ${timerState.subject}"
                            } else {
                                "Paused · ${timerState.subject}"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatClock(timerState.displaySeconds),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = palette.textPrimary
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (timerState.isRunning) onPause() else onStartOrResume()
                    }
                    .testTag("ambient_screen_timer_toggle_button"),
                shape = RoundedCornerShape(16.dp),
                color = accent.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (timerState.isRunning) "Pause focus timer" else "Resume focus timer",
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (timerState.isRunning) "Pause" else "Resume",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientEssentialGridCard(
    preset: AmbientSoundPreset,
    isSelected: Boolean,
    isPlaying: Boolean,
    waveAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val (icon, tint) = remember(preset) { ambientPresetVisual(preset) }
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "essentialCardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isPlaying -> tint.copy(alpha = 0.72f)
            isSelected -> tint.copy(alpha = 0.45f)
            else -> palette.glassBorderSubtle
        },
        label = "essentialCardBorder"
    )
    val fillColor by animateColorAsState(
        targetValue = when {
            isPlaying -> tint.copy(alpha = if (palette.isDark) 0.24f else 0.14f)
            isSelected -> palette.glassFillElevated
            else -> palette.glassFill
        },
        label = "essentialCardFill"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = 114.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("ambient_screen_preset_${preset.name.lowercase()}"),
        shape = RoundedCornerShape(24.dp),
        color = fillColor,
        border = BorderStroke(if (isPlaying || isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tint.copy(alpha = if (isPlaying) 0.28f else 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = preset.displayName,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                }

                if (isPlaying) {
                    AmbientWaveBars(
                        color = tint,
                        amplitude = waveAmplitude,
                        barCount = 3
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) tint.copy(alpha = 0.18f) else palette.chipUnselected
                    ) {
                        Text(
                            text = when {
                                preset == AmbientSoundPreset.OFF && isSelected -> "Active"
                                preset == AmbientSoundPreset.OFF -> "Mute"
                                isSelected -> "Paused"
                                else -> "Play"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) tint else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
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
    }
}

@Composable
private fun AmbientSoundListTile(
    preset: AmbientSoundPreset,
    unlocked: Boolean,
    isSelected: Boolean,
    isPlaying: Boolean,
    waveAmplitude: Float,
    userGems: Int,
    onClick: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val (icon, tint) = remember(preset) { ambientPresetVisual(preset) }
    val canAfford = userGems >= preset.gemCost

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("ambient_screen_preset_${preset.name.lowercase()}"),
        shape = RoundedCornerShape(22.dp),
        color = when {
            isPlaying -> tint.copy(alpha = if (palette.isDark) 0.22f else 0.12f)
            isSelected -> palette.glassFillElevated
            else -> palette.glassFill
        },
        border = BorderStroke(
            if (isPlaying || isSelected) 1.4.dp else 1.dp,
            when {
                isPlaying -> tint.copy(alpha = 0.65f)
                isSelected -> tint.copy(alpha = 0.38f)
                else -> palette.glassBorderSubtle
            }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(tint.copy(alpha = if (unlocked) 0.18f else 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (unlocked) tint else palette.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (unlocked) palette.textPrimary else palette.textSecondary,
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
                isPlaying -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AmbientWaveBars(color = tint, amplitude = waveAmplitude, barCount = 4)
                        Surface(
                            shape = CircleShape,
                            color = tint,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = "Pause ${preset.displayName}",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                unlocked -> {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) tint.copy(alpha = 0.22f) else palette.chipUnselected,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) tint.copy(alpha = 0.50f) else palette.glassBorderSubtle
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play ${preset.displayName}",
                                tint = if (isSelected) tint else palette.textPrimary,
                                modifier = Modifier.size(20.dp)
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
