package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GlobalLeaderboardUiState
import com.example.data.model.PublicStudyProfile
import com.example.data.model.SocialDashboardUiState
import com.example.data.model.StudyChallenge
import com.example.data.model.StudyChallengesUiState
import com.example.data.model.StudyChatUiState
import com.example.data.model.StudyLiveStatus
import com.example.data.model.UserProfile
import com.example.ui.components.ConfirmLeaveOrDeleteChallengeDialog
import com.example.ui.components.CreateStudyChallengeDialog
import com.example.ui.components.EditStudyHandleDialog
import com.example.ui.components.FriendDirectChatSheet
import com.example.ui.components.FriendSafetyActionDialog
import com.example.ui.components.InviteFriendsToChallengeDialog
import com.example.ui.components.JoinChallengeByCodeDialog
import com.example.ui.components.LeaderboardEmptyOrErrorCard
import com.example.ui.components.LeaderboardLoadingCard
import com.example.ui.components.LeaderboardPodiumCard
import com.example.ui.components.LeaderboardScholarRow
import com.example.ui.components.LiquidCrystalSyncDot
import com.example.ui.components.LiquidGlassFriendCard
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.components.LocalTactileSound
import com.example.ui.components.MorphismActionButton
import com.example.ui.components.MorphismSurface
import com.example.ui.components.MyLeaderboardRankCard
import com.example.ui.components.PendingChallengeInviteCard
import com.example.ui.components.SlidingGlassSegmentedBar
import com.example.ui.components.SocialCircleLiquidHero
import com.example.ui.components.SocialSafetyShieldDialog
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StudyChallengeSharedProgressCard
import com.example.ui.components.StudyChallengesActionStrip
import com.example.ui.components.StudyChallengesEmptyCard
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.util.Locale

/**
 * Liquid Glass Social Dashboard:
 * - Fluid animated transitions across Study Circle, Group Study Challenges, and Global Streak Ranks.
 * - Authentic multi-layered Liquid Glass panels with drifting caustics, specular highlights, and animated grid items.
 * - Built-in Social Safety & Privacy Shield (on-device screen-time lock, Private Study Mode, Block/Unblock, safe confirmations).
 */
