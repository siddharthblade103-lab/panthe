package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountModerationRecord
import com.example.data.model.AdminNotice
import com.example.data.model.AdminNoticeKind
import com.example.data.model.AdminPanelUiState
import com.example.data.model.AppLiveConfig
import com.example.data.model.PublicStudyProfile
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Exclusive Admin entry card rendered ONLY when [isVerifiedAdmin] is true.
 * Regular users never see this card or any admin UI.
 */
@Composable
fun AdminConsoleEntryCard(
    liveConfig: AppLiveConfig,
    publicUsersCount: Int,
    onOpenAdminPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val adminGold = Color(0xFFF59E0B)
    val adminViolet = Color(0xFF7C3AED)

    LiquidGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_entry_card"),
        cornerRadius = 26.dp,
        elevated = true,
        accentTint = adminViolet,
        animatedCaustics = true,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(listOf(adminViolet, adminGold))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = "Verified Administrator",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.admin_badge_you_are_admin),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Role: admin · $publicUsersCount monitored users · Live v${liveConfig.configVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = adminGold.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, adminGold.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "ROLE: ADMIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = adminGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            MorphismActionButton(
                text = stringResource(R.string.admin_open_console),
                icon = Icons.Rounded.CloudSync,
                onClick = onOpenAdminPanel,
                containerColor = adminViolet,
                lipColor = Color(0xFF5B21B6),
                hapticEvent = SystemHapticEvent.CONFIRM,
                testTag = "open_admin_console_button"
            )
        }
    }
}

