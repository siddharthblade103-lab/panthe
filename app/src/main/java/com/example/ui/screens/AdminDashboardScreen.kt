package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.AdminNoticeKind
import com.example.data.model.AdminPanelUiState
import com.example.data.model.AdminUserCountsSnapshot
import com.example.data.model.AppLiveConfig
import com.example.data.model.PublicStudyProfile
import com.example.data.model.StudyLiveStatus
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.components.LocalSystemHaptics
import com.example.ui.components.MorphismActionButton
import com.example.ui.components.MorphismChip
import com.example.ui.components.MorphismSurface
import com.example.ui.components.SlidingGlassSegmentedBar
import com.example.ui.components.SystemHapticEvent
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import com.example.ui.theme.TryAgainPalette
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dedicated full-screen glassmorphic Admin Dashboard reachable ONLY by authenticated users
 * whose Firestore profile holds `role == "admin"`.
 *
 * Provides:
 * - Real-time Firestore User Counts monitor (server-side aggregation + live presence split)
 * - App-wide live settings & feature flag management (silent background sync)
 * - Zero-PII public user account list with delete & direct notice actions
 * - Official broadcast composer
 */
@Composable
fun AdminDashboardScreen(
    state: AdminPanelUiState,
    currentAdminUid: String,
    onRefreshUserCounts: () -> Unit,
    onPushLiveConfig: (AppLiveConfig) -> Unit,
    onSendNotice: (targetUid: String?, targetLabel: String, title: String, body: String, kind: AdminNoticeKind) -> Unit,
    onDeleteNotice: (String) -> Unit,
    onDeleteUserAccount: (PublicStudyProfile, String) -> Unit,
    onClearMessages: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVerifiedAdmin) return

    val palette = LocalTryAgainPalette.current
    val systemHaptics = LocalSystemHaptics.current
    val adminViolet = Color(0xFF7C3AED)
    val adminGold = Color(0xFFF59E0B)

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Draft app-wide live settings state synced to latest config revision
    val live = state.liveConfig
    var draftTagline by remember(live.configVersion) { mutableStateOf(live.liveTagline) }
    var draftUpdateLabel by remember(live.configVersion) { mutableStateOf(live.updateNoticeLabel) }
    var draftAnnouncementActive by remember(live.configVersion) { mutableStateOf(live.announcementActive) }
    var draftAnnouncementTitle by remember(live.configVersion) { mutableStateOf(live.announcementTitle) }
    var draftAnnouncementBody by remember(live.configVersion) { mutableStateOf(live.announcementBody) }
    var draftGemMultiplierPct by remember(live.configVersion) { mutableFloatStateOf(live.gemMultiplierPercent.toFloat()) }
    var draftDefaultMinutes by remember(live.configVersion) { mutableFloatStateOf(live.defaultFocusMinutes.toFloat()) }
    var draftSocialEnabled by remember(live.configVersion) { mutableStateOf(live.socialEnabled) }
    var draftLauncherEnabled by remember(live.configVersion) { mutableStateOf(live.launcherEnabled) }
    var draftAmbientEnabled by remember(live.configVersion) { mutableStateOf(live.ambientDrawerEnabled) }
    var draftMaintenanceBanner by remember(live.configVersion) { mutableStateOf(live.maintenanceBannerEnabled) }

    // Users tab state
    var userSearchQuery by remember { mutableStateOf("") }
    var pendingDeleteProfile by remember { mutableStateOf<PublicStudyProfile?>(null) }
    var deleteReason by remember { mutableStateOf("Account removed by administrator.") }
    var directMessageTarget by remember { mutableStateOf<PublicStudyProfile?>(null) }
    var directTitle by remember { mutableStateOf("") }
    var directBody by remember { mutableStateOf("") }

    // Broadcasts tab state
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastBody by remember { mutableStateOf("") }
    var broadcastKind by remember { mutableStateOf(AdminNoticeKind.UPDATE) }

    BackHandler(onBack = onBack)

    // Delete account confirmation dialog
    pendingDeleteProfile?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteProfile = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = palette.coralAccent
                )
            },
            title = {
                Text(
                    text = "Delete @${target.handle}?",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = NunitoDisplayFamily)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Removes ${target.displayName}'s public study profile and @${target.handle} handle immediately and notifies their app silently.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it.take(160) },
                        label = { Text("Reason shown to user") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        systemHaptics.reject()
                        onDeleteUserAccount(target, deleteReason)
                        pendingDeleteProfile = null
                    },
                    modifier = Modifier.testTag("confirm_admin_delete_user")
                ) {
                    Text("Delete User", color = palette.coralAccent, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        systemHaptics.buttonTap()
                        pendingDeleteProfile = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Direct admin notice dialog
    directMessageTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { directMessageTarget = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Campaign,
                    contentDescription = null,
                    tint = palette.primary
                )
            },
            title = {
                Text(
                    text = "Message @${target.handle}",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = NunitoDisplayFamily)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Delivered silently to ${target.displayName}'s Focus Hub without accessing any private data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = directTitle,
                        onValueChange = { directTitle = it.take(80) },
                        label = { Text("Notice title") },
                        placeholder = { Text("e.g. Awesome Study Streak!") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = directBody,
                        onValueChange = { directBody = it.take(400) },
                        label = { Text("Message") },
                        placeholder = { Text("Write an official Admin message...") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (directTitle.isNotBlank() && directBody.isNotBlank()) {
                            systemHaptics.confirm()
                            onSendNotice(
                                target.userId,
                                "@${target.handle}",
                                directTitle,
                                directBody,
                                AdminNoticeKind.DIRECT
                            )
                            directTitle = ""
                            directBody = ""
                            directMessageTarget = null
                        }
                    },
                    enabled = directTitle.isNotBlank() && directBody.isNotBlank(),
                    modifier = Modifier.testTag("confirm_admin_direct_send")
                ) {
                    Text("Send Silently", color = palette.primary, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { directMessageTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.canvasTop,
                        palette.canvasBottom
                    )
                )
            )
            .testTag("admin_dashboard_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Glass App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                systemHaptics.buttonTap()
                                onBack()
                            }
                            .testTag("admin_dashboard_back_button"),
                        shape = CircleShape,
                        color = palette.glassFillElevated,
                        border = BorderStroke(1.dp, palette.glassBorderHighlight)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back from Admin Dashboard",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Admin Dashboard",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = adminGold.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, adminGold.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "ROLE: ADMIN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.7.sp
                                    ),
                                    color = adminGold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Live Firestore counts & silent app settings · v${live.configVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !state.userCounts.isRefreshing) {
                            systemHaptics.tick()
                            onRefreshUserCounts()
                        }
                        .testTag("admin_refresh_counts_button"),
                    shape = CircleShape,
                    color = palette.glassFillElevated,
                    border = BorderStroke(1.dp, palette.glassBorderHighlight)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.userCounts.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = adminViolet
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh live user counts",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }

            // Live Feedback / Error Banner
            AnimatedVisibility(visible = state.feedbackMessage != null || state.errorMessage != null) {
                val isError = state.errorMessage != null
                val bannerColor = if (isError) palette.coralAccent else palette.primary
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bannerColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearMessages() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.errorMessage ?: state.feedbackMessage.orEmpty(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = bannerColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss banner",
                            tint = bannerColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Hero Glass User Counts Telemetry Monitor
            UserCountsHeroGlassCard(
                counts = state.userCounts,
                configVersion = live.configVersion,
                adminViolet = adminViolet,
                palette = palette
            )

            // 2x2 Glass Stat Tile Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminMetricGlassTile(
                    title = "Focusing Now",
                    count = state.userCounts.focusingNow,
                    subtitle = "Live study sprints",
                    icon = Icons.Rounded.Bolt,
                    accent = palette.primary,
                    modifier = Modifier.weight(1f),
                    testTag = "admin_metric_focusing"
                )
                AdminMetricGlassTile(
                    title = "Active Today",
                    count = state.userCounts.activeToday,
                    subtitle = "Studied today",
                    icon = Icons.Rounded.Today,
                    accent = palette.gemColor,
                    modifier = Modifier.weight(1f),
                    testTag = "admin_metric_active_today"
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminMetricGlassTile(
                    title = "On Streak",
                    count = state.userCounts.onStreakCount,
                    subtitle = "Active study streaks",
                    icon = Icons.Rounded.LocalFireDepartment,
                    accent = palette.streakColor,
                    modifier = Modifier.weight(1f),
                    testTag = "admin_metric_streaks"
                )
                AdminMetricGlassTile(
                    title = "Removed",
                    count = state.userCounts.removedAccounts,
                    subtitle = "Moderated accounts",
                    icon = Icons.Rounded.PersonOff,
                    accent = palette.coralAccent,
                    modifier = Modifier.weight(1f),
                    testTag = "admin_metric_removed"
                )
            }

            // Compact Zero-PII Privacy Shield Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.primarySoft.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PrivacyTip,
                        contentDescription = null,
                        tint = palette.primaryLip,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.admin_zero_pii_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            // Clean 3-Section Glass Switcher
            SlidingGlassSegmentedBar(
                labels = listOf("App Settings", "Users", "Broadcasts"),
                selectedIndex = selectedTabIndex,
                onSelect = { selectedTabIndex = it },
                tagPrefix = "admin_dash_tab"
            )

            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: App-Wide Live Settings (Silent Background Sync)
                    MorphismSurface(
                        cornerRadius = 24.dp,
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(adminViolet.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = null,
                                        tint = adminViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "App-Wide Live Settings",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = NunitoDisplayFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "Pushes silently over Firestore to all active apps without loading screens",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = draftTagline,
                                onValueChange = { draftTagline = it.take(100) },
                                label = { Text("Live App Tagline (Focus Hub)") },
                                placeholder = { Text("Leave blank for default status") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = dashboardFieldColors(palette),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_live_tagline_input")
                            )

                            OutlinedTextField(
                                value = draftUpdateLabel,
                                onValueChange = { draftUpdateLabel = it.take(60) },
                                label = { Text("Release / Live Channel Tag") },
                                placeholder = { Text("e.g. v2.6 Silent Patch") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = dashboardFieldColors(palette),
                                modifier = Modifier.fillMaxWidth()
                            )

                            DashboardToggleRow(
                                title = "Announcement Banner",
                                subtitle = "Show live update banner at top of Focus Hub",
                                checked = draftAnnouncementActive,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftAnnouncementActive = it
                                },
                                testTag = "admin_toggle_announcement"
                            )

                            AnimatedVisibility(visible = draftAnnouncementActive) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = draftAnnouncementTitle,
                                        onValueChange = { draftAnnouncementTitle = it.take(80) },
                                        label = { Text("Banner Headline") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = dashboardFieldColors(palette),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = draftAnnouncementBody,
                                        onValueChange = { draftAnnouncementBody = it.take(240) },
                                        label = { Text("Banner Message") },
                                        minLines = 2,
                                        maxLines = 3,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = dashboardFieldColors(palette),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            HorizontalDivider(color = palette.glassBorderSubtle)

                            // Gem Reward Multiplier Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Global Gem Multiplier",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2fx Gems", draftGemMultiplierPct / 100f),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = palette.gemColor
                                    )
                                }
                                Slider(
                                    value = draftGemMultiplierPct,
                                    onValueChange = { draftGemMultiplierPct = (it / 25f).roundToInt() * 25f },
                                    valueRange = 100f..300f,
                                    steps = 7,
                                    colors = SliderDefaults.colors(
                                        thumbColor = palette.gemColor,
                                        activeTrackColor = palette.gemColor
                                    )
                                )
                            }

                            // Default Focus Duration Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Default Focus Sprint",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "${draftDefaultMinutes.roundToInt()} min",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = palette.primary
                                    )
                                }
                                Slider(
                                    value = draftDefaultMinutes,
                                    onValueChange = { draftDefaultMinutes = (it / 5f).roundToInt() * 5f },
                                    valueRange = 15f..60f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = palette.primary,
                                        activeTrackColor = palette.primary
                                    )
                                )
                            }

                            HorizontalDivider(color = palette.glassBorderSubtle)

                            Text(
                                text = "Feature Toggles",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.textPrimary
                            )
                            DashboardToggleRow(
                                title = "Social Circle Tab",
                                subtitle = "Show or hide Social Circle across the app",
                                checked = draftSocialEnabled,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftSocialEnabled = it
                                },
                                testTag = "admin_toggle_social"
                            )
                            DashboardToggleRow(
                                title = "Ambient Sounds Screen",
                                subtitle = "Enable local background ambient soundscape picker",
                                checked = draftAmbientEnabled,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftAmbientEnabled = it
                                },
                                testTag = "admin_toggle_ambient"
                            )
                            DashboardToggleRow(
                                title = "Maintenance Status Pill",
                                subtitle = "Display a calm non-blocking maintenance notice",
                                checked = draftMaintenanceBanner,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftMaintenanceBanner = it
                                },
                                testTag = "admin_toggle_maintenance"
                            )

                            MorphismActionButton(
                                text = if (state.isPushingUpdate) "Syncing Settings..." else "Save & Push App Settings",
                                icon = Icons.Rounded.CloudSync,
                                enabled = !state.isPushingUpdate,
                                onClick = {
                                    onPushLiveConfig(
                                        live.copy(
                                            liveTagline = draftTagline,
                                            updateNoticeLabel = draftUpdateLabel,
                                            announcementActive = draftAnnouncementActive,
                                            announcementTitle = draftAnnouncementTitle,
                                            announcementBody = draftAnnouncementBody,
                                            gemMultiplierPercent = draftGemMultiplierPct.roundToInt().coerceIn(100, 300),
                                            defaultFocusMinutes = draftDefaultMinutes.roundToInt().coerceIn(15, 60),
                                            socialEnabled = draftSocialEnabled,
                                            launcherEnabled = draftLauncherEnabled,
                                            ambientDrawerEnabled = draftAmbientEnabled,
                                            maintenanceBannerEnabled = draftMaintenanceBanner
                                        )
                                    )
                                },
                                containerColor = adminViolet,
                                lipColor = Color(0xFF5B21B6),
                                hapticEvent = SystemHapticEvent.CONFIRM,
                                testTag = "admin_push_live_update_button"
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: User Account Directory (Zero PII, Delete & Direct Interact)
                    val filteredUsers = remember(state.publicDirectory, userSearchQuery) {
                        val q = userSearchQuery.trim().lowercase(Locale.US)
                        if (q.isEmpty()) state.publicDirectory
                        else state.publicDirectory.filter {
                            it.handle.lowercase(Locale.US).contains(q) ||
                                it.displayName.lowercase(Locale.US).contains(q)
                        }
                    }

                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it.take(40) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = { Text("Search users by @handle or name...") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = dashboardFieldColors(palette),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_user_search_input")
                    )

                    if (filteredUsers.isEmpty()) {
                        MorphismSurface(cornerRadius = 22.dp, contentPadding = PaddingValues(18.dp)) {
                            Text(
                                text = "No user accounts match your search.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredUsers.forEach { user ->
                                val isSelf = user.userId == currentAdminUid
                                val isDeleting = state.deletingUserId == user.userId
                                val statusColor = when (user.liveStatus) {
                                    StudyLiveStatus.FOCUSING -> palette.primary
                                    StudyLiveStatus.BREAK -> palette.streakColor
                                    StudyLiveStatus.READY -> palette.gemColor
                                }

                                MorphismSurface(
                                    cornerRadius = 22.dp,
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(statusColor)
                                                )
                                                Text(
                                                    text = user.displayName,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold
                                                    ),
                                                    color = palette.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isSelf) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = adminGold.copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = "ADMIN",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 9.sp
                                                            ),
                                                            color = adminGold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "@${user.handle} · ${user.liveStatus.label} · Lv.${user.level}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                            Text(
                                                text = "${user.currentStreak}d streak · ${user.totalFocusMinutes}m focused",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textMuted,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("admin_interact_${user.handle}")
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        systemHaptics.buttonTap()
                                                        directTitle = ""
                                                        directBody = ""
                                                        directMessageTarget = user
                                                    },
                                                shape = CircleShape,
                                                color = palette.primarySoft,
                                                border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                                        contentDescription = "Message ${user.displayName}",
                                                        tint = palette.primaryLip,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }

                                            if (!isSelf) {
                                                Surface(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("admin_delete_${user.handle}")
                                                        .clip(CircleShape)
                                                        .clickable(enabled = !isDeleting) {
                                                            systemHaptics.reject()
                                                            deleteReason = "Account removed by administrator."
                                                            pendingDeleteProfile = user
                                                        },
                                                    shape = CircleShape,
                                                    color = palette.coralAccent.copy(alpha = 0.14f),
                                                    border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.35f))
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.DeleteOutline,
                                                            contentDescription = "Delete account ${user.displayName}",
                                                            tint = palette.coralAccent,
                                                            modifier = Modifier.size(21.dp)
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

                2 -> {
                    // TAB 2: Official Broadcasts & Active Notices
                    MorphismSurface(
                        cornerRadius = 24.dp,
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Broadcast to All Scholars",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    AdminNoticeKind.UPDATE,
                                    AdminNoticeKind.BROADCAST,
                                    AdminNoticeKind.ALERT
                                ).forEach { kind ->
                                    MorphismChip(
                                        label = kind.displayName,
                                        selected = broadcastKind == kind,
                                        onClick = { broadcastKind = kind },
                                        testTag = "admin_kind_${kind.name.lowercase()}"
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = broadcastTitle,
                                onValueChange = { broadcastTitle = it.take(80) },
                                label = { Text("Headline") },
                                placeholder = { Text("e.g. Weekend Study Boost!") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = dashboardFieldColors(palette),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_broadcast_title_input")
                            )
                            OutlinedTextField(
                                value = broadcastBody,
                                onValueChange = { broadcastBody = it.take(400) },
                                label = { Text("Message to everyone") },
                                placeholder = { Text("Delivered silently in real time...") },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(16.dp),
                                colors = dashboardFieldColors(palette),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_broadcast_body_input")
                            )
                            MorphismActionButton(
                                text = if (state.isSendingNotice) "Sending..." else "Send Global Broadcast",
                                icon = Icons.Rounded.Campaign,
                                enabled = !state.isSendingNotice && broadcastTitle.isNotBlank() && broadcastBody.isNotBlank(),
                                onClick = {
                                    onSendNotice(
                                        null,
                                        "All Scholars",
                                        broadcastTitle,
                                        broadcastBody,
                                        broadcastKind
                                    )
                                    broadcastTitle = ""
                                    broadcastBody = ""
                                },
                                hapticEvent = SystemHapticEvent.CONFIRM,
                                testTag = "admin_send_broadcast_button"
                            )
                        }
                    }

                    if (state.notices.isNotEmpty()) {
                        Text(
                            text = "Active Notices (${state.notices.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.textSecondary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.notices.forEach { notice ->
                                MorphismSurface(
                                    cornerRadius = 20.dp,
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${notice.noticeKind.displayName} · ${notice.targetLabel}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = palette.primary
                                            )
                                            Text(
                                                text = notice.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = notice.body,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                systemHaptics.reject()
                                                onDeleteNotice(notice.id)
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("admin_delete_notice_${notice.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete notice",
                                                tint = palette.coralAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun UserCountsHeroGlassCard(
    counts: AdminUserCountsSnapshot,
    configVersion: Int,
    adminViolet: Color,
    palette: TryAgainPalette
) {
    val animatedTotal by animateIntAsState(
        targetValue = counts.totalUsers,
        animationSpec = tween(durationMillis = 420),
        label = "totalUsersCount"
    )
    val safeTotal = counts.totalUsers.coerceAtLeast(1)
    val focusingWeight = (counts.focusingNow.toFloat() / safeTotal).coerceIn(0f, 1f)
    val breakWeight = (counts.onBreakNow.toFloat() / safeTotal).coerceIn(0f, 1f)
    val readyWeight = (1f - focusingWeight - breakWeight).coerceIn(0f, 1f)

    LiquidGlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_counts_hero"),
        cornerRadius = 28.dp,
        elevated = true,
        accentTint = adminViolet,
        animatedCaustics = true,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = adminViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "TOTAL REGISTERED SCHOLARS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.9.sp
                            ),
                            color = palette.textSecondary
                        )
                    }
                    Text(
                        text = animatedTotal.toString(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Live · v$configVersion",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.primary
                        )
                    }
                }
            }

            // Presence distribution glass bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.chipUnselected),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (focusingWeight > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(focusingWeight)
                                .fillMaxSize()
                                .background(palette.primary)
                        )
                    }
                    if (breakWeight > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(breakWeight)
                                .fillMaxSize()
                                .background(palette.streakColor)
                        )
                    }
                    if (readyWeight > 0.01f || counts.totalUsers == 0) {
                        Box(
                            modifier = Modifier
                                .weight(if (counts.totalUsers == 0) 1f else readyWeight)
                                .fillMaxSize()
                                .background(palette.gemColor.copy(alpha = 0.55f))
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PresenceLegendDot("Focusing (${counts.focusingNow})", palette.primary, palette)
                    PresenceLegendDot("Break (${counts.onBreakNow})", palette.streakColor, palette)
                    PresenceLegendDot("Ready (${counts.readyNow})", palette.gemColor, palette)
                }
            }
        }
    }
}

@Composable
private fun PresenceLegendDot(
    label: String,
    dotColor: Color,
    palette: TryAgainPalette
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textSecondary
        )
    }
}

@Composable
private fun AdminMetricGlassTile(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val palette = LocalTryAgainPalette.current
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 360),
        label = title
    )

    MorphismSurface(
        modifier = modifier.testTag(testTag),
        cornerRadius = 22.dp,
        accentTint = accent,
        contentPadding = PaddingValues(15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = animatedCount.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = NunitoDisplayFamily,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalTryAgainPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.primary
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun dashboardFieldColors(palette: TryAgainPalette) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.primary,
        unfocusedBorderColor = palette.glassBorderSubtle,
        focusedLabelColor = palette.primary,
        unfocusedLabelColor = palette.textSecondary,
        focusedTextColor = palette.textPrimary,
        unfocusedTextColor = palette.textPrimary,
        cursorColor = palette.primary
    )
