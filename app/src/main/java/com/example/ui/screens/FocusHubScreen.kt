package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.AccountModerationRecord
import com.example.data.model.AdminNotice
import com.example.data.model.AmbientLightingStyle
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.AppLiveConfig
import com.example.data.model.AppPreferencesState
import com.example.data.model.CompletionSoundPreset
import com.example.data.model.DailyUsagePoint
import com.example.data.model.DayFocusPoint
import com.example.data.model.FocusSession
import com.example.data.model.GeminiInsightsUiState
import com.example.data.model.HapticIntensityLevel
import com.example.data.model.MascotBadgeId
import com.example.data.model.PathMilestone
import com.example.data.model.PomodoroPhase
import com.example.data.model.PomodoroTask
import com.example.data.model.ScreenUsageUiState
import com.example.data.model.StreakDayProgress
import com.example.data.model.StreakReminderUiStatus
import com.example.data.model.StudyGoalPeriod
import com.example.data.model.SubjectStudyAlarm
import com.example.data.model.TimerUiState
import com.example.data.model.UserProfile
import com.example.ui.components.AtmosphereCustomizationDrawer
import com.example.ui.components.CustomAppIconEmblem
import com.example.ui.components.DailyStreakReminderCard
import com.example.ui.components.DailyUsageBarChartCard
import com.example.ui.components.FocusStreaksTrackerCard
import com.example.ui.components.GemPillBadge
import com.example.ui.components.GeminiInsightsCard
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.components.LocalSystemHaptics
import com.example.ui.components.LocalTactileSound
import com.example.ui.components.MorphismActionButton
import com.example.ui.components.MorphismChip
import com.example.ui.components.MorphismSurface
import com.example.ui.components.ScreenUsageCard
import com.example.ui.components.SilentLiveUpdateFeedCard
import com.example.ui.components.SlidingGlassSegmentedBar
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StreakPillBadge
import com.example.ui.components.StudyGoalProgressCard
import com.example.ui.components.StudyHistoryCard
import com.example.ui.components.SubjectStudyAlarmsCard
import com.example.ui.components.TryAgainFocusRingCard
import com.example.ui.components.WeeklyPdfReportCard
import com.example.ui.components.WeeklySummaryDashboardCard
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

private enum class FocusWorkspaceMode {
    FOCUS,
    INSIGHTS
}

