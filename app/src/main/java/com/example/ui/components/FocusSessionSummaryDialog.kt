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
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.FocusSessionSummaryDialogState
import com.example.data.model.SessionCoachState
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.util.Locale

@Composable
fun FocusSessionSummaryDialog(
    state: FocusSessionSummaryDialogState,
    onRetryCoach: () -> Unit,
    onStartBreak: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    var revealed by remember(state.sessionId) { mutableStateOf(false) }
    LaunchedEffect(state.sessionId) { revealed = true }

    val entranceScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "summaryDialogScale"
    )

    val goalFraction = (state.todayTotalMinutes.toFloat() / state.dailyGoalMinutes.coerceAtLeast(15))
        .coerceIn(0f, 1f)
    val animatedGoal by animateFloatAsState(
        targetValue = if (revealed) goalFraction else 0f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessLow),
        label = "summaryGoalBar"
    )

    val infinite = rememberInfiniteTransition(label = "summaryHalo")
    val haloAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "summaryHaloSpin"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 420.dp)
                .graphicsLayer {
                    scaleX = entranceScale
                    scaleY = entranceScale
                }
                .testTag("session_summary_dialog"),
            contentAlignment = Alignment.TopCenter
        ) {
            MorphismSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 34.dp),
                cornerRadius = 32.dp,
                elevated = true,
                shimmer = true,
                frostedBlur = true,
                blurRadius = 24.dp,
                accentTint = palette.primary,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 46.dp,
                    bottom = 20.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top status pill + Subject tag
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primarySoft,
                            border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = stringResource(R.string.session_summary_badge),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.9.sp,
                                    fontSize = 10.sp
                                ),
                                color = palette.primaryLip,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.glassFillElevated,
                            border = BorderStroke(1.dp, palette.glassBorderSubtle)
                        ) {
                            Text(
                                text = state.subject,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.session_summary_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Two Hero Metric Tiles: Time Spent & Points Earned
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMetricTile(
                            icon = Icons.Rounded.Schedule,
                            label = stringResource(R.string.session_summary_time_label),
                            value = "${state.completedMinutes} min",
                            footnote = "of ${state.plannedMinutes}m sprint",
                            accent = palette.primary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("summary_time_spent")
                        )
                        SummaryMetricTile(
                            icon = Icons.Rounded.Stars,
                            label = stringResource(R.string.session_summary_points_label),
                            value = "+${state.pointsEarned} pts",
                            footnote = "+${state.gemsEarned} Gems earned",
                            accent = palette.streakColor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("summary_points_earned")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Reward Strip: Gems, Streak & Today's Daily Goal Progress
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = palette.glassFillElevated,
                        border = BorderStroke(1.dp, palette.glassBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = palette.streakColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${state.streakDays}-day streak",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Diamond,
                                        contentDescription = null,
                                        tint = palette.gemColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Today ${state.todayTotalMinutes}/${state.dailyGoalMinutes}m",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                            LinearProgressIndicator(
                                progress = { animatedGoal },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = palette.primary,
                                trackColor = palette.glassBorderSubtle
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Gemini AI Coach Quick Summary Panel
                    GeminiCoachQuickSummaryPanel(
                        coachState = state.coachState,
                        onRetry = {
                            tactile?.playTap()
                            onRetryCoach()
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary & Secondary Actions
                    MorphismActionButton(
                        text = "START ${state.nextPhaseName.uppercase(Locale.US)}",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onStartBreak,
                        containerColor = palette.gemColor,
                        lipColor = palette.gemLip,
                        height = 50.dp,
                        cornerRadius = 17.dp,
                        testTag = "summary_start_break_button"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                tactile?.playTap()
                                onDismiss()
                            }
                            .testTag("summary_dialog_dismiss_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = palette.chipUnselected,
                        border = BorderStroke(1.dp, palette.glassBorderSubtle)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.session_summary_done),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            }

            // Close Icon Button (top-right inside card corner)
            IconButton(
                onClick = {
                    tactile?.playTap()
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 42.dp, end = 8.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("summary_dialog_close_icon")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close session summary",
                    tint = palette.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Overlapping Celebratory Monkey Medallion with Rotating Prism Halo
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer { rotationZ = haloAngle }
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    palette.primary,
                                    palette.prismCyan,
                                    palette.prismViolet,
                                    palette.streakColor,
                                    palette.primary
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(palette.glassFillElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_tryagain_launcher_fg_1795336082633),
                        contentDescription = "Celebrating Try Again mascot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    footnote: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = palette.glassFillElevated,
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent.copy(alpha = if (palette.isDark) 0.24f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = NunitoDisplayFamily,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = footnote,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = accent
            )
        }
    }
}

@Composable
private fun GeminiCoachQuickSummaryPanel(
    coachState: SessionCoachState,
    onRetry: () -> Unit
) {
    val palette = LocalTryAgainPalette.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("summary_ai_coach_panel"),
        shape = RoundedCornerShape(22.dp),
        color = palette.prismViolet.copy(alpha = if (palette.isDark) 0.16f else 0.08f),
        border = BorderStroke(1.2.dp, palette.prismViolet.copy(alpha = 0.34f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.prismViolet, palette.prismCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.session_summary_coach_title),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }

                if (coachState is SessionCoachState.Success) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.prismViolet.copy(alpha = if (palette.isDark) 0.28f else 0.16f)
                    ) {
                        Text(
                            text = coachState.summary.celebrationTag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = if (palette.isDark) Color.White else palette.prismViolet,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = coachState,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
                label = "summaryCoachContent"
            ) { state ->
                when (state) {
                    is SessionCoachState.Loading -> {
                        LiquidCoachSkeleton(
                            label = stringResource(R.string.session_summary_coach_loading)
                        )
                    }

                    is SessionCoachState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(
                                text = state.summary.quickSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = palette.glassFillElevated,
                                border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.28f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = palette.primaryLip,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.session_summary_next_move),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.7.sp
                                            ),
                                            color = palette.primaryLip
                                        )
                                        Text(
                                            text = state.summary.nextMoveTip,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is SessionCoachState.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = palette.coralAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.coralAccent
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onRetry)
                                    .testTag("summary_coach_retry_button"),
                                shape = RoundedCornerShape(12.dp),
                                color = palette.prismViolet.copy(alpha = 0.16f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Retry AI coach summary",
                                        tint = palette.prismViolet,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.session_summary_retry),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = palette.prismViolet
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
