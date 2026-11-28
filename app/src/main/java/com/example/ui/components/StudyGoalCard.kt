package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.StudyGoalPeriod
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Liquid-Crystal Study Goals card with interactive Daily/Weekly switcher,
 * spring-animated prismatic progress bar that fills up as focus sessions complete,
 * and a 1-tap goal editor dialog.
 */
@Composable
fun StudyGoalProgressCard(
    todayMinutes: Int,
    dailyGoalMinutes: Int,
    weeklyMinutes: Int,
    weeklyGoalMinutes: Int,
    selectedPeriod: StudyGoalPeriod,
    onSelectPeriod: (StudyGoalPeriod) -> Unit,
    onSaveStudyGoals: (StudyGoalPeriod, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    sprintMinutesReference: Int = 25,
    extraBottomContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    var showGoalDialog by remember { mutableStateOf(false) }

    val safeDailyGoal = dailyGoalMinutes.coerceAtLeast(15)
    val safeWeeklyGoal = weeklyGoalMinutes.coerceAtLeast(60)

    val activeMinutes = if (selectedPeriod == StudyGoalPeriod.DAILY) todayMinutes else weeklyMinutes
    val activeGoal = if (selectedPeriod == StudyGoalPeriod.DAILY) safeDailyGoal else safeWeeklyGoal
    val rawProgress = (activeMinutes.toFloat() / activeGoal.toFloat()).coerceIn(0f, 1f)
    val percent = ((activeMinutes.toFloat() / activeGoal.toFloat()) * 100f).roundToInt().coerceAtLeast(0)
    val goalReached = activeMinutes >= activeGoal
    val remainingMinutes = (activeGoal - activeMinutes).coerceAtLeast(0)

    val otherPeriod = if (selectedPeriod == StudyGoalPeriod.DAILY) StudyGoalPeriod.WEEKLY else StudyGoalPeriod.DAILY
    val otherMinutes = if (otherPeriod == StudyGoalPeriod.DAILY) todayMinutes else weeklyMinutes
    val otherGoal = if (otherPeriod == StudyGoalPeriod.DAILY) safeDailyGoal else safeWeeklyGoal
    val otherPercent = ((otherMinutes.toFloat() / otherGoal.toFloat()) * 100f).roundToInt().coerceAtMost(999)

    if (showGoalDialog) {
        SetStudyGoalDialog(
            initialPeriod = selectedPeriod,
            initialDailyMinutes = safeDailyGoal,
            initialWeeklyMinutes = safeWeeklyGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { period, newDaily, newWeekly ->
                showGoalDialog = false
                onSaveStudyGoals(period, newDaily, newWeekly)
            }
        )
    }

    MorphismSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        elevated = true,
        shimmer = goalReached,
        accentTint = if (goalReached) palette.streakColor else palette.primary,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row: Icon + Title + 1-tap Set Goal Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (goalReached) palette.streakColor.copy(alpha = 0.18f)
                                else palette.primarySoft
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (goalReached) Icons.Rounded.EmojiEvents else Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = if (goalReached) palette.streakColor else palette.primaryLip,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.study_goal_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (selectedPeriod == StudyGoalPeriod.DAILY) {
                                "Today's target • updates live"
                            } else {
                                "7-day target • updates live"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("edit_study_goal_button")
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            tactile?.playTap()
                            showGoalDialog = true
                        },
                    shape = RoundedCornerShape(50),
                    color = palette.glassFillElevated,
                    border = BorderStroke(1.dp, palette.glassBorderHighlight),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Set study goal",
                            tint = palette.primaryLip,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.study_goal_edit_cta),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fluid Daily / Weekly Sliding Glass Switcher
            SlidingGlassSegmentedBar(
                labels = listOf(
                    stringResource(R.string.study_goal_daily_tab),
                    stringResource(R.string.study_goal_weekly_tab)
                ),
                selectedIndex = if (selectedPeriod == StudyGoalPeriod.DAILY) 0 else 1,
                onSelect = { idx ->
                    onSelectPeriod(if (idx == 0) StudyGoalPeriod.DAILY else StudyGoalPeriod.WEEKLY)
                },
                tagPrefix = "goal_period_tab"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Readout + Percentage Pill
            AnimatedContent(
                targetState = Triple(selectedPeriod, activeMinutes, activeGoal),
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "goalMetricsSwap"
            ) { (period, doneMins, targetMins) ->
                val isDone = doneMins >= targetMins
                val pct = ((doneMins.toFloat() / targetMins.toFloat()) * 100f).roundToInt().coerceAtLeast(0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatStudyDurationCompact(doneMins),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp
                                ),
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "/ ${formatStudyDurationCompact(targetMins)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = palette.textSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val statusText = if (isDone) {
                            val extra = doneMins - targetMins
                            if (extra > 0) "Goal crushed! +${formatStudyDurationCompact(extra)} bonus focus"
                            else "Goal reached! Keep the momentum soaring"
                        } else {
                            val sprintsNeeded = ceil(
                                (targetMins - doneMins).toDouble() / sprintMinutesReference.coerceAtLeast(5)
                            ).toInt().coerceAtLeast(1)
                            val periodWord = if (period == StudyGoalPeriod.DAILY) "today" else "this week"
                            "${formatStudyDurationCompact(targetMins - doneMins)} left $periodWord • ~$sprintsNeeded ${if (sprintsNeeded == 1) "sprint" else "sprints"}"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDone) palette.streakColor else palette.textSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isDone) palette.streakColor.copy(alpha = 0.18f) else palette.primarySoft,
                        border = BorderStroke(
                            1.dp,
                            if (isDone) palette.streakColor.copy(alpha = 0.45f) else palette.primary.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.streakColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "${pct.coerceAtMost(999)}%",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (isDone) palette.streakColor else palette.primaryLip
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Prismatic Liquid-Glass Progress Bar
            LiquidStudyProgressBar(
                progress = rawProgress,
                goalReached = goalReached,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("study_goal_progress_bar")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Complementary Period Summary Pill (tap to flip between Daily & Weekly)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("goal_secondary_summary_pill")
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        tactile?.playTap()
                        onSelectPeriod(otherPeriod)
                    },
                shape = RoundedCornerShape(16.dp),
                color = palette.chipUnselected.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, palette.glassBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (otherPeriod == StudyGoalPeriod.WEEKLY) {
                            "Weekly progress: ${formatStudyDurationCompact(otherMinutes)} / ${formatStudyDurationCompact(otherGoal)}"
                        } else {
                            "Today's progress: ${formatStudyDurationCompact(otherMinutes)} / ${formatStudyDurationCompact(otherGoal)}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textSecondary
                    )
                    Text(
                        text = "$otherPercent% • View",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.primaryLip
                    )
                }
            }

            if (extraBottomContent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                extraBottomContent()
            }
        }
    }
}

