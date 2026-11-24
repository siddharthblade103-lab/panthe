package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AiCoachUiState
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.AppPreferencesState
import com.example.data.model.MascotBadgeId
import com.example.data.model.PomodoroPhase
import com.example.data.model.PomodoroTask
import com.example.data.model.StreakDayProgress
import com.example.data.model.TimerUiState
import com.example.data.model.UserProfile
import com.example.ui.components.FocusStreaksTrackerCard
import com.example.ui.components.GemPillBadge
import com.example.ui.components.LocalTactileSound
import com.example.ui.components.MetamorphosisContainer
import com.example.ui.components.MorphismChip
import com.example.ui.components.MorphismSurface
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StreakPillBadge
import com.example.ui.components.TryAgainFocusRingCard
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

@Composable
fun FocusHubScreen(
    profile: UserProfile,
    timerState: TimerUiState,
    preferences: AppPreferencesState,
    tasks: List<PomodoroTask>,
    streakDays: List<StreakDayProgress>,
    activeAmbient: AmbientSoundPreset,
    isAmbientPlaying: Boolean,
    aiCoachState: AiCoachUiState,
    onSelectSubject: (String) -> Unit,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onSetDuration: (Int) -> Unit,
    onAdjustDuration: (Int) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onCompleteOrSkip: () -> Unit,
    onToggleAmbient: (AmbientSoundPreset) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenShop: () -> Unit,
    onRequestStudyTip: () -> Unit,
    onRequestMotivation: () -> Unit,
    onAskCoach: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (PomodoroTask) -> Unit,
    onPinTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val equippedBadge = remember(profile.equippedBadge) { MascotBadgeId.fromWire(profile.equippedBadge) }
    val activeTaskTitle = remember(tasks, timerState.activeTaskId) {
        tasks.firstOrNull { it.id == timerState.activeTaskId }?.title
    }

    // Friendly breathing monkey companion bob
    val companionMotion = rememberInfiniteTransition(label = "monkeyBob")
    val monkeyBobY by companionMotion.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "monkeyBobY"
    )

    val friendlyLine = remember(timerState.isRunning, timerState.isPaused, timerState.phase, timerState.subject, profile.displayName) {
        when {
            timerState.isRunning && timerState.phase == PomodoroPhase.FOCUS ->
                "Deep flow on ${timerState.subject} — you've got this!"
            timerState.isRunning ->
                "Breathe easy, ${profile.displayName}. Recharge for the next round."
            timerState.isPaused ->
                "Paused for a sec — tap Resume whenever you're ready!"
            else ->
                "Hey ${profile.displayName}! Ready for a calm ${timerState.plannedMinutes}m sprint?"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Compact Header + Friendly Monkey Companion Status
        item {
            StaggeredEntrance(index = 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tryagain_launcher_fg_1795336082633),
                                contentDescription = "Try Again mascot",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .offset(y = monkeyBobY.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, palette.glassBorderHighlight, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Try Again",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = friendlyLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StreakPillBadge(streakDays = profile.currentStreak)
                            GemPillBadge(
                                gems = profile.gems,
                                onClick = onOpenShop,
                                modifier = Modifier.testTag("top_gem_pill")
                            )
                        }
                    }
                }
            }
        }

        // 2. Minimal Subject Chips Strip
        item {
            StaggeredEntrance(index = 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(profile.subjects.ifEmpty { UserProfile.DEFAULT_SUBJECTS }) { subject ->
                        MorphismChip(
                            label = subject,
                            selected = timerState.subject == subject,
                            onClick = { onSelectSubject(subject) },
                            testTag = "subject_chip_${subject.lowercase()}"
                        )
                    }
                }
            }
        }

        // 3. Hero Liquid-Glass Circular Pomodoro Timer Card
        item {
            StaggeredEntrance(index = 2) {
                TryAgainFocusRingCard(
                    timerState = timerState,
                    equippedBadge = equippedBadge,
                    activeTaskTitle = activeTaskTitle,
                    onSelectPhase = onSelectPhase,
                    onSetDuration = onSetDuration,
                    onAdjustDuration = onAdjustDuration,
                    onStartOrResume = onStartOrResume,
                    onPause = onPause,
                    onReset = onReset,
                    onCompleteOrSkip = onCompleteOrSkip
                )
            }
        }

        // 4. Visual Day-by-Day Focus Streaks Tracker Card
        item {
            StaggeredEntrance(index = 3) {
                FocusStreaksTrackerCard(
                    currentStreak = profile.currentStreak,
                    longestStreak = profile.longestStreak,
                    streakDays = streakDays,
                    showTwoWeekRibbon = false
                )
            }
        }

        // 5. Gemini AI Study Coach Card
        item {
            StaggeredEntrance(index = 4) {
                GeminiCoachGlassCard(
                    aiState = aiCoachState,
                    subject = timerState.subject,
                    onStudyTip = onRequestStudyTip,
                    onMotivate = onRequestMotivation,
                    onAskCustom = onAskCoach
                )
            }
        }

        // 6. Compact Ambient Sound Strip
        item {
            StaggeredEntrance(index = 5) {
                AmbientQuickStripCard(
                    unlockedSounds = profile.unlockedSounds,
                    activeAmbient = activeAmbient,
                    isPlaying = isAmbientPlaying,
                    volume = preferences.ambientVolume,
                    onSelectSound = { preset ->
                        val unlocked = preset.gemCost == 0 || preset.name in profile.unlockedSounds
                        if (unlocked) onToggleAmbient(preset) else onOpenShop()
                    },
                    onVolumeChange = onVolumeChange,
                    onOpenShop = onOpenShop
                )
            }
        }

        // 7. Clean Minimal Study Tasks Card
        item {
            StaggeredEntrance(index = 6) {
                StudyTasksGlassCard(
                    tasks = tasks,
                    activeTaskId = timerState.activeTaskId,
                    onAddTask = onAddTask,
                    onToggleTask = onToggleTask,
                    onPinTask = { id ->
                        tactile?.playTap()
                        onPinTask(id)
                    },
                    onDeleteTask = onDeleteTask
                )
            }
        }
    }
}

