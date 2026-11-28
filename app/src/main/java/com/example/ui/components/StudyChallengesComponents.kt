package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ChallengeParticipant
import com.example.data.model.FriendLiveCardState
import com.example.data.model.StudyChallenge
import com.example.data.model.StudyChallengeWithProgress
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dual Liquid Crystal CTA strip for Creating a new Study Challenge or Joining one via 6-char code.
 */
@Composable
fun StudyChallengesActionStrip(
    isBusy: Boolean,
    onOpenCreateDialog: () -> Unit,
    onOpenJoinCodeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MorphismActionButton(
            text = stringResource(R.string.challenges_create_cta),
            icon = Icons.Rounded.Add,
            onClick = onOpenCreateDialog,
            enabled = !isBusy,
            height = 50.dp,
            cornerRadius = 18.dp,
            modifier = Modifier.weight(1.15f),
            testTag = "open_create_challenge_button"
        )
        MorphismActionButton(
            text = stringResource(R.string.challenges_join_code_cta),
            icon = Icons.Rounded.Key,
            onClick = onOpenJoinCodeDialog,
            enabled = !isBusy,
            containerColor = palette.gemColor,
            lipColor = palette.gemLip,
            height = 50.dp,
            cornerRadius = 18.dp,
            modifier = Modifier.weight(0.95f),
            testTag = "open_join_challenge_code_button"
        )
    }
}

/**
 * Glowing frosted glass invitation card for pending Study Challenges sent by friends.
 */
@Composable
fun PendingChallengeInviteCard(
    challenge: StudyChallenge,
    isBusy: Boolean,
    onAcceptInvite: () -> Unit,
    onDeclineInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pending_invite_card_${challenge.id}"),
        cornerRadius = 26.dp,
        elevated = true,
        shimmer = true,
        accentTint = palette.streakColor,
        frostedBlur = true,
        blurRadius = 22.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .clip(CircleShape)
                            .background(palette.streakColor.copy(alpha = 0.18f))
                            .border(1.dp, palette.streakColor.copy(alpha = 0.42f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MarkEmailUnread,
                            contentDescription = null,
                            tint = palette.streakColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${challenge.creatorName} invited you!",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.streakColor
                        )
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.primarySoft
                ) {
                    Text(
                        text = formatStudyDurationCompact(challenge.targetMinutes),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.primaryLip,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = "${challenge.subject} • ${challenge.memberUids.size} ${if (challenge.memberUids.size == 1) "scholar" else "scholars"} joined • Code ${challenge.inviteCode}",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(0.40f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("decline_challenge_${challenge.id}")
                        .clip(RoundedCornerShape(15.dp))
                        .clickable(enabled = !isBusy) {
                            tactile?.playTap()
                            onDeclineInvite()
                        },
                    shape = RoundedCornerShape(15.dp),
                    color = palette.glassFillElevated,
                    border = BorderStroke(1.dp, palette.glassBorderSubtle)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.challenges_decline_invite),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.textSecondary
                        )
                    }
                }

                MorphismActionButton(
                    text = stringResource(R.string.challenges_accept_join),
                    icon = Icons.Rounded.Groups,
                    onClick = onAcceptInvite,
                    enabled = !isBusy,
                    containerColor = palette.streakColor,
                    lipColor = palette.streakLip,
                    height = 48.dp,
                    cornerRadius = 15.dp,
                    modifier = Modifier.weight(0.60f),
                    testTag = "accept_challenge_${challenge.id}"
                )
            }
        }
    }
}

/**
 * Centerpiece Liquid Crystal card displaying a Study Challenge's live aggregated group progress,
 * combined LiquidStudyProgressBar, invite/share controls, and per-participant contribution bars.
 */
