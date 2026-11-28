package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AppSettingsAlt
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppUsageEntry
import com.example.data.model.ScreenUsageUiState
import com.example.service.ScreenUsageTracker
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.util.Locale

/**
 * Frosted glassmorphism Screen Time & App Usage tracker card.
 * Displays live Try Again in-app time, total device screen time, Focus-to-Screen balance,
 * and a real per-app breakdown powered by Android UsageStatsManager.
 */
@Composable
fun ScreenUsageCard(
    usageState: ScreenUsageUiState,
    todayFocusMinutes: Int,
    onRefreshUsage: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val focusMillis = todayFocusMinutes.coerceAtLeast(0) * 60_000L
    val denominatorMillis = if (usageState.hasUsagePermission && usageState.totalScreenTimeMillis > 0L) {
        maxOf(usageState.totalScreenTimeMillis, focusMillis)
    } else {
        maxOf(usageState.thisAppUsageMillis, focusMillis, 60_000L)
    }
    val focusShare = (focusMillis.toFloat() / denominatorMillis.toFloat()).coerceIn(0f, 1f)
    val animatedFocusShare by animateFloatAsState(
        targetValue = if (revealed) focusShare else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "focusBalanceRatio"
    )

    val balanceLabel = when {
        focusShare >= 0.65f -> "Deep Flow"
        focusShare >= 0.30f -> "Balanced"
        todayFocusMinutes > 0 -> "Warming Up"
        else -> "Start a Sprint"
    }
    val balanceColor = when {
        focusShare >= 0.65f -> palette.primary
        focusShare >= 0.30f -> palette.gemColor
        else -> palette.streakColor
    }

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("screen_usage_card"),
        cornerRadius = 28.dp,
        elevated = true,
        accentTint = palette.gemColor,
        frostedBlur = true,
        blurRadius = 20.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Header
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
                            .background(palette.gemColor.copy(alpha = if (palette.isDark) 0.22f else 0.14f))
                            .border(
                                width = 1.2.dp,
                                color = palette.gemColor.copy(alpha = 0.36f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = "Screen and app usage",
                            tint = palette.gemColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.usage_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (usageState.hasUsagePermission) {
                                stringResource(R.string.usage_subtitle_active)
                            } else {
                                stringResource(R.string.usage_subtitle_local)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = {
                        tactile?.playTap()
                        onRefreshUsage()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_usage_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.usage_refresh),
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Dual Semi-Transparent Frosted Metric Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UsageMetricGlassTile(
                    icon = Icons.Rounded.PhoneAndroid,
                    label = stringResource(R.string.usage_total_screen),
                    value = if (usageState.hasUsagePermission) {
                        ScreenUsageTracker.formatDuration(usageState.totalScreenTimeMillis)
                    } else {
                        "Locked"
                    },
                    caption = if (usageState.hasUsagePermission) {
                        "All device apps today"
                    } else {
                        "Enable access below"
                    },
                    accent = palette.gemColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("total_screen_time_tile")
                )

                UsageMetricGlassTile(
                    icon = Icons.Rounded.HourglassTop,
                    label = stringResource(R.string.usage_this_app),
                    value = ScreenUsageTracker.formatDuration(usageState.thisAppUsageMillis),
                    caption = "Focused: ${todayFocusMinutes}m",
                    accent = palette.primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("this_app_usage_tile")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Mindful Focus-to-Screen Balance Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (palette.isDark) Color.White.copy(alpha = 0.06f)
                        else Color.White.copy(alpha = 0.44f)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.72f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 13.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (usageState.hasUsagePermission) {
                            "Focus vs. Screen Time"
                        } else {
                            "Focus vs. In-App Time"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = balanceColor.copy(alpha = if (palette.isDark) 0.20f else 0.13f),
                        border = BorderStroke(1.dp, balanceColor.copy(alpha = 0.30f))
                    ) {
                        Text(
                            text = "$balanceLabel · ${(focusShare * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = balanceColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (palette.isDark) Color.White.copy(alpha = 0.10f)
                            else palette.glassBorderSubtle.copy(alpha = 0.70f)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFocusShare.coerceAtLeast(0.03f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(palette.primary, palette.gemColor)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Per-App Breakdown OR 1-Tap Usage Access Grant CTA
            if (usageState.hasUsagePermission) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.usage_top_apps_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Live",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.primaryLip
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (usageState.topApps.isEmpty()) {
                    Text(
                        text = "App activity will appear here as you switch between apps today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        usageState.topApps.forEachIndexed { idx, app ->
                            AppUsageBreakdownRow(
                                entry = app,
                                index = idx,
                                revealed = revealed
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.gemColor.copy(alpha = if (palette.isDark) 0.12f else 0.08f))
                        .border(
                            width = 1.dp,
                            color = palette.gemColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.usage_permission_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    MorphismActionButton(
                        text = stringResource(R.string.usage_enable_cta),
                        icon = Icons.Rounded.AppSettingsAlt,
                        onClick = onRequestUsageAccess,
                        containerColor = palette.gemColor,
                        lipColor = palette.gemLip,
                        height = 46.dp,
                        cornerRadius = 15.dp,
                        testTag = "enable_usage_access_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageMetricGlassTile(
    icon: ImageVector,
    label: String,
    value: String,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (palette.isDark) Color.White.copy(alpha = 0.07f)
                else Color.White.copy(alpha = 0.52f)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.78f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = NunitoDisplayFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            ),
            color = palette.textPrimary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = palette.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AppUsageBreakdownRow(
    entry: AppUsageEntry,
    index: Int,
    revealed: Boolean
) {
    val palette = LocalTryAgainPalette.current
    val animatedBar by animateFloatAsState(
        targetValue = if (revealed) entry.shareFraction else 0.06f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessLow),
        label = "appUsageBar_${entry.packageName}"
    )

    val rowAccent = when {
        entry.isThisApp -> palette.primary
        index == 0 -> palette.streakColor
        index == 1 -> palette.gemColor
        else -> palette.secondary
    }

    val initial = entry.appName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("usage_app_row_$index"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(rowAccent.copy(alpha = if (palette.isDark) 0.22f else 0.14f))
                .border(1.dp, rowAccent.copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = rowAccent
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.appName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.isThisApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primarySoft
                        ) {
                            Text(
                                text = "FOCUS APP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = palette.primaryLip,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ScreenUsageTracker.formatDuration(entry.usageMillis),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (palette.isDark) Color.White.copy(alpha = 0.08f)
                        else palette.chipUnselected
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedBar)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(rowAccent)
                )
            }
        }
    }
}
