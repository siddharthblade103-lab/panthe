package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.GlassThemeId
import com.example.firebase.configureAppCheck
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.signOutGoogle
import com.example.ui.components.GemCelebrationBurst
import com.example.ui.components.LocalTactileSound
import com.example.ui.components.MetamorphosisBackdrop
import com.example.ui.components.MetamorphosisContainer
import com.example.ui.components.TryAgainFloatingDock
import com.example.ui.components.TryAgainSplashOverlay
import com.example.ui.components.TryAgainTab
import com.example.ui.screens.FocusHubScreen
import com.example.ui.screens.GemShopScreen
import com.example.ui.screens.JourneyPathScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.TryAgainTheme
import com.example.viewmodel.TryAgainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (REQUIRED): Configure App Check with launch intent before Compose UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            TryAgainAppRoot()
        }
    }
}

@Composable
private fun TryAgainAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
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
        val tasks by vm.tasks.collectAsStateWithLifecycle()
        val activeAmbient by vm.activeAmbientPreset.collectAsStateWithLifecycle()
        val isAmbientPlaying by vm.isAmbientPlaying.collectAsStateWithLifecycle()
        val aiCoachState by vm.aiCoachState.collectAsStateWithLifecycle()
        val todayMinutes by vm.todayMinutes.collectAsStateWithLifecycle()
        val weeklyPoints by vm.weeklyPoints.collectAsStateWithLifecycle()
        val streakHistoryDays by vm.streakHistoryDays.collectAsStateWithLifecycle()
        val milestones by vm.pathMilestones.collectAsStateWithLifecycle()
        val statusBanner by vm.statusBanner.collectAsStateWithLifecycle()
        val gemRewardBanner by vm.gemRewardBanner.collectAsStateWithLifecycle()

        var selectedTab by rememberSaveable { mutableStateOf(TryAgainTab.FOCUS) }

        if (selectedTab != TryAgainTab.FOCUS) {
            BackHandler {
                selectedTab = TryAgainTab.FOCUS
            }
        }

        val equippedTheme = remember(profile.equippedTheme) {
            GlassThemeId.fromWire(profile.equippedTheme)
        }

        TryAgainTheme(themeId = equippedTheme) {
            CompositionLocalProvider(LocalTactileSound provides vm.tactileEngine) {
                val palette = LocalTryAgainPalette.current

                Box(modifier = Modifier.fillMaxSize()) {
                    MetamorphosisBackdrop {
                        // Main 4-Tab Content with Smooth Metamorphosis Transition
                        MetamorphosisContainer(
                            targetState = selectedTab,
                            modifier = Modifier.fillMaxSize(),
                            label = "mainTabMetamorphosis"
                        ) { tab ->
                            when (tab) {
                                TryAgainTab.FOCUS -> FocusHubScreen(
                                    profile = profile,
                                    timerState = timerState,
                                    preferences = preferences,
                                    tasks = tasks,
                                    streakDays = streakHistoryDays,
                                    activeAmbient = activeAmbient,
                                    isAmbientPlaying = isAmbientPlaying,
                                    aiCoachState = aiCoachState,
                                    onSelectSubject = vm::selectSubject,
                                    onSelectPhase = vm::selectPhase,
                                    onSetDuration = vm::setTimerDuration,
                                    onAdjustDuration = vm::adjustTimerDuration,
                                    onStartOrResume = vm::startOrResumeTimer,
                                    onPause = vm::pauseTimer,
                                    onReset = vm::resetTimer,
                                    onCompleteOrSkip = vm::completeOrSkipPhase,
                                    onToggleAmbient = vm::selectOrToggleAmbientSound,
                                    onVolumeChange = vm::setAmbientVolume,
                                    onOpenShop = { selectedTab = TryAgainTab.SHOP },
                                    onRequestStudyTip = vm::requestStudyTip,
                                    onRequestMotivation = vm::requestMotivation,
                                    onAskCoach = vm::askCoachQuestion,
                                    onAddTask = { vm.addTask(it) },
                                    onToggleTask = vm::toggleTaskCompletion,
                                    onPinTask = vm::pinTaskToTimer,
                                    onDeleteTask = vm::deleteTask
                                )

                                TryAgainTab.JOURNEY -> JourneyPathScreen(
                                    profile = profile,
                                    todayMinutes = todayMinutes,
                                    weeklyPoints = weeklyPoints,
                                    streakDays = streakHistoryDays,
                                    milestones = milestones,
                                    onClaimMilestone = vm::claimMilestone,
                                    onOpenShop = { selectedTab = TryAgainTab.SHOP }
                                )

                                TryAgainTab.SHOP -> GemShopScreen(
                                    profile = profile,
                                    activeAmbient = activeAmbient,
                                    isAmbientPlaying = isAmbientPlaying,
                                    onUnlockOrPlaySound = vm::unlockAmbientSound,
                                    onUnlockOrEquipTheme = vm::unlockOrEquipTheme,
                                    onUnlockOrEquipBadge = vm::unlockOrEquipBadge
                                )

                                TryAgainTab.PROFILE -> ProfileScreen(
                                    profile = profile,
                                    preferences = preferences,
                                    userEmail = currentUser?.email,
                                    isSigningIn = isSigningIn,
                                    authError = authError,
                                    onUpdateDisplayName = vm::updateDisplayName,
                                    onUpdateDailyGoal = vm::updateDailyGoalMinutes,
                                    onToggleUiSounds = vm::setUiClickSoundsEnabled,
                                    onToggleHaptics = vm::setHapticsEnabled,
                                    onToggleAutoPlayAmbient = vm::setAmbientAutoPlayWithTimer,
                                    onGoogleSignIn = {
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
                                    },
                                    onSignOut = {
                                        signOutGoogle(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    }
                                )
                            }
                        }

                        // Floating Apple Liquid-Glass Bottom Navigation Dock
                        TryAgainFloatingDock(
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it },
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

                    // Silent Duolingo + Swiggy Style Opening Splash with Bouncing Monkey
                    if (showSplash) {
                        TryAgainSplashOverlay(onFinished = { showSplash = false })
                    }
                }
            }
        }
    }
}