@Composable
private fun GeminiCoachGlassCard(
    aiState: AiCoachUiState,
    subject: String,
    onStudyTip: () -> Unit,
    onMotivate: () -> Unit,
    onAskCustom: (String) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    var showQuestionBox by remember { mutableStateOf(false) }
    var questionText by remember { mutableStateOf("") }

    val sparklePulse = rememberInfiniteTransition(label = "coachSparkle")
    val iconScale by sparklePulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )

    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        accentTint = palette.primary,
        shimmer = aiState is AiCoachUiState.Loading || aiState is AiCoachUiState.Streaming,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.primarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Gemini AI Study Coach",
                            tint = palette.primaryLip,
                            modifier = Modifier
                                .size(19.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.focus_ai_coach_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Smart boost for $subject",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                MorphismChip(
                    label = if (showQuestionBox) "Hide" else "Ask",
                    selected = showQuestionBox,
                    onClick = { showQuestionBox = !showQuestionBox },
                    selectedColor = palette.gemColor,
                    testTag = "ai_ask_toggle"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MetamorphosisContainer(
                targetState = aiState,
                label = "aiCoachOutput"
            ) { state ->
                when (state) {
                    AiCoachUiState.Idle -> {
                        Text(
                            text = "Tap Study Tip or Motivate Me for a crisp AI study boost while you focus.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }

                    AiCoachUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.4.dp,
                                color = palette.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Crafting a $subject focus boost…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }

                    is AiCoachUiState.Streaming -> {
                        Text(
                            text = state.text,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = palette.textPrimary
                        )
                    }

                    is AiCoachUiState.Success -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.glassFillElevated,
                            border = BorderStroke(1.dp, palette.glassBorderSubtle)
                        ) {
                            Text(
                                text = state.text,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = palette.textPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    is AiCoachUiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.coralAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.coralAccent,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showQuestionBox) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.focus_ai_ask_hint),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.glassBorderSubtle,
                                focusedContainerColor = palette.glassFillElevated,
                                unfocusedContainerColor = palette.glassFillElevated
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (questionText.isNotBlank()) {
                                    onAskCustom(questionText)
                                    questionText = ""
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_question_input")
                        )
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ai_send_button")
                                .clip(CircleShape)
                                .clickable(enabled = questionText.isNotBlank()) {
                                    onAskCustom(questionText)
                                    questionText = ""
                                },
                            shape = CircleShape,
                            color = if (questionText.isNotBlank()) palette.primary else palette.chipUnselected
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send study question",
                                    tint = if (questionText.isNotBlank()) Color.White else palette.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MorphismChip(
                    label = stringResource(R.string.focus_ai_tip_cta),
                    leadingIcon = Icons.Rounded.TipsAndUpdates,
                    selected = false,
                    onClick = {
                        tactile?.playTap()
                        onStudyTip()
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "ai_study_tip_button"
                )
                MorphismChip(
                    label = stringResource(R.string.focus_ai_motivate_cta),
                    leadingIcon = Icons.Rounded.AutoAwesome,
                    selected = true,
                    selectedColor = palette.primary,
                    onClick = {
                        tactile?.playTap()
                        onMotivate()
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "ai_motivate_button"
                )
            }
        }
    }
}

@Composable
private fun AmbientQuickStripCard(
    unlockedSounds: List<String>,
    activeAmbient: AmbientSoundPreset,
    isPlaying: Boolean,
    volume: Float,
    onSelectSound: (AmbientSoundPreset) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenShop: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val waveAnim = rememberInfiniteTransition(label = "ambientWave")
    val barPulse by waveAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barPulse"
    )

    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (isPlaying) palette.primary else palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.focus_ambient_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                }

                if (activeAmbient != AmbientSoundPreset.OFF) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectSound(activeAmbient) },
                        shape = RoundedCornerShape(50),
                        color = if (isPlaying) palette.primarySoft else palette.chipUnselected
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause sound" else "Play sound",
                                tint = if (isPlaying) palette.primaryLip else palette.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeAmbient.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPlaying) palette.primaryLip else palette.textPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Unlock more →",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.gemColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                tactile?.playTap()
                                onOpenShop()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AmbientSoundPreset.unlockableEntries) { preset ->
                    val unlocked = preset.gemCost == 0 || preset.name in unlockedSounds
                    val active = activeAmbient == preset && isPlaying
                    MorphismChip(
                        label = if (unlocked) preset.displayName else "${preset.displayName} • ${preset.gemCost}",
                        leadingIcon = when {
                            !unlocked -> Icons.Rounded.Lock
                            active -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        selected = active,
                        onClick = { onSelectSound(preset) },
                        testTag = "ambient_chip_${preset.name.lowercase()}"
                    )
                }
            }

            AnimatedVisibility(visible = isPlaying || activeAmbient != AmbientSoundPreset.OFF) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Ambient volume",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0.05f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary,
                            inactiveTrackColor = palette.glassBorderSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isPlaying) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(20.dp)
                        ) {
                            listOf(0.55f, 1f, 0.72f, 0.9f).forEachIndexed { idx, factor ->
                                val phase = if (idx % 2 == 0) barPulse else (1.3f - barPulse).coerceIn(0.28f, 1f)
                                val h = (5f + 14f * factor * phase).dp
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h)
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyTasksGlassCard(
    tasks: List<PomodoroTask>,
    activeTaskId: String?,
    onAddTask: (String) -> Unit,
    onToggleTask: (PomodoroTask) -> Unit,
    onPinTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    var draft by remember { mutableStateOf("") }

    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.focus_tasks_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    text = "+5 Gems per task",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.gemColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.focus_add_task),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.glassBorderSubtle,
                        focusedContainerColor = palette.glassFillElevated,
                        unfocusedContainerColor = palette.glassFillElevated
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (draft.isNotBlank()) {
                            onAddTask(draft)
                            draft = ""
                        }
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_input")
                )

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("add_task_button")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = draft.isNotBlank()) {
                            onAddTask(draft)
                            draft = ""
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (draft.isNotBlank()) palette.primary else palette.chipUnselected
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add task",
                            tint = if (draft.isNotBlank()) Color.White else palette.textMuted
                        )
                    }
                }
            }

            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        val isPinned = task.id == activeTaskId
                        val checkScale by animateFloatAsState(
                            targetValue = if (task.completed) 1.15f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "taskCheckScale"
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = if (isPinned) palette.primarySoft else palette.glassFillElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isPinned) palette.primary.copy(alpha = 0.4f) else palette.glassBorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleTask(task) },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("task_toggle_${task.id}")
                                ) {
                                    Icon(
                                        imageVector = if (task.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = if (task.completed) "Mark incomplete" else "Mark complete",
                                        tint = if (task.completed) palette.primary else palette.textMuted,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = checkScale
                                            scaleY = checkScale
                                        }
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (task.completed) palette.textMuted else palette.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${task.subject} • ${task.completedPomodoros}/${task.estimatedPomodoros} sprints",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }

                                if (!task.completed) {
                                    IconButton(
                                        onClick = { onPinTask(task.id) },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PushPin,
                                            contentDescription = "Pin task to timer",
                                            tint = if (isPinned) palette.primaryLip else palette.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteTask(task.id) },
                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete task",
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