/**
 * Custom Canvas Liquid-Crystal progress bar with spring fill animation,
 * milestone tick marks at 25/50/75%, glossy top reflection, and leading glow cap.
 */
@Composable
fun LiquidStudyProgressBar(
    progress: Float,
    goalReached: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    val palette = LocalTryAgainPalette.current
    val clampedTarget = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = clampedTarget,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquidGoalFill"
    )

    val infinite = rememberInfiniteTransition(label = "barSheen")
    val sheenPhase by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheenSlide"
    )

    val fillColors = if (goalReached) {
        listOf(palette.streakColor, Color(0xFFFFD54F), palette.primary)
    } else {
        listOf(palette.primary, palette.prismCyan, palette.primary)
    }

    Canvas(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(50))
    ) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)

        // 1. Frosted recessed glass track
        drawRoundRect(
            color = palette.chipUnselected,
            size = size,
            cornerRadius = corner
        )
        drawRoundRect(
            color = palette.glassBorderSubtle,
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )

        // 2. Subtle milestone tick dividers (25%, 50%, 75%)
        listOf(0.25f, 0.50f, 0.75f).forEach { mark ->
            val x = size.width * mark
            drawLine(
                color = palette.textMuted.copy(alpha = 0.24f),
                start = Offset(x, size.height * 0.24f),
                end = Offset(x, size.height * 0.76f),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        // 3. Animated prismatic fill capsule
        if (animatedProgress > 0.004f) {
            val minCapWidth = size.height
            val fillWidth = (size.width * animatedProgress).coerceAtLeast(minCapWidth).coerceAtMost(size.width)
            val fillSize = Size(fillWidth, size.height)

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = fillColors,
                    startX = 0f,
                    endX = fillWidth
                ),
                size = fillSize,
                cornerRadius = corner
            )

            // Glossy top specular meniscus
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.52f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.55f
                ),
                topLeft = Offset(2.dp.toPx(), 1.2.dp.toPx()),
                size = Size((fillWidth - 4.dp.toPx()).coerceAtLeast(0f), size.height * 0.42f),
                cornerRadius = CornerRadius(size.height / 3f, size.height / 3f)
            )

            // Subtle traveling optical sheen inside the filled capsule
            val sheenCenter = fillWidth * sheenPhase
            val sheenSpan = size.width * 0.22f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    startX = sheenCenter - sheenSpan,
                    endX = sheenCenter + sheenSpan
                ),
                size = fillSize,
                cornerRadius = corner
            )

            // Leading crystal pinpoint highlight
            val capCenterX = (fillWidth - size.height * 0.45f).coerceAtLeast(size.height * 0.45f)
            drawCircle(
                color = Color.White.copy(alpha = 0.72f),
                radius = size.height * 0.20f,
                center = Offset(capCenterX, size.height * 0.38f)
            )
        }
    }
}

