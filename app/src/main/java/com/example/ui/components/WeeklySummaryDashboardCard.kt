package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stars
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DayFocusPoint
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlin.math.ceil

private enum class WeeklyChartSeriesMode(val label: String) {
    COMBINED("Combined"),
    FOCUS_TIME("Focus Time"),
    POINTS("Points")
}

/**
 * Interactive Native Jetpack Compose Weekly Summary Dashboard visualizing the user's
 * Total Focus Time (gradient bar chart) and Play Points Earned (smooth cubic-bezier
 * area & line chart) over the past 7 days, complete with KPI summary tiles, mode
 * switcher, interactive crosshair selection, and a floating glass day-detail tooltip.
 */
@Composable
fun WeeklySummaryDashboardCard(
    weeklyPoints: List<DayFocusPoint>,
    dailyGoalMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (weeklyPoints.isEmpty()) return

    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    var mode by remember { mutableStateOf(WeeklyChartSeriesMode.COMBINED) }
    var selectedIdx by remember(weeklyPoints.size) {
        mutableIntStateOf(weeklyPoints.lastIndex.coerceAtLeast(0))
    }
    val safeIdx = selectedIdx.coerceIn(0, weeklyPoints.lastIndex)
    val selectedDay = weeklyPoints[safeIdx]

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val revealProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "weeklyChartReveal"
    )

    val totalFocusMinutes = remember(weeklyPoints) { weeklyPoints.sumOf { it.minutes } }
    val totalPointsEarned = remember(weeklyPoints) { weeklyPoints.sumOf { it.pointsEarned } }
    val totalSessions = remember(weeklyPoints) { weeklyPoints.sumOf { it.sessionsCount } }
    val activeDaysCount = remember(weeklyPoints) { weeklyPoints.count { it.minutes > 0 || it.pointsEarned > 0 } }
    val avgDailyMinutes = remember(totalFocusMinutes) { totalFocusMinutes / 7 }
    val peakDay = remember(weeklyPoints) {
        weeklyPoints.maxByOrNull { it.minutes * 10 + it.pointsEarned }
    }

    val focusAxisMax = remember(weeklyPoints, dailyGoalMinutes) {
        val raw = maxOf(
            weeklyPoints.maxOfOrNull { it.minutes } ?: 0,
            dailyGoalMinutes.coerceAtMost(90)
        ).coerceAtLeast(30)
        val step = when {
            raw <= 60 -> 15
            raw <= 180 -> 30
            else -> 60
        }
        (ceil(raw.toDouble() / step).toInt() * step).coerceAtLeast(30)
    }

    val pointsAxisMax = remember(weeklyPoints) {
        val raw = (weeklyPoints.maxOfOrNull { it.pointsEarned } ?: 0).coerceAtLeast(90)
        val step = when {
            raw <= 180 -> 30
            raw <= 600 -> 100
            else -> 200
        }
        (ceil(raw.toDouble() / step).toInt() * step).coerceAtLeast(90)
    }

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_summary_dashboard_card"),
        cornerRadius = 28.dp,
        elevated = true,
        shimmer = activeDaysCount >= 3,
        accentTint = palette.prismCyan,
        frostedBlur = true,
        blurRadius = 20.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Header Row
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.primary, palette.prismCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = "Weekly summary dashboard",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.weekly_summary_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.weekly_summary_subtitle),
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
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.32f))
                ) {
                    Text(
                        text = "$activeDaysCount/7 Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.primaryLip,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Hero 7-Day KPI Bento Tiles (Focus Time, Points Earned, Sprints)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeeklyKpiTile(
                    icon = Icons.Rounded.Schedule,
                    label = stringResource(R.string.weekly_summary_total_focus),
                    value = formatCompactDuration(totalFocusMinutes),
                    subtext = "${avgDailyMinutes}m/day avg",
                    accent = palette.primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weekly_kpi_focus_time")
                )
                WeeklyKpiTile(
                    icon = Icons.Rounded.Stars,
                    label = stringResource(R.string.weekly_summary_points_earned),
                    value = "+$totalPointsEarned",
                    subtext = if (peakDay != null && peakDay.pointsEarned > 0) "Best ${peakDay.dayLabel}" else "Play Points",
                    accent = palette.streakColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weekly_kpi_points_earned")
                )
                WeeklyKpiTile(
                    icon = Icons.Rounded.Bolt,
                    label = stringResource(R.string.weekly_summary_sprints),
                    value = "$totalSessions",
                    subtext = "past 7 days",
                    accent = palette.prismViolet,
                    modifier = Modifier
                        .weight(0.86f)
                        .testTag("weekly_kpi_sessions")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Sliding Glass Series Mode Switcher (Combined / Focus Time / Points)
            SlidingGlassSegmentedBar(
                labels = WeeklyChartSeriesMode.entries.map { it.label },
                selectedIndex = WeeklyChartSeriesMode.entries.indexOf(mode),
                onSelect = { idx -> mode = WeeklyChartSeriesMode.entries[idx] },
                tagPrefix = "weekly_summary_mode"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Compact Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mode != WeeklyChartSeriesMode.POINTS) {
                    SummaryLegendItem(
                        label = stringResource(R.string.weekly_summary_legend_focus),
                        color = palette.primary,
                        isLine = false
                    )
                }
                if (mode != WeeklyChartSeriesMode.FOCUS_TIME) {
                    SummaryLegendItem(
                        label = stringResource(R.string.weekly_summary_legend_points),
                        color = palette.streakColor,
                        isLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Native Interactive Composed Chart (Bars + Smooth Spline Area Curve + Dual Y-Axes)
            val chartHeight = 162.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Left Y-Axis (Focus Minutes or Points when in Points-only mode)
                Column(
                    modifier = Modifier
                        .width(34.dp)
                        .height(chartHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    val leftTop = if (mode == WeeklyChartSeriesMode.POINTS) "$pointsAxisMax" else formatCompactDuration(focusAxisMax)
                    val leftMid = if (mode == WeeklyChartSeriesMode.POINTS) "${pointsAxisMax / 2}" else formatCompactDuration(focusAxisMax / 2)
                    Text(
                        text = leftTop,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textMuted
                    )
                    Text(
                        text = leftMid,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textMuted
                    )
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textMuted
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Chart Plot Area + Interactive Touch Columns
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                    ) {
                        val gridColor = palette.glassBorderSubtle.copy(alpha = 0.55f)
                        val barPrimary = palette.primary
                        val barSecondary = palette.prismCyan
                        val linePrimary = palette.streakColor
                        val lineViolet = palette.prismViolet
                        val crosshairColor = palette.textPrimary.copy(alpha = 0.24f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val topPad = 10.dp.toPx()
                            val bottomPad = 4.dp.toPx()
                            val plotHeight = (h - topPad - bottomPad).coerceAtLeast(1f)
                            val count = weeklyPoints.size.coerceAtLeast(1)
                            val slotWidth = w / count

                            // Dashed Cartesian Gridlines (0%, 50%, 100%)
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            listOf(0f, 0.5f, 1f).forEach { frac ->
                                val y = topPad + plotHeight * frac
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (frac < 1f) dashEffect else null
                                )
                            }

                            // Selected-day subtle vertical highlight pill & crosshair
                            val selCenterX = slotWidth * safeIdx + slotWidth / 2f
                            drawRoundRect(
                                color = palette.primarySoft.copy(alpha = if (palette.isDark) 0.22f else 0.36f),
                                topLeft = Offset(selCenterX - slotWidth * 0.40f, 2.dp.toPx()),
                                size = Size(slotWidth * 0.80f, h - 2.dp.toPx()),
                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                            )
                            drawLine(
                                color = crosshairColor,
                                start = Offset(selCenterX, topPad),
                                end = Offset(selCenterX, topPad + plotHeight),
                                strokeWidth = 1.1.dp.toPx(),
                                pathEffect = dashEffect
                            )

                            // Layer A: Focus Time Gradient Bars
                            if (mode != WeeklyChartSeriesMode.POINTS) {
                                val barWidth = (slotWidth * 0.44f).coerceIn(10.dp.toPx(), 24.dp.toPx())
                                val radiusPx = 8.dp.toPx()
                                weeklyPoints.forEachIndexed { idx, pt ->
                                    val centerX = slotWidth * idx + slotWidth / 2f
                                    val barLeft = centerX - barWidth / 2f
                                    val frac = (pt.minutes.toFloat() / focusAxisMax.toFloat()).coerceIn(0f, 1f) * revealProgress
                                    val barHeight = if (pt.minutes > 0) {
                                        (plotHeight * frac).coerceAtLeast(6.dp.toPx())
                                    } else {
                                        3.dp.toPx()
                                    }
                                    val barTop = topPad + plotHeight - barHeight
                                    val isSelected = idx == safeIdx
                                    val alphaBoost = when {
                                        pt.minutes == 0 -> 0.22f
                                        isSelected -> 1f
                                        else -> 0.72f
                                    }

                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                barPrimary.copy(alpha = alphaBoost),
                                                barSecondary.copy(alpha = alphaBoost * 0.78f)
                                            ),
                                            startY = barTop,
                                            endY = topPad + plotHeight
                                        ),
                                        topLeft = Offset(barLeft, barTop),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                                    )

                                    if (isSelected && pt.minutes > 0) {
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.72f),
                                            topLeft = Offset(barLeft, barTop),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                                            style = Stroke(width = 1.2.dp.toPx())
                                        )
                                    }
                                }
                            }

                            // Layer B: Smooth Cubic-Bezier Points Earned Area & Spline Curve
                            if (mode != WeeklyChartSeriesMode.FOCUS_TIME && weeklyPoints.size >= 2) {
                                val curvePoints = weeklyPoints.mapIndexed { idx, pt ->
                                    val x = slotWidth * idx + slotWidth / 2f
                                    val frac = (pt.pointsEarned.toFloat() / pointsAxisMax.toFloat()).coerceIn(0f, 1f) * revealProgress
                                    val y = topPad + plotHeight - (plotHeight * frac)
                                    Offset(x, y)
                                }

                                val linePath = Path().apply {
                                    moveTo(curvePoints.first().x, curvePoints.first().y)
                                    for (i in 0 until curvePoints.lastIndex) {
                                        val p0 = curvePoints[i]
                                        val p1 = curvePoints[i + 1]
                                        val midX = (p0.x + p1.x) / 2f
                                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                    }
                                }

                                val areaPath = Path().apply {
                                    addPath(linePath)
                                    lineTo(curvePoints.last().x, topPad + plotHeight)
                                    lineTo(curvePoints.first().x, topPad + plotHeight)
                                    close()
                                }

                                drawPath(
                                    path = areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            linePrimary.copy(alpha = if (mode == WeeklyChartSeriesMode.POINTS) 0.34f else 0.18f),
                                            lineViolet.copy(alpha = 0.04f),
                                            Color.Transparent
                                        ),
                                        startY = topPad,
                                        endY = topPad + plotHeight
                                    )
                                )

                                drawPath(
                                    path = linePath,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(linePrimary, lineViolet, linePrimary)
                                    ),
                                    style = Stroke(
                                        width = 2.8.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                // Luminous data point nodes
                                curvePoints.forEachIndexed { idx, pos ->
                                    val isSelected = idx == safeIdx
                                    val outerR = if (isSelected) 7.5.dp.toPx() else 4.2.dp.toPx()
                                    val innerR = if (isSelected) 4.2.dp.toPx() else 2.5.dp.toPx()
                                    if (isSelected) {
                                        drawCircle(
                                            color = linePrimary.copy(alpha = 0.28f),
                                            radius = outerR * 1.45f,
                                            center = pos
                                        )
                                    }
                                    drawCircle(
                                        color = Color.White,
                                        radius = outerR,
                                        center = pos
                                    )
                                    drawCircle(
                                        color = if (isSelected) linePrimary else lineViolet,
                                        radius = innerR,
                                        center = pos
                                    )
                                }
                            }
                        }

                        // Invisible accessible 48dp-friendly touch columns overlaid on the plot
                        Row(modifier = Modifier.fillMaxSize()) {
                            weeklyPoints.forEachIndexed { idx, pt ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .testTag("weekly_summary_day_$idx")
                                        .semantics {
                                            contentDescription = "${pt.dayLabel} ${pt.monthDayLabel}: ${pt.minutes} focus minutes and ${pt.pointsEarned} points"
                                        }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (selectedIdx != idx) {
                                                tactile?.playTap()
                                                selectedIdx = idx
                                            }
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // X-Axis Day Labels (clickable with crisp selected pill highlight)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weeklyPoints.forEachIndexed { idx, pt ->
                            val isSelected = idx == safeIdx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 28.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) palette.primarySoft else Color.Transparent)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (selectedIdx != idx) {
                                            tactile?.playTap()
                                            selectedIdx = idx
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (pt.isToday) "Today" else pt.dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected || pt.isToday) FontWeight.ExtraBold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) palette.primaryLip else palette.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Right Y-Axis (Points Scale when in Combined mode)
                if (mode == WeeklyChartSeriesMode.COMBINED) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(
                        modifier = Modifier
                            .width(32.dp)
                            .height(chartHeight),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "${pointsAxisMax}p",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = palette.streakColor
                        )
                        Text(
                            text = "${pointsAxisMax / 2}p",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = palette.streakColor.copy(alpha = 0.78f)
                        )
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = palette.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Floating Liquid-Glass Selected Day Tooltip Callout
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_summary_tooltip"),
                shape = RoundedCornerShape(18.dp),
                color = palette.glassFillElevated,
                border = BorderStroke(1.dp, palette.glassBorderSubtle)
            ) {
                AnimatedContent(
                    targetState = selectedDay,
                    transitionSpec = {
                        fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(130, easing = FastOutSlowInEasing))
                    },
                    label = "weeklyTooltip"
                ) { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = buildString {
                                        append(day.dayLabel)
                                        if (day.monthDayLabel.isNotBlank()) {
                                            append(" · ")
                                            append(day.monthDayLabel)
                                        }
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.textPrimary
                                )
                                if (day.isToday) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = palette.primarySoft
                                    ) {
                                        Text(
                                            text = "TODAY",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = palette.primaryLip,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (day.sessionsCount > 0) {
                                    "${day.sessionsCount} focus ${if (day.sessionsCount == 1) "sprint" else "sprints"} completed"
                                } else {
                                    "Tap any day above to inspect your 7-day trend"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TooltipMetricPill(
                                value = formatCompactDuration(day.minutes),
                                color = palette.primary
                            )
                            TooltipMetricPill(
                                value = "+${day.pointsEarned} pts",
                                color = palette.streakColor
                            )
                            if (day.gemsEarned > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = palette.gemColor.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
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
                                            text = "${day.gemsEarned}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp
                                            ),
                                            color = palette.gemColor
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

@Composable
private fun WeeklyKpiTile(
    icon: ImageVector,
    label: String,
    value: String,
    subtext: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = palette.glassFillElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = NunitoDisplayFamily,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary,
                maxLines = 1
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryLegendItem(
    label: String,
    color: Color,
    isLine: Boolean
) {
    val palette = LocalTryAgainPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLine) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = palette.textSecondary
        )
    }
}

@Composable
private fun TooltipMetricPill(
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.32f))
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatCompactDuration(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(0)
    val h = safe / 60
    val m = safe % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