/**
 * Verified Admin-only Liquid-Glass Control Center.
 * - Tab 0: Silent Live App Updates (background real-time config push, zero client loading)
 * - Tab 1: Public Accounts Management (delete account / direct interact, strictly zero personal data)
 * - Tab 2: Official Broadcasts & Direct Notices
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlCenterSheet(
    state: AdminPanelUiState,
    currentAdminUid: String,
    onPushLiveConfig: (AppLiveConfig) -> Unit,
    onSendNotice: (targetUid: String?, targetLabel: String, title: String, body: String, kind: AdminNoticeKind) -> Unit,
    onDeleteNotice: (String) -> Unit,
    onDeleteUserAccount: (PublicStudyProfile, String) -> Unit,
    onClearMessages: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVerifiedAdmin) return

    val palette = LocalTryAgainPalette.current
    val systemHaptics = LocalSystemHaptics.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Draft Live App Config state
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

    // Accounts tab state
    var userSearchQuery by remember { mutableStateOf("") }
    var pendingDeleteProfile by remember { mutableStateOf<PublicStudyProfile?>(null) }
    var deleteReason by remember { mutableStateOf("Removed by administrator.") }
    var directMessageTarget by remember { mutableStateOf<PublicStudyProfile?>(null) }
    var directTitle by remember { mutableStateOf("") }
    var directBody by remember { mutableStateOf("") }

    // Broadcast tab state
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastBody by remember { mutableStateOf("") }
    var broadcastKind by remember { mutableStateOf(AdminNoticeKind.UPDATE) }

    BackHandler(onBack = onDismiss)

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
                        text = "This removes ${target.displayName}'s public study profile and @${target.handle} handle immediately and notifies their app silently.",
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
                    Text("Delete Account", color = palette.coralAccent, fontWeight = FontWeight.ExtraBold)
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

    // Direct Admin-to-User interaction dialog
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
                    text = "Interact with @${target.handle}",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = NunitoDisplayFamily)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Delivered silently in real time to ${target.displayName}'s Focus Hub without accessing any personal data.",
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
                        placeholder = { Text("Write a direct Admin message...") },
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (palette.isDark) Color(0xFF101926) else Color(0xFFF6F9FC),
        dragHandle = { BottomSheetDefaults.DragHandle(color = palette.textMuted.copy(alpha = 0.45f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .testTag("admin_control_center_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Admin Verified Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFF59E0B)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = "Admin Control Center",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.admin_panel_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.admin_panel_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
                IconButton(
                    onClick = {
                        systemHaptics.buttonTap()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("close_admin_panel_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Admin Panel",
                        tint = palette.textSecondary
                    )
                }
            }

            // Privacy Shield Guarantee Banner (Zero Personal Data)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.primarySoft,
                border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PrivacyTip,
                        contentDescription = null,
                        tint = palette.primaryLip,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.admin_zero_pii_title),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.primaryLip
                        )
                        Text(
                            text = stringResource(R.string.admin_zero_pii_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            // Live Feedback / Error Pill
            AnimatedVisibility(visible = state.feedbackMessage != null || state.errorMessage != null) {
                val isError = state.errorMessage != null
                val bannerColor = if (isError) palette.coralAccent else palette.primary
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = bannerColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearMessages() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
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
                            contentDescription = "Dismiss message",
                            tint = bannerColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 3-Mode Admin Switcher
            SlidingGlassSegmentedBar(
                labels = listOf("Live Updates", "Accounts", "Interact"),
                selectedIndex = selectedTabIndex,
                onSelect = { selectedTabIndex = it },
                tagPrefix = "admin_tab"
            )

            when (selectedTabIndex) {
                0 -> {
                    // TAB 1: Silent Background App Updates
                    MorphismSurface(
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Silent Background Push",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = NunitoDisplayFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "Revision v${live.configVersion} · Updates apps over internet without loading screens",
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
                                shape = RoundedCornerShape(14.dp),
                                colors = adminFieldColors(palette),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_live_tagline_input")
                            )

                            OutlinedTextField(
                                value = draftUpdateLabel,
                                onValueChange = { draftUpdateLabel = it.take(60) },
                                label = { Text("Live Channel / Release Tag") },
                                placeholder = { Text("e.g. v2.5 Silent Patch") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = adminFieldColors(palette),
                                modifier = Modifier.fillMaxWidth()
                            )

                            AdminToggleRow(
                                title = "Show Live Announcement Banner",
                                subtitle = "Displays silently at the top of Focus Hub",
                                checked = draftAnnouncementActive,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftAnnouncementActive = it
                                },
                                testTag = "admin_toggle_announcement"
                            )

                            AnimatedVisibility(visible = draftAnnouncementActive) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = draftAnnouncementTitle,
                                        onValueChange = { draftAnnouncementTitle = it.take(80) },
                                        label = { Text("Banner Headline") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = adminFieldColors(palette),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = draftAnnouncementBody,
                                        onValueChange = { draftAnnouncementBody = it.take(240) },
                                        label = { Text("Banner Details") },
                                        minLines = 2,
                                        maxLines = 3,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = adminFieldColors(palette),
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
                                        text = "Live Gem Reward Boost",
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
                                text = "Live Feature Flags (Instant Background Sync)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            AdminToggleRow(
                                title = "Social Circle Tab",
                                subtitle = "Enable or hide the Social navigation tab live",
                                checked = draftSocialEnabled,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftSocialEnabled = it
                                },
                                testTag = "admin_toggle_social"
                            )
                            AdminToggleRow(
                                title = "Study Launcher Mode",
                                subtitle = "Enable Galaxy-style Education Launcher mode",
                                checked = draftLauncherEnabled,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftLauncherEnabled = it
                                },
                                testTag = "admin_toggle_launcher"
                            )
                            AdminToggleRow(
                                title = "Ambient Sound Drawer",
                                subtitle = "Enable background ambient soundscapes bar",
                                checked = draftAmbientEnabled,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftAmbientEnabled = it
                                },
                                testTag = "admin_toggle_ambient"
                            )
                            AdminToggleRow(
                                title = "Maintenance Notice Pill",
                                subtitle = "Show a calm non-blocking maintenance badge",
                                checked = draftMaintenanceBanner,
                                onCheckedChange = {
                                    systemHaptics.toggle(it)
                                    draftMaintenanceBanner = it
                                },
                                testTag = "admin_toggle_maintenance"
                            )

                            MorphismActionButton(
                                text = if (state.isPushingUpdate) "Pushing Live Update..." else "Push Silent Live Update",
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
                                containerColor = Color(0xFF7C3AED),
                                lipColor = Color(0xFF5B21B6),
                                hapticEvent = SystemHapticEvent.CONFIRM,
                                testTag = "admin_push_live_update_button"
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 2: Zero-PII Public Account Management & Direct Interaction
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
                        placeholder = { Text("Search public @handle or name...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = adminFieldColors(palette),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_user_search_input")
                    )

                    if (filteredUsers.isEmpty()) {
                        MorphismSurface(cornerRadius = 20.dp) {
                            Text(
                                text = "No public study accounts match right now.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredUsers.forEach { user ->
                                val isSelf = user.userId == currentAdminUid
                                val isDeleting = state.deletingUserId == user.userId
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
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
                                                        color = Color(0xFFF59E0B).copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = "YOU",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 9.sp
                                                            ),
                                                            color = Color(0xFFF59E0B),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "@${user.handle} · Lv.${user.level}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = palette.streakColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "${user.currentStreak}d streak · ${user.totalFocusMinutes}m total",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textMuted
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                modifier = Modifier
                                                    .size(44.dp)
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
                                                        contentDescription = "Interact with ${user.displayName}",
                                                        tint = palette.primaryLip,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            if (!isSelf) {
                                                Surface(
                                                    modifier = Modifier
                                                        .size(44.dp)
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
                                                            modifier = Modifier.size(20.dp)
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
                    // TAB 3: Global Broadcasts & Active Notices
                    MorphismSurface(
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Broadcast to All Scholars",
                                style = MaterialTheme.typography.titleSmall.copy(
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
                                shape = RoundedCornerShape(14.dp),
                                colors = adminFieldColors(palette),
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
                                shape = RoundedCornerShape(14.dp),
                                colors = adminFieldColors(palette),
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
                            color = palette.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.notices.forEach { notice ->
                                MorphismSurface(
                                    cornerRadius = 18.dp,
                                    contentPadding = PaddingValues(12.dp)
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
                                            modifier = Modifier.testTag("admin_delete_notice_${notice.id}")
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

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Silently hydrated live banner & official Admin interaction card rendered on Focus Hub.
 * Updates instantaneously in the background over the internet with zero loading spinners.
 */
