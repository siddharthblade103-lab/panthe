package com.example.ui.components

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.StreakDayProgress
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

/**
 * Visual day-by-day Focus Streaks tracker card rendered with frosted glassmorphism.
 * Features a 7-day connected progress ring chain, interactive day inspector pill,
 * live rolling streak counter, and an optional 14-day habit consistency ribbon.
 */
@Composable
fun FocusStreaksTrackerCard(
    currentStreak: Int,
    longestStreak: Int,
    streakDays: List<StreakDayProgress>,
    modifier: Modifier = Modifier,
    showTwoWeekRibbon: Boolean = false
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    val recentSeven = remember(streakDays) {
        if (streakDays.size >= 7) streakDays.takeLast(7) else streakDays
    }
    if (recentSeven.isEmpty()) return

    var selectedIndex by remember(recentSeven.size) {
        mutableIntStateOf(recentSeven.lastIndex.coerceAtLeast(0))
    }
    val safeSelectedIndex = selectedIndex.coerceIn(0, recentSeven.lastIndex)
    val selectedDay = recentSeven[safeSelectedIndex]
    val todayDay = recentSeven.lastOrNull()
    val todayLit = todayDay?.hasFocused == true

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val infinite = rememberInfiniteTransition(label = "streakFlameAnim")
    val flameScale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flamePulse"
    )
    val todayBeaconAlpha by infinite.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.46f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "todayBeacon"
    )

    val subtitleText = when {
        todayLit -> stringResource(R.string.focus_streaks_protected)
        currentStreak > 0 -> stringResource(R.string.focus_streaks_prompt)
        else -> stringResource(R.string.focus_streaks_start_prompt)
    }

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("focus_streaks_card"),
        cornerRadius = 28.dp,
        elevated = true,
        accentTint = if (todayLit) palette.streakColor else palette.primary,
        shimmer = todayLit,
        frostedBlur = true,
        blurRadius = 20.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Header: Flame Emblem + Title + Live Streak Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        palette.streakColor.copy(alpha = if (todayLit) 0.28f else 0.16f),
                                        palette.streakColor.copy(alpha = 0.06f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                color = palette.streakColor.copy(alpha = if (todayLit) 0.55f else 0.28f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (todayLit) Icons.Rounded.Whatshot else Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Focus streak flame",
                            tint = palette.streakColor,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    if (todayLit) {
                                        scaleX = flameScale
                                        scaleY = flameScale
                                    }
                                }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.focus_streaks_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Current Streak + Best Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.streakColor.copy(alpha = if (palette.isDark) 0.18f else 0.13f),
                    border = BorderStroke(1.dp, palette.streakColor.copy(alpha = 0.34f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedCounterText(
                                count = currentStreak,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.streakColor
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (currentStreak == 1) "day" else "days",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.streakColor
                            )
                        }
                        Text(
                            text = stringResource(R.string.focus_streaks_best_label, longestStreak),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = palette.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Day-by-Day 7-Day Connected Progress Chain
            Box(modifier = Modifier.fillMaxWidth()) {
                // Behind-the-nodes glowing horizontal streak connector rail
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    if (recentSeven.size >= 2) {
                        val centerY = size.height / 2f
                        val count = recentSeven.size
                        val stepX = size.width / (count - 1).coerceAtLeast(1)

                        // Soft translucent frosted baseline rail
                        drawLine(
                            color = if (palette.isDark) {
                                Color.White.copy(alpha = 0.12f)
                            } else {
                                palette.glassBorderSubtle.copy(alpha = 0.65f)
                            },
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = 3.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Highlighted streak chain segments between consecutive focused days
                        for (i in 0 until count - 1) {
                            val leftFocused = recentSeven[i].hasFocused
                            val rightFocused = recentSeven[i + 1].hasFocused
                            if (leftFocused && rightFocused) {
                                val startX = i * stepX
                                val endX = (i + 1) * stepX
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(palette.streakColor, palette.primary),
                                        startX = startX,
                                        endX = endX
                                    ),
                                    start = Offset(startX, centerY),
                                    end = Offset(endX, centerY),
                                    strokeWidth = 4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                // 7 Interactive Circular Day Nodes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    recentSeven.forEachIndexed { idx, day ->
                        StreakDayNode(
                            day = day,
                            selected = idx == safeSelectedIndex,
                            revealed = revealed,
                            todayBeaconAlpha = todayBeaconAlpha,
                            onClick = {
                                tactile?.playTap()
                                selectedIndex = idx
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Interactive Selected Day Frosted Inspector Capsule
            MetamorphosisContainer(
                targetState = selectedDay,
                label = "streakSelectedDayDetail"
            ) { day ->
                val dayHeading = if (day.isToday) {
                    "Today · ${day.monthDayLabel}"
                } else {
                    "${day.dayFullLabel} · ${day.monthDayLabel}"
                }

                val statusLabel = when {
                    day.goalMet -> "Goal Met"
                    day.hasFocused -> "Streak Lit"
                    day.isToday -> "Ready"
                    else -> "Rest Day"
                }
                val statusColor = when {
                    day.goalMet -> palette.streakColor
                    day.hasFocused -> palette.primary
                    day.isToday -> palette.gemColor
                    else -> palette.textMuted
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (palette.isDark) {
                                Color.White.copy(alpha = 0.06f)
                            } else {
                                Color.White.copy(alpha = 0.45f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.72f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = dayHeading,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            Text(
                                text = if (day.hasFocused) {
                                    val sprintWord = if (day.sessionsCount == 1) "sprint" else "sprints"
                                    "${day.focusedMinutes}m / ${day.goalMinutes}m · ${day.sessionsCount} $sprintWord"
                                } else if (day.isToday) {
                                    "0m / ${day.goalMinutes}m · Complete 1 sprint to ignite today"
                                } else {
                                    "No focus sessions recorded"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = if (palette.isDark) 0.20f else 0.14f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.32f))
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 4. Optional 14-Day Habit Consistency Ribbon (shown on Focus Path screen)
            if (showTwoWeekRibbon && streakDays.size >= 14) {
                Spacer(modifier = Modifier.height(14.dp))
                val activeCount = streakDays.count { it.hasFocused }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.focus_streaks_two_week_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textSecondary
                    )
                    Text(
                        text = "$activeCount / 14 active days",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.primaryLip
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    streakDays.forEach { day ->
                        val tileColor = when {
                            day.goalMet -> palette.streakColor
                            day.progress >= 0.5f -> palette.primary
                            day.hasFocused -> palette.primary.copy(alpha = 0.58f)
                            else -> if (palette.isDark) Color.White.copy(alpha = 0.09f) else palette.chipUnselected
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(tileColor)
                                .then(
                                    if (day.isToday) {
                                        Modifier.border(
                                            width = 1.2.dp,
                                            color = palette.textPrimary.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(5.dp)
                                        )
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakDayNode(
    day: StreakDayProgress,
    selected: Boolean,
    revealed: Boolean,
    todayBeaconAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current

    val animatedRingProgress by animateFloatAsState(
        targetValue = if (revealed) day.progress else 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessLow),
        label = "dayProgress_${day.dateKey}"
    )

    val nodeScale by animateFloatAsState(
        targetValue = if (selected) 1.09f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dayScale_${day.dateKey}"
    )

    val labelColor by animateColorAsState(
        targetValue = when {
            selected -> palette.textPrimary
            day.isToday -> palette.primaryLip
            else -> palette.textMuted
        },
        animationSpec = tween(200),
        label = "dayLabelColor_${day.dateKey}"
    )

    val a11yDescription = buildString {
        append(if (day.isToday) "Today" else day.dayFullLabel)
        append(", ")
        append(day.monthDayLabel)
        if (day.hasFocused) {
            append(", ${day.focusedMinutes} minutes focused")
        } else {
            append(", no sessions yet")
        }
    }

    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 48.dp)
            .testTag("streak_day_${day.dateKey}")
            .semantics { contentDescription = a11yDescription }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = nodeScale
                    scaleY = nodeScale
                },
            contentAlignment = Alignment.Center
        ) {
            // Subtle breathing pulse halo on Today when waiting for first sprint
            if (day.isToday && !day.hasFocused) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.primary.copy(alpha = todayBeaconAlpha * 0.45f))
                )
            }

            // Outer day progress ring Canvas
            Canvas(modifier = Modifier.size(40.dp)) {
                val stroke = 3.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)

                // Track circle
                drawArc(
                    color = when {
                        selected -> palette.textPrimary.copy(alpha = 0.28f)
                        day.isToday -> palette.primary.copy(alpha = 0.35f)
                        else -> if (palette.isDark) Color.White.copy(alpha = 0.14f) else palette.glassBorderSubtle
                    },
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                // Filled goal progress arc
                if (animatedRingProgress > 0.01f) {
                    val sweep = (animatedRingProgress * 360f).coerceIn(12f, 360f)
                    val arcBrush = Brush.sweepGradient(
                        colors = if (day.goalMet) {
                            listOf(palette.streakColor, Color(0xFFFFC800), palette.streakColor)
                        } else {
                            listOf(palette.primary, Color(0xFF89E219), palette.primary)
                        }
                    )
                    drawArc(
                        brush = arcBrush,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            // Inner circular status orb
            val innerFillBrush = when {
                day.goalMet -> Brush.linearGradient(
                    colors = listOf(Color(0xFFFFB020), palette.streakColor)
                )
                day.hasFocused -> Brush.linearGradient(
                    colors = listOf(Color(0xFF7AE81A), palette.primary)
                )
                else -> Brush.linearGradient(
                    colors = if (palette.isDark) {
                        listOf(Color(0x8820362A), Color(0x6616261D))
                    } else {
                        listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.55f))
                    }
                )
            }

            Box(
                modifier = Modifier
                    .size(31.dp)
                    .clip(CircleShape)
                    .background(innerFillBrush)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (day.hasFocused) {
                            Color.White.copy(alpha = 0.55f)
                        } else if (selected) {
                            palette.primary.copy(alpha = 0.70f)
                        } else {
                            Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.80f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    day.goalMet -> {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    day.hasFocused -> {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "${day.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (day.isToday || selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = if (day.isToday) palette.primaryLip else palette.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = if (day.isToday) "Today" else day.dayFullLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (day.isToday || selected) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 10.sp
            ),
            color = labelColor,
            maxLines = 1
        )
    }
}