@Composable
fun StudyChallengeSharedProgressCard(
    item: StudyChallengeWithProgress,
    currentUid: String,
    onInviteFriendsClick: () -> Unit,
    onCodeCopied: (String) -> Unit,
    onLeaveOrDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val context = LocalContext.current

    val challenge = item.challenge
    val isCreator = currentUid.isNotBlank() && challenge.creatorUid == currentUid
    val totalMinutes = item.totalContributedMinutes
    val targetMinutes = challenge.targetMinutes.coerceAtLeast(30)
    val pct = ((totalMinutes.toFloat() / targetMinutes.toFloat()) * 100f).roundToInt().coerceAtLeast(0)
    val remaining = (targetMinutes - totalMinutes).coerceAtLeast(0)

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("challenge_card_${challenge.id}"),
        cornerRadius = 28.dp,
        elevated = true,
        shimmer = item.isCompleted,
        accentTint = if (item.isCompleted) palette.streakColor else palette.prismCyan,
        frostedBlur = true,
        blurRadius = 20.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Title + Subject + Sync Dot + Leave/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isCompleted) palette.streakColor.copy(alpha = 0.20f)
                                else palette.primarySoft
                            )
                            .border(
                                1.2.dp,
                                Color.White.copy(alpha = if (palette.isDark) 0.35f else 0.9f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isCompleted) Icons.Rounded.EmojiEvents else Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = if (item.isCompleted) palette.streakColor else palette.primaryLip,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = challenge.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            LiquidCrystalSyncDot(
                                color = if (item.isSyncing) palette.prismCyan else palette.primary,
                                isLivePulse = true,
                                isSyncing = item.isSyncing,
                                dotSize = 6.dp
                            )
                        }
                        Text(
                            text = "${challenge.subject} • ${item.participants.size.coerceAtLeast(challenge.memberUids.size)} participants • ${item.totalSessionsCompleted} sprints",
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
                        onLeaveOrDelete()
                    },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("challenge_leave_or_delete_${challenge.id}")
                ) {
                    Icon(
                        imageVector = if (isCreator) Icons.Rounded.DeleteOutline else Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = if (isCreator) "Delete challenge" else "Leave challenge",
                        tint = palette.textMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shared Group Aggregated Metrics + Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatStudyDurationCompact(totalMinutes),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp
                            ),
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "/ ${formatStudyDurationCompact(targetMinutes)} team goal",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.textSecondary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Text(
                        text = if (item.isCompleted) {
                            "Shared goal reached! Team crushed it together!"
                        } else {
                            "${formatStudyDurationCompact(remaining)} remaining together • focus sessions auto-count"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isCompleted) palette.streakColor else palette.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (item.isCompleted) palette.streakColor.copy(alpha = 0.18f) else palette.primarySoft,
                    border = BorderStroke(
                        1.dp,
                        if (item.isCompleted) palette.streakColor.copy(alpha = 0.45f) else palette.primary.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.isCompleted) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = palette.streakColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "${pct.coerceAtMost(999)}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (item.isCompleted) palette.streakColor else palette.primaryLip
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Combined Shared Liquid Progress Bar
            LiquidStudyProgressBar(
                progress = item.progressFraction,
                goalReached = item.isCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("challenge_shared_bar_${challenge.id}")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Aggregated Per-Participant Breakdown
            if (item.participants.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = palette.glassFillElevated.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, palette.glassBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PARTICIPANT CONTRIBUTIONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = palette.textMuted
                            )
                            if (challenge.invitedUids.isNotEmpty()) {
                                Text(
                                    text = "${challenge.invitedUids.size} invited",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.gemColor
                                )
                            }
                        }

                        val barDenominator = maxOf(
                            targetMinutes,
                            item.participants.maxOfOrNull { it.contributedMinutes } ?: 1
                        ).coerceAtLeast(1)

                        item.participants.forEachIndexed { idx, participant ->
                            ParticipantContributionRow(
                                participant = participant,
                                rank = idx + 1,
                                isCurrentUser = participant.userId == currentUid,
                                teamTotalMinutes = totalMinutes,
                                barDenominator = barDenominator
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Controls: Invite Code Copy Chip + Invite Friends + Share Invite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .testTag("copy_challenge_code_${challenge.inviteCode}")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            tactile?.playTap()
                            copyTextToClipboard(context, "Challenge Code", challenge.inviteCode)
                            onCodeCopied(challenge.inviteCode)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = palette.chipUnselected,
                    border = BorderStroke(1.dp, palette.glassBorderHighlight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy challenge code",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = challenge.inviteCode,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = palette.textPrimary
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp)
                        .testTag("invite_friends_challenge_${challenge.id}")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            tactile?.playTap()
                            onInviteFriendsClick()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = palette.primarySoft,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = stringResource(R.string.challenges_invite_friends),
                            tint = palette.primaryLip,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Invite",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.primaryLip
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                        .testTag("share_challenge_${challenge.id}")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            tactile?.playTap()
                            shareChallengeInvite(context, challenge)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = palette.gemColor.copy(alpha = if (palette.isDark) 0.24f else 0.14f),
                    border = BorderStroke(1.dp, palette.gemColor.copy(alpha = 0.38f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.challenges_share_code),
                            tint = palette.gemColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.gemColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantContributionRow(
    participant: ChallengeParticipant,
    rank: Int,
    isCurrentUser: Boolean,
    teamTotalMinutes: Int,
    barDenominator: Int
) {
    val palette = LocalTryAgainPalette.current
    val sharePercent = if (teamTotalMinutes > 0) {
        ((participant.contributedMinutes * 100f) / teamTotalMinutes).roundToInt().coerceIn(0, 100)
    } else 0
    val targetFraction = (participant.contributedMinutes.toFloat() / barDenominator.toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "partBar_${participant.userId}"
    )

    val accent = when {
        isCurrentUser -> palette.primary
        rank == 1 -> palette.streakColor
        else -> palette.gemColor
    }
    val initials = remember(participant.displayName) {
        participant.displayName.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "S" }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.20f))
                        .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCurrentUser) "${participant.displayName} (You)" else participant.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${formatStudyDurationCompact(participant.contributedMinutes)} • $sharePercent%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = if (isCurrentUser) palette.primaryLip else palette.textSecondary
            )
        }

        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = accent,
            trackColor = palette.glassBorderSubtle
        )
    }
}

