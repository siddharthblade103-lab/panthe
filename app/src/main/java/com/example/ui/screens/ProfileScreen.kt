package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppIconShapeId
import com.example.data.model.AppLiveConfig
import com.example.data.model.AppPreferencesState
import com.example.data.model.CompletionSoundPreset
import com.example.data.model.CustomAppIconId
import com.example.data.model.MascotBadgeId
import com.example.data.model.PlayPointsTier
import com.example.data.model.ScholarProfileDetails
import com.example.data.model.StreakReminderUiStatus
import com.example.data.model.UserProfile
import com.example.ui.components.AdminConsoleEntryCard
import com.example.ui.components.CustomAppIconEmblem
import com.example.ui.components.CustomAppIconStudioCard
import com.example.ui.components.DailyStreakReminderCard
import com.example.ui.components.EditScholarDetailsDialog
import com.example.ui.components.EmailZohoSignInDialog
import com.example.ui.components.LocalTactileSound
import com.example.ui.components.MorphismActionButton
import com.example.ui.components.MorphismChip
import com.example.ui.components.MorphismSurface
import com.example.ui.components.PhoneOtpSignInDialog
import com.example.ui.components.ScholarProfileDetailsCard
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    profile: UserProfile,
    preferences: AppPreferencesState,
    streakReminderStatus: StreakReminderUiStatus,
    userEmail: String?,
    isSigningIn: Boolean,
    authError: String?,
    isExportingPdf: Boolean,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateDailyGoal: (Int) -> Unit,
    onUpdateWeeklyGoal: (Int) -> Unit,
    onToggleUiSounds: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleAutoPlayAmbient: (Boolean) -> Unit,
    onSelectCompletionSound: (CompletionSoundPreset) -> Unit,
    onToggleWeekendPdf: (Boolean) -> Unit,
    onToggleStreakReminder: (Boolean) -> Unit,
    onSelectStreakReminderTime: (Int, Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestStreakReminder: () -> Unit,
    onExportWeeklyPdf: () -> Unit,
    onSelectCustomIcon: (CustomAppIconId) -> Unit,
    onSelectCustomIconShape: (AppIconShapeId) -> Unit,
    onCustomShortcutFeedback: (String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit,
    scholarDetails: ScholarProfileDetails = ScholarProfileDetails(),
    onUpdateScholarDetails: (ScholarProfileDetails) -> Unit = {},
    onPickProfilePhoto: (Uri) -> Unit = {},
    onClearProfilePhoto: () -> Unit = {},
    phoneVerificationId: String? = null,
    onEmailZohoSignIn: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSendPhoneOtp: (String) -> Unit = {},
    onVerifyPhoneOtp: (String) -> Unit = {},
    onGitHubSignIn: () -> Unit = {},
    onClearAuthError: () -> Unit = {},
    isVerifiedAdmin: Boolean = false,
    liveAppConfig: AppLiveConfig = AppLiveConfig(),
    publicUsersCount: Int = 0,
    onOpenAdminPanel: () -> Unit = {}
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val badge = remember(profile.equippedBadge) { MascotBadgeId.fromWire(profile.equippedBadge) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditDetailsDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPhoneOtpDialog by remember { mutableStateOf(false) }
    var goalSlider by remember(profile.dailyGoalMinutes) { mutableFloatStateOf(profile.dailyGoalMinutes.coerceIn(15, 360).toFloat()) }
    var weeklyGoalSlider by remember(preferences.weeklyGoalMinutes) {
        mutableFloatStateOf(preferences.weeklyGoalMinutes.coerceIn(60, 2100).toFloat())
    }

    // Zero-permission Android Photo Picker for Profile Avatar
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickProfilePhoto(uri)
        }
    }

    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = scholarDetails.avatarLocalPath) {
        value = if (scholarDetails.avatarLocalPath.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(scholarDetails.avatarLocalPath)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                }.getOrNull()
            }
        }
    }

    if (showEditDetailsDialog) {
        EditScholarDetailsDialog(
            details = scholarDetails,
            subjects = profile.subjects.ifEmpty { UserProfile.DEFAULT_SUBJECTS },
            onSave = { updated ->
                onUpdateScholarDetails(updated)
                showEditDetailsDialog = false
            },
            onDismiss = { showEditDetailsDialog = false }
        )
    }

    if (showEmailDialog && userEmail == null) {
        EmailZohoSignInDialog(
            isBusy = isSigningIn,
            error = authError,
            onSubmit = { email, pass, create -> onEmailZohoSignIn(email, pass, create) },
            onDismiss = {
                onClearAuthError()
                showEmailDialog = false
            }
        )
    }

    if (showPhoneOtpDialog && userEmail == null) {
        PhoneOtpSignInDialog(
            isBusy = isSigningIn,
            verificationId = phoneVerificationId,
            error = authError,
            onSendOtp = onSendPhoneOtp,
            onVerifyOtp = onVerifyPhoneOtp,
            onDismiss = {
                onClearAuthError()
                showPhoneOtpDialog = false
            }
        )
    }

    val avatarMotion = rememberInfiniteTransition(label = "profileAvatar")
    val haloAngle by avatarMotion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotate"
    )
    val floatY by avatarMotion.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarFloat"
    )

    val auraColors = when (badge) {
        MascotBadgeId.CLASSIC_MONKEY -> listOf(palette.primary, Color(0xFF89E219), palette.primary)
        MascotBadgeId.SCHOLAR_OWL -> listOf(Color(0xFF1CB0F6), Color(0xFF6366F1), Color(0xFF1CB0F6))
        MascotBadgeId.ROYAL_CROWN -> listOf(Color(0xFFFFC800), Color(0xFFFF7A00), Color(0xFFFFC800))
        MascotBadgeId.COSMIC_AURA -> listOf(Color(0xFF58CC02), Color(0xFF1CB0F6), Color(0xFFEC4899), Color(0xFF58CC02))
    }

    if (showEditNameDialog) {
        var draftName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    "Edit Scholar Name",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = NunitoDisplayFamily)
                )
            },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draftName.isNotBlank()) {
                            onUpdateDisplayName(draftName)
                            showEditNameDialog = false
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            StaggeredEntrance(index = 0) {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = NunitoDisplayFamily),
                    color = palette.textPrimary
                )
            }
        }

        // Verified Admin-Only Control Center Entry (completely invisible to non-admins)
        if (isVerifiedAdmin) {
            item {
                StaggeredEntrance(index = 0) {
                    AdminConsoleEntryCard(
                        liveConfig = liveAppConfig,
                        publicUsersCount = publicUsersCount,
                        onOpenAdminPanel = onOpenAdminPanel
                    )
                }
            }
        }

        // Hero Floating Mascot & Rotating Cosmetic Halo Card
        item {
            StaggeredEntrance(index = 1) {
                MorphismSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    elevated = true,
                    shimmer = true,
                    accentTint = palette.primary,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(106.dp)
                                .offset(y = floatY.dp)
                                .testTag("profile_avatar_picker_button")
                                .clickable {
                                    tactile?.playTap()
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(98.dp)
                                    .graphicsLayer { rotationZ = haloAngle }
                                    .clip(CircleShape)
                                    .background(Brush.sweepGradient(auraColors))
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(palette.glassFillElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                val localBmp = avatarBitmap
                                if (localBmp != null) {
                                    Image(
                                        bitmap = localBmp,
                                        contentDescription = "Scholar custom profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    CustomAppIconEmblem(
                                        iconId = preferences.customAppIconId,
                                        shapeId = preferences.customIconShapeId,
                                        size = 82.dp,
                                        contentDescription = "Scholar custom icon avatar"
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp),
                                shape = CircleShape,
                                color = palette.primary,
                                border = BorderStroke(2.dp, Color.White),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = stringResource(R.string.profile_change_photo),
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    tactile?.playTap()
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.profile_change_photo),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.primaryLip
                                )
                            }
                            if (avatarBitmap != null) {
                                TextButton(
                                    onClick = {
                                        tactile?.playTap()
                                        onClearProfilePhoto()
                                    }
                                ) {
                                    Text(
                                        text = "Reset Logo",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = NunitoDisplayFamily),
                                color = palette.textPrimary
                            )
                            IconButton(
                                onClick = {
                                    tactile?.playTap()
                                    showEditNameDialog = true
                                },
                                modifier = Modifier.testTag("edit_name_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit display name",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        val pointsTier = remember(profile.focusXp) { PlayPointsTier.forPoints(profile.focusXp) }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primarySoft
                        ) {
                            Text(
                                text = "Level ${profile.level} • ${pointsTier.displayName} Tier • ${badge.displayName}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.primaryLip,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Play Points Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Text(
                                text = "${profile.focusXp} pts (${profile.xpInCurrentLevel} / 250 next lv)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { profile.levelProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(50)),
                            color = palette.primary,
                            trackColor = palette.glassBorderSubtle
                        )
                    }
                }
            }
        }

        // 3 Equal-Width Stat Tiles
        item {
            StaggeredEntrance(index = 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatTile(
                        icon = Icons.Rounded.Schedule,
                        value = "${profile.totalFocusMinutes}m",
                        label = "Focused",
                        accent = palette.primary,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatTile(
                        icon = Icons.Rounded.LocalFireDepartment,
                        value = "${profile.longestStreak}d",
                        label = "Best Streak",
                        accent = palette.streakColor,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatTile(
                        icon = Icons.Rounded.Diamond,
                        value = "${profile.gems}",
                        label = "Gems",
                        accent = palette.gemColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Full Scholar Profile Details Card (Bio, School/Course, Goal, Shift, Phone, Email)
        item {
            StaggeredEntrance(index = 2) {
                ScholarProfileDetailsCard(
                    details = scholarDetails,
                    onEditDetails = { showEditDetailsDialog = true }
                )
            }
        }

        // Compact, Hidden-by-Default Custom App Icon Studio Card
        item {
            StaggeredEntrance(index = 2) {
                CustomAppIconStudioCard(
                    selectedIcon = preferences.customAppIconId,
                    selectedShape = preferences.customIconShapeId,
                    onSelectIcon = onSelectCustomIcon,
                    onSelectShape = onSelectCustomIconShape,
                    onShortcutFeedback = onCustomShortcutFeedback
                )
            }
        }

        // Daily Streak Push Notification Card (Firebase Cloud Messaging)
        item {
            StaggeredEntrance(index = 3) {
                DailyStreakReminderCard(
                    status = streakReminderStatus,
                    currentStreak = profile.currentStreak,
                    onToggleEnabled = onToggleStreakReminder,
                    onSelectTime = onSelectStreakReminderTime,
                    onRequestPermission = onRequestNotificationPermission,
                    onSendTestAlert = onSendTestStreakReminder,
                    compact = false
                )
            }
        }

        // Tactile Studio & Study Goal Preferences Card
        item {
            StaggeredEntrance(index = 3) {
                MorphismSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Feel & Study Preferences",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Focus Goal",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "${goalSlider.roundToInt()} min / day",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.primaryLip
                                )
                            }
                            Slider(
                                value = goalSlider,
                                onValueChange = { goalSlider = ((it / 15f).roundToInt() * 15).coerceIn(15, 360).toFloat() },
                                onValueChangeFinished = { onUpdateDailyGoal(goalSlider.roundToInt()) },
                                valueRange = 15f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primary,
                                    activeTrackColor = palette.primary,
                                    inactiveTrackColor = palette.glassBorderSubtle
                                )
                            )
                        }

                        Column {
                            val weeklyInt = weeklyGoalSlider.roundToInt()
                            val weeklyLabel = if (weeklyInt % 60 == 0) {
                                "${weeklyInt / 60}h / week"
                            } else {
                                "${weeklyInt / 60}h ${weeklyInt % 60}m / week"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Weekly Focus Goal",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = weeklyLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.primaryLip
                                )
                            }
                            Slider(
                                value = weeklyGoalSlider,
                                onValueChange = { weeklyGoalSlider = ((it / 30f).roundToInt() * 30).coerceIn(60, 2100).toFloat() },
                                onValueChangeFinished = { onUpdateWeeklyGoal(weeklyGoalSlider.roundToInt()) },
                                valueRange = 60f..2100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primary,
                                    activeTrackColor = palette.primary,
                                    inactiveTrackColor = palette.glassBorderSubtle
                                ),
                                modifier = Modifier.testTag("profile_weekly_goal_slider")
                            )
                        }

                        PreferenceToggleRow(
                            icon = Icons.Rounded.TouchApp,
                            title = "Tactile Click Sounds",
                            subtitle = "Crisp glass tap & reward chimes (silent on app open/close)",
                            checked = preferences.uiClickSoundsEnabled,
                            onCheckedChange = onToggleUiSounds,
                            testTag = "toggle_ui_sounds"
                        )

                        PreferenceToggleRow(
                            icon = Icons.Rounded.Vibration,
                            title = "Haptic Touch",
                            subtitle = "Subtle micro-vibrations on button press",
                            checked = preferences.hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            testTag = "toggle_haptics"
                        )

                        PreferenceToggleRow(
                            icon = Icons.Rounded.VolumeUp,
                            title = "Auto-Play Ambient Audio",
                            subtitle = "Automatically play & pause focus sounds with timer",
                            checked = preferences.ambientAutoPlayWithTimer,
                            onCheckedChange = onToggleAutoPlayAmbient,
                            testTag = "toggle_autoplay_ambient"
                        )

                        // Custom Session Completion Chime Picker
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = palette.primaryLip,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Session Complete Chime (plays even in background)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CompletionSoundPreset.entries) { preset ->
                                    MorphismChip(
                                        label = preset.displayName,
                                        selected = preferences.completionSound == preset,
                                        onClick = { onSelectCompletionSound(preset) },
                                        testTag = "profile_chime_${preset.name.lowercase()}"
                                    )
                                }
                            }
                        }

                        PreferenceToggleRow(
                            icon = Icons.Rounded.CalendarMonth,
                            title = stringResource(R.string.report_pdf_weekend_toggle),
                            subtitle = stringResource(R.string.report_pdf_weekend_desc),
                            checked = preferences.weekendPdfReminderEnabled,
                            onCheckedChange = onToggleWeekendPdf,
                            testTag = "toggle_weekend_pdf_profile"
                        )

                        MorphismActionButton(
                            text = if (isExportingPdf) "GENERATING PDF…" else stringResource(R.string.report_pdf_export_cta),
                            icon = Icons.Rounded.PictureAsPdf,
                            onClick = onExportWeeklyPdf,
                            enabled = !isExportingPdf,
                            containerColor = palette.gemColor,
                            lipColor = palette.gemLip,
                            height = 46.dp,
                            cornerRadius = 15.dp,
                            testTag = "profile_export_pdf_button"
                        )
                    }
                }
            }
        }

        // Cloud Sync Card (Google Sign-In via Credential Manager)
        item {
            StaggeredEntrance(index = 4) {
                MorphismSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp,
                    accentTint = palette.gemColor,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (userEmail != null) Icons.Rounded.CloudDone else Icons.Rounded.CloudQueue,
                                contentDescription = null,
                                tint = if (userEmail != null) palette.primary else palette.gemColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (userEmail != null) "Cloud Sync Active" else "Backup & Cloud Sync",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = userEmail ?: "On-device mode active — sign in with Google to sync gems & unlocks across devices",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        if (!authError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.coralAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = authError,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.coralAccent,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (userEmail == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MorphismActionButton(
                                    text = if (isSigningIn) "SIGNING IN…" else stringResource(R.string.profile_sign_in_google).uppercase(),
                                    onClick = onGoogleSignIn,
                                    enabled = !isSigningIn,
                                    containerColor = palette.gemColor,
                                    lipColor = palette.gemLip,
                                    height = 48.dp,
                                    testTag = "google_sign_in_button"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MorphismActionButton(
                                        text = stringResource(R.string.profile_auth_email_zoho),
                                        icon = Icons.Rounded.Email,
                                        onClick = {
                                            onClearAuthError()
                                            showEmailDialog = true
                                        },
                                        enabled = !isSigningIn,
                                        containerColor = palette.primary,
                                        lipColor = palette.primaryLip,
                                        height = 44.dp,
                                        cornerRadius = 14.dp,
                                        modifier = Modifier.weight(1f),
                                        testTag = "email_zoho_sign_in_button"
                                    )
                                    MorphismActionButton(
                                        text = stringResource(R.string.profile_auth_phone_otp),
                                        icon = Icons.Rounded.PhoneAndroid,
                                        onClick = {
                                            onClearAuthError()
                                            showPhoneOtpDialog = true
                                        },
                                        enabled = !isSigningIn,
                                        containerColor = palette.streakColor,
                                        lipColor = palette.streakLip,
                                        height = 44.dp,
                                        cornerRadius = 14.dp,
                                        modifier = Modifier.weight(1f),
                                        testTag = "phone_otp_sign_in_button"
                                    )
                                }
                                MorphismActionButton(
                                    text = "CONTINUE WITH ${stringResource(R.string.profile_auth_github).uppercase()}",
                                    icon = Icons.Rounded.Code,
                                    onClick = onGitHubSignIn,
                                    enabled = !isSigningIn,
                                    containerColor = Color(0xFF1F2937),
                                    lipColor = Color(0xFF111827),
                                    height = 44.dp,
                                    cornerRadius = 14.dp,
                                    testTag = "github_sign_in_button"
                                )
                            }
                        } else {
                            MorphismActionButton(
                                text = stringResource(R.string.profile_sign_out).uppercase(),
                                icon = Icons.AutoMirrored.Rounded.Logout,
                                onClick = onSignOut,
                                containerColor = palette.coralAccent,
                                lipColor = Color(0xFFD13232),
                                height = 46.dp,
                                testTag = "google_sign_out_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatTile(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    MorphismSurface(
        modifier = modifier,
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = NunitoDisplayFamily),
                color = palette.textPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    icon: ImageVector,
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
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.chipUnselected),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.primaryLip,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = palette.textMuted,
                uncheckedTrackColor = palette.chipUnselected
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