@Composable
fun FocusHubScreen(
    profile: UserProfile,
    timerState: TimerUiState,
    preferences: AppPreferencesState,
    streakReminderStatus: StreakReminderUiStatus,
    tasks: List<PomodoroTask>,
    streakDays: List<StreakDayProgress>,
    screenUsageState: ScreenUsageUiState,
    dailyUsagePoints: List<DailyUsagePoint>,
    weeklyPoints: List<DayFocusPoint>,
    todayFocusMinutes: Int,
    weeklyFocusMinutes: Int,
    activeAmbient: AmbientSoundPreset,
    isAmbientPlaying: Boolean,
    ambientWaveAmplitude: Float,
    geminiInsightsState: GeminiInsightsUiState,
    sessionsCount: Int,
    onSelectGoalPeriod: (StudyGoalPeriod) -> Unit,
    onSaveStudyGoals: (StudyGoalPeriod, Int, Int) -> Unit,
    onGenerateGeminiInsights: () -> Unit,
    onToggleStreakReminder: (Boolean) -> Unit,
    onSelectStreakReminderTime: (Int, Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestStreakReminder: () -> Unit,
    onSelectSubject: (String) -> Unit,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onSetDuration: (Int) -> Unit,
    onAdjustDuration: (Int) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onCompleteOrSkip: () -> Unit,
    onToggleAmbient: (AmbientSoundPreset) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenAmbientSoundsScreen: () -> Unit,
    onSelectLightingStyle: (AmbientLightingStyle) -> Unit,
    onLightingIntensityChange: (Float) -> Unit,
    onToggleEdgeGlow: (Boolean) -> Unit,
    onSelectHapticIntensity: (HapticIntensityLevel) -> Unit,
    onToggleUiSounds: (Boolean) -> Unit,
    onSelectCompletionSound: (CompletionSoundPreset) -> Unit,
    onOpenShop: () -> Unit,
    onOpenIconStudio: () -> Unit = {},
    onRefreshScreenUsage: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (PomodoroTask) -> Unit,
    onPinTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    goalStreakDays: Int = 0,
    subjectAlarms: List<SubjectStudyAlarm> = emptyList(),
    onAddSubjectAlarm: (String, Int, Int, Int, String) -> Unit = { _, _, _, _, _ -> },
    onToggleSubjectAlarm: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteSubjectAlarm: (String) -> Unit = {},
    onApplySubjectAlarmNow: (String, Int) -> Unit = { _, _ -> },
    sessions: List<FocusSession> = emptyList(),
    milestones: List<PathMilestone> = emptyList(),
    weekendPdfEnabled: Boolean = true,
    isExportingPdf: Boolean = false,
    hideStudyHistory: Boolean = false,
    onExportWeeklyPdf: () -> Unit = {},
    onToggleWeekendPdf: (Boolean) -> Unit = {},
    onToggleHideStudyHistory: (Boolean) -> Unit = {},
    onClaimMilestone: (PathMilestone) -> Unit = {},
    liveAppConfig: AppLiveConfig = AppLiveConfig(),
    adminNotices: List<AdminNotice> = emptyList(),
    accountModeration: AccountModerationRecord? = null,
    isVerifiedAdmin: Boolean = false,
    onOpenAdminPanel: () -> Unit = {}
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current
    val equippedBadge = remember(profile.equippedBadge) { MascotBadgeId.fromWire(profile.equippedBadge) }
    val activeTaskTitle = remember(tasks, timerState.activeTaskId) {
        tasks.firstOrNull { it.id == timerState.activeTaskId }?.title
    }

    var workspaceMode by rememberSaveable { mutableStateOf(FocusWorkspaceMode.FOCUS) }
    var showSafeResetDialog by remember { mutableStateOf(false) }
    var showAtmosphereDrawer by remember { mutableStateOf(false) }
    var dismissedNoticeIds by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val latestVisibleNotice = remember(adminNotices, dismissedNoticeIds) {
        adminNotices.firstOrNull { it.id !in dismissedNoticeIds }
    }

    if (workspaceMode != FocusWorkspaceMode.FOCUS) {
        BackHandler {
            workspaceMode = FocusWorkspaceMode.FOCUS
        }
    }

    val companionMotion = rememberInfiniteTransition(label = "monkeyBob")
    val monkeyBobY by companionMotion.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "monkeyBobY"
    )

    val friendlyLine = remember(
        timerState.isRunning,
        timerState.isPaused,
        timerState.phase,
        timerState.subject,
        profile.displayName,
        liveAppConfig.liveTagline
    ) {
        when {
            timerState.isRunning && timerState.phase == PomodoroPhase.FOCUS ->
                "Deep flow on ${timerState.subject}"
            timerState.isRunning ->
                "Breathe & recharge, ${profile.displayName}"
            timerState.isPaused ->
                "Paused & streak protected"
            liveAppConfig.liveTagline.isNotBlank() ->
                liveAppConfig.liveTagline
            else ->
                "Ready for a calm ${timerState.plannedMinutes}m sprint"
        }
    }

    if (showAtmosphereDrawer) {
        AtmosphereCustomizationDrawer(
            preferences = preferences,
            onSelectLightingStyle = onSelectLightingStyle,
            onLightingIntensityChange = onLightingIntensityChange,
            onToggleEdgeGlow = onToggleEdgeGlow,
            onSelectHapticIntensity = onSelectHapticIntensity,
            onToggleUiSounds = onToggleUiSounds,
            onSelectCompletionSound = onSelectCompletionSound,
            onOpenAmbientDrawer = {
                showAtmosphereDrawer = false
                onOpenAmbientSoundsScreen()
            },
            onOpenIconStudio = onOpenIconStudio,
            onDismiss = { showAtmosphereDrawer = false }
        )
    }

    if (showSafeResetDialog) {
        SafeResetSessionDialog(
            elapsedMinutes = timerState.elapsedMinutes.coerceAtLeast(1),
            subject = timerState.subject,
            onSaveAndKeepStreak = {
                showSafeResetDialog = false
                onCompleteOrSkip()
            },
            onResumeSprint = {
                showSafeResetDialog = false
                if (!timerState.isRunning) onStartOrResume()
            },
            onDiscardSprint = {
                showSafeResetDialog = false
                onReset()
            },
            onDismiss = { showSafeResetDialog = false }
        )
    }

    val availableModes = remember { listOf(FocusWorkspaceMode.FOCUS, FocusWorkspaceMode.INSIGHTS) }
    val workspaceLabels = availableModes.map { mode ->
        when (mode) {
            FocusWorkspaceMode.FOCUS -> stringResource(R.string.workspace_tab_focus)
            FocusWorkspaceMode.INSIGHTS -> stringResource(R.string.workspace_tab_insights)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sleek Uncluttered Liquid-Glass Command Bar
        item {
            StaggeredEntrance(index = 0) {
                LiquidGlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_liquid_glass_header"),
                    cornerRadius = 26.dp,
                    elevated = true,
                    accentTint = if (timerState.isRunning) palette.primary else palette.prismCyan,
                    animatedCaustics = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
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
                                    .offset(y = monkeyBobY.dp)
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("main_custom_icon_button")
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        systemHaptics.buttonTap()
                                        tactile?.playTap()
                                        onOpenIconStudio()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CustomAppIconEmblem(
                                    iconId = preferences.customAppIconId,
                                    shapeId = preferences.customIconShapeId,
                                    size = 36.dp,
                                    contentDescription = "Customize Try Again app icon"
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Try Again",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = friendlyLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isVerifiedAdmin) {
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("header_admin_panel_button")
                                        .clip(CircleShape)
                                        .clickable {
                                            systemHaptics.confirm()
                                            tactile?.playTap()
                                            onOpenAdminPanel()
                                        },
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B).copy(alpha = if (palette.isDark) 0.24f else 0.16f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.AdminPanelSettings,
                                            contentDescription = "Open Admin Control Center",
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("header_atmosphere_button")
                                    .clip(CircleShape)
                                    .clickable {
                                        systemHaptics.buttonTap()
                                        tactile?.playTap()
                                        showAtmosphereDrawer = true
                                    },
                                shape = CircleShape,
                                color = palette.prismViolet.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                                border = BorderStroke(1.dp, palette.prismViolet.copy(alpha = 0.35f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Customize ambient lighting and haptics",
                                        tint = palette.prismViolet,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            StreakPillBadge(streakDays = maxOf(goalStreakDays, profile.currentStreak))
                            GemPillBadge(
                                gems = profile.gems,
                                onClick = onOpenShop,
                                modifier = Modifier.testTag("top_gem_pill")
                            )
                        }
                    }
                }
            }
        }

        // 2. Calm Liquid-Glass Workspace Switcher (Focus | Insights)
        item {
            StaggeredEntrance(index = 1) {
                SlidingGlassSegmentedBar(
                    labels = workspaceLabels,
                    selectedIndex = availableModes.indexOf(workspaceMode).coerceAtLeast(0),
                    onSelect = { idx -> workspaceMode = availableModes[idx] },
                    enabled = true,
                    tagPrefix = "focus_workspace_tab"
                )
            }
        }

        // Silent Live Background App Updates & Official Admin Notices (zero loading spinners)
        if (liveAppConfig.hasVisibleAnnouncement ||
            liveAppConfig.maintenanceBannerEnabled ||
            latestVisibleNotice != null ||
            accountModeration?.isRemoved == true
        ) {
            item {
                SilentLiveUpdateFeedCard(
                    liveConfig = liveAppConfig,
                    latestNotice = latestVisibleNotice,
                    accountModeration = accountModeration,
                    onDismissNoticeId = { id ->
                        dismissedNoticeIds = (dismissedNoticeIds + id).distinct()
                    }
                )
            }
        }

        when (workspaceMode) {
            FocusWorkspaceMode.FOCUS -> {
                // Consecutive Goal-Met Focus Streak Fire Counter Banner
                item {
                    StaggeredEntrance(index = 2) {
                        FocusGoalStreakCounterBanner(
                            goalStreakDays = goalStreakDays,
                            activeStreakDays = profile.currentStreak,
                            todayMinutes = todayFocusMinutes,
                            dailyGoalMinutes = profile.dailyGoalMinutes,
                            onClick = { workspaceMode = FocusWorkspaceMode.INSIGHTS }
                        )
                    }
                }

                // Clean Subject Selector Pills
                item {
                    StaggeredEntrance(index = 2) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(profile.subjects.ifEmpty { UserProfile.DEFAULT_SUBJECTS }) { subject ->
                                MorphismChip(
                                    label = subject,
                                    selected = timerState.subject == subject,
                                    onClick = { onSelectSubject(subject) },
                                    testTag = "subject_chip_${subject.lowercase()}"
                                )
                            }
                        }
                    }
                }

                // Hero Liquid-Glass Circular Pomodoro Timer Card (with 1-tap Soundscapes Drawer bar)
                item {
                    StaggeredEntrance(index = 3) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TryAgainFocusRingCard(
                                timerState = timerState,
                                equippedBadge = equippedBadge,
                                activeTaskTitle = activeTaskTitle,
                                activeAmbient = activeAmbient,
                                isAmbientPlaying = isAmbientPlaying,
                                ambientWaveAmplitude = ambientWaveAmplitude,
                                gemMultiplierPercent = liveAppConfig.gemMultiplierPercent,
                                showAmbientDrawerTrigger = liveAppConfig.ambientDrawerEnabled,
                                onSelectPhase = onSelectPhase,
                                onSetDuration = onSetDuration,
                                onAdjustDuration = onAdjustDuration,
                                onStartOrResume = onStartOrResume,
                                onPause = onPause,
                                onReset = {
                                    if (timerState.hasSignificantFocusProgress) {
                                        if (timerState.isRunning) onPause()
                                        showSafeResetDialog = true
                                    } else {
                                        onReset()
                                    }
                                },
                                onCompleteOrSkip = onCompleteOrSkip,
                                onOpenAmbientDrawer = onOpenAmbientSoundsScreen,
                                onQuickToggleAmbient = { onToggleAmbient(activeAmbient) }
                            )
                            AnimatedVisibility(visible = timerState.isRunning || timerState.isPaused) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.glassFillElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (timerState.isPaused) palette.streakColor.copy(alpha = 0.34f)
                                        else palette.primary.copy(alpha = 0.28f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (timerState.isPaused) {
                                                Icon(
                                                    imageVector = Icons.Rounded.VerifiedUser,
                                                    contentDescription = null,
                                                    tint = palette.streakColor,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Text(
                                                text = if (timerState.isRunning) {
                                                    stringResource(R.string.focus_running_status_line)
                                                } else {
                                                    stringResource(R.string.focus_paused_status_line)
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = palette.textSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Chime: ${preferences.completionSound.displayName}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.primaryLip
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Multi-Task Per-Subject Study Alarms Card (Math 6:00 AM, Science 7:00 AM, etc.)
                item {
                    StaggeredEntrance(index = 4) {
                        SubjectStudyAlarmsCard(
                            alarms = subjectAlarms,
                            subjects = profile.subjects.ifEmpty { UserProfile.DEFAULT_SUBJECTS },
                            onAddAlarm = onAddSubjectAlarm,
                            onToggleAlarm = onToggleSubjectAlarm,
                            onDeleteAlarm = onDeleteSubjectAlarm,
                            onLoadSubjectNow = onApplySubjectAlarmNow
                        )
                    }
                }

                // Clean 2-Tile Quick Utility Strip (Ambient Sounds | Lighting & Haptics)
                item {
                    StaggeredEntrance(index = 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickStudioPillTile(
                                title = if (isAmbientPlaying) activeAmbient.displayName else "Ambient Sounds",
                                subtitle = if (isAmbientPlaying) "Playing · Local" else "Rain, White Noise & more",
                                icon = Icons.Rounded.GraphicEq,
                                accent = palette.gemColor,
                                onClick = onOpenAmbientSoundsScreen,
                                testTag = "quick_tile_sounds",
                                modifier = Modifier.weight(1f)
                            )
                            QuickStudioPillTile(
                                title = "Lighting & Haptics",
                                subtitle = preferences.hapticIntensity.displayName,
                                icon = Icons.Rounded.AutoAwesome,
                                accent = palette.prismViolet,
                                onClick = { showAtmosphereDrawer = true },
                                testTag = "quick_tile_atmosphere",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Clean Minimal Study Tasks Card
                item {
                    StaggeredEntrance(index = 5) {
                        StudyTasksGlassCard(
                            tasks = tasks,
                            activeTaskId = timerState.activeTaskId,
                            onAddTask = onAddTask,
                            onToggleTask = onToggleTask,
                            onPinTask = { id ->
                                tactile?.playTap()
                                onPinTask(id)
                            },
                            onDeleteTask = onDeleteTask
                        )
                    }
                }
            }

            FocusWorkspaceMode.INSIGHTS -> {
                // Consecutive Goal-Met Focus Streak Fire Counter
                item {
                    StaggeredEntrance(index = 0) {
                        FocusGoalStreakCounterBanner(
                            goalStreakDays = goalStreakDays,
                            activeStreakDays = profile.currentStreak,
                            todayMinutes = todayFocusMinutes,
                            dailyGoalMinutes = profile.dailyGoalMinutes,
                            onClick = {}
                        )
                    }
                }

                // Daily & Weekly Study Goals Progress Card
                item {
                    StaggeredEntrance(index = 1) {
                        StudyGoalProgressCard(
                            todayMinutes = todayFocusMinutes,
                            dailyGoalMinutes = profile.dailyGoalMinutes,
                            weeklyMinutes = weeklyFocusMinutes,
                            weeklyGoalMinutes = preferences.weeklyGoalMinutes,
                            selectedPeriod = preferences.selectedGoalPeriod,
                            onSelectPeriod = onSelectGoalPeriod,
                            onSaveStudyGoals = onSaveStudyGoals,
                            sprintMinutesReference = timerState.focusMinutesSetting
                        )
                    }
                }

                // Interactive 7-Day Weekly Summary Dashboard (Moved from Path)
                item {
                    StaggeredEntrance(index = 2) {
                        WeeklySummaryDashboardCard(
                            weeklyPoints = weeklyPoints,
                            dailyGoalMinutes = profile.dailyGoalMinutes
                        )
                    }
                }

                // Focus Streaks Day-by-Day Tracker + 14-Day Habit Consistency Ribbon (Moved from Path)
                item {
                    StaggeredEntrance(index = 3) {
                        FocusStreaksTrackerCard(
                            currentStreak = profile.currentStreak,
                            longestStreak = profile.longestStreak,
                            streakDays = streakDays,
                            showTwoWeekRibbon = true
                        )
                    }
                }

                // Daily FCM Streak Push Notification Guard Card
                item {
                    StaggeredEntrance(index = 4) {
                        DailyStreakReminderCard(
                            status = streakReminderStatus,
                            currentStreak = profile.currentStreak,
                            onToggleEnabled = onToggleStreakReminder,
                            onSelectTime = onSelectStreakReminderTime,
                            onRequestPermission = onRequestNotificationPermission,
                            onSendTestAlert = onSendTestStreakReminder,
                            compact = true
                        )
                    }
                }

                // Personalized Gemini Productivity Insights Card
                item {
                    StaggeredEntrance(index = 4) {
                        GeminiInsightsCard(
                            insightsState = geminiInsightsState,
                            sessionsCount = sessionsCount,
                            onGenerateInsights = onGenerateGeminiInsights
                        )
                    }
                }

                // Weekly Study PDF Report & Weekend Delivery Card (Moved from Path)
                item {
                    StaggeredEntrance(index = 5) {
                        WeeklyPdfReportCard(
                            weeklyPoints = weeklyPoints,
                            sessions = sessions,
                            weekendAutoReportEnabled = weekendPdfEnabled,
                            isExportingPdf = isExportingPdf,
                            onExportAndSendPdf = onExportWeeklyPdf,
                            onToggleWeekendAutoReport = onToggleWeekendPdf
                        )
                    }
                }

                // Recent Study History Card with Hide Toggle (Moved from Path)
                item {
                    StaggeredEntrance(index = 5) {
                        StudyHistoryCard(
                            sessions = sessions,
                            isHidden = hideStudyHistory,
                            onToggleHidden = onToggleHideStudyHistory
                        )
                    }
                }

                // Screen Time & App Usage Card
                item {
                    StaggeredEntrance(index = 6) {
                        ScreenUsageCard(
                            usageState = screenUsageState,
                            todayFocusMinutes = todayFocusMinutes,
                            onRefreshUsage = onRefreshScreenUsage,
                            onRequestUsageAccess = onRequestUsageAccess
                        )
                    }
                }

                // Daily Focus vs. Distraction Bar Chart Card
                item {
                    StaggeredEntrance(index = 6) {
                        DailyUsageBarChartCard(
                            dailyPoints = dailyUsagePoints,
                            hasUsagePermission = screenUsageState.hasUsagePermission
                        )
                    }
                }

                // Ambient Quick Strip Card
                item {
                    StaggeredEntrance(index = 7) {
                        AmbientQuickStripCard(
                            unlockedSounds = profile.unlockedSounds,
                            activeAmbient = activeAmbient,
                            isPlaying = isAmbientPlaying,
                            volume = preferences.ambientVolume,
                            onSelectSound = { preset ->
                                val unlocked = preset.gemCost == 0 || preset.name in profile.unlockedSounds
                                if (unlocked) onToggleAmbient(preset) else onOpenAmbientSoundsScreen()
                            },
                            onVolumeChange = onVolumeChange,
                            onOpenDrawer = onOpenAmbientSoundsScreen
                        )
                    }
                }

                // Full Focus Milestone Path & Gem Chests (Moved from Path into Insights!)
                if (milestones.isNotEmpty()) {
                    val firstPendingIdx = milestones.indexOfFirst { !it.isClaimed }
                        .takeIf { it >= 0 } ?: milestones.lastIndex
                    item {
                        StaggeredEntrance(index = 7) {
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                Text(
                                    text = stringResource(R.string.journey_title),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Complete study milestones along your path to claim bonus Gem chests",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                    itemsIndexed(milestones, key = { _, item -> "insights_path_${item.id}" }) { index, milestone ->
                        StaggeredEntrance(index = (index % 5) + 2) {
                            MilestonePathRow(
                                milestone = milestone,
                                stepNumber = index + 1,
                                isCurrent = index == firstPendingIdx,
                                isLast = index == milestones.lastIndex,
                                onClaim = { onClaimMilestone(milestone) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusGoalStreakCounterBanner(
    goalStreakDays: Int,
    activeStreakDays: Int,
    todayMinutes: Int,
    dailyGoalMinutes: Int,
    onClick: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val safeGoal = dailyGoalMinutes.coerceAtLeast(15)
    val goalReached = todayMinutes >= safeGoal
    val remaining = (safeGoal - todayMinutes).coerceAtLeast(0)
    val progress = (todayMinutes.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val displayStreak = maxOf(goalStreakDays, activeStreakDays)

    LiquidGlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_goal_streak_banner")
            .clickable(onClick = onClick),
        cornerRadius = 22.dp,
        elevated = goalReached,
        accentTint = palette.streakColor,
        animatedCaustics = false,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        shape = CircleShape,
                        color = palette.streakColor.copy(alpha = if (palette.isDark) 0.24f else 0.16f),
                        border = BorderStroke(1.2.dp, palette.streakColor.copy(alpha = 0.50f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "Focus Streak Fire Counter",
                                tint = palette.streakColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$displayStreak-Day Focus Streak",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary
                            )
                            if (goalStreakDays > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = palette.streakColor.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "${goalStreakDays}d Goal",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = palette.streakLip,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (goalReached) {
                                stringResource(R.string.focus_streak_goal_met_today)
                            } else {
                                stringResource(R.string.focus_streak_goal_remaining, remaining)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "${todayMinutes}/${safeGoal}m",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (goalReached) palette.primaryLip else palette.streakColor
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (goalReached) palette.primary else palette.streakColor,
                trackColor = palette.glassBorderSubtle
            )
        }
    }
}

@Composable
private fun QuickStudioPillTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(22.dp))
            .clickable {
                tactile?.playTap()
                onClick()
            },
        shape = RoundedCornerShape(22.dp),
        color = palette.glassFillElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SafeResetSessionDialog(
    elapsedMinutes: Int,
    subject: String,
    onSaveAndKeepStreak: () -> Unit,
    onResumeSprint: () -> Unit,
    onDiscardSprint: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    Dialog(onDismissRequest = onDismiss) {
        MorphismSurface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("safe_reset_dialog"),
            cornerRadius = 28.dp,
            elevated = true,
            accentTint = palette.streakColor,
            frostedBlur = true,
            blurRadius = 20.dp,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.streakColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, palette.streakColor.copy(alpha = 0.4f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Streak protection shield",
                            tint = palette.streakColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.focus_reset_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = palette.textPrimary
                )

                Text(
                    text = stringResource(R.string.focus_reset_dialog_body, elapsedMinutes, subject),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                MorphismActionButton(
                    text = stringResource(R.string.focus_reset_save_and_keep),
                    icon = Icons.Rounded.CheckCircle,
                    onClick = onSaveAndKeepStreak,
                    containerColor = palette.primary,
                    lipColor = palette.primaryLip,
                    testTag = "safe_reset_save_button"
                )

                MorphismActionButton(
                    text = stringResource(R.string.focus_reset_resume),
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onResumeSprint,
                    containerColor = palette.gemColor,
                    lipColor = palette.gemLip,
                    testTag = "safe_reset_resume_button"
                )

                TextButton(
                    onClick = {
                        tactile?.playTap()
                        onDiscardSprint()
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("safe_reset_discard_button")
                ) {
                    Text(
                        text = stringResource(R.string.focus_reset_discard),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = palette.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientQuickStripCard(
    unlockedSounds: List<String>,
    activeAmbient: AmbientSoundPreset,
    isPlaying: Boolean,
    volume: Float,
    onSelectSound: (AmbientSoundPreset) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val waveAnim = rememberInfiniteTransition(label = "ambientWave")
    val barPulse by waveAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barPulse"
    )

    LiquidGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        elevated = isPlaying,
        accentTint = if (isPlaying) palette.primary else palette.prismCyan,
        animatedCaustics = isPlaying,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (isPlaying) palette.primary else palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.focus_ambient_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeAmbient != AmbientSoundPreset.OFF) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSelectSound(activeAmbient) },
                            shape = RoundedCornerShape(50),
                            color = if (isPlaying) palette.primarySoft else palette.chipUnselected
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause sound" else "Play sound",
                                    tint = if (isPlaying) palette.primaryLip else palette.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeAmbient.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isPlaying) palette.primaryLip else palette.textPrimary
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .testTag("ambient_strip_open_drawer_button")
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                tactile?.playTap()
                                onOpenDrawer()
                            },
                        shape = RoundedCornerShape(50),
                        color = palette.primarySoft,
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.32f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = palette.primaryLip,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.ambient_drawer_all_sounds),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.primaryLip
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AmbientSoundPreset.unlockableEntries) { preset ->
                    val unlocked = preset.gemCost == 0 || preset.name in unlockedSounds
                    val active = activeAmbient == preset && isPlaying
                    MorphismChip(
                        label = if (unlocked) preset.displayName else "${preset.displayName} • ${preset.gemCost}",
                        leadingIcon = when {
                            !unlocked -> Icons.Rounded.Lock
                            active -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        selected = active,
                        onClick = { onSelectSound(preset) },
                        testTag = "ambient_chip_${preset.name.lowercase()}"
                    )
                }
            }

            AnimatedVisibility(visible = isPlaying || activeAmbient != AmbientSoundPreset.OFF) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Ambient volume",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0.05f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary,
                            inactiveTrackColor = palette.glassBorderSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isPlaying) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(20.dp)
                        ) {
                            listOf(0.55f, 1f, 0.72f, 0.9f).forEachIndexed { idx, factor ->
                                val phase = if (idx % 2 == 0) barPulse else (1.3f - barPulse).coerceIn(0.28f, 1f)
                                val h = (5f + 14f * factor * phase).dp
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h)
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyTasksGlassCard(
    tasks: List<PomodoroTask>,
    activeTaskId: String?,
    onAddTask: (String) -> Unit,
    onToggleTask: (PomodoroTask) -> Unit,
    onPinTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    var draft by remember { mutableStateOf("") }

    LiquidGlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        elevated = false,
        accentTint = palette.gemColor,
        animatedCaustics = false,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.focus_tasks_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    text = "+5 Gems per task",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.gemColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.focus_add_task),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.glassBorderSubtle,
                        focusedContainerColor = palette.glassFillElevated,
                        unfocusedContainerColor = palette.glassFillElevated
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (draft.isNotBlank()) {
                            onAddTask(draft)
                            draft = ""
                        }
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_input")
                )

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("add_task_button")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = draft.isNotBlank()) {
                            onAddTask(draft)
                            draft = ""
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (draft.isNotBlank()) palette.primary else palette.chipUnselected
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add task",
                            tint = if (draft.isNotBlank()) Color.White else palette.textMuted
                        )
                    }
                }
            }

            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEach { task ->
                        val isPinned = task.id == activeTaskId
                        val checkScale by animateFloatAsState(
                            targetValue = if (task.completed) 1.15f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "taskCheckScale"
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = if (isPinned) palette.primarySoft else palette.glassFillElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isPinned) palette.primary.copy(alpha = 0.4f) else palette.glassBorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleTask(task) },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                        .testTag("task_toggle_${task.id}")
                                ) {
                                    Icon(
                                        imageVector = if (task.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = if (task.completed) "Mark incomplete" else "Mark complete",
                                        tint = if (task.completed) palette.primary else palette.textMuted,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = checkScale
                                            scaleY = checkScale
                                        }
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (task.completed) palette.textMuted else palette.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${task.subject} • ${task.completedPomodoros}/${task.estimatedPomodoros} sprints",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }

                                if (!task.completed) {
                                    IconButton(
                                        onClick = { onPinTask(task.id) },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PushPin,
                                            contentDescription = "Pin task to timer",
                                            tint = if (isPinned) palette.primaryLip else palette.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteTask(task.id) },
                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete task",
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
    }
}