/**
 * Empty state card when the user hasn't created or joined any Study Challenges yet.
 */
@Composable
fun StudyChallengesEmptyCard(modifier: Modifier = Modifier) {
    val palette = LocalTryAgainPalette.current
    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("challenges_empty_card"),
        cornerRadius = 26.dp,
        accentTint = palette.prismCyan,
        frostedBlur = true,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.primarySoft)
                    .border(1.dp, palette.glassBorderHighlight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = palette.primaryLip,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = stringResource(R.string.challenges_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = NunitoDisplayFamily,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.challenges_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Liquid Crystal modal dialog to Create a new Study Challenge, pick shared focus goal,
 * choose a subject, and invite Study Circle friends immediately.
 */
@Composable
fun CreateStudyChallengeDialog(
    subjects: List<String>,
    friends: List<FriendLiveCardState>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, subject: String, targetMinutes: Int, invitedFriendUids: List<String>) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    var title by remember { mutableStateOf("") }
    val subjectOptions = remember(subjects) { listOf("All Subjects") + subjects }
    var selectedSubject by remember { mutableStateOf("All Subjects") }
    var targetSlider by remember { mutableFloatStateOf(300f) } // default 5h
    val selectedFriends = remember { mutableStateListOf<String>() }

    val goalPresets = listOf(120, 300, 600, 1200, 1800) // 2h, 5h, 10h, 20h, 30h

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
                    .widthIn(max = 430.dp),
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create Study Challenge",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Pool everyone's focus sessions into one live shared goal.",
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
                                .testTag("close_create_challenge_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close create challenge dialog",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 80) title = it },
                        label = { Text("Challenge Name") },
                        placeholder = { Text("e.g., Weekend 10h Finals Sprint") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.glassBorderSubtle,
                            focusedContainerColor = palette.glassFillElevated,
                            unfocusedContainerColor = palette.glassFillElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("challenge_title_input")
                    )

                    // Subject filter
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Focus Subject",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subjectOptions.forEach { subj ->
                                MorphismChip(
                                    label = subj,
                                    selected = selectedSubject == subj,
                                    onClick = { selectedSubject = subj },
                                    testTag = "challenge_subject_${subj.lowercase(Locale.US).replace(" ", "_")}"
                                )
                            }
                        }
                    }

                    // Shared Focus Goal
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = palette.glassFillElevated,
                        border = BorderStroke(1.dp, palette.glassBorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shared Focus Goal",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = formatStudyDurationCompact(targetSlider.roundToInt()),
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
                                goalPresets.forEach { preset ->
                                    MorphismChip(
                                        label = formatStudyDurationCompact(preset),
                                        selected = targetSlider.roundToInt() == preset,
                                        onClick = { targetSlider = preset.toFloat() },
                                        testTag = "challenge_goal_preset_$preset"
                                    )
                                }
                            }

                            Slider(
                                value = targetSlider,
                                onValueChange = { targetSlider = ((it / 30f).roundToInt() * 30).coerceIn(60, 3000).toFloat() },
                                valueRange = 60f..3000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primary,
                                    activeTrackColor = palette.primary,
                                    inactiveTrackColor = palette.glassBorderSubtle
                                ),
                                modifier = Modifier.testTag("challenge_target_slider")
                            )
                        }
                    }

                    // Optional Friends Picker
                    if (friends.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Invite Friends Now (${selectedFriends.size} selected)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                friends.forEach { friendState ->
                                    val uid = friendState.profile.userId
                                    val picked = uid in selectedFriends
                                    MorphismChip(
                                        label = friendState.profile.displayName,
                                        selected = picked,
                                        onClick = {
                                            if (picked) selectedFriends.remove(uid) else selectedFriends.add(uid)
                                        },
                                        testTag = "challenge_invite_friend_${friendState.profile.handle}"
                                    )
                                }
                            }
                        }
                    }

                    MorphismActionButton(
                        text = if (isBusy) "CREATING…" else "LAUNCH CHALLENGE",
                        icon = Icons.Rounded.Flag,
                        enabled = !isBusy && title.trim().length >= 2,
                        onClick = {
                            onCreate(
                                title.trim(),
                                selectedSubject,
                                targetSlider.roundToInt(),
                                selectedFriends.toList()
                            )
                        },
                        height = 52.dp,
                        cornerRadius = 18.dp,
                        testTag = "submit_create_challenge_button"
                    )
                }
            }
        }
    }
}