@Composable
fun SocialDashboardScreen(
    profile: UserProfile,
    currentUid: String,
    todayFocusMinutes: Int,
    socialState: SocialDashboardUiState,
    challengesState: StudyChallengesUiState,
    leaderboardState: GlobalLeaderboardUiState,
    chatState: StudyChatUiState,
    isSigningIn: Boolean,
    authError: String?,
    onGoogleSignIn: () -> Unit,
    onAddFriendByHandle: (String) -> Unit,
    onUpdateStudyHandle: (String) -> Unit,
    onOpenFriendChat: (PublicStudyProfile) -> Unit,
    onCloseFriendChat: () -> Unit,
    onSendFriendMessage: (String) -> Unit,
    onClearChatError: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    onBlockUser: (String, String, String) -> Unit,
    onUnblockUser: (String) -> Unit,
    onTogglePrivateStudyMode: (Boolean) -> Unit,
    onClearSocialError: () -> Unit,
    onHandleCopied: (String) -> Unit,
    onCreateChallenge: (String, String, Int, List<String>) -> Unit,
    onJoinChallengeByCode: (String) -> Unit,
    onAcceptChallengeInvite: (String) -> Unit,
    onDeclineChallengeInvite: (String) -> Unit,
    onInviteFriendsToChallenge: (String, List<String>) -> Unit,
    onLeaveOrDeleteChallenge: (StudyChallenge) -> Unit,
    onClearChallengeError: () -> Unit,
    onChallengeCodeCopied: (String) -> Unit,
    onRefreshLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val context = LocalContext.current

    // 0 = Study Circle, 1 = Study Challenges, 2 = Global Leaderboard
    var socialSectionTab by rememberSaveable { mutableIntStateOf(0) }
    var handleInput by rememberSaveable { mutableStateOf("") }
    var filterTab by rememberSaveable { mutableIntStateOf(0) } // 0 = All Friends, 1 = Focusing Now

    var showSafetyShieldDialog by remember { mutableStateOf(false) }
    var showEditHandleDialog by remember { mutableStateOf(false) }
    var selectedFriendForActions by remember { mutableStateOf<PublicStudyProfile?>(null) }
    var showCreateChallengeDialog by remember { mutableStateOf(false) }
    var showJoinCodeDialog by remember { mutableStateOf(false) }
    var inviteTargetChallenge by remember { mutableStateOf<StudyChallenge?>(null) }
    var pendingExitChallenge by remember { mutableStateOf<StudyChallenge?>(null) }

    val isLeaderboardSyncing = (leaderboardState as? GlobalLeaderboardUiState.Success)?.isSyncing == true
    val rankedCount = (leaderboardState as? GlobalLeaderboardUiState.Success)?.entries?.size ?: 0
    val isChallengesSyncing =
        challengesState.isBusy || challengesState.isSyncing || challengesState.activeChallenges.any { it.isSyncing }

    val focusingCount = remember(socialState.friends) {
        socialState.friends.count { it.profile.liveStatus == StudyLiveStatus.FOCUSING }
    }
    val circleFriendsTodayMinutes = remember(socialState.friends) {
        socialState.friends.sumOf { it.profile.todayFocusMinutes.coerceAtLeast(0) }
    }
    val displayedFriends = remember(socialState.friends, filterTab) {
        if (filterTab == 1) {
            socialState.friends.filter { it.profile.liveStatus == StudyLiveStatus.FOCUSING }
        } else {
            socialState.friends
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .statusBarsPadding()
        ) {
            // Fixed Top Header + Safety Shield + Live Sync Pill (smoothly animates on tab switch)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = if (!socialState.isSignedIn) -1 else socialSectionTab,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInHorizontally { it / 12 })
                                .togetherWith(fadeOut(tween(140)))
                        },
                        modifier = Modifier.weight(1f),
                        label = "socialHeaderText"
                    ) { tab ->
                        Column {
                            Text(
                                text = when (tab) {
                                    1 -> stringResource(R.string.challenges_title)
                                    2 -> stringResource(R.string.leaderboard_title)
                                    else -> stringResource(R.string.social_title)
                                },
                                style = MaterialTheme.typography.headlineLarge,
                                color = palette.textPrimary
                            )
                            Text(
                                text = when (tab) {
                                    1 -> stringResource(R.string.challenges_subtitle)
                                    2 -> stringResource(R.string.leaderboard_subtitle)
                                    else -> stringResource(R.string.social_subtitle)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }

                    if (socialState.isSignedIn) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Safety & Privacy Shield Icon Pill (48dp touch target)
                            Surface(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                                    .testTag("social_safety_shield_button")
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        tactile?.playTap()
                                        showSafetyShieldDialog = true
                                    },
                                shape = RoundedCornerShape(50),
                                color = if (socialState.privateStudyMode) {
                                    palette.prismViolet.copy(alpha = if (palette.isDark) 0.28f else 0.16f)
                                } else {
                                    palette.glassFillElevated
                                },
                                border = BorderStroke(
                                    1.1.dp,
                                    if (socialState.privateStudyMode) {
                                        palette.prismViolet.copy(alpha = 0.55f)
                                    } else {
                                        palette.primary.copy(alpha = 0.42f)
                                    }
                                ),
                                shadowElevation = 4.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = stringResource(R.string.social_safety_button_desc),
                                        tint = if (socialState.privateStudyMode) palette.prismViolet else palette.primaryLip,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Live Sync Indicator Pill
                            val activeSync = when (socialSectionTab) {
                                1 -> isChallengesSyncing
                                2 -> isLeaderboardSyncing
                                else -> socialState.isSyncingCircle
                            }
                            Surface(
                                modifier = Modifier.animateContentSize(
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                                ),
                                shape = RoundedCornerShape(50),
                                color = palette.glassFillElevated,
                                border = BorderStroke(
                                    1.1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (palette.isDark) 0.38f else 0.92f),
                                            palette.prismCyan.copy(alpha = 0.48f)
                                        )
                                    )
                                ),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LiquidCrystalSyncDot(
                                        color = when {
                                            socialSectionTab == 1 -> palette.prismCyan
                                            socialSectionTab == 2 -> palette.streakColor
                                            focusingCount > 0 -> palette.primary
                                            else -> palette.prismCyan
                                        },
                                        isLivePulse = true,
                                        isSyncing = activeSync,
                                        dotSize = 7.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            activeSync -> stringResource(R.string.social_syncing_now)
                                            socialSectionTab == 1 && challengesState.activeChallenges.isNotEmpty() ->
                                                "${challengesState.activeChallenges.size} Active"
                                            socialSectionTab == 1 -> stringResource(R.string.social_live_sync)
                                            socialSectionTab == 2 && rankedCount > 0 -> "Top $rankedCount"
                                            socialSectionTab == 2 -> stringResource(R.string.social_live_sync)
                                            focusingCount > 0 -> "$focusingCount Live"
                                            else -> stringResource(R.string.social_live_sync)
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                if (socialState.isSignedIn) {
                    val challengesLabel = if (challengesState.pendingInvites.isNotEmpty()) {
                        "${stringResource(R.string.social_tab_challenges)} (${challengesState.pendingInvites.size})"
                    } else {
                        stringResource(R.string.social_tab_challenges)
                    }
                    SlidingGlassSegmentedBar(
                        labels = listOf(
                            stringResource(R.string.social_tab_circle),
                            challengesLabel,
                            stringResource(R.string.social_tab_leaderboard)
                        ),
                        selectedIndex = socialSectionTab,
                        onSelect = { socialSectionTab = it },
                        tagPrefix = "social_mode"
                    )
                }
            }

            if (!socialState.isSignedIn) {
                // Signed-Out Liquid Glass Google Sign-In Gate
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 124.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "social_signin_gate") {
                        StaggeredEntrance(index = 0) {
                            LiquidGlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 30.dp,
                                elevated = true,
                                accentTint = palette.prismCyan,
                                contentPadding = PaddingValues(22.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        palette.primary.copy(alpha = 0.24f),
                                                        palette.prismCyan.copy(alpha = 0.30f),
                                                        palette.prismViolet.copy(alpha = 0.24f)
                                                    )
                                                )
                                            )
                                            .border(
                                                1.3.dp,
                                                Color.White.copy(alpha = if (palette.isDark) 0.42f else 0.94f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Groups,
                                            contentDescription = null,
                                            tint = palette.primaryLip,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = stringResource(R.string.social_sign_in_title),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = NunitoDisplayFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = palette.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.social_sign_in_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )

                                    if (!authError.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = authError,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.coralAccent
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    MorphismActionButton(
                                        text = if (isSigningIn) "SIGNING IN…" else stringResource(R.string.profile_sign_in_google).uppercase(Locale.US),
                                        icon = Icons.Rounded.Login,
                                        onClick = onGoogleSignIn,
                                        enabled = !isSigningIn,
                                        containerColor = palette.primary,
                                        lipColor = palette.primaryLip,
                                        testTag = "social_google_sign_in_button"
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Silky Directional Liquid Glass Transition Across Circle / Challenges / Ranks
                AnimatedContent(
                    targetState = socialSectionTab,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val forward = targetState >= initialState
                        val offsetFn = { width: Int -> (width * 0.08f).toInt() }
                        val enter = slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { if (forward) offsetFn(it) else -offsetFn(it) }
                        ) + fadeIn(tween(240, easing = FastOutSlowInEasing)) + scaleIn(
                            initialScale = 0.975f,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                        )
                        val exit = slideOutHorizontally(
                            animationSpec = tween(170, easing = FastOutSlowInEasing),
                            targetOffsetX = { if (forward) -offsetFn(it) else offsetFn(it) }
                        ) + fadeOut(tween(150, easing = FastOutSlowInEasing)) + scaleOut(
                            targetScale = 0.985f,
                            animationSpec = tween(150)
                        )
                        enter.togetherWith(exit).using(SizeTransform(clip = false))
                    },
                    label = "socialSectionGlide"
                ) { activeTab ->
                    when (activeTab) {
                        1 -> {
                            // --- TAB 1: GROUP STUDY CHALLENGES ---
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 124.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "challenges_actions") {
                                    StaggeredEntrance(index = 0, modifier = Modifier.animateItem()) {
                                        StudyChallengesActionStrip(
                                            isBusy = challengesState.isBusy,
                                            onOpenCreateDialog = {
                                                if (challengesState.errorMessage != null) onClearChallengeError()
                                                showCreateChallengeDialog = true
                                            },
                                            onOpenJoinCodeDialog = {
                                                if (challengesState.errorMessage != null) onClearChallengeError()
                                                showJoinCodeDialog = true
                                            }
                                        )
                                    }
                                }

                                if (!challengesState.errorMessage.isNullOrBlank()) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "challenges_error_banner") {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = palette.coralAccent.copy(alpha = if (palette.isDark) 0.20f else 0.12f),
                                            border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.45f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = challengesState.errorMessage.orEmpty(),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = palette.coralAccent,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = onClearChallengeError,
                                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Dismiss challenge error",
                                                        tint = palette.coralAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (challengesState.pendingInvites.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "challenges_pending_header") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem()
                                                .padding(top = 4.dp, start = 2.dp, end = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.challenges_pending_invites_title),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = NunitoDisplayFamily,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = "${challengesState.pendingInvites.size} waiting",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = palette.streakColor
                                            )
                                        }
                                    }

                                    itemsIndexed(
                                        items = challengesState.pendingInvites,
                                        span = { _, _ -> GridItemSpan(maxLineSpan) },
                                        key = { _, invite -> "pending_${invite.id}" }
                                    ) { idx, invite ->
                                        StaggeredEntrance(index = (idx % 4) + 1, modifier = Modifier.animateItem()) {
                                            PendingChallengeInviteCard(
                                                challenge = invite,
                                                isBusy = challengesState.isBusy,
                                                onAcceptInvite = { onAcceptChallengeInvite(invite.id) },
                                                onDeclineInvite = { onDeclineChallengeInvite(invite.id) }
                                            )
                                        }
                                    }
                                }

                                if (challengesState.activeChallenges.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "challenges_active_header") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem()
                                                .padding(top = 4.dp, start = 2.dp, end = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.challenges_active_title),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = NunitoDisplayFamily,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = "${challengesState.activeChallenges.size} active",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = palette.textMuted
                                            )
                                        }
                                    }

                                    itemsIndexed(
                                        items = challengesState.activeChallenges,
                                        span = { _, _ -> GridItemSpan(maxLineSpan) },
                                        key = { _, item -> "challenge_${item.challenge.id}" }
                                    ) { idx, challengeItem ->
                                        StaggeredEntrance(index = (idx % 5) + 1, modifier = Modifier.animateItem()) {
                                            StudyChallengeSharedProgressCard(
                                                item = challengeItem,
                                                currentUid = currentUid,
                                                onInviteFriendsClick = { inviteTargetChallenge = challengeItem.challenge },
                                                onCodeCopied = onChallengeCodeCopied,
                                                onLeaveOrDelete = { pendingExitChallenge = challengeItem.challenge }
                                            )
                                        }
                                    }
                                } else if (challengesState.isSyncing) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "challenges_loading") {
                                        LiquidGlassPanel(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem(),
                                            cornerRadius = 24.dp,
                                            elevated = false
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.4.dp,
                                                    color = palette.primary
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Syncing live study challenges…",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }
                                    }
                                } else if (challengesState.pendingInvites.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "challenges_empty") {
                                        StaggeredEntrance(index = 1, modifier = Modifier.animateItem()) {
                                            StudyChallengesEmptyCard()
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // --- TAB 2: GLOBAL STUDY STREAKS LEADERBOARD ---
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 124.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (leaderboardState) {
                                    is GlobalLeaderboardUiState.Loading -> {
                                        item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_loading") {
                                            StaggeredEntrance(index = 0) {
                                                LeaderboardLoadingCard()
                                            }
                                        }
                                    }

                                    is GlobalLeaderboardUiState.Error -> {
                                        item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_error") {
                                            StaggeredEntrance(index = 0) {
                                                LeaderboardEmptyOrErrorCard(
                                                    isError = true,
                                                    message = leaderboardState.message,
                                                    onRetry = onRefreshLeaderboard
                                                )
                                            }
                                        }
                                    }

                                    is GlobalLeaderboardUiState.Success -> {
                                        item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_my_rank") {
                                            StaggeredEntrance(index = 0, modifier = Modifier.animateItem()) {
                                                MyLeaderboardRankCard(
                                                    myEntry = leaderboardState.myEntry,
                                                    nextAboveEntry = leaderboardState.nextAboveEntry,
                                                    totalRankedScholars = leaderboardState.entries.size,
                                                    fallbackStreak = profile.currentStreak,
                                                    fallbackTotalMinutes = profile.totalFocusMinutes,
                                                    isSyncing = leaderboardState.isSyncing,
                                                    onRefresh = onRefreshLeaderboard
                                                )
                                            }
                                        }

                                        if (leaderboardState.entries.isEmpty()) {
                                            item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_empty") {
                                                StaggeredEntrance(index = 1) {
                                                    LeaderboardEmptyOrErrorCard(
                                                        isError = false,
                                                        message = null,
                                                        onRetry = onRefreshLeaderboard
                                                    )
                                                }
                                            }
                                        } else {
                                            item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_podium") {
                                                StaggeredEntrance(index = 1, modifier = Modifier.animateItem()) {
                                                    LeaderboardPodiumCard(
                                                        topEntries = leaderboardState.entries.take(3)
                                                    )
                                                }
                                            }

                                            item(span = { GridItemSpan(maxLineSpan) }, key = "leaderboard_list_header") {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 4.dp, start = 2.dp, end = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.leaderboard_all_scholars_title),
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontFamily = NunitoDisplayFamily,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        color = palette.textPrimary
                                                    )
                                                    Text(
                                                        text = "${leaderboardState.entries.size} scholars",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = palette.textMuted
                                                    )
                                                }
                                            }

                                            itemsIndexed(
                                                items = leaderboardState.entries,
                                                span = { _, _ -> GridItemSpan(maxLineSpan) },
                                                key = { _, entry -> "lb_${entry.profile.userId}" }
                                            ) { idx, entry ->
                                                StaggeredEntrance(index = (idx % 6) + 1, modifier = Modifier.animateItem()) {
                                                    LeaderboardScholarRow(
                                                        entry = entry,
                                                        isAddingFriend = socialState.isAddingFriend,
                                                        onAddFriendByHandle = onAddFriendByHandle
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // --- TAB 0: LIQUID GLASS STUDY CIRCLE ---
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 124.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Signature Liquid Glass Circle Hero Card
                                item(span = { GridItemSpan(maxLineSpan) }, key = "my_study_card") {
                                    val myStudy = socialState.myStudyProfile
                                    val myHandle = myStudy?.handle.orEmpty()
                                    StaggeredEntrance(index = 0, modifier = Modifier.animateItem()) {
                                        SocialCircleLiquidHero(
                                            myHandle = myHandle,
                                            myLiveStatus = myStudy?.liveStatus ?: StudyLiveStatus.READY,
                                            privateStudyMode = socialState.privateStudyMode,
                                            isSyncing = socialState.isSyncingCircle,
                                            todayFocusMinutes = todayFocusMinutes,
                                            streakDays = profile.currentStreak,
                                            friendsCount = socialState.friends.size,
                                            focusingFriendsCount = focusingCount,
                                            circleTodayMinutes = circleFriendsTodayMinutes,
                                            onCopyHandle = {
                                                copyHandleToClipboard(context, "@$myHandle")
                                                onHandleCopied("@$myHandle")
                                            },
                                            onShareHandle = {
                                                shareStudyHandle(context, profile.displayName, myHandle)
                                            },
                                            onEditHandle = {
                                                if (socialState.errorMessage != null) onClearSocialError()
                                                showEditHandleDialog = true
                                            },
                                            onOpenSafetyShield = { showSafetyShieldDialog = true },
                                            isUpdatingHandle = socialState.isUpdatingHandle
                                        )
                                    }
                                }

                                // Liquid Glass Add Friend Input Card
                                item(span = { GridItemSpan(maxLineSpan) }, key = "add_friend_card") {
                                    StaggeredEntrance(index = 1, modifier = Modifier.animateItem()) {
                                        LiquidGlassPanel(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 24.dp,
                                            accentTint = palette.primary,
                                            elevated = false,
                                            animatedCaustics = false,
                                            contentPadding = PaddingValues(14.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = handleInput,
                                                        onValueChange = {
                                                            handleInput = it
                                                            if (socialState.errorMessage != null) onClearSocialError()
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("add_friend_handle_input"),
                                                        placeholder = {
                                                            Text(
                                                                text = stringResource(R.string.social_add_friend_placeholder),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = palette.textMuted
                                                            )
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Rounded.AlternateEmail,
                                                                contentDescription = null,
                                                                tint = palette.primaryLip,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(18.dp),
                                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                        keyboardActions = KeyboardActions(
                                                            onDone = {
                                                                if (handleInput.isNotBlank() && !socialState.isAddingFriend) {
                                                                    onAddFriendByHandle(handleInput)
                                                                    handleInput = ""
                                                                }
                                                            }
                                                        ),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = palette.primary,
                                                            unfocusedBorderColor = palette.glassBorderSubtle,
                                                            focusedTextColor = palette.textPrimary,
                                                            unfocusedTextColor = palette.textPrimary,
                                                            focusedContainerColor = palette.glassFill.copy(alpha = 0.45f),
                                                            unfocusedContainerColor = palette.glassFill.copy(alpha = 0.30f)
                                                        )
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .width(86.dp)
                                                            .height(52.dp)
                                                            .testTag("add_friend_submit_button")
                                                            .clip(RoundedCornerShape(18.dp))
                                                            .background(
                                                                if (handleInput.isNotBlank() && !socialState.isAddingFriend) {
                                                                    palette.primary
                                                                } else {
                                                                    palette.chipUnselected
                                                                }
                                                            )
                                                            .border(
                                                                1.dp,
                                                                Color.White.copy(alpha = 0.42f),
                                                                RoundedCornerShape(18.dp)
                                                            )
                                                            .clickable(
                                                                enabled = handleInput.isNotBlank() && !socialState.isAddingFriend
                                                            ) {
                                                                tactile?.playTap()
                                                                val submitted = handleInput
                                                                onAddFriendByHandle(submitted)
                                                                handleInput = ""
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (socialState.isAddingFriend) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(20.dp),
                                                                strokeWidth = 2.4.dp,
                                                                color = palette.primaryLip
                                                            )
                                                        } else {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.PersonAdd,
                                                                    contentDescription = stringResource(R.string.social_add_friend_cta),
                                                                    tint = if (handleInput.isNotBlank()) Color.White else palette.textMuted,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = stringResource(R.string.social_add_friend_cta),
                                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                                    color = if (handleInput.isNotBlank()) Color.White else palette.textMuted
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                AnimatedVisibility(
                                                    visible = !socialState.errorMessage.isNullOrBlank(),
                                                    enter = fadeIn(tween(180)) + expandVertically(),
                                                    exit = fadeOut(tween(140)) + shrinkVertically()
                                                ) {
                                                    Text(
                                                        text = socialState.errorMessage.orEmpty(),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = palette.coralAccent,
                                                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Filter Bar when friends exist
                                if (socialState.friends.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "friends_filter_bar") {
                                        StaggeredEntrance(index = 2, modifier = Modifier.animateItem()) {
                                            SlidingGlassSegmentedBar(
                                                labels = listOf(
                                                    "All Friends (${socialState.friends.size})",
                                                    "Focusing Now ($focusingCount)"
                                                ),
                                                selectedIndex = filterTab,
                                                onSelect = { filterTab = it },
                                                tagPrefix = "social_filter"
                                            )
                                        }
                                    }
                                }

                                // Live Study Hub: Who's Online & Studying Now + Challenges & Top Ranks (always visible!)
                                item(span = { GridItemSpan(maxLineSpan) }, key = "social_live_hub_overview") {
                                    val liveEntries = (leaderboardState as? GlobalLeaderboardUiState.Success)?.entries.orEmpty()
                                    val onlineScholars = remember(liveEntries) {
                                        liveEntries
                                            .sortedWith(
                                                compareByDescending<com.example.data.model.GlobalLeaderboardEntry> {
                                                    it.profile.liveStatus == StudyLiveStatus.FOCUSING
                                                }
                                                    .thenByDescending { it.profile.liveStatus == StudyLiveStatus.BREAK }
                                                    .thenByDescending { it.profile.todayFocusMinutes }
                                            )
                                            .take(6)
                                    }
                                    val topRanks = remember(liveEntries) { liveEntries.take(3) }

                                    StaggeredEntrance(index = 2, modifier = Modifier.animateItem()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            // 1. Live Who's Online & Studying Right Now Card
                                            LiquidGlassPanel(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("social_online_studying_card"),
                                                cornerRadius = 24.dp,
                                                accentTint = palette.primary,
                                                elevated = true,
                                                animatedCaustics = false,
                                                contentPadding = PaddingValues(16.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Live Study Pulse",
                                                                style = MaterialTheme.typography.titleMedium.copy(
                                                                    fontFamily = NunitoDisplayFamily,
                                                                    fontWeight = FontWeight.ExtraBold
                                                                ),
                                                                color = palette.textPrimary
                                                            )
                                                            Text(
                                                                text = "See who is online & how much everyone studied today",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = palette.textSecondary
                                                            )
                                                        }
                                                        val liveActiveCount = onlineScholars.count {
                                                            it.profile.liveStatus == StudyLiveStatus.FOCUSING ||
                                                                it.profile.todayFocusMinutes > 0
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(50),
                                                            color = palette.primarySoft
                                                        ) {
                                                            Text(
                                                                text = "$liveActiveCount Active",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                                color = palette.primaryLip,
                                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }

                                                    if (onlineScholars.isEmpty()) {
                                                        Text(
                                                            text = "Start a focus sprint or add a friend by @handle above — live study presence updates here in real time.",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = palette.textSecondary
                                                        )
                                                    } else {
                                                        onlineScholars.forEach { entry ->
                                                            val p = entry.profile
                                                            val isLive = p.liveStatus == StudyLiveStatus.FOCUSING
                                                            val isBreak = p.liveStatus == StudyLiveStatus.BREAK
                                                            Surface(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                shape = RoundedCornerShape(16.dp),
                                                                color = palette.glassFillElevated,
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (isLive) palette.primary.copy(alpha = 0.42f) else palette.glassBorderSubtle
                                                                )
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                                ) {
                                                                    LiquidCrystalSyncDot(
                                                                        color = when {
                                                                            isLive -> palette.primary
                                                                            isBreak -> palette.streakColor
                                                                            else -> palette.prismCyan
                                                                        },
                                                                        isLivePulse = isLive || isBreak,
                                                                        isSyncing = false,
                                                                        dotSize = 7.dp
                                                                    )
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = if (entry.isCurrentUser) "${p.displayName} (You)" else p.displayName,
                                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                                            color = palette.textPrimary,
                                                                            maxLines = 1
                                                                        )
                                                                        Text(
                                                                            text = when {
                                                                                isLive -> "Studying ${p.currentSubject} • ${p.todayFocusMinutes}m today"
                                                                                isBreak -> "On break • ${p.todayFocusMinutes}m today"
                                                                                p.todayFocusMinutes > 0 -> "Studied ${p.todayFocusMinutes}m today • @${p.handle}"
                                                                                else -> "Online ready • @${p.handle}"
                                                                            },
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = palette.textSecondary,
                                                                            maxLines = 1
                                                                        )
                                                                    }
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Rounded.LocalFireDepartment,
                                                                            contentDescription = null,
                                                                            tint = palette.streakColor,
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                        Text(
                                                                            text = "${p.currentStreak}d",
                                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                                            color = palette.streakColor
                                                                        )
                                                                    }
                                                                    if (!entry.isCurrentUser && !entry.isFriend && p.handle.isNotBlank()) {
                                                                        Surface(
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(50))
                                                                                .clickable(enabled = !socialState.isAddingFriend) {
                                                                                    tactile?.playTap()
                                                                                    onAddFriendByHandle(p.handle)
                                                                                },
                                                                            shape = RoundedCornerShape(50),
                                                                            color = palette.primary
                                                                        ) {
                                                                            Text(
                                                                                text = "+ Add",
                                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                                                color = Color.White,
                                                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // 2. Challenges & Global Streak Rankings Quick Hub
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                LiquidGlassPanel(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("social_quick_challenges_card")
                                                        .clickable {
                                                            tactile?.playTap()
                                                            socialSectionTab = 1
                                                        },
                                                    cornerRadius = 22.dp,
                                                    accentTint = palette.prismCyan,
                                                    elevated = false,
                                                    animatedCaustics = false,
                                                    contentPadding = PaddingValues(14.dp)
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Flag,
                                                                contentDescription = null,
                                                                tint = palette.prismCyan,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Text(
                                                                text = "Challenges",
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                                color = palette.textPrimary
                                                            )
                                                        }
                                                        Text(
                                                            text = if (challengesState.activeChallenges.isNotEmpty()) {
                                                                "${challengesState.activeChallenges.size} active group goals"
                                                            } else {
                                                                "Create or join a live study challenge"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = palette.textSecondary
                                                        )
                                                        Text(
                                                            text = "Open Challenges →",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                            color = palette.primaryLip
                                                        )
                                                    }
                                                }

                                                LiquidGlassPanel(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("social_quick_ranks_card")
                                                        .clickable {
                                                            tactile?.playTap()
                                                            socialSectionTab = 2
                                                        },
                                                    cornerRadius = 22.dp,
                                                    accentTint = palette.streakColor,
                                                    elevated = false,
                                                    animatedCaustics = false,
                                                    contentPadding = PaddingValues(14.dp)
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.EmojiEvents,
                                                                contentDescription = null,
                                                                tint = palette.streakColor,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Text(
                                                                text = "Top Ranks",
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                                color = palette.textPrimary
                                                            )
                                                        }
                                                        val leaderLine = topRanks.firstOrNull()?.let {
                                                            "#1 ${it.profile.displayName} (${it.profile.currentStreak}d)"
                                                        } ?: "Climb the global streak podium"
                                                        Text(
                                                            text = leaderLine,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = palette.textSecondary,
                                                            maxLines = 2
                                                        )
                                                        Text(
                                                            text = "View Leaderboard →",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                            color = palette.streakColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Friends Liquid Glass Grid
                                if (socialState.friends.isNotEmpty() && displayedFriends.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "social_filter_empty") {
                                        MorphismSurface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem(),
                                            cornerRadius = 22.dp,
                                            frostedBlur = true
                                        ) {
                                            Text(
                                                text = "None of your added friends are in a focus session right now. Switch to All Friends to check everyone's streaks.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }
                                } else if (displayedFriends.isNotEmpty()) {
                                    itemsIndexed(
                                        items = displayedFriends,
                                        key = { _, state -> state.profile.userId }
                                    ) { index, friendState ->
                                        StaggeredEntrance(
                                            index = (index % 6) + 1,
                                            modifier = Modifier.animateItem()
                                        ) {
                                            LiquidGlassFriendCard(
                                                friendState = friendState,
                                                onMessageFriend = {
                                                    onOpenFriendChat(friendState.profile)
                                                },
                                                onOpenFriendActions = {
                                                    selectedFriendForActions = friendState.profile
                                                }
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

        // --- Liquid Glass Safety & Challenge Modals ---

        if (showSafetyShieldDialog) {
            SocialSafetyShieldDialog(
                privateStudyMode = socialState.privateStudyMode,
                blockedUsers = socialState.blockedUsers,
                onTogglePrivateStudyMode = onTogglePrivateStudyMode,
                onUnblockUser = onUnblockUser,
                onDismiss = { showSafetyShieldDialog = false }
            )
        }

        if (showEditHandleDialog) {
            EditStudyHandleDialog(
                currentHandle = socialState.myStudyProfile?.handle.orEmpty(),
                isUpdating = socialState.isUpdatingHandle,
                onSaveHandle = onUpdateStudyHandle,
                onDismiss = { showEditHandleDialog = false }
            )
        }

        if (chatState.activeFriend != null) {
            FriendDirectChatSheet(
                chatState = chatState,
                currentUid = currentUid,
                onSendMessage = onSendFriendMessage,
                onClearError = onClearChatError,
                onDismiss = onCloseFriendChat
            )
        }

        selectedFriendForActions?.let { friend ->
            FriendSafetyActionDialog(
                friend = friend,
                onMessageFriend = {
                    selectedFriendForActions = null
                    onOpenFriendChat(friend)
                },
                onRemoveFriend = {
                    onRemoveFriend(friend.userId)
                    selectedFriendForActions = null
                },
                onBlockFriend = {
                    onBlockUser(friend.userId, friend.handle, friend.displayName)
                    selectedFriendForActions = null
                },
                onDismiss = { selectedFriendForActions = null }
            )
        }

        if (showCreateChallengeDialog) {
            CreateStudyChallengeDialog(
                subjects = profile.subjects.ifEmpty { UserProfile.DEFAULT_SUBJECTS },
                friends = socialState.friends,
                isBusy = challengesState.isBusy,
                onDismiss = { showCreateChallengeDialog = false },
                onCreate = { title, subject, targetMinutes, invitedUids ->
                    onCreateChallenge(title, subject, targetMinutes, invitedUids)
                    showCreateChallengeDialog = false
                }
            )
        }

        if (showJoinCodeDialog) {
            JoinChallengeByCodeDialog(
                isBusy = challengesState.isBusy,
                onDismiss = { showJoinCodeDialog = false },
                onJoinCode = { code ->
                    onJoinChallengeByCode(code)
                    showJoinCodeDialog = false
                }
            )
        }

        inviteTargetChallenge?.let { target ->
            InviteFriendsToChallengeDialog(
                challenge = target,
                friends = socialState.friends,
                isBusy = challengesState.isBusy,
                onDismiss = { inviteTargetChallenge = null },
                onInviteFriends = { uids ->
                    onInviteFriendsToChallenge(target.id, uids)
                    inviteTargetChallenge = null
                },
                onCodeCopied = onChallengeCodeCopied
            )
        }

        pendingExitChallenge?.let { target ->
            ConfirmLeaveOrDeleteChallengeDialog(
                challenge = target,
                isCreator = currentUid.isNotBlank() && target.creatorUid == currentUid,
                onConfirm = {
                    onLeaveOrDeleteChallenge(target)
                    pendingExitChallenge = null
                },
                onDismiss = { pendingExitChallenge = null }
            )
        }
    }
}

private fun copyHandleToClipboard(context: Context, handleWithAt: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Try Again Study Handle", handleWithAt))
}

private fun shareStudyHandle(context: Context, displayName: String, handle: String) {
    val message =
        "Study with me on Try Again! Add my study handle @$handle ($displayName) to sync our focus streaks and live study sessions."
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join my Try Again Study Circle")
        putExtra(Intent.EXTRA_TEXT, message)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share Study Handle"))
}
