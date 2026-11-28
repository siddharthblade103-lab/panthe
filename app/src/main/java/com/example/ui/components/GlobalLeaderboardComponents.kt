package com.example.ui.components

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GlobalLeaderboardEntry
import com.example.data.model.StudyLiveStatus
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.util.Locale

private val GoldMedalColor = Color(0xFFFFB800)
private val SilverMedalColor = Color(0xFF94A3B8)
private val BronzeMedalColor = Color(0xFFD97706)

/**
 * Compact Liquid-Crystal summary card showing the signed-in scholar's live rank,
 * current streak flame, and competitive gap to the scholar right above them.
 */
@Composable
fun MyLeaderboardRankCard(
    myEntry: GlobalLeaderboardEntry?,
    nextAboveEntry: GlobalLeaderboardEntry?,
    totalRankedScholars: Int,
    fallbackStreak: Int,
    fallbackTotalMinutes: Int,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    val currentStreak = myEntry?.profile?.currentStreak ?: fallbackStreak
    val totalMins = myEntry?.profile?.totalFocusMinutes ?: fallbackTotalMinutes
    val totalHoursLabel = remember(totalMins) { formatHoursCompact(totalMins) }

    val gapMessage = remember(myEntry, nextAboveEntry) {
        when {
            myEntry == null -> "Complete a sprint today to climb into the Global Top 50!"
            myEntry.rank == 1 -> "You hold the #1 Global Crown! Keep your streak blazing."
            nextAboveEntry != null -> {
                val streakDiff = (nextAboveEntry.profile.currentStreak - myEntry.profile.currentStreak).coerceAtLeast(0)
                if (streakDiff == 0) {
                    "Tied in streak with #${nextAboveEntry.rank} ${nextAboveEntry.profile.displayName} — focus longer to overtake!"
                } else {
                    "$streakDiff more streak ${if (streakDiff == 1) "day" else "days"} to tie #${nextAboveEntry.rank} ${nextAboveEntry.profile.displayName}"
                }
            }
            else -> "Ranked among $totalRankedScholars active global scholars"
        }
    }

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("leaderboard_my_rank_card"),
        cornerRadius = 26.dp,
        elevated = true,
        shimmer = myEntry?.rank == 1,
        accentTint = if (myEntry?.rank == 1) GoldMedalColor else palette.streakColor,
        frostedBlur = true,
        blurRadius = 20.dp,
        contentPadding = PaddingValues(16.dp)
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        (if (myEntry?.rank == 1) GoldMedalColor else palette.streakColor).copy(alpha = 0.24f),
                                        palette.prismCyan.copy(alpha = 0.18f)
                                    )
                                )
                            )
                            .border(1.2.dp, Color.White.copy(alpha = if (palette.isDark) 0.35f else 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (myEntry != null) "#${myEntry.rank}" else "—",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (myEntry?.rank == 1) GoldMedalColor else palette.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.leaderboard_your_rank),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            LiquidCrystalSyncDot(
                                color = if (isSyncing) palette.prismCyan else palette.primary,
                                isLivePulse = true,
                                isSyncing = isSyncing,
                                dotSize = 6.dp
                            )
                        }
                        Text(
                            text = "$currentStreak-day streak • $totalHoursLabel total",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        tactile?.playTap()
                        onRefresh()
                    },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("leaderboard_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.leaderboard_refresh),
                        tint = palette.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = palette.chipUnselected.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, palette.glassBorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (myEntry?.rank == 1) GoldMedalColor else palette.streakColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = gapMessage,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Iconic Liquid Crystal 3-slot champions podium (#2 left, #1 elevated center with crown glow, #3 right).
 */
@Composable
fun LeaderboardPodiumCard(
    topEntries: List<GlobalLeaderboardEntry>,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val first = topEntries.getOrNull(0) ?: return
    val second = topEntries.getOrNull(1)
    val third = topEntries.getOrNull(2)

    val crownMotion = rememberInfiniteTransition(label = "podiumCrown")
    val crownFloat by crownMotion.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crownFloatY"
    )

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("leaderboard_podium_card"),
        cornerRadius = 28.dp,
        elevated = true,
        shimmer = true,
        accentTint = GoldMedalColor,
        frostedBlur = true,
        blurRadius = 22.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = GoldMedalColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.leaderboard_podium_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GoldMedalColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, GoldMedalColor.copy(alpha = 0.36f))
                ) {
                    Text(
                        text = "TOP ${topEntries.size.coerceAtMost(3)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = GoldMedalColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // #2 Silver Slot (Left)
                if (second != null) {
                    PodiumPillarSlot(
                        entry = second,
                        medalColor = SilverMedalColor,
                        pillarHeight = 76.dp,
                        avatarSize = 48.dp,
                        crownOffsetY = 0f,
                        showCrown = false,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // #1 Gold Champion Slot (Center, Tallest)
                PodiumPillarSlot(
                    entry = first,
                    medalColor = GoldMedalColor,
                    pillarHeight = 102.dp,
                    avatarSize = 58.dp,
                    crownOffsetY = crownFloat,
                    showCrown = true,
                    modifier = Modifier.weight(1.12f)
                )

                // #3 Bronze Slot (Right)
                if (third != null) {
                    PodiumPillarSlot(
                        entry = third,
                        medalColor = BronzeMedalColor,
                        pillarHeight = 62.dp,
                        avatarSize = 46.dp,
                        crownOffsetY = 0f,
                        showCrown = false,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PodiumPillarSlot(
    entry: GlobalLeaderboardEntry,
    medalColor: Color,
    pillarHeight: Dp,
    avatarSize: Dp,
    crownOffsetY: Float,
    showCrown: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val scholar = entry.profile
    val initials = remember(scholar.displayName) { scholarInitials(scholar.displayName) }
    val isFocusing = scholar.liveStatus == StudyLiveStatus.FOCUSING

    Column(
        modifier = modifier.testTag("podium_rank_${entry.rank}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showCrown) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = "First place trophy",
                tint = GoldMedalColor,
                modifier = Modifier
                    .size(22.dp)
                    .offset(y = crownOffsetY.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
        }

        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                medalColor.copy(alpha = 0.34f),
                                palette.prismCyan.copy(alpha = 0.24f)
                            )
                        )
                    )
                    .border(2.dp, medalColor.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = palette.textPrimary
                )
            }

            if (isFocusing) {
                LiquidCrystalSyncDot(
                    color = palette.primary,
                    isLivePulse = true,
                    isSyncing = false,
                    dotSize = 7.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (entry.isCurrentUser) "${scholar.displayName} (You)" else scholar.displayName,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "@${scholar.handle}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = palette.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Frosted Glass Podium Pedestal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillarHeight)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            medalColor.copy(alpha = if (palette.isDark) 0.34f else 0.26f),
                            medalColor.copy(alpha = if (palette.isDark) 0.12f else 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    color = medalColor.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(horizontal = 6.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = medalColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = palette.streakColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${scholar.currentStreak}d",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }
                if (pillarHeight >= 76.dp) {
                    Text(
                        text = formatHoursCompact(scholar.totalFocusMinutes),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Ranked Liquid-Crystal leaderboard row with rank crest, live status indicator,
 * streak flame count, study hours, and 1-tap "Add Friend" action for non-friends.
 */
@Composable
fun LeaderboardScholarRow(
    entry: GlobalLeaderboardEntry,
    isAddingFriend: Boolean,
    onAddFriendByHandle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val scholar = entry.profile
    val initials = remember(scholar.displayName) { scholarInitials(scholar.displayName) }
    val isFocusing = scholar.liveStatus == StudyLiveStatus.FOCUSING

    val rankAccent = when (entry.rank) {
        1 -> GoldMedalColor
        2 -> SilverMedalColor
        3 -> BronzeMedalColor
        else -> if (entry.isCurrentUser) palette.primary else palette.prismCyan
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "leaderboardRowScale"
    )

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.rank}")
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                tactile?.playTap()
            },
        cornerRadius = 22.dp,
        elevated = entry.isCurrentUser || entry.rank <= 3,
        shimmer = entry.isCurrentUser,
        accentTint = if (entry.isCurrentUser) palette.primary else if (entry.rank <= 3) rankAccent else null,
        frostedBlur = true,
        blurRadius = 16.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Pill
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(rankAccent.copy(alpha = if (entry.rank <= 3 || entry.isCurrentUser) 0.20f else 0.12f))
                    .border(1.dp, rankAccent.copy(alpha = 0.38f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (entry.rank <= 3) rankAccent else palette.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Avatar + Live Focus Dot
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    rankAccent.copy(alpha = 0.25f),
                                    palette.prismViolet.copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(1.1.dp, Color.White.copy(alpha = if (palette.isDark) 0.35f else 0.90f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary
                    )
                }
                if (isFocusing) {
                    LiquidCrystalSyncDot(
                        color = palette.primary,
                        isLivePulse = true,
                        isSyncing = false,
                        dotSize = 6.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name, YOU tag, Handle & Study Meta
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = scholar.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.isCurrentUser) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primarySoft
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.primaryLip,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (entry.isFriend) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.gemColor.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = palette.gemColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Friend",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = palette.gemColor
                                )
                            }
                        }
                    }
                }
                Text(
                    text = buildString {
                        append("@${scholar.handle}")
                        append(" • ")
                        if (isFocusing) {
                            append("Focusing ${scholar.currentSubject}")
                        } else {
                            append("Lv ${scholar.level} • ${formatHoursCompact(scholar.totalFocusMinutes)}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocusing) palette.primaryLip else palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Streak Flame Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.streakColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, palette.streakColor.copy(alpha = 0.36f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = "Study streak",
                        tint = palette.streakColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${scholar.currentStreak}d",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.streakColor
                    )
                }
            }

            // 1-tap Add Friend button for competitive scholars not yet in your Study Circle
            if (!entry.isCurrentUser && !entry.isFriend && scholar.handle.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        tactile?.playTap()
                        onAddFriendByHandle(scholar.handle)
                    },
                    enabled = !isAddingFriend,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("leaderboard_add_friend_${scholar.handle}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = stringResource(R.string.leaderboard_add_friend),
                        tint = palette.primaryLip,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardLoadingCard(modifier: Modifier = Modifier) {
    val palette = LocalTryAgainPalette.current
    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("leaderboard_loading_card"),
        cornerRadius = 26.dp,
        elevated = true,
        shimmer = true,
        accentTint = palette.prismCyan,
        frostedBlur = true,
        contentPadding = PaddingValues(18.dp)
    ) {
        LiquidLeaderboardSkeleton(
            label = stringResource(R.string.leaderboard_loading)
        )
    }
}

@Composable
fun LeaderboardEmptyOrErrorCard(
    isError: Boolean,
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isError) "leaderboard_error_card" else "leaderboard_empty_card"),
        cornerRadius = 26.dp,
        accentTint = if (isError) palette.coralAccent else palette.streakColor,
        frostedBlur = true,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isError) palette.coralAccent else palette.streakColor).copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isError) Icons.Rounded.Public else Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = if (isError) palette.coralAccent else palette.streakColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = if (isError) "Could Not Sync Leaderboard" else stringResource(R.string.leaderboard_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = NunitoDisplayFamily,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary
            )
            Text(
                text = message ?: stringResource(R.string.leaderboard_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
            if (isError) {
                Spacer(modifier = Modifier.height(4.dp))
                MorphismActionButton(
                    text = stringResource(R.string.leaderboard_retry),
                    icon = Icons.Rounded.Refresh,
                    onClick = onRetry,
                    height = 46.dp,
                    cornerRadius = 15.dp,
                    testTag = "leaderboard_retry_button"
                )
            }
        }
    }
}

private fun scholarInitials(displayName: String): String =
    displayName.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "S" }

private fun formatHoursCompact(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(0)
    val hours = safe / 60f
    return if (hours >= 1f) String.format(Locale.US, "%.1fh", hours) else "${safe}m"
}
