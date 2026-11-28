package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DayFocusPoint
import com.example.data.model.FocusSession
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

/**
 * Compact, single-action Liquid Glass Export & PDF card:
 * Combines weekly summary stats + one unified Export & Share PDF button + subtle weekend auto-report toggle pill.
 */
@Composable
fun WeeklyPdfReportCard(
    weeklyPoints: List<DayFocusPoint>,
    sessions: List<FocusSession>,
    weekendAutoReportEnabled: Boolean,
    isExportingPdf: Boolean,
    onExportAndSendPdf: () -> Unit,
    onToggleWeekendAutoReport: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val weeklyTotalMins = weeklyPoints.sumOf { it.minutes }
    val activeDays = weeklyPoints.count { it.minutes > 0 }
    val topSubject = sessions
        .groupBy { it.subject }
        .maxByOrNull { (_, list) -> list.sumOf { it.completedMinutes } }
        ?.key ?: "Ready"

    LiquidGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_pdf_report_card"),
        cornerRadius = 26.dp,
        elevated = true,
        accentTint = palette.gemColor,
        animatedCaustics = false,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.gemColor, palette.prismViolet)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.report_pdf_unified_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${weeklyTotalMins}m • $activeDays/7 days • $topSubject",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Subtle compact Weekend Auto-Report Pill Toggle
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("toggle_weekend_pdf_path")
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            tactile?.playToggle(!weekendAutoReportEnabled)
                            onToggleWeekendAutoReport(!weekendAutoReportEnabled)
                        },
                    shape = RoundedCornerShape(50),
                    color = if (weekendAutoReportEnabled) {
                        palette.primarySoft
                    } else {
                        palette.glassFillElevated
                    },
                    border = BorderStroke(
                        1.dp,
                        if (weekendAutoReportEnabled) palette.primary.copy(alpha = 0.45f) else palette.glassBorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = stringResource(R.string.report_pdf_weekend_toggle),
                            tint = if (weekendAutoReportEnabled) palette.primaryLip else palette.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (weekendAutoReportEnabled) "Sun Auto" else "Auto Off",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (weekendAutoReportEnabled) palette.primaryLip else palette.textMuted
                        )
                    }
                }
            }

            // Single unified Export & PDF action button
            MorphismActionButton(
                text = if (isExportingPdf) "GENERATING PDF…" else stringResource(R.string.report_pdf_export_cta),
                icon = Icons.Rounded.PictureAsPdf,
                onClick = onExportAndSendPdf,
                enabled = !isExportingPdf,
                containerColor = palette.gemColor,
                lipColor = palette.gemLip,
                height = 50.dp,
                cornerRadius = 16.dp,
                testTag = "export_weekly_pdf_button"
            )
        }
    }
}

/**
 * Recent Study History card with persistent Hide / Show option so users can keep the screen uncluttered.
 */
@Composable
fun StudyHistoryCard(
    sessions: List<FocusSession>,
    isHidden: Boolean = false,
    onToggleHidden: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    LiquidGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_history_card"),
        cornerRadius = 26.dp,
        elevated = !isHidden,
        accentTint = palette.primary,
        animatedCaustics = false,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = if (isHidden) palette.textMuted else palette.primary,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.study_history_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (isHidden) {
                                stringResource(R.string.study_history_hidden_hint, sessions.size)
                            } else {
                                "${sessions.size} logged"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Hide / Show Persistent Pill Button (>= 48dp accessible touch target)
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                        .testTag("toggle_study_history_visibility")
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            tactile?.playToggle(isHidden)
                            onToggleHidden(!isHidden)
                        },
                    shape = RoundedCornerShape(50),
                    color = if (isHidden) palette.primarySoft else palette.glassFillElevated,
                    border = BorderStroke(
                        1.dp,
                        if (isHidden) palette.primary.copy(alpha = 0.45f) else palette.glassBorderHighlight
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (isHidden) {
                                stringResource(R.string.study_history_show_cta)
                            } else {
                                stringResource(R.string.study_history_hide_cta)
                            },
                            tint = if (isHidden) palette.primaryLip else palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isHidden) {
                                stringResource(R.string.study_history_show_cta)
                            } else {
                                stringResource(R.string.study_history_hide_cta)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isHidden) palette.primaryLip else palette.textSecondary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isHidden,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (sessions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.study_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sessions.take(8).forEach { session ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.glassFillElevated,
                                    border = BorderStroke(1.dp, palette.glassBorderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(11.dp))
                                                    .background(palette.primarySoft),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Schedule,
                                                    contentDescription = null,
                                                    tint = palette.primaryLip,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${session.subject} • ${session.completedMinutes} min",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = palette.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${session.dateKey.ifBlank { "Today" }} • ${session.ambientSound}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = palette.gemColor.copy(alpha = 0.14f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Diamond,
                                                        contentDescription = null,
                                                        tint = palette.gemColor,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "+${session.gemsEarned}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 10.sp
                                                        ),
                                                        color = palette.gemColor
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = palette.streakColor.copy(alpha = 0.14f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Stars,
                                                        contentDescription = null,
                                                        tint = palette.streakColor,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "+${session.xpEarned}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 10.sp
                                                        ),
                                                        color = palette.streakColor
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
            }
        }
    }
}
