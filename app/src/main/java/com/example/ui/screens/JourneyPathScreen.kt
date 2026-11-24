package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DayFocusPoint
import com.example.data.model.PathMilestone
import com.example.data.model.StreakDayProgress
import com.example.data.model.UserProfile
import com.example.ui.components.FocusStreaksTrackerCard
import com.example.ui.components.GemPillBadge
import com.example.ui.components.LocalTactileSound
import com.example.ui.components.MorphismActionButton
import com.example.ui.components.MorphismSurface
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

@Composable
fun JourneyPathScreen(
    profile: UserProfile,
    todayMinutes: Int,
    weeklyPoints: List<DayFocusPoint>,
    streakDays: List<StreakDayProgress>,
    milestones: List<PathMilestone>,
    onClaimMilestone: (PathMilestone) -> Unit,
    onOpenShop: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val dailyProgress = (todayMinutes.toFloat() / profile.dailyGoalMinutes.coerceAtLeast(15)).coerceIn(0f, 1f)
    val firstPendingIdx = milestones.indexOfFirst { !it.isClaimed }.takeIf { it >= 0 } ?: milestones.lastIndex

    var chartRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartRevealed = true }

    val animatedGoal by animateFloatAsState(
        targetValue = if (chartRevealed) dailyProgress else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "dailyGoalProgress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            StaggeredEntrance(index = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.journey_title),
                            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = NunitoDisplayFamily),
                            color = palette.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.journey_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                    GemPillBadge(gems = profile.gems, onClick = onOpenShop)
                }
            }
        }

        // Focus Streaks Day-by-Day Tracker + 14-Day Habit Consistency Ribbon
        item {
            StaggeredEntrance(index = 1) {
                FocusStreaksTrackerCard(
                    currentStreak = profile.currentStreak,
                    longestStreak = profile.longestStreak,
                    streakDays = streakDays,
                    showTwoWeekRibbon = true
                )
            }
        }

        // Animated Daily Goal & Growing 7-Day Glass Bars Card
        item {
            StaggeredEntrance(index = 2) {
                MorphismSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    elevated = true,
                    shimmer = true,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Focus Goal",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "$todayMinutes / ${profile.dailyGoalMinutes} min completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.primarySoft
                            ) {
                                Text(
                                    text = "${(dailyProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.primaryLip,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { animatedGoal },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                            color = palette.primary,
                            trackColor = palette.glassBorderSubtle
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        val maxMin = (weeklyPoints.maxOfOrNull { it.minutes } ?: 30).coerceAtLeast(30)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(106.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeklyPoints.forEachIndexed { idx, point ->
                                val isToday = idx == weeklyPoints.lastIndex
                                val targetFraction = (point.minutes.toFloat() / maxMin).coerceIn(0.08f, 1f)
                                val barFraction by animateFloatAsState(
                                    targetValue = if (chartRevealed) targetFraction else 0.05f,
                                    animationSpec = spring(
                                        dampingRatio = 0.72f,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "bar_$idx"
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (point.minutes > 0) "${point.minutes}m" else "",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = palette.textMuted
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(64.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(palette.chipUnselected)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(barFraction)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isToday) palette.primary else palette.primary.copy(alpha = 0.56f)
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = point.dayLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
                                        ),
                                        color = if (isToday) palette.primaryLip else palette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Label
        item {
            StaggeredEntrance(index = 2) {
                Text(
                    text = "Milestone Path",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = NunitoDisplayFamily),
                    color = palette.textPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Living Duolingo-Style Stepping-Stone Path
        itemsIndexed(milestones, key = { _, item -> item.id }) { index, milestone ->
            val isCurrent = index == firstPendingIdx
            val isLast = index == milestones.lastIndex
            StaggeredEntrance(index = index + 3) {
                MilestonePathRow(
                    milestone = milestone,
                    stepNumber = index + 1,
                    isCurrent = isCurrent,
                    isLast = isLast,
                    onClaim = { onClaimMilestone(milestone) }
                )
            }
        }
    }
}

@Composable
private fun MilestonePathRow(
    milestone: PathMilestone,
    stepNumber: Int,
    isCurrent: Boolean,
    isLast: Boolean,
    onClaim: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val canClaim = milestone.isUnlocked && !milestone.isClaimed

    val nodeMotion = rememberInfiniteTransition(label = "nodeMotion_${milestone.id}")
    val bobY by nodeMotion.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "monkeyNodeBob"
    )
    val beaconAlpha by nodeMotion.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconGlow"
    )
    val chestWiggle by nodeMotion.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chestWiggle"
    )

    val nodeColor = when {
        milestone.isClaimed -> palette.primary
        canClaim -> palette.streakColor
        isCurrent -> palette.gemColor
        else -> palette.chipUnselected
    }
    val nodeLip = when {
        milestone.isClaimed -> palette.primaryLip
        canClaim -> palette.streakLip
        isCurrent -> palette.gemLip
        else -> palette.glassBorderSubtle
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Left Stepping-Stone Rail
        Box(
            modifier = Modifier
                .width(70.dp)
                .padding(top = 4.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .width(6.dp)
                        .height(122.dp)
                        .offset(y = 56.dp)
                ) {
                    drawLine(
                        color = if (milestone.isClaimed) palette.primary else palette.glassBorderSubtle,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Pulsing beacon halo behind current or claimable node
            if (isCurrent || canClaim) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            (if (canClaim) palette.streakColor else palette.primary).copy(alpha = beaconAlpha)
                        )
                )
            }

            // 3D Duolingo-style circular milestone button
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        if (isCurrent) translationY = bobY
                        if (canClaim) rotationZ = chestWiggle
                    }
                    .clickable(enabled = canClaim) {
                        tactile?.playTap()
                        onClaim()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .offset(y = 4.dp)
                        .clip(CircleShape)
                        .background(nodeLip)
                )
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(if (isCurrent || canClaim) 9.dp else 2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(nodeColor)
                        .border(2.dp, Color.White.copy(alpha = 0.62f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isCurrent -> {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tryagain_launcher_fg_1795336082633),
                                contentDescription = "Current focus milestone",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                            )
                        }

                        milestone.isClaimed -> {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Milestone claimed",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        canClaim -> {
                            Icon(
                                imageVector = Icons.Rounded.CardGiftcard,
                                contentDescription = "Claim gem chest",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        else -> {
                            Icon(
                                imageVector = if (stepNumber == 6) Icons.Rounded.Star else Icons.Rounded.Lock,
                                contentDescription = "Locked milestone",
                                tint = palette.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Clean Frosted Glass Milestone Card
        MorphismSurface(
            modifier = Modifier.weight(1f),
            cornerRadius = 22.dp,
            elevated = isCurrent || canClaim,
            shimmer = canClaim,
            accentTint = when {
                canClaim -> palette.streakColor
                isCurrent -> palette.primary
                else -> null
            },
            contentPadding = PaddingValues(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = milestone.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.gemColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Diamond,
                                contentDescription = null,
                                tint = palette.gemColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "+${milestone.gemReward}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.gemColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { milestone.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (milestone.isUnlocked) palette.primary else palette.gemColor,
                        trackColor = palette.glassBorderSubtle
                    )
                    Text(
                        text = milestone.targetLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textMuted
                    )
                }

                if (canClaim) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MorphismActionButton(
                        text = "CLAIM +${milestone.gemReward} GEMS",
                        icon = Icons.Rounded.CardGiftcard,
                        onClick = onClaim,
                        containerColor = palette.streakColor,
                        lipColor = palette.streakLip,
                        height = 44.dp,
                        cornerRadius = 14.dp,
                        testTag = "claim_milestone_${milestone.id}"
                    )
                } else if (milestone.isClaimed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chest Claimed ✓",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.primaryLip
                    )
                }
            }
        }
    }
}