/**
 * Liquid Crystal modal dialog for setting both Daily and Weekly study goals
 * using quick presets or precision sliders.
 */
@Composable
fun SetStudyGoalDialog(
    initialPeriod: StudyGoalPeriod,
    initialDailyMinutes: Int,
    initialWeeklyMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (StudyGoalPeriod, Int, Int) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    var draftPeriod by remember(initialPeriod) { mutableStateOf(initialPeriod) }
    var draftDaily by remember(initialDailyMinutes) {
        mutableFloatStateOf(initialDailyMinutes.coerceIn(15, 360).toFloat())
    }
    var draftWeekly by remember(initialWeeklyMinutes) {
        mutableFloatStateOf(initialWeeklyMinutes.coerceIn(60, 2100).toFloat())
    }

    val dailyPresets = listOf(30, 60, 90, 120, 180)
    val weeklyPresets = listOf(180, 300, 420, 600, 900) // 3h, 5h, 7h, 10h, 15h

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            MorphismSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                cornerRadius = 30.dp,
                elevated = true,
                shimmer = true,
                accentTint = palette.primary,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.study_goal_dialog_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.study_goal_dialog_subtitle),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                tactile?.playTap()
                                onDismiss()
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("close_study_goal_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close study goals dialog",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    // Active Tracker View Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Primary Progress Bar View",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        SlidingGlassSegmentedBar(
                            labels = listOf("Daily Goal", "Weekly Goal"),
                            selectedIndex = if (draftPeriod == StudyGoalPeriod.DAILY) 0 else 1,
                            onSelect = { idx ->
                                draftPeriod = if (idx == 0) StudyGoalPeriod.DAILY else StudyGoalPeriod.WEEKLY
                            },
                            tagPrefix = "dialog_goal_period"
                        )
                    }

                    // Daily Study Goal Section
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = palette.glassFillElevated,
                        border = BorderStroke(
                            1.dp,
                            if (draftPeriod == StudyGoalPeriod.DAILY) palette.primary.copy(alpha = 0.45f)
                            else palette.glassBorderSubtle
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Study Goal",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "${formatStudyDurationCompact(draftDaily.roundToInt())} / day",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.primaryLip
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dailyPresets.forEach { preset ->
                                    MorphismChip(
                                        label = formatStudyDurationCompact(preset),
                                        selected = draftDaily.roundToInt() == preset,
                                        onClick = { draftDaily = preset.toFloat() },
                                        testTag = "daily_preset_$preset"
                                    )
                                }
                            }

                            Slider(
                                value = draftDaily,
                                onValueChange = { draftDaily = ((it / 15f).roundToInt() * 15).coerceIn(15, 360).toFloat() },
                                valueRange = 15f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primary,
                                    activeTrackColor = palette.primary,
                                    inactiveTrackColor = palette.glassBorderSubtle
                                ),
                                modifier = Modifier.testTag("daily_goal_slider")
                            )
                        }
                    }

                    // Weekly Study Goal Section
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = palette.glassFillElevated,
                        border = BorderStroke(
                            1.dp,
                            if (draftPeriod == StudyGoalPeriod.WEEKLY) palette.primary.copy(alpha = 0.45f)
                            else palette.glassBorderSubtle
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val weeklyInt = draftWeekly.roundToInt()
                            val avgDailyPace = (weeklyInt / 7f).roundToInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Weekly Study Goal",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "Pace: ~${formatStudyDurationCompact(avgDailyPace)}/day",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textMuted
                                    )
                                }
                                Text(
                                    text = "${formatStudyDurationCompact(weeklyInt)} / week",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.primaryLip
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                weeklyPresets.forEach { preset ->
                                    MorphismChip(
                                        label = formatStudyDurationCompact(preset),
                                        selected = weeklyInt == preset,
                                        onClick = { draftWeekly = preset.toFloat() },
                                        testTag = "weekly_preset_$preset"
                                    )
                                }
                            }

                            Slider(
                                value = draftWeekly,
                                onValueChange = { draftWeekly = ((it / 30f).roundToInt() * 30).coerceIn(60, 2100).toFloat() },
                                valueRange = 60f..2100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primary,
                                    activeTrackColor = palette.primary,
                                    inactiveTrackColor = palette.glassBorderSubtle
                                ),
                                modifier = Modifier.testTag("weekly_goal_slider")
                            )
                        }
                    }

                    // Primary Save Button
                    MorphismActionButton(
                        text = stringResource(R.string.study_goal_save_cta),
                        icon = Icons.Rounded.Flag,
                        onClick = {
                            onSave(
                                draftPeriod,
                                draftDaily.roundToInt(),
                                draftWeekly.roundToInt()
                            )
                        },
                        height = 52.dp,
                        cornerRadius = 18.dp,
                        testTag = "save_study_goals_button"
                    )
                }
            }
        }
    }
}

fun formatStudyDurationCompact(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours == 0 -> "${mins}m"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}