@Composable
fun SilentLiveUpdateFeedCard(
    liveConfig: AppLiveConfig,
    latestNotice: AdminNotice?,
    accountModeration: AccountModerationRecord?,
    onDismissNoticeId: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val systemHaptics = LocalSystemHaptics.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = accountModeration?.isRemoved == true,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            accountModeration?.let { mod ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.coralAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Account notice",
                            tint = palette.coralAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Public Study Account Removed",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.coralAccent
                            )
                            Text(
                                text = mod.reason.ifBlank { "Your public profile was removed by the administrator." },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = liveConfig.maintenanceBannerEnabled || liveConfig.hasVisibleAnnouncement,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            MorphismSurface(
                cornerRadius = 20.dp,
                accentTint = if (liveConfig.maintenanceBannerEnabled) palette.streakColor else palette.prismCyan,
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                (if (liveConfig.maintenanceBannerEnabled) palette.streakColor else palette.prismCyan)
                                    .copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (liveConfig.maintenanceBannerEnabled) Icons.Rounded.Shield else Icons.Rounded.CloudSync,
                            contentDescription = "Live update",
                            tint = if (liveConfig.maintenanceBannerEnabled) palette.streakColor else palette.prismCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val headline = when {
                            liveConfig.hasVisibleAnnouncement && liveConfig.announcementTitle.isNotBlank() ->
                                liveConfig.announcementTitle
                            liveConfig.maintenanceBannerEnabled ->
                                "Background Maintenance Active"
                            else ->
                                "Live Update"
                        }
                        val subtext = when {
                            liveConfig.hasVisibleAnnouncement && liveConfig.announcementBody.isNotBlank() ->
                                liveConfig.announcementBody
                            liveConfig.maintenanceBannerEnabled ->
                                "Your focus timer & local progress keep working smoothly."
                            else ->
                                ""
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = headline,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.textPrimary
                            )
                            if (liveConfig.updateNoticeLabel.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = palette.primarySoft
                                ) {
                                    Text(
                                        text = liveConfig.updateNoticeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = palette.primaryLip,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (subtext.isNotBlank()) {
                            Text(
                                text = subtext,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = latestNotice != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            latestNotice?.let { notice ->
                MorphismSurface(
                    cornerRadius = 20.dp,
                    accentTint = palette.primary,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Official Admin Notice",
                            tint = palette.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${notice.noticeKind.displayName} · Official Admin",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
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
                                color = palette.textSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                systemHaptics.buttonTap()
                                onDismissNoticeId(notice.id)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MarkEmailRead,
                                contentDescription = "Dismiss notice",
                                tint = palette.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminToggleRow(
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.primary
            )
        )
    }
}

@Composable
private fun adminFieldColors(palette: com.example.ui.theme.TryAgainPalette) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.primary,
        unfocusedBorderColor = palette.glassBorderSubtle,
        focusedTextColor = palette.textPrimary,
        unfocusedTextColor = palette.textPrimary
    )
