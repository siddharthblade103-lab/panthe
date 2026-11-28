package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DailyUsagePoint
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlin.math.ceil

private enum class UsageChartMode(val label: String) {
    GROUPED("Side-by-Side"),
    STACKED("Stacked")
}

/**
 * Native Jetpack Compose frosted-glass bar chart visualization comparing daily
 * Focus time vs. Distraction screen time over the last 7 days.
 */
@Composable
fun DailyUsageBarChartCard(
    dailyPoints: List<DailyUsagePoint>,
    hasUsagePermission: Boolean,
    modifier: Modifier = Modifier
) {
    if (dailyPoints.isEmpty()) return

    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    var chartMode by remember { mutableStateOf(UsageChartMode.GROUPED) }
    var selectedIdx by remember(dailyPoints.size) {
        mutableIntStateOf(dailyPoints.lastIndex.coerceAtLeast(0))
    }
    val safeSelectedIdx = selectedIdx.coerceIn(0, dailyPoints.lastIndex)
    val selectedDay = dailyPoints[safeSelectedIdx]

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val totalFocusWeek = remember(dailyPoints) { dailyPoints.sumOf { it.focusMinutes } }
    val totalDistractionWeek = remember(dailyPoints) { dailyPoints.sumOf { it.distractionMinutes } }
    val weekTotal = totalFocusWeek + totalDistractionWeek
    val weekFocusPct = if (weekTotal > 0) ((totalFocusWeek * 100f) / weekTotal).toInt() else 0

    // Compute clean rounded Y-axis ceiling (minimum 30m so empty days remain legible)
    val rawMax = remember(dailyPoints, chartMode) {
        when (chartMode) {
            UsageChartMode.GROUPED -> dailyPoints.maxOfOrNull { maxOf(it.focusMinutes, it.distractionMinutes) } ?: 30
            UsageChartMode.STACKED -> dailyPoints.maxOfOrNull { it.totalMinutes } ?: 30
        }
    }
    val yAxisMax = remember(rawMax) {
        val base = rawMax.coerceAtLeast(30)
        val step = when {
            base <= 60 -> 15
            base <= 180 -> 30
            else -> 60
        }
        (ceil(base.toDouble() / step).toInt() * step).coerceAtLeast(30)
    }

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_usage_bar_chart_card"),
        cornerRadius = 28.dp,
        elevated = true,
        accentTint = palette.primary,
        frostedBlur = true,
        blurRadius = 20.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Header + Weekly Focus Share Pill
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.primarySoft)
                            .border(1.2.dp, palette.primary.copy(alpha = 0.36f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "Daily usage bar chart",
                            tint = palette.primaryLip,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.usage_chart_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.usage_chart_subtitle),
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
                    color = palette.primarySoft,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = if (weekTotal > 0) "$weekFocusPct% Focus" else "7 Days",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.primaryLip,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Fluid Sliding Glass Mode Toggle (Side-by-Side vs Stacked)
            SlidingGlassSegmentedBar(
                labels = UsageChartMode.entries.map { it.label },
                selectedIndex = UsageChartMode.entries.indexOf(chartMode),
                onSelect = { idx -> chartMode = UsageChartMode.entries[idx] },
                tagPrefix = "usage_chart_mode"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Legend & Weekly Totals Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(
                        label = "${stringResource(R.string.usage_legend_focus)} (${formatShortMinutes(totalFocusWeek)})",
                        color = palette.primary
                    )
                    ChartLegendDot(
                        label = "${stringResource(R.string.usage_legend_distraction)} (${formatShortMinutes(totalDistractionWeek)})",
                        color = palette.coralAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Native Compose Interactive Bar Chart Canvas + Columns
            val chartHeight = 148.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 28.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Y-Axis Scale Labels
                Column(
                    modifier = Modifier
                        .width(34.dp)
                        .height(chartHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatShortMinutes(yAxisMax),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textMuted
                    )
                    Text(
                        text = formatShortMinutes(yAxisMax / 2),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textMuted
                    )
                    Text(
                        text = "0m",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Plot Area: Dashed Gridlines behind Interactive Animated Bar Columns
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                    ) {
                        val gridColor = if (palette.isDark) {
                            Color.White.copy(alpha = 0.12f)
                        } else {
                            palette.glassBorderSubtle.copy(alpha = 0.72f)
                        }
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        listOf(0f, 0.5f, 1f).forEach { fraction ->
                            val y = size.height * fraction
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (fraction < 1f) dash else null
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dailyPoints.forEachIndexed { idx, point ->
                            val isSelected = idx == safeSelectedIdx
                            val focusFrac by animateFloatAsState(
                                targetValue = if (revealed) {
                                    (point.focusMinutes.toFloat() / yAxisMax).coerceIn(0f, 1f)
                                } else 0f,
                                animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessLow),
                                label = "focusBar_$idx"
                            )
                            val distFrac by animateFloatAsState(
                                targetValue = if (revealed) {
                                    (point.distractionMinutes.toFloat() / yAxisMax).coerceIn(0f, 1f)
                                } else 0f,
                                animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessLow),
                                label = "distBar_$idx"
                            )
                            val labelColor by animateColorAsState(
                                targetValue = when {
                                    isSelected -> palette.textPrimary
                                    point.isToday -> palette.primaryLip
                                    else -> palette.textMuted
                                },
                                animationSpec = tween(180),
                                label = "barLabel_$idx"
                            )

                            val a11y = "${if (point.isToday) "Today" else point.dayLabel}, " +
                                "${point.focusMinutes} minutes focus, " +
                                "${point.distractionMinutes} minutes distraction"

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .defaultMinSize(minWidth = 36.dp, minHeight = 48.dp)
                                    .testTag("usage_chart_day_$idx")
                                    .semantics { contentDescription = a11y }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            if (palette.isDark) Color.White.copy(alpha = 0.08f)
                                            else Color.White.copy(alpha = 0.52f)
                                        } else Color.Transparent
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        tactile?.playTap()
                                        selectedIdx = idx
                                    }
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(chartHeight - 4.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    when (chartMode) {
                                        UsageChartMode.GROUPED -> {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.Bottom,
                                                modifier = Modifier.fillMaxHeight()
                                            ) {
                                                // Focus Bar
                                                Box(
                                                    modifier = Modifier
                                                        .width(10.dp)
                                                        .fillMaxHeight(
                                                            if (point.focusMinutes > 0) focusFrac.coerceAtLeast(0.05f) else 0.025f
                                                        )
                                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                        .background(
                                                            if (point.focusMinutes > 0) {
                                                                Brush.verticalGradient(
                                                                    colors = listOf(Color(0xFF7AE81A), palette.primary)
                                                                )
                                                            } else {
                                                                Brush.verticalGradient(
                                                                    colors = listOf(
                                                                        palette.glassBorderSubtle.copy(alpha = 0.45f),
                                                                        palette.glassBorderSubtle.copy(alpha = 0.25f)
                                                                    )
                                                                )
                                                            }
                                                        )
                                                )

                                                // Distraction Bar
                                                Box(
                                                    modifier = Modifier
                                                        .width(10.dp)
                                                        .fillMaxHeight(
                                                            if (point.distractionMinutes > 0) distFrac.coerceAtLeast(0.05f) else 0.025f
                                                        )
                                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                        .background(
                                                            if (point.distractionMinutes > 0) {
                                                                Brush.verticalGradient(
                                                                    colors = listOf(palette.coralAccent, palette.streakColor)
                                                                )
                                                            } else {
                                                                Brush.verticalGradient(
                                                                    colors = listOf(
                                                                        palette.glassBorderSubtle.copy(alpha = 0.45f),
                                                                        palette.glassBorderSubtle.copy(alpha = 0.25f)
                                                                    )
                                                                )
                                                            }
                                                        )
                                                )
                                            }
                                        }

                                        UsageChartMode.STACKED -> {
                                            val totalFrac = (focusFrac + distFrac).coerceIn(0f, 1f)
                                            Column(
                                                modifier = Modifier
                                                    .width(18.dp)
                                                    .fillMaxHeight(
                                                        if (point.totalMinutes > 0) totalFrac.coerceAtLeast(0.06f) else 0.03f
                                                    )
                                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                                                verticalArrangement = Arrangement.Bottom
                                            ) {
                                                if (point.totalMinutes == 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(palette.glassBorderSubtle.copy(alpha = 0.42f))
                                                    )
                                                } else {
                                                    val focusWeight = point.focusMinutes.toFloat().coerceAtLeast(0f)
                                                    val distWeight = point.distractionMinutes.toFloat().coerceAtLeast(0f)
                                                    if (distWeight > 0f) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .weight(distWeight)
                                                                .background(
                                                                    Brush.verticalGradient(
                                                                        colors = listOf(palette.coralAccent, palette.streakColor)
                                                                    )
                                                                )
                                                        )
                                                    }
                                                    if (focusWeight > 0f) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .weight(focusWeight)
                                                                .background(
                                                                    Brush.verticalGradient(
                                                                        colors = listOf(Color(0xFF7AE81A), palette.primary)
                                                                    )
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (point.isToday) "Today" else point.dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected || point.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 10.sp
                                    ),
                                    color = labelColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Interactive Selected Day Tooltip Pill
            MetamorphosisContainer(
                targetState = selectedDay,
                label = "selectedUsageDayTooltip"
            ) { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (palette.isDark) Color.White.copy(alpha = 0.06f)
                            else Color.White.copy(alpha = 0.48f)
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.74f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (day.isToday) "Today · ${day.monthDayLabel}" else "${day.dayLabel} · ${day.monthDayLabel}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Focus: ${formatShortMinutes(day.focusMinutes)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.primaryLip
                            )
                            Text(
                                text = "Distraction: ${formatShortMinutes(day.distractionMinutes)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.coralAccent
                            )
                        }
                    }

                    val pillColor = when {
                        day.totalMinutes == 0 -> palette.textMuted
                        day.focusPercent >= 60 -> palette.primary
                        day.focusPercent >= 30 -> palette.gemColor
                        else -> palette.coralAccent
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pillColor.copy(alpha = if (palette.isDark) 0.20f else 0.13f),
                        border = BorderStroke(1.dp, pillColor.copy(alpha = 0.30f))
                    ) {
                        Text(
                            text = if (day.totalMinutes > 0) "${day.focusPercent}% Focus" else "No Data",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = pillColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!hasUsagePermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tip: Enable Usage Access above to automatically track distraction time across all apps.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = palette.textMuted
                )
            }
        }
    }
}

@Composable
private fun ChartLegendDot(
    label: String,
    color: Color
) {
    val palette = LocalTryAgainPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textSecondary
        )
    }
}

private fun formatShortMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val h = safe / 60
    val m = safe % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
