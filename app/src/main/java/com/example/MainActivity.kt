package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.GlassThemeId
import com.example.data.model.HapticIntensityLevel
import com.example.firebase.configureAppCheck
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.signInOrRegisterWithEmail
import com.example.firebase.signInWithGitHub
import com.example.firebase.signOutGoogle
import com.example.firebase.startPhoneOtpVerification
import com.example.firebase.verifyPhoneOtpCode
import com.example.service.StreakNotificationManager
import com.example.service.SubjectStudyAlarmScheduler
import com.example.ui.components.FocusSessionSummaryDialog
import com.example.ui.components.GemCelebrationBurst
import com.example.ui.components.LocalSystemHaptics
import com.example.ui.components.LocalTactileSound
import com.example.ui.components.MetamorphosisBackdrop
import com.example.ui.components.TryAgainFloatingDock
import com.example.ui.components.TryAgainSplashOverlay
import com.example.ui.components.TryAgainTab
import com.example.ui.components.TryAgainTabTransition
import com.example.ui.components.rememberSystemHapticController
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AmbientSoundsScreen
import com.example.ui.screens.FocusHubScreen
import com.example.ui.screens.GemShopScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SocialDashboardScreen
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.TryAgainTheme
import com.example.viewmodel.TryAgainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val openFocusTabEvent = MutableStateFlow(0)
    private val alarmSubjectLaunchEvent = MutableStateFlow<Pair<String, Int>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (REQUIRED): Configure App Check with launch intent before Compose UI or ViewModels.
        configureAppCheck(this, intent)
        StreakNotificationManager.ensureNotificationChannel(this)
        SubjectStudyAlarmScheduler.ensureNotificationChannel(this)
        handleLaunchIntent(intent)

        enableEdgeToEdge()
        setContent {
            TryAgainAppRoot(
                openFocusTabEvent = openFocusTabEvent,
                alarmSubjectLaunchEvent = alarmSubjectLaunchEvent
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        val alarmSubject = intent?.getStringExtra(SubjectStudyAlarmScheduler.EXTRA_ALARM_SUBJECT)
        if (!alarmSubject.isNullOrBlank()) {
            val duration = intent.getIntExtra(SubjectStudyAlarmScheduler.EXTRA_ALARM_DURATION, 25)
            alarmSubjectLaunchEvent.value = alarmSubject to duration
            openFocusTabEvent.value += 1
        } else if (intent?.getBooleanExtra(StreakNotificationManager.EXTRA_OPEN_FOCUS_FROM_REMINDER, false) == true) {
            openFocusTabEvent.value += 1
        }
    }
}

