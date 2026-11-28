package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GeminiInsightsUiState
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

@Composable
fun GeminiInsightsCard(
    insightsState: GeminiInsightsUiState,
    sessionsCount: Int,
    onGenerateInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_insights_card"),
        cornerRadius = 28.dp,
        elevated = insightsState is GeminiInsightsUiState.Success,
        shimmer = insightsState is GeminiInsightsUiState.Loading || insightsState is GeminiInsightsUiState.Success,
        frostedBlur = true,
        blurRadius = 20.dp,
        accentTint = palette.prismViolet,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(palette.prismViolet, palette.prismCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.gemini_insights_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.gemini_insights_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                if (insightsState is GeminiInsightsUiState.Success) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.prismViolet.copy(alpha = if (palette.isDark) 0.24f else 0.14f),
                        border = BorderStroke(1.dp, palette.prismViolet.copy(alpha = 0.36f))
                    ) {
                        Text(
                            text = insightsState.report.focusScoreLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = if (palette.isDark) Color.White else palette.prismViolet,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedContent(
                targetState = insightsState,
                transitionSpec = {
                    fadeIn(tween(240)) togetherWith fadeOut(tween(180))
                },
                label = "geminiInsightsBody"
            ) { state ->
                when (state) {
                    is GeminiInsightsUiState.Idle -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.glassFillElevated,
                                border = BorderStroke(1.dp, palette.glassBorderSubtle)
                            ) {
                                Text(
                                    text = if (sessionsCount > 0) {
                                        "Ready to review your $sessionsCount logged focus sessions, subject balance, and streak momentum for tailored productivity tips."
                                    } else {
                                        "Tap below anytime to get a personalized study kickoff plan and encouraging productivity insights based on your goals and tasks."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }

                            MorphismActionButton(
                                text = stringResource(R.string.gemini_insights_cta),
                                icon = Icons.Rounded.AutoAwesome,
                                onClick = onGenerateInsights,
                                containerColor = palette.prismViolet,
                                lipColor = Color(0xFF6D28D9),
                                height = 48.dp,
                                cornerRadius = 16.dp,
                                testTag = "generate_gemini_insights_button"
                            )
                        }
                    }

                    is GeminiInsightsUiState.Loading -> {
                        LiquidInsightsSkeleton(
                            label = stringResource(R.string.gemini_insights_loading)
                        )
                    }

                    is GeminiInsightsUiState.Error -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = palette.coralAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.36f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = palette.coralAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.coralAccent
                                    )
                                }
                            }
                            MorphismActionButton(
                                text = "TRY AGAIN",
                                icon = Icons.Rounded.Refresh,
                                onClick = onGenerateInsights,
                                containerColor = palette.prismViolet,
                                lipColor = Color(0xFF6D28D9),
                                height = 46.dp,
                                cornerRadius = 15.dp,
                                testTag = "retry_gemini_insights_button"
                            )
                        }
                    }

                    is GeminiInsightsUiState.Success -> {
                        val report = state.report
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Encouraging Headline + Message Glass Panel
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = palette.glassFillElevated,
                                border = BorderStroke(1.dp, palette.prismCyan.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = report.headline,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = NunitoDisplayFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = palette.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = report.encouragingMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            // Personalized Productivity Insights Bullets
                            if (report.productivityInsights.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    report.productivityInsights.forEach { tip ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.primarySoft),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lightbulb,
                                                    contentDescription = null,
                                                    tint = palette.primaryLip,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = tip,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Next Sprint Recommendation Pill
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = palette.primarySoft,
                                border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.32f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = palette.primaryLip,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "NEXT SPRINT TIP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = palette.primaryLip
                                        )
                                        Text(
                                            text = report.nextSessionTip,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }

                            MorphismActionButton(
                                text = stringResource(R.string.gemini_insights_refresh),
                                icon = Icons.Rounded.Refresh,
                                onClick = onGenerateInsights,
                                containerColor = palette.prismViolet,
                                lipColor = Color(0xFF6D28D9),
                                height = 44.dp,
                                cornerRadius = 15.dp,
                                testTag = "refresh_gemini_insights_button"
                            )
                        }
                    }
                }
            }
        }
    }
}