/**
 * Liquid Crystal modal dialog for joining a friend's Study Challenge via 6-character code.
 */
@Composable
fun JoinChallengeByCodeDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onJoinCode: (String) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    var codeInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            MorphismSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                cornerRadius = 28.dp,
                elevated = true,
                accentTint = palette.gemColor,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Join Study Challenge",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        IconButton(
                            onClick = {
                                tactile?.playTap()
                                onDismiss()
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close join dialog",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    Text(
                        text = "Enter the 6-character invite code shared by a friend to pool your study sessions together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { raw ->
                            val clean = raw.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(6)
                            codeInput = clean
                        },
                        label = { Text("6-Character Code") },
                        placeholder = { Text("TRY48X") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (codeInput.length == 6 && !isBusy) onJoinCode(codeInput)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.gemColor,
                            unfocusedBorderColor = palette.glassBorderSubtle,
                            focusedContainerColor = palette.glassFillElevated,
                            unfocusedContainerColor = palette.glassFillElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_challenge_code_input")
                    )

                    MorphismActionButton(
                        text = if (isBusy) "JOINING…" else "JOIN CHALLENGE",
                        icon = Icons.Rounded.Groups,
                        enabled = !isBusy && codeInput.length == 6,
                        containerColor = palette.gemColor,
                        lipColor = palette.gemLip,
                        onClick = { onJoinCode(codeInput) },
                        height = 50.dp,
                        cornerRadius = 16.dp,
                        testTag = "submit_join_challenge_code_button"
                    )
                }
            }
        }
    }
}

/**
 * Dialog to invite additional Study Circle friends to an existing Study Challenge or share its code.
 */
@Composable
fun InviteFriendsToChallengeDialog(
    challenge: StudyChallenge,
    friends: List<FriendLiveCardState>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onInviteFriends: (List<String>) -> Unit,
    onCodeCopied: (String) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val context = LocalContext.current

    val eligibleFriends = remember(friends, challenge.memberUids, challenge.invitedUids) {
        friends.filter {
            it.profile.userId !in challenge.memberUids && it.profile.userId !in challenge.invitedUids
        }
    }
    val chosen = remember { mutableStateListOf<String>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            MorphismSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 410.dp),
                cornerRadius = 28.dp,
                elevated = true,
                accentTint = palette.primary,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Invite to Challenge",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary
                            )
                            Text(
                                text = challenge.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                tactile?.playTap()
                                onDismiss()
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close invite friends dialog",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    // Share Code Quick Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = palette.glassFillElevated,
                        border = BorderStroke(1.dp, palette.glassBorderHighlight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "INVITE CODE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textMuted
                                )
                                Text(
                                    text = challenge.inviteCode,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.4.sp
                                    ),
                                    color = palette.primaryLip
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        tactile?.playTap()
                                        copyTextToClipboard(context, "Challenge Code", challenge.inviteCode)
                                        onCodeCopied(challenge.inviteCode)
                                    },
                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy invite code",
                                        tint = palette.textPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        tactile?.playTap()
                                        shareChallengeInvite(context, challenge)
                                    },
                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "Share invite code",
                                        tint = palette.gemColor
                                    )
                                }
                            }
                        }
                    }

                    if (eligibleFriends.isEmpty()) {
                        Text(
                            text = "All your current Study Circle friends are already joined or invited! Share the code above with anyone else.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    } else {
                        Text(
                            text = "Tap Study Circle friends to send an in-app invite:",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            eligibleFriends.forEach { friendState ->
                                val uid = friendState.profile.userId
                                val selected = uid in chosen
                                MorphismChip(
                                    label = friendState.profile.displayName,
                                    selected = selected,
                                    onClick = {
                                        if (selected) chosen.remove(uid) else chosen.add(uid)
                                    }
                                )
                            }
                        }

                        MorphismActionButton(
                            text = if (isBusy) "SENDING…" else "SEND INVITES (${chosen.size})",
                            icon = Icons.Rounded.GroupAdd,
                            enabled = !isBusy && chosen.isNotEmpty(),
                            onClick = { onInviteFriends(chosen.toList()) },
                            height = 48.dp,
                            cornerRadius = 16.dp,
                            testTag = "confirm_invite_friends_button"
                        )
                    }
                }
            }
        }
    }
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareChallengeInvite(context: Context, challenge: StudyChallenge) {
    val goalLabel = formatStudyDurationCompact(challenge.targetMinutes)
    val shareBody = "Join my Try Again Study Challenge \"${challenge.title}\" ($goalLabel • ${challenge.subject})! Open Social → Challenges → Join by Code and enter: ${challenge.inviteCode}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join my Try Again Study Challenge!")
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share Study Challenge"))
    }
}