@Composable
private fun TryAgainAppRoot(
    openFocusTabEvent: MutableStateFlow<Int>,
    alarmSubjectLaunchEvent: MutableStateFlow<Pair<String, Int>?>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var phoneVerificationId by remember { mutableStateOf<String?>(null) }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val uidKey = currentUser?.uid ?: "local_scholar"

    key(uidKey) {
        val vm: TryAgainViewModel = viewModel(
            key = uidKey,
            factory = TryAgainViewModel.Factory(
                application = context.applicationContext as android.app.Application,
                isCloudSignedIn = currentUser != null
            )
        )

        val profile by vm.userProfile.collectAsStateWithLifecycle()
        val preferences by vm.preferences.collectAsStateWithLifecycle()
        val timerState by vm.timerState.collectAsStateWithLifecycle()
        val sessions by vm.sessions.collectAsStateWithLifecycle()
        val tasks by vm.tasks.collectAsStateWithLifecycle()
        val activeAmbient by vm.activeAmbientPreset.collectAsStateWithLifecycle()
        val isAmbientPlaying by vm.isAmbientPlaying.collectAsStateWithLifecycle()
        val ambientWaveAmplitude by vm.ambientWaveAmplitude.collectAsStateWithLifecycle()
        val screenUsageState by vm.screenUsageState.collectAsStateWithLifecycle()
        val dailyUsagePoints by vm.dailyUsagePoints.collectAsStateWithLifecycle()
        val todayMinutes by vm.todayMinutes.collectAsStateWithLifecycle()
        val goalStreakDays by vm.goalStreakDays.collectAsStateWithLifecycle()
        val weeklyMinutes by vm.weeklyMinutes.collectAsStateWithLifecycle()
        val weeklyPoints by vm.weeklyPoints.collectAsStateWithLifecycle()
        val streakHistoryDays by vm.streakHistoryDays.collectAsStateWithLifecycle()
        val milestones by vm.pathMilestones.collectAsStateWithLifecycle()
        val subjectAlarms by vm.subjectAlarms.collectAsStateWithLifecycle()
        val scholarDetails by vm.scholarDetails.collectAsStateWithLifecycle()
        val proUnlockStatus by vm.proUnlockStatus.collectAsStateWithLifecycle()
        val proAiState by vm.proAiState.collectAsStateWithLifecycle()
        val statusBanner by vm.statusBanner.collectAsStateWithLifecycle()
        val gemRewardBanner by vm.gemRewardBanner.collectAsStateWithLifecycle()
        val socialState by vm.socialState.collectAsStateWithLifecycle()
        val activeChatState by vm.activeChatState.collectAsStateWithLifecycle()
        val challengesState by vm.challengesState.collectAsStateWithLifecycle()
        val leaderboardState by vm.leaderboardState.collectAsStateWithLifecycle()
        val geminiInsightsState by vm.geminiInsightsState.collectAsStateWithLifecycle()
        val sessionSummaryDialog by vm.sessionSummaryDialog.collectAsStateWithLifecycle()
        val isExportingPdf by vm.isExportingPdf.collectAsStateWithLifecycle()
        val streakReminderStatus by vm.streakReminderStatus.collectAsStateWithLifecycle()
        val liveAppConfig by vm.liveAppConfig.collectAsStateWithLifecycle()
        val isAdminRole by vm.isAdminRole.collectAsStateWithLifecycle()
        val adminNotices by vm.adminNotices.collectAsStateWithLifecycle()
        val accountModeration by vm.accountModeration.collectAsStateWithLifecycle()
        val adminPanelState by vm.adminPanelState.collectAsStateWithLifecycle()
        val focusDeepLinkTick by openFocusTabEvent.collectAsStateWithLifecycle()
        val pendingAlarmLaunch by alarmSubjectLaunchEvent.collectAsStateWithLifecycle()

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {
            vm.refreshNotificationPermissionStatus()
        }

        val ensureNotificationPermissionIfNeeded: () -> Unit = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.refreshNotificationPermissionStatus()
                }
            } else {
                vm.refreshNotificationPermissionStatus()
            }
        }

        val triggerGoogleSignIn: () -> Unit = {
            isSigningIn = true
            authError = null
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isSigningIn = false
                    authError = null
                },
                onAuthError = { err ->
                    isSigningIn = false
                    authError = err
                },
                scope = scope,
                onAuthCancelled = {
                    isSigningIn = false
                    authError = null
                }
            )
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, vm) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        vm.setAppInForeground(true)
                        vm.refreshNotificationPermissionStatus()
                    }
                    Lifecycle.Event.ON_PAUSE -> vm.setAppInForeground(false)
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                vm.setAppInForeground(false)
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        var selectedTab by rememberSaveable { mutableStateOf(TryAgainTab.FOCUS) }
        var showAmbientSoundsScreen by rememberSaveable { mutableStateOf(false) }
        var showAdminDashboard by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(isAdminRole) {
            if (!isAdminRole) {
                showAdminDashboard = false
            }
        }

        LaunchedEffect(liveAppConfig.socialEnabled, selectedTab) {
            if (!liveAppConfig.socialEnabled && selectedTab == TryAgainTab.SOCIAL) {
                selectedTab = TryAgainTab.FOCUS
            }
        }

        LaunchedEffect(focusDeepLinkTick) {
            if (focusDeepLinkTick > 0) {
                showAmbientSoundsScreen = false
                showAdminDashboard = false
                selectedTab = TryAgainTab.FOCUS
            }
        }

        LaunchedEffect(pendingAlarmLaunch) {
            val payload = pendingAlarmLaunch ?: return@LaunchedEffect
            vm.applyAlarmLaunch(payload.first, payload.second)
            alarmSubjectLaunchEvent.value = null
            selectedTab = TryAgainTab.FOCUS
        }

        if (selectedTab != TryAgainTab.FOCUS && !showAmbientSoundsScreen && !showAdminDashboard) {
            BackHandler {
                selectedTab = TryAgainTab.FOCUS
            }
        }

        val equippedTheme = remember(profile.equippedTheme) {
            GlassThemeId.fromWire(profile.equippedTheme)
        }
        val systemHaptics = rememberSystemHapticController(
            enabled = preferences.hapticsEnabled && preferences.hapticIntensity != HapticIntensityLevel.OFF
        )

        TryAgainTheme(themeId = equippedTheme) {
            CompositionLocalProvider(
                LocalTactileSound provides vm.tactileEngine,
                LocalSystemHaptics provides systemHaptics
            ) {
                val palette = LocalTryAgainPalette.current

                Box(modifier = Modifier.fillMaxSize()) {
                    MetamorphosisBackdrop(
                        lightingStyle = preferences.ambientLightingStyle,
                        lightingIntensity = preferences.ambientLightingIntensity,
                        edgeGlowEnabled = preferences.ambientEdgeGlowEnabled,
                        isFocusActive = timerState.isRunning
                    ) {
                        // Main 5-Tab Content with Smooth Directional Liquid-Glass Transition
                        TryAgainTabTransition(
                            selectedTab = selectedTab,
                            modifier = Modifier.fillMaxSize()
                        ) { tab ->
                            when (tab) {
                                TryAgainTab.FOCUS -> FocusHubScreen(
                                    profile = profile,
                                    timerState = timerState,
                                    preferences = preferences,
                                    streakReminderStatus = streakReminderStatus,
                                    tasks = tasks,
                                    streakDays = streakHistoryDays,
                                    screenUsageState = screenUsageState,
                                    dailyUsagePoints = dailyUsagePoints,
                                    weeklyPoints = weeklyPoints,
                                    todayFocusMinutes = todayMinutes,
                                    weeklyFocusMinutes = weeklyMinutes,
                                    activeAmbient = activeAmbient,
                                    isAmbientPlaying = isAmbientPlaying,
                                    ambientWaveAmplitude = ambientWaveAmplitude,
                                    geminiInsightsState = geminiInsightsState,
                                    sessionsCount = sessions.size,
                                    onSelectGoalPeriod = vm::setSelectedGoalPeriod,
                                    onSaveStudyGoals = vm::saveStudyGoals,
                                    onGenerateGeminiInsights = vm::generateGeminiInsights,
                                    onToggleStreakReminder = { enabled ->
                                        if (enabled) ensureNotificationPermissionIfNeeded()
                                        vm.setStreakReminderEnabled(enabled)
                                    },
                                    onSelectStreakReminderTime = vm::setStreakReminderTime,
                                    onRequestNotificationPermission = ensureNotificationPermissionIfNeeded,
                                    onSendTestStreakReminder = {
                                        ensureNotificationPermissionIfNeeded()
                                        vm.sendTestStreakReminder()
                                    },
                                    onSelectSubject = vm::selectSubject,
                                    onSelectPhase = vm::selectPhase,
                                    onSetDuration = vm::setTimerDuration,
                                    onAdjustDuration = vm::adjustTimerDuration,
                                    onStartOrResume = {
                                        ensureNotificationPermissionIfNeeded()
                                        vm.startOrResumeTimer()
                                    },
                                    onPause = vm::pauseTimer,
                                    onReset = vm::resetTimer,
                                    onCompleteOrSkip = vm::completeOrSkipPhase,
                                    onToggleAmbient = vm::selectOrToggleAmbientSound,
                                    onVolumeChange = vm::setAmbientVolume,
                                    onOpenAmbientSoundsScreen = { showAmbientSoundsScreen = true },
                                    onSelectLightingStyle = vm::setAmbientLightingStyle,
                                    onLightingIntensityChange = vm::setAmbientLightingIntensity,
                                    onToggleEdgeGlow = vm::setAmbientEdgeGlowEnabled,
                                    onSelectHapticIntensity = vm::setHapticIntensity,
                                    onToggleUiSounds = vm::setUiClickSoundsEnabled,
                                    onSelectCompletionSound = vm::selectCompletionSound,
                                    onOpenShop = { selectedTab = TryAgainTab.SHOP },
                                    onOpenIconStudio = { selectedTab = TryAgainTab.PROFILE },
                                    onRefreshScreenUsage = vm::refreshScreenUsage,
                                    onRequestUsageAccess = vm::openUsageAccessSettings,
                                    onAddTask = { vm.addTask(it) },
                                    onToggleTask = vm::toggleTaskCompletion,
                                    onPinTask = vm::pinTaskToTimer,
                                    onDeleteTask = vm::deleteTask,
                                    goalStreakDays = goalStreakDays,
                                    subjectAlarms = subjectAlarms,
                                    onAddSubjectAlarm = { subj, h, m, dur, note ->
                                        ensureNotificationPermissionIfNeeded()
                                        vm.addSubjectAlarm(subj, h, m, dur, note)
                                    },
                                    onToggleSubjectAlarm = { id, enabled ->
                                        if (enabled) ensureNotificationPermissionIfNeeded()
                                        vm.toggleSubjectAlarm(id, enabled)
                                    },
                                    onDeleteSubjectAlarm = vm::deleteSubjectAlarm,
                                    onApplySubjectAlarmNow = vm::applyAlarmLaunch,
                                    sessions = sessions,
                                    milestones = milestones,
                                    weekendPdfEnabled = preferences.weekendPdfReminderEnabled,
                                    isExportingPdf = isExportingPdf,
                                    hideStudyHistory = preferences.hideStudyHistory,
                                    onExportWeeklyPdf = vm::exportAndShareWeeklyPdf,
                                    onToggleWeekendPdf = { enabled ->
                                        if (enabled) ensureNotificationPermissionIfNeeded()
                                        vm.setWeekendPdfReminderEnabled(enabled)
                                    },
                                    onToggleHideStudyHistory = vm::setHideStudyHistory,
                                    onClaimMilestone = vm::claimMilestone,
                                    liveAppConfig = liveAppConfig,
                                    adminNotices = adminNotices,
                                    accountModeration = accountModeration,
                                    isVerifiedAdmin = isAdminRole,
                                    onOpenAdminPanel = {
                                        if (isAdminRole) {
                                            vm.refreshAdminUserCounts()
                                            showAdminDashboard = true
                                        }
                                    }
                                )

                                TryAgainTab.SOCIAL -> SocialDashboardScreen(
                                    profile = profile,
                                    currentUid = currentUser?.uid.orEmpty(),
                                    todayFocusMinutes = todayMinutes,
                                    socialState = socialState,
                                    challengesState = challengesState,
                                    leaderboardState = leaderboardState,
                                    chatState = activeChatState,
                                    isSigningIn = isSigningIn,
                                    authError = authError,
                                    onGoogleSignIn = triggerGoogleSignIn,
                                    onAddFriendByHandle = vm::addFriendByHandle,
                                    onUpdateStudyHandle = vm::updateMyStudyHandle,
                                    onOpenFriendChat = vm::openFriendChat,
                                    onCloseFriendChat = vm::closeFriendChat,
                                    onSendFriendMessage = vm::sendFriendMessage,
                                    onClearChatError = vm::clearChatError,
                                    onRemoveFriend = vm::removeFriend,
                                    onBlockUser = vm::blockUser,
                                    onUnblockUser = vm::unblockUser,
                                    onTogglePrivateStudyMode = vm::setPrivateStudyMode,
                                    onClearSocialError = vm::clearSocialError,
                                    onHandleCopied = vm::notifyHandleCopied,
                                    onCreateChallenge = vm::createStudyChallenge,
                                    onJoinChallengeByCode = vm::joinChallengeByCode,
                                    onAcceptChallengeInvite = vm::acceptChallengeInvite,
                                    onDeclineChallengeInvite = vm::declineChallengeInvite,
                                    onInviteFriendsToChallenge = vm::inviteFriendsToChallenge,
                                    onLeaveOrDeleteChallenge = vm::leaveOrDeleteChallenge,
                                    onClearChallengeError = vm::clearChallengesError,
                                    onChallengeCodeCopied = vm::notifyChallengeCodeCopied,
                                    onRefreshLeaderboard = vm::refreshLeaderboard
                                )

                                TryAgainTab.SHOP -> GemShopScreen(
                                    profile = profile,
                                    activeAmbient = activeAmbient,
                                    isAmbientPlaying = isAmbientPlaying,
                                    completionSound = preferences.completionSound,
                                    proUnlockStatus = proUnlockStatus,
                                    proAiState = proAiState,
                                    onGenerateProNotes = vm::generateProStudyNotes,
                                    onAskProTutor = vm::askProAiTutor,
                                    onClearProAiState = vm::clearProAiState,
                                    onUnlockOrPlaySound = vm::unlockAmbientSound,
                                    onSelectCompletionSound = vm::selectCompletionSound,
                                    onUnlockOrEquipTheme = vm::unlockOrEquipTheme,
                                    onUnlockOrEquipBadge = vm::unlockOrEquipBadge,
                                    onClaimPlayPointsTier = vm::claimPlayPointsTierReward
                                )

                                TryAgainTab.PROFILE -> ProfileScreen(
                                    profile = profile,
                                    preferences = preferences,
                                    streakReminderStatus = streakReminderStatus,
                                    userEmail = currentUser?.email ?: currentUser?.phoneNumber,
                                    isSigningIn = isSigningIn,
                                    authError = authError,
                                    isExportingPdf = isExportingPdf,
                                    onUpdateDisplayName = vm::updateDisplayName,
                                    onUpdateDailyGoal = vm::updateDailyGoalMinutes,
                                    onUpdateWeeklyGoal = vm::updateWeeklyGoalMinutes,
                                    onToggleUiSounds = vm::setUiClickSoundsEnabled,
                                    onToggleHaptics = vm::setHapticsEnabled,
                                    onToggleAutoPlayAmbient = vm::setAmbientAutoPlayWithTimer,
                                    onSelectCompletionSound = vm::selectCompletionSound,
                                    onToggleWeekendPdf = { enabled ->
                                        if (enabled) ensureNotificationPermissionIfNeeded()
                                        vm.setWeekendPdfReminderEnabled(enabled)
                                    },
                                    onToggleStreakReminder = { enabled ->
                                        if (enabled) ensureNotificationPermissionIfNeeded()
                                        vm.setStreakReminderEnabled(enabled)
                                    },
                                    onSelectStreakReminderTime = vm::setStreakReminderTime,
                                    onRequestNotificationPermission = ensureNotificationPermissionIfNeeded,
                                    onSendTestStreakReminder = {
                                        ensureNotificationPermissionIfNeeded()
                                        vm.sendTestStreakReminder()
                                    },
                                    onExportWeeklyPdf = vm::exportAndShareWeeklyPdf,
                                    onSelectCustomIcon = vm::setCustomAppIcon,
                                    onSelectCustomIconShape = vm::setCustomIconShape,
                                    onCustomShortcutFeedback = vm::notifyCustomShortcutResult,
                                    onGoogleSignIn = triggerGoogleSignIn,
                                    onSignOut = {
                                        showAdminDashboard = false
                                        phoneVerificationId = null
                                        signOutGoogle(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    },
                                    scholarDetails = scholarDetails,
                                    onUpdateScholarDetails = vm::updateScholarDetails,
                                    onPickProfilePhoto = vm::saveProfileAvatarFromUri,
                                    onClearProfilePhoto = vm::clearProfileAvatar,
                                    phoneVerificationId = phoneVerificationId,
                                    onEmailZohoSignIn = { email, password, create ->
                                        isSigningIn = true
                                        authError = null
                                        signInOrRegisterWithEmail(
                                            email = email,
                                            password = password,
                                            createAccount = create,
                                            onAuthSuccess = {
                                                isSigningIn = false
                                                authError = null
                                            },
                                            onAuthError = { err ->
                                                isSigningIn = false
                                                authError = err
                                            },
                                            scope = scope
                                        )
                                    },
                                    onSendPhoneOtp = { rawPhone ->
                                        isSigningIn = true
                                        authError = null
                                        startPhoneOtpVerification(
                                            context = context,
                                            rawPhoneNumber = rawPhone,
                                            onCodeSent = { verId ->
                                                isSigningIn = false
                                                phoneVerificationId = verId
                                                authError = null
                                            },
                                            onAutoVerified = {
                                                isSigningIn = false
                                                phoneVerificationId = null
                                                authError = null
                                            },
                                            onAuthError = { err ->
                                                isSigningIn = false
                                                authError = err
                                            }
                                        )
                                    },
                                    onVerifyPhoneOtp = { code ->
                                        val verId = phoneVerificationId ?: return@ProfileScreen
                                        isSigningIn = true
                                        authError = null
                                        verifyPhoneOtpCode(
                                            verificationId = verId,
                                            smsCode = code,
                                            onAuthSuccess = {
                                                isSigningIn = false
                                                phoneVerificationId = null
                                                authError = null
                                            },
                                            onAuthError = { err ->
                                                isSigningIn = false
                                                authError = err
                                            },
                                            scope = scope
                                        )
                                    },
                                    onGitHubSignIn = {
                                        isSigningIn = true
                                        authError = null
                                        signInWithGitHub(
                                            context = context,
                                            onAuthSuccess = {
                                                isSigningIn = false
                                                authError = null
                                            },
                                            onAuthError = { err ->
                                                isSigningIn = false
                                                authError = err
                                            }
                                        )
                                    },
                                    onClearAuthError = {
                                        authError = null
                                        phoneVerificationId = null
                                    },
                                    isVerifiedAdmin = isAdminRole,
                                    liveAppConfig = liveAppConfig,
                                    publicUsersCount = adminPanelState.userCounts.totalUsers,
                                    onOpenAdminPanel = {
                                        if (isAdminRole) {
                                            vm.refreshAdminUserCounts()
                                            showAdminDashboard = true
                                        }
                                    }
                                )
                            }
                        }

                        // Floating Apple Liquid-Glass Bottom Navigation Dock
                        TryAgainFloatingDock(
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it },
                            showSocialTab = liveAppConfig.socialEnabled,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        // Celebratory Gem Sparkle Confetti Burst
                        gemRewardBanner?.let { reward ->
                            GemCelebrationBurst(
                                triggerKey = reward.timestampMs,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Celebratory Gem Reward Banner
                        AnimatedVisibility(
                            visible = gemRewardBanner != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                        ) {
                            gemRewardBanner?.let { reward ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = palette.glassFillElevated,
                                    border = BorderStroke(1.5.dp, palette.gemColor.copy(alpha = 0.55f)),
                                    shadowElevation = 12.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Diamond,
                                            contentDescription = null,
                                            tint = palette.gemColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${reward.title}  +${reward.gemsDelta} Gems",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Subtle Status Toast Pill
                        AnimatedVisibility(
                            visible = statusBanner != null && gemRewardBanner == null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                        ) {
                            statusBanner?.let { msg ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = palette.textPrimary,
                                    shadowElevation = 10.dp
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (palette.isDark) Color(0xFF0E1A12) else Color.White,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Immediate Post-Session Summary Dialog (Time, Play Points & Gemini AI Coach)
                    sessionSummaryDialog?.let { summaryState ->
                        FocusSessionSummaryDialog(
                            state = summaryState,
                            onRetryCoach = vm::retrySessionCoachSummary,
                            onStartBreak = {
                                ensureNotificationPermissionIfNeeded()
                                vm.startBreakFromSummary()
                            },
                            onDismiss = vm::dismissSessionSummaryDialog
                        )
                    }

                    // Full-screen Liquid-Glass Ambient Sounds Selection Screen (local audio player, works live during focus timer)
                    AnimatedVisibility(
                        visible = showAmbientSoundsScreen,
                        enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut()
                    ) {
                        AmbientSoundsScreen(
                            timerState = timerState,
                            activeAmbient = activeAmbient,
                            isPlaying = isAmbientPlaying,
                            waveAmplitude = ambientWaveAmplitude,
                            volume = preferences.ambientVolume,
                            autoPlayWithTimer = preferences.ambientAutoPlayWithTimer,
                            unlockedSounds = profile.unlockedSounds,
                            userGems = profile.gems,
                            onSelectOrToggleSound = vm::selectOrToggleAmbientSound,
                            onUnlockSound = vm::unlockAmbientSound,
                            onVolumeChange = vm::setAmbientVolume,
                            onToggleAutoPlay = vm::setAmbientAutoPlayWithTimer,
                            onStartOrResumeTimer = {
                                ensureNotificationPermissionIfNeeded()
                                vm.startOrResumeTimer()
                            },
                            onPauseTimer = vm::pauseTimer,
                            onOpenShop = {
                                showAmbientSoundsScreen = false
                                selectedTab = TryAgainTab.SHOP
                            },
                            onBack = { showAmbientSoundsScreen = false }
                        )
                    }

                    // Full-screen Glassmorphic Admin Dashboard — reachable ONLY by Firestore 'admin' role
                    AnimatedVisibility(
                        visible = isAdminRole && showAdminDashboard,
                        enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut()
                    ) {
                        AdminDashboardScreen(
                            state = adminPanelState,
                            currentAdminUid = currentUser?.uid.orEmpty(),
                            onRefreshUserCounts = vm::refreshAdminUserCounts,
                            onPushLiveConfig = vm::adminPublishLiveConfig,
                            onSendNotice = vm::adminSendNotice,
                            onDeleteNotice = vm::adminDeleteNotice,
                            onDeleteUserAccount = vm::adminDeleteUserAccount,
                            onClearMessages = vm::clearAdminMessages,
                            onBack = { showAdminDashboard = false }
                        )
                    }

                    // Silent Liquid-Glass Opening Animation showcasing the user's Custom App Icon
                    if (showSplash) {
                        TryAgainSplashOverlay(
                            onFinished = { showSplash = false },
                            customIconId = preferences.customAppIconId,
                            customIconShapeId = preferences.customIconShapeId
                        )
                    }
                }
            }
        }
    }
}
