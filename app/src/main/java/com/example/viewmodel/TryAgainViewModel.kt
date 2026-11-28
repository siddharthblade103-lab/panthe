package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.AccountModerationRecord
import com.example.data.model.AdminIdentityPolicy
import com.example.data.model.AdminNotice
import com.example.data.model.AdminNoticeKind
import com.example.data.model.AdminPanelUiState
import com.example.data.model.AdminUserCountsSnapshot
import com.example.data.model.AmbientLightingStyle
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.AppIconShapeId
import com.example.data.model.AppLiveConfig
import com.example.data.model.AppPreferencesState
import com.example.data.model.BlockedUser
import com.example.data.model.CompletionSoundPreset
import com.example.data.model.CustomAppIconId
import com.example.data.model.DailyUsagePoint
import com.example.data.model.DayFocusPoint
import com.example.data.model.FocusSession
import com.example.data.model.FocusSessionSummaryDialogState
import com.example.data.model.FriendLiveCardState
import com.example.data.model.GemRewardBanner
import com.example.data.model.GeminiInsightsUiState
import com.example.data.model.GlassThemeId
import com.example.data.model.GlobalLeaderboardEntry
import com.example.data.model.GlobalLeaderboardUiState
import com.example.data.model.HapticIntensityLevel
import com.example.data.model.MascotBadgeId
import com.example.data.model.PathMilestone
import com.example.data.model.PlayPointsTier
import com.example.data.model.PomodoroPhase
import com.example.data.model.PomodoroTask
import com.example.data.model.ProAiStudioUiState
import com.example.data.model.ProUnlockStatus
import com.example.data.model.PublicStudyProfile
import com.example.data.model.ScholarProfileDetails
import com.example.data.model.ScreenUsageUiState
import com.example.data.model.SessionCoachState
import com.example.data.model.SocialDashboardUiState
import com.example.data.model.StreakDayProgress
import com.example.data.model.StreakReminderUiStatus
import com.example.data.model.StudyChallenge
import com.example.data.model.StudyChallengesUiState
import com.example.data.model.StudyChatMessage
import com.example.data.model.StudyChatUiState
import com.example.data.model.StudyGoalPeriod
import com.example.data.model.StudyLiveStatus
import com.example.data.model.SubjectStudyAlarm
import com.example.data.model.TimerUiState
import com.example.data.model.UserProfile
import com.example.data.repository.FocusFirestoreRepository
import com.example.service.AmbientAudioService
import com.example.service.AmbientSoundEngine
import com.example.service.CompletedTimerEvent
import com.example.service.DailyStreakReminderScheduler
import com.example.service.FocusStreakCalculator
import com.example.service.FocusTimerRuntime
import com.example.service.FocusTimerService
import com.example.service.GeminiInsightsService
import com.example.service.ScreenUsageTracker
import com.example.service.StreakNotificationManager
import com.example.service.StreakReminderPolicy
import com.example.service.StudyReportPdfGenerator
import com.example.service.SubjectStudyAlarmScheduler
import com.example.service.TactileSoundEngine
import com.example.service.WeekendReportScheduler
import com.example.ui.components.formatClock
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "TryAgainViewModel"

class TryAgainViewModel(
    application: Application,
    val isCloudSignedIn: Boolean
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefsStore = LocalPreferencesStore(appContext)
    private val firestoreRepo = if (isCloudSignedIn) FocusFirestoreRepository(appContext) else null
    private val geminiService = GeminiInsightsService()
    val tactileEngine = TactileSoundEngine(appContext)
    private val ambientEngine: AmbientSoundEngine = AmbientSoundEngine.shared

    private var appUsageTickerJob: Job? = null
    private var sessionSummaryCoachJob: Job? = null
    private var persistedAppUsageSecondsToday: Int = 0
    private var unsavedForegroundSeconds: Int = 0
    private val handledSessionIds = mutableSetOf<String>()

    val preferences: StateFlow<AppPreferencesState> = prefsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferencesState())

    val subjectAlarms: StateFlow<List<SubjectStudyAlarm>> = prefsStore.subjectAlarmsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val scholarDetails: StateFlow<ScholarProfileDetails> = prefsStore.scholarDetailsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScholarProfileDetails())

    private val _proAiState = MutableStateFlow<ProAiStudioUiState>(ProAiStudioUiState.Idle)
    val proAiState: StateFlow<ProAiStudioUiState> = _proAiState.asStateFlow()

    private val _liveAppConfig = MutableStateFlow(AppLiveConfig())
    val liveAppConfig: StateFlow<AppLiveConfig> = _liveAppConfig.asStateFlow()

    val accountModeration: StateFlow<AccountModerationRecord?> = if (firestoreRepo != null) {
        firestoreRepo.observeMyAccountModeration()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    } else {
        MutableStateFlow(null)
    }

    private val localProfileFlow = prefsStore.localProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    private val localDailyLogsFlow = prefsStore.localDailyLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val persistedLocalSessionsFlow: StateFlow<List<FocusSession>> = prefsStore.localSessionHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val localTasks = MutableStateFlow(
        listOf(
            PomodoroTask(
                id = "starter_1",
                userId = "local_scholar",
                title = "Complete 25m deep focus sprint",
                subject = "Math",
                estimatedPomodoros = 1,
                completedPomodoros = 0,
                completed = false
            ),
            PomodoroTask(
                id = "starter_2",
                userId = "local_scholar",
                title = "Review key chapter notes & formulas",
                subject = "Science",
                estimatedPomodoros = 2,
                completedPomodoros = 0,
                completed = false
            )
        )
    )

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _gemRewardBanner = MutableStateFlow<GemRewardBanner?>(null)
    val gemRewardBanner: StateFlow<GemRewardBanner?> = _gemRewardBanner.asStateFlow()

    private val _geminiInsightsState = MutableStateFlow<GeminiInsightsUiState>(GeminiInsightsUiState.Idle)
    val geminiInsightsState: StateFlow<GeminiInsightsUiState> = _geminiInsightsState.asStateFlow()

    private val _sessionSummaryDialog = MutableStateFlow<FocusSessionSummaryDialogState?>(null)
    val sessionSummaryDialog: StateFlow<FocusSessionSummaryDialogState?> = _sessionSummaryDialog.asStateFlow()

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(
        StreakNotificationManager.hasNotificationPermission(appContext)
    )
    private val _isSyncingFcm = MutableStateFlow(false)
    private val _fcmCloudSynced = MutableStateFlow(false)

    val userProfile: StateFlow<UserProfile> = if (firestoreRepo != null) {
        firestoreRepo.observeProfile()
            .map { it ?: UserProfile() }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Profile stream error", error)
                emit(localProfileFlow.value)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())
    } else {
        localProfileFlow
    }

    /**
     * Reactive Firestore RBAC gate: true ONLY when signed in AND the user's Firestore
     * document (`/users/{uid}`) holds `role == "admin"`.
     */
    val isAdminRole: StateFlow<Boolean> = userProfile
        .map { profile -> isCloudSignedIn && profile.hasAdminRole }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminNotices: StateFlow<List<AdminNotice>> = if (firestoreRepo != null) {
        isAdminRole
            .flatMapLatest { isAdmin -> firestoreRepo.observeAdminNotices(isAdmin = isAdmin) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val adminPublicDirectoryFlow: StateFlow<List<PublicStudyProfile>> =
        if (firestoreRepo != null) {
            isAdminRole
                .flatMapLatest { isAdmin ->
                    if (isAdmin) firestoreRepo.observePublicDirectoryForAdmin() else flowOf(emptyList())
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    private val _serverUserCounts = MutableStateFlow<AdminUserCountsSnapshot?>(null)
    private val _isRefreshingUserCounts = MutableStateFlow(false)
    private val _isAdminPushingUpdate = MutableStateFlow(false)
    private val _isAdminSendingNotice = MutableStateFlow(false)
    private val _adminDeletingUid = MutableStateFlow<String?>(null)
    private val _adminFeedback = MutableStateFlow<String?>(null)
    private val _adminError = MutableStateFlow<String?>(null)

    private val adminActionFlags = combine(
        _isAdminPushingUpdate,
        _isAdminSendingNotice,
        _adminDeletingUid,
        _adminFeedback,
        _adminError
    ) { pushing, sending, deletingUid, feedback, error ->
        AdminActionFlags(pushing, sending, deletingUid, feedback, error)
    }

    private val adminCountsFlow: StateFlow<AdminUserCountsSnapshot> = combine(
        adminPublicDirectoryFlow,
        _serverUserCounts,
        _isRefreshingUserCounts
    ) { directory, serverCounts, isRefreshing ->
        val todayKey = todayDateKey()
        val liveFocusing = directory.count { it.liveStatus == StudyLiveStatus.FOCUSING }
        val liveBreak = directory.count { it.liveStatus == StudyLiveStatus.BREAK }
        val liveActiveToday = directory.count { it.lastFocusDate == todayKey || it.todayFocusMinutes > 0 }
        val liveStreak = directory.count { it.currentStreak > 0 }
        val liveMinutes = directory.sumOf { it.totalFocusMinutes.toLong().coerceAtLeast(0L) }

        val total = maxOf(serverCounts?.totalUsers ?: 0, directory.size)
        val focusing = maxOf(serverCounts?.focusingNow ?: 0, liveFocusing)
        val onBreak = maxOf(serverCounts?.onBreakNow ?: 0, liveBreak)
        val ready = (total - focusing - onBreak).coerceAtLeast(0)
        val activeToday = maxOf(serverCounts?.activeToday ?: 0, liveActiveToday)
        val onStreak = maxOf(serverCounts?.onStreakCount ?: 0, liveStreak)

        AdminUserCountsSnapshot(
            totalUsers = total,
            focusingNow = focusing,
            onBreakNow = onBreak,
            readyNow = ready,
            activeToday = activeToday,
            onStreakCount = onStreak,
            removedAccounts = serverCounts?.removedAccounts ?: 0,
            totalFocusMinutesAcrossDirectory = maxOf(
                serverCounts?.totalFocusMinutesAcrossDirectory ?: 0L,
                liveMinutes
            ),
            isRefreshing = isRefreshing,
            lastUpdatedMs = serverCounts?.lastUpdatedMs ?: System.currentTimeMillis()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUserCountsSnapshot())

    val adminPanelState: StateFlow<AdminPanelUiState> = combine(
        isAdminRole,
        adminCountsFlow,
        adminPublicDirectoryFlow,
        adminNotices,
        combine(liveAppConfig, adminActionFlags) { cfg, flags -> cfg to flags }
    ) { isAdmin, counts, directory, notices, (config, flags) ->
        AdminPanelUiState(
            isVerifiedAdmin = isAdmin,
            firestoreRole = if (isAdmin) "admin" else "user",
            userCounts = if (isAdmin) counts else AdminUserCountsSnapshot(),
            publicDirectory = if (isAdmin) directory else emptyList(),
            notices = if (isAdmin) notices else emptyList(),
            liveConfig = config,
            isPushingUpdate = flags.isPushingUpdate,
            isSendingNotice = flags.isSendingNotice,
            deletingUserId = flags.deletingUserId,
            feedbackMessage = flags.feedbackMessage,
            errorMessage = flags.errorMessage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AdminPanelUiState()
    )

    val sessions: StateFlow<List<FocusSession>> = if (firestoreRepo != null) {
        combine(
            firestoreRepo.observeSessions().catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Sessions stream error", error)
                emit(emptyList())
            },
            persistedLocalSessionsFlow
        ) { cloudList, localList ->
            if (cloudList.isNotEmpty()) cloudList else localList
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        persistedLocalSessionsFlow
    }

    val tasks: StateFlow<List<PomodoroTask>> = if (firestoreRepo != null) {
        firestoreRepo.observeTasks()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Tasks stream error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        localTasks.asStateFlow()
    }

    // Backed directly by FocusTimerRuntime so background service and UI stay 100% in sync
    val timerState: StateFlow<TimerUiState> = FocusTimerRuntime.state

    val activeAmbientPreset: StateFlow<AmbientSoundPreset> = ambientEngine.selectedPreset
    val isAmbientPlaying: StateFlow<Boolean> = ambientEngine.isPlaying
    val ambientWaveAmplitude: StateFlow<Float> = ambientEngine.waveAmplitude

    private val _screenUsageState = MutableStateFlow(ScreenUsageUiState())
    val screenUsageState: StateFlow<ScreenUsageUiState> = _screenUsageState.asStateFlow()

    private val _isSyncingCircle = MutableStateFlow(false)
    private val _isAddingFriend = MutableStateFlow(false)
    private val _isUpdatingHandle = MutableStateFlow(false)
    private val _socialError = MutableStateFlow<String?>(null)

    private val _activeChatFriend = MutableStateFlow<PublicStudyProfile?>(null)
    private val _chatMessages = MutableStateFlow<List<StudyChatMessage>>(emptyList())
    private val _isChatLoading = MutableStateFlow(false)
    private val _isChatSending = MutableStateFlow(false)
    private val _chatError = MutableStateFlow<String?>(null)
    private var activeChatJob: Job? = null

    val activeChatState: StateFlow<StudyChatUiState> = combine(
        _activeChatFriend,
        _chatMessages,
        _isChatLoading,
        _isChatSending,
        _chatError
    ) { friend, messages, loading, sending, err ->
        StudyChatUiState(
            activeFriend = friend,
            messages = messages,
            isLoading = loading,
            isSending = sending,
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyChatUiState())

    private val myStudyProfileFlow: StateFlow<PublicStudyProfile?> = if (firestoreRepo != null) {
        firestoreRepo.observeMyStudyProfile()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "My study profile stream error", error)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    } else {
        MutableStateFlow(null)
    }

    private val friendsLiveStatesFlow: StateFlow<List<FriendLiveCardState>> = if (firestoreRepo != null) {
        firestoreRepo.observeFriendsLiveStates()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Friends live stream error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    private val blockedUsersFlow: StateFlow<List<BlockedUser>> = if (firestoreRepo != null) {
        firestoreRepo.observeBlockedUsers()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Blocked users stream error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    private val socialPrivacyCombinedFlow = combine(
        blockedUsersFlow,
        preferences.map { it.privateStudyMode }.distinctUntilChanged()
    ) { blocked, privateMode -> blocked to privateMode }

    private val socialFlagsCombinedFlow = combine(
        _isSyncingCircle,
        _isAddingFriend,
        _isUpdatingHandle,
        _socialError
    ) { syncing, adding, updatingHandle, err ->
        SocialFlagsState(syncing, adding, updatingHandle, err)
    }

    val socialState: StateFlow<SocialDashboardUiState> = combine(
        myStudyProfileFlow,
        friendsLiveStatesFlow,
        socialPrivacyCombinedFlow,
        socialFlagsCombinedFlow
    ) { myProfile, friends, (blocked, privateMode), flags ->
        val blockedSet = blocked.mapTo(hashSetOf()) { it.blockedUid }
        val safeFriends = friends.filterNot { it.profile.userId in blockedSet }
        SocialDashboardUiState(
            isSignedIn = isCloudSignedIn,
            myStudyProfile = myProfile,
            friends = safeFriends,
            blockedUsers = blocked,
            privateStudyMode = privateMode,
            isSyncingCircle = flags.isSyncingCircle || safeFriends.any { it.isSyncing },
            isAddingFriend = flags.isAddingFriend,
            isUpdatingHandle = flags.isUpdatingHandle,
            errorMessage = flags.errorMessage,
            lastCircleSyncMs = safeFriends.maxOfOrNull { it.lastSyncedAtMs } ?: 0L
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SocialDashboardUiState(isSignedIn = isCloudSignedIn)
    )

    private val _leaderboardRefreshTick = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val leaderboardState: StateFlow<GlobalLeaderboardUiState> = if (firestoreRepo != null) {
        _leaderboardRefreshTick
            .flatMapLatest {
                combine(
                    firestoreRepo.observeGlobalStreakLeaderboard(limit = 50L),
                    friendsLiveStatesFlow,
                    blockedUsersFlow
                ) { snapshot, friends, blocked ->
                    val currentUid = Firebase.auth.currentUser?.uid.orEmpty()
                    val blockedSet = blocked.mapTo(hashSetOf()) { it.blockedUid }
                    val friendUids = friends.mapTo(hashSetOf()) { it.profile.userId }
                    val visibleProfiles = snapshot.profiles.filterNot { it.userId in blockedSet }
                    val entries = visibleProfiles.mapIndexed { idx, pubProfile ->
                        GlobalLeaderboardEntry(
                            rank = idx + 1,
                            profile = pubProfile,
                            isCurrentUser = currentUid.isNotBlank() && pubProfile.userId == currentUid,
                            isFriend = pubProfile.userId in friendUids
                        )
                    }
                    val myIdx = entries.indexOfFirst { it.isCurrentUser }
                    val myEntry = entries.getOrNull(myIdx)
                    val nextAbove = if (myIdx > 0) entries.getOrNull(myIdx - 1) else null
                    GlobalLeaderboardUiState.Success(
                        entries = entries,
                        myEntry = myEntry,
                        nextAboveEntry = nextAbove,
                        isSyncing = snapshot.isSyncing,
                        lastSyncedAtMs = snapshot.lastSyncedAtMs
                    ) as GlobalLeaderboardUiState
                }
                    .onStart { emit(GlobalLeaderboardUiState.Loading) }
                    .catch { error ->
                        val fsError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (error is CancellationException && fsError == null) throw error
                        Log.e(TAG, "Global leaderboard stream error", error)
                        emit(
                            GlobalLeaderboardUiState.Error(
                                fsError?.localizedMessage
                                    ?: error.localizedMessage
                                    ?: "Unable to load global study streaks right now."
                            )
                        )
                    }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                GlobalLeaderboardUiState.Loading
            )
    } else {
        flowOf<GlobalLeaderboardUiState>(
            GlobalLeaderboardUiState.Success(
                entries = emptyList(),
                myEntry = null,
                nextAboveEntry = null,
                isSyncing = false,
                lastSyncedAtMs = 0L
            )
        ).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GlobalLeaderboardUiState.Loading
        )
    }

    private val _isChallengeBusy = MutableStateFlow(false)
    private val _challengeError = MutableStateFlow<String?>(null)

    private val joinedChallengesFlow = if (firestoreRepo != null) {
        firestoreRepo.observeJoinedChallengesWithProgress()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Joined challenges stream error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    private val pendingChallengeInvitesFlow = if (firestoreRepo != null) {
        firestoreRepo.observePendingChallengeInvites()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Challenge invites stream error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val challengesState: StateFlow<StudyChallengesUiState> = combine(
        joinedChallengesFlow,
        pendingChallengeInvitesFlow,
        blockedUsersFlow,
        _isChallengeBusy,
        _challengeError
    ) { active, pending, blocked, busy, err ->
        val blockedSet = blocked.mapTo(hashSetOf()) { it.blockedUid }
        StudyChallengesUiState(
            activeChallenges = active,
            pendingInvites = pending.filterNot { it.creatorUid in blockedSet },
            isSyncing = active.any { it.isSyncing },
            isBusy = busy,
            errorMessage = err
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StudyChallengesUiState()
    )

    private val dailyStatsByDate: StateFlow<Map<String, Pair<Int, Int>>> = combine(
        sessions,
        localDailyLogsFlow
    ) { sessList, persistedLocal ->
        val fromSessions = sessList.groupBy { it.dateKey }.mapValues { (_, list) ->
            list.sumOf { it.completedMinutes } to list.size
        }
        val merged = persistedLocal.toMutableMap()
        fromSessions.forEach { (dateKey, pair) ->
            val existing = merged[dateKey]
            if (existing == null || pair.first > existing.first || pair.second > existing.second) {
                merged[dateKey] = pair
            }
        }
        merged
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val todayMinutes: StateFlow<Int> = dailyStatsByDate.map { byDate ->
        byDate[todayDateKey()]?.first ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Consecutive days the scholar met or exceeded their daily study goal.
     */
    val goalStreakDays: StateFlow<Int> = combine(
        dailyStatsByDate,
        userProfile
    ) { byDate, profile ->
        val minsByDate = byDate.mapValues { it.value.first }
        FocusStreakCalculator.calculateConsecutiveGoalStreak(
            dailyMinutesByDate = minsByDate,
            dailyGoalMinutes = profile.dailyGoalMinutes,
            todayDateKey = todayDateKey()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Un-bypassable 500-Hour Pro Edition gate:
     * Strictly computed from verified accumulated study minutes (30,000 min = 500.0 hours).
     * Cannot be unlocked with Gems, toggles, or shortcuts.
     */
    val proUnlockStatus: StateFlow<ProUnlockStatus> = combine(
        userProfile,
        sessions
    ) { profile, sessList ->
        val loggedSum = sessList.sumOf { it.completedMinutes.coerceAtLeast(0) }
        val verifiedMinutes = maxOf(profile.totalFocusMinutes.coerceAtLeast(0), loggedSum)
        ProUnlockStatus(
            verifiedStudyMinutes = verifiedMinutes,
            requiredStudyMinutes = ProUnlockStatus.PRO_REQUIRED_STUDY_MINUTES
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProUnlockStatus())

    val weeklyPoints: StateFlow<List<DayFocusPoint>> = combine(
        dailyStatsByDate,
        sessions
    ) { byDate, sessList ->
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val monthDayFormat = SimpleDateFormat("MMM d", Locale.US)
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sessionsByDate = sessList.groupBy { it.dateKey }
        val cal = Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val key = keyFormat.format(c.time)
            val daySessions = sessionsByDate[key].orEmpty()
            val minutes = maxOf(
                byDate[key]?.first ?: 0,
                daySessions.sumOf { it.completedMinutes }
            )
            val sessionsCount = maxOf(
                byDate[key]?.second ?: 0,
                daySessions.size
            )
            val loggedPoints = daySessions.sumOf { it.xpEarned }
            val loggedGems = daySessions.sumOf { it.gemsEarned }
            val effectivePoints = if (loggedPoints > 0) loggedPoints else minutes * 3
            val effectiveGems = if (loggedGems > 0) loggedGems else if (minutes > 0) (minutes * 2) + (sessionsCount * 15) else 0

            DayFocusPoint(
                dayLabel = dayFormat.format(c.time).take(3),
                dateKey = key,
                minutes = minutes,
                monthDayLabel = monthDayFormat.format(c.time),
                pointsEarned = effectivePoints,
                gemsEarned = effectiveGems,
                sessionsCount = sessionsCount,
                isToday = daysAgo == 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyMinutes: StateFlow<Int> = weeklyPoints.map { points ->
        points.sumOf { it.minutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val fcmSyncFlagsFlow = combine(
        _hasNotificationPermission,
        _isSyncingFcm,
        _fcmCloudSynced
    ) { hasPerm, syncing, cloudSynced -> Triple(hasPerm, syncing, cloudSynced) }

    val streakReminderStatus: StateFlow<StreakReminderUiStatus> = combine(
        preferences,
        userProfile,
        todayMinutes,
        timerState,
        fcmSyncFlagsFlow
    ) { prefs, profile, todayMins, timer, (hasPerm, syncing, cloudSynced) ->
        val today = todayDateKey()
        val hasStartedToday = todayMins > 0 ||
            prefs.lastSessionStartedDate == today ||
            profile.lastFocusDate == today ||
            (timer.phase == PomodoroPhase.FOCUS &&
                (timer.isRunning || timer.isPaused || timer.elapsedSeconds >= 60) &&
                timer.sessionStartedDateKey == today)
        val preview = prefs.fcmToken.takeIf { it.length >= 12 }
            ?.let { "${it.take(6)}…${it.takeLast(4)}" }
            .orEmpty()
        StreakReminderUiStatus(
            enabled = prefs.streakReminderEnabled,
            hour = prefs.streakReminderHour,
            minute = prefs.streakReminderMinute,
            formattedTime = StreakReminderPolicy.formatReminderTime(
                prefs.streakReminderHour,
                prefs.streakReminderMinute
            ),
            hasStartedSessionToday = hasStartedToday,
            hasNotificationPermission = hasPerm,
            fcmConnected = prefs.fcmToken.isNotBlank(),
            fcmTokenPreview = preview,
            fcmTopic = prefs.fcmSubscribedTopic.ifBlank {
                if (prefs.streakReminderEnabled) {
                    StreakReminderPolicy.buildReminderSlotTopic(
                        prefs.streakReminderHour,
                        prefs.streakReminderMinute
                    )
                } else {
                    ""
                }
            },
            cloudSynced = isCloudSignedIn && cloudSynced,
            isSyncingFcm = syncing
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StreakReminderUiStatus(
            hasNotificationPermission = StreakNotificationManager.hasNotificationPermission(appContext)
        )
    )

    val streakHistoryDays: StateFlow<List<StreakDayProgress>> = combine(
        dailyStatsByDate,
        userProfile
    ) { byDate, profile ->
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val monthDayFormat = SimpleDateFormat("MMM d", Locale.US)
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val goal = profile.dailyGoalMinutes.coerceAtLeast(15)
        (13 downTo 0).map { daysAgo ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val key = keyFormat.format(c.time)
            val fullDay = dayFormat.format(c.time).take(3)
            val stats = byDate[key]
            StreakDayProgress(
                dateKey = key,
                dayShortLabel = fullDay.take(1),
                dayFullLabel = fullDay,
                monthDayLabel = monthDayFormat.format(c.time),
                dayOfMonth = c.get(Calendar.DAY_OF_MONTH),
                focusedMinutes = stats?.first ?: 0,
                sessionsCount = stats?.second ?: 0,
                goalMinutes = goal,
                isToday = daysAgo == 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyUsagePoints: StateFlow<List<DailyUsagePoint>> = combine(
        dailyStatsByDate,
        screenUsageState
    ) { byDate, usageState ->
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val monthDayFormat = SimpleDateFormat("MMM d", Locale.US)
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val key = keyFormat.format(c.time)
            val focusMins = byDate[key]?.first ?: 0
            val distractionMins = if (usageState.hasUsagePermission) {
                usageState.dailyDistractionMinutesByDate[key] ?: 0
            } else if (daysAgo == 0) {
                val appMinsToday = (usageState.thisAppUsageMillis / 60_000L).toInt()
                (appMinsToday - focusMins).coerceAtLeast(0)
            } else {
                0
            }
            DailyUsagePoint(
                dateKey = key,
                dayLabel = dayFormat.format(c.time).take(3),
                monthDayLabel = monthDayFormat.format(c.time),
                focusMinutes = focusMins,
                distractionMinutes = distractionMins,
                isToday = daysAgo == 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pathMilestones: StateFlow<List<PathMilestone>> = combine(userProfile, sessions, tasks) { profile, sessList, taskList ->
        val completedTasks = taskList.count { it.completed }
        val completedSessions = sessList.size
        val claimed = profile.claimedMilestones.toSet()

        listOf(
            PathMilestone(
                id = "first_spark",
                title = "First Focus Spark",
                description = "Complete your first focus session",
                gemReward = 25,
                targetLabel = "${completedSessions.coerceAtMost(1)}/1 session",
                progress = (completedSessions / 1f).coerceIn(0f, 1f),
                isUnlocked = completedSessions >= 1 || profile.totalFocusMinutes >= 1,
                isClaimed = "first_spark" in claimed
            ),
            PathMilestone(
                id = "task_finisher",
                title = "Checklist Crusher",
                description = "Finish 2 study tasks",
                gemReward = 35,
                targetLabel = "${completedTasks.coerceAtMost(2)}/2 tasks",
                progress = (completedTasks / 2f).coerceIn(0f, 1f),
                isUnlocked = completedTasks >= 2,
                isClaimed = "task_finisher" in claimed
            ),
            PathMilestone(
                id = "one_hour_flow",
                title = "Deep Flow Hour",
                description = "Accumulate 60 minutes of focused study",
                gemReward = 50,
                targetLabel = "${profile.totalFocusMinutes.coerceAtMost(60)}/60 min",
                progress = (profile.totalFocusMinutes / 60f).coerceIn(0f, 1f),
                isUnlocked = profile.totalFocusMinutes >= 60,
                isClaimed = "one_hour_flow" in claimed
            ),
            PathMilestone(
                id = "sound_collector",
                title = "Soundscape Curator",
                description = "Unlock 3 or more ambient sounds",
                gemReward = 60,
                targetLabel = "${profile.unlockedSounds.size.coerceAtMost(3)}/3 sounds",
                progress = (profile.unlockedSounds.size / 3f).coerceIn(0f, 1f),
                isUnlocked = profile.unlockedSounds.size >= 3,
                isClaimed = "sound_collector" in claimed
            ),
            PathMilestone(
                id = "streak_keeper",
                title = "Streak Flame",
                description = "Reach a 3-day focus streak",
                gemReward = 80,
                targetLabel = "${profile.longestStreak.coerceAtMost(3)}/3 days",
                progress = (profile.longestStreak / 3f).coerceIn(0f, 1f),
                isUnlocked = profile.longestStreak >= 3,
                isClaimed = "streak_keeper" in claimed
            ),
            PathMilestone(
                id = "grand_scholar",
                title = "Try Again Master",
                description = "Reach 300 total focus minutes",
                gemReward = 150,
                targetLabel = "${profile.totalFocusMinutes.coerceAtMost(300)}/300 min",
                progress = (profile.totalFocusMinutes / 300f).coerceIn(0f, 1f),
                isUnlocked = profile.totalFocusMinutes >= 300,
                isClaimed = "grand_scholar" in claimed
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            preferences.collect { pref ->
                tactileEngine.soundsEnabled = pref.uiClickSoundsEnabled
                tactileEngine.hapticStrengthScale = pref.hapticIntensity.scale
                tactileEngine.hapticsEnabled = pref.hapticsEnabled && pref.hapticIntensity != HapticIntensityLevel.OFF
                tactileEngine.completionSoundPreset = pref.completionSound
                ambientEngine.setVolume(pref.ambientVolume)
                WeekendReportScheduler.syncSchedule(appContext, pref.weekendPdfReminderEnabled)
                DailyStreakReminderScheduler.syncSchedule(
                    context = appContext,
                    enabled = pref.streakReminderEnabled,
                    hour = pref.streakReminderHour,
                    minute = pref.streakReminderMinute
                )
            }
        }

        // Sync scheduled per-subject daily study alarms with AlarmManager
        viewModelScope.launch {
            subjectAlarms.collect { alarms ->
                SubjectStudyAlarmScheduler.syncAllAlarms(appContext, alarms)
            }
        }

        // Sync Firebase Cloud Messaging registration token, topics & Firestore reminder schedule
        viewModelScope.launch {
            combine(preferences, userProfile) { pref, profile ->
                val latestActivityDate = maxOf(pref.lastSessionStartedDate, profile.lastFocusDate)
                StreakReminderSyncKey(
                    enabled = pref.streakReminderEnabled,
                    hour = pref.streakReminderHour,
                    minute = pref.streakReminderMinute,
                    currentStreak = profile.currentStreak,
                    lastFocusDate = latestActivityDate
                )
            }
                .distinctUntilChanged()
                .collect { syncKey ->
                    syncFcmStreakReminder(syncKey)
                }
        }

        // Listen for completed focus/break events emitted by FocusTimerService
        viewModelScope.launch {
            FocusTimerRuntime.completedEvents.collect { event ->
                onServicePhaseCompleted(event)
            }
        }

        // Hydrate persisted active or paused focus session seamlessly across restarts
        viewModelScope.launch {
            val hydration = FocusTimerRuntime.hydrateIfNeeded(prefsStore)
            when {
                hydration.completedWhileAway && hydration.finishedPhase != null -> {
                    handleRestoredCompletionWhileAway(
                        phase = hydration.finishedPhase,
                        completedMinutes = hydration.finishedMinutes,
                        plannedMinutes = hydration.finishedPlannedMinutes,
                        subject = hydration.finishedSubject,
                        pinnedTaskId = hydration.finishedPinnedTaskId,
                        sessionStartedDateKey = hydration.finishedSessionStartedDateKey
                    )
                }
                hydration.didRestore && hydration.restoredState.isRunning -> {
                    FocusTimerService.startOrResume(appContext)
                }
                hydration.didRestore && hydration.restoredState.isPaused -> {
                    val left = formatClock(hydration.restoredState.displaySeconds)
                    showBanner("Paused ${hydration.restoredState.subject} sprint restored ($left left) — streak safe!")
                }
            }
        }

        if (firestoreRepo != null) {
            viewModelScope.launch {
                val seed = prefsStore.localProfileFlow.first()
                firestoreRepo.ensureUserProfile(seed)
            }
            viewModelScope.launch {
                isAdminRole.collect { isAdmin ->
                    if (isAdmin) {
                        refreshAdminUserCounts()
                    } else {
                        _serverUserCounts.value = null
                    }
                }
            }
            viewModelScope.launch {
                combine(userProfile, todayMinutes, timerState, preferences) { profile, todayMins, timer, prefs ->
                    val liveStatus = when {
                        prefs.privateStudyMode -> StudyLiveStatus.READY
                        timer.isRunning && timer.phase == PomodoroPhase.FOCUS -> StudyLiveStatus.FOCUSING
                        timer.isRunning && timer.phase != PomodoroPhase.FOCUS -> StudyLiveStatus.BREAK
                        else -> StudyLiveStatus.READY
                    }
                    val visibleSubject = if (prefs.privateStudyMode) "Private Study" else timer.subject
                    StudyPresenceSnapshot(
                        displayName = profile.displayName,
                        status = liveStatus,
                        currentSubject = visibleSubject,
                        todayFocusMinutes = todayMins,
                        dailyGoalMinutes = profile.dailyGoalMinutes,
                        currentStreak = profile.currentStreak,
                        totalFocusMinutes = profile.totalFocusMinutes,
                        level = profile.level,
                        equippedBadge = profile.equippedBadge,
                        lastFocusDate = profile.lastFocusDate
                    )
                }
                    .distinctUntilChanged()
                    .collect { snap ->
                        _isSyncingCircle.value = true
                        firestoreRepo.publishMyStudyProfile(
                            displayName = snap.displayName,
                            status = snap.status,
                            currentSubject = snap.currentSubject,
                            todayFocusMinutes = snap.todayFocusMinutes,
                            dailyGoalMinutes = snap.dailyGoalMinutes,
                            currentStreak = snap.currentStreak,
                            totalFocusMinutes = snap.totalFocusMinutes,
                            level = snap.level,
                            equippedBadge = snap.equippedBadge,
                            lastFocusDate = snap.lastFocusDate
                        )
                        _isSyncingCircle.value = false
                    }
            }
        }

        // Silent background live app updates: hydrate cached config immediately (zero loading),
        // then stream real-time updates from Firestore whenever the admin publishes a change.
        viewModelScope.launch {
            val cached = prefsStore.cachedLiveConfigFlow.first()
            _liveAppConfig.value = cached
        }
        if (firestoreRepo != null) {
            viewModelScope.launch {
                firestoreRepo.observeLiveAppConfig().collect { remoteConfig ->
                    if (remoteConfig != null) {
                        val previousVersion = _liveAppConfig.value.configVersion
                        _liveAppConfig.value = remoteConfig
                        prefsStore.saveCachedLiveConfig(remoteConfig)
                        // Silently apply updated default sprint duration only when timer is idle
                        val curTimer = timerState.value
                        if (remoteConfig.configVersion != previousVersion &&
                            !curTimer.isRunning &&
                            !curTimer.isPaused &&
                            curTimer.phase == PomodoroPhase.FOCUS &&
                            curTimer.elapsedSeconds == 0 &&
                            remoteConfig.defaultFocusMinutes != curTimer.plannedMinutes
                        ) {
                            FocusTimerRuntime.setDuration(remoteConfig.defaultFocusMinutes)
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            persistedAppUsageSecondsToday = prefsStore.appUsageSecondsForDateFlow(todayDateKey()).first()
            refreshScreenUsage()
        }
    }

    // --- Timer Controls (delegated to Background FocusTimerService + Durable Persistence) ---

    fun selectSubject(subject: String) {
        FocusTimerRuntime.selectSubject(subject)
        viewModelScope.launch { FocusTimerRuntime.persistNow(prefsStore) }
    }

    fun selectPhase(phase: PomodoroPhase) {
        FocusTimerRuntime.selectPhase(phase)
        viewModelScope.launch { FocusTimerRuntime.persistNow(prefsStore) }
    }

    fun setTimerDuration(minutes: Int) {
        FocusTimerRuntime.setDuration(minutes)
        viewModelScope.launch { FocusTimerRuntime.persistNow(prefsStore) }
    }

    fun adjustTimerDuration(delta: Int) {
        FocusTimerRuntime.adjustDuration(delta)
        viewModelScope.launch { FocusTimerRuntime.persistNow(prefsStore) }
    }

    fun pinTaskToTimer(taskId: String?) {
        val nextId = if (timerState.value.activeTaskId == taskId) null else taskId
        FocusTimerRuntime.pinTask(nextId)
        viewModelScope.launch { FocusTimerRuntime.persistNow(prefsStore) }
    }

    fun startOrResumeTimer() {
        val prefs = preferences.value
        val cur = timerState.value
        if (cur.phase == PomodoroPhase.FOCUS && prefs.ambientAutoPlayWithTimer) {
            val chosen = if (ambientEngine.selectedPreset.value != AmbientSoundPreset.OFF) {
                ambientEngine.selectedPreset.value
            } else {
                prefs.preferredAmbientSound
            }
            if (isSoundUnlocked(chosen)) {
                AmbientAudioService.play(
                    context = appContext,
                    preset = chosen,
                    volume = prefs.ambientVolume,
                    subtitle = "Focusing on ${cur.subject}"
                )
            }
        }
        if (cur.phase == PomodoroPhase.FOCUS) {
            val today = todayDateKey()
            StreakNotificationManager.cancelStreakReminderNotification(appContext)
            viewModelScope.launch {
                prefsStore.markSessionStartedToday(today)
            }
        }
        FocusTimerService.startOrResume(appContext)
    }

    fun pauseTimer() {
        FocusTimerService.pause(appContext)
        viewModelScope.launch {
            // Ensure immediate persistence even if service command is queued
            FocusTimerRuntime.persistNow(prefsStore)
        }
    }

    fun resetTimer() {
        FocusTimerService.reset(appContext)
        viewModelScope.launch {
            FocusTimerRuntime.persistNow(prefsStore)
        }
    }

    fun completeOrSkipPhase() {
        FocusTimerService.completeNow(appContext)
    }

    private suspend fun handleRestoredCompletionWhileAway(
        phase: PomodoroPhase,
        completedMinutes: Int,
        plannedMinutes: Int,
        subject: String,
        pinnedTaskId: String?,
        sessionStartedDateKey: String
    ) {
        val today = todayDateKey()
        val yesterday = yesterdayDateKey()
        val soundLabel = AmbientSoundEngine.shared.selectedPreset.value.displayName
        val sessionId = UUID.randomUUID().toString()
        val gemsEarned = if (phase == PomodoroPhase.FOCUS) (completedMinutes * 2) + 15 else 0
        val xpEarned = if (phase == PomodoroPhase.FOCUS) completedMinutes * 3 else 0

        if (phase == PomodoroPhase.FOCUS) {
            val session = FocusSession(
                id = sessionId,
                userId = "local_scholar",
                subject = subject,
                plannedMinutes = plannedMinutes,
                completedMinutes = completedMinutes,
                ambientSound = soundLabel,
                xpEarned = xpEarned,
                gemsEarned = gemsEarned,
                dateKey = today,
                createdAt = Timestamp.now()
            )
            runCatching {
                prefsStore.recordBackgroundCompletedSession(
                    session = session,
                    todayDateKey = today,
                    yesterdayDateKey = yesterday,
                    sessionStartedDateKey = sessionStartedDateKey.ifBlank { today }
                )
            }
        }

        onServicePhaseCompleted(
            CompletedTimerEvent(
                sessionId = sessionId,
                phase = phase,
                subject = subject,
                plannedMinutes = plannedMinutes,
                completedMinutes = completedMinutes,
                ambientSound = soundLabel,
                xpEarned = xpEarned,
                gemsEarned = gemsEarned,
                dateKey = today,
                pinnedTaskId = pinnedTaskId,
                alreadyPersistedLocally = true,
                sessionStartedDateKey = sessionStartedDateKey.ifBlank { today }
            )
        )
    }

    private fun onServicePhaseCompleted(event: CompletedTimerEvent) {
        if (!handledSessionIds.add(event.sessionId)) return

        if (event.phase != PomodoroPhase.FOCUS) {
            showBanner("Break finished! Ready to focus again.")
            return
        }

        val current = userProfile.value
        val newStreak = FocusStreakCalculator.calculateNextStreak(
            currentStreak = current.currentStreak,
            lastFocusDate = current.lastFocusDate,
            completionDateKey = event.dateKey,
            sessionStartedDateKey = event.sessionStartedDateKey.ifBlank { null }
        )
        val newLongest = maxOf(current.longestStreak, newStreak)
        val newTotalMinutes = current.totalFocusMinutes + event.completedMinutes
        val newXp = current.focusXp + event.xpEarned
        val multiplierPercent = _liveAppConfig.value.gemMultiplierPercent.coerceIn(100, 300)
        val effectiveGemsEarned = ((event.gemsEarned * multiplierPercent) / 100).coerceAtLeast(event.gemsEarned)
        val newGems = current.gems + effectiveGemsEarned
        val newTotalGems = current.totalGemsEarned + effectiveGemsEarned

        viewModelScope.launch {
            if (firestoreRepo != null) {
                firestoreRepo.logSessionAndUpdateProfile(
                    subject = event.subject,
                    plannedMinutes = event.plannedMinutes,
                    completedMinutes = event.completedMinutes,
                    ambientSound = event.ambientSound,
                    xpEarned = event.xpEarned,
                    gemsEarned = effectiveGemsEarned,
                    dateKey = event.dateKey,
                    updatedProfileFields = mapOf(
                        "currentStreak" to newStreak,
                        "longestStreak" to newLongest,
                        "totalFocusMinutes" to newTotalMinutes,
                        "focusXp" to newXp,
                        "gems" to newGems,
                        "totalGemsEarned" to newTotalGems,
                        "lastFocusDate" to event.dateKey
                    )
                )
                firestoreRepo.contributeFocusToActiveChallenges(
                    completedMinutes = event.completedMinutes,
                    subject = event.subject,
                    displayName = current.displayName,
                    handle = myStudyProfileFlow.value?.handle.orEmpty()
                )
            }

            if (event.pinnedTaskId != null) {
                val targetTask = tasks.value.firstOrNull { it.id == event.pinnedTaskId }
                if (targetTask != null) {
                    val nextCount = targetTask.completedPomodoros + 1
                    val done = targetTask.completed || nextCount >= targetTask.estimatedPomodoros
                    updateTaskState(targetTask, done, nextCount)
                }
            }
        }

        val updatedTodayMinutes = maxOf(todayMinutes.value, todayMinutes.value + event.completedMinutes)
        val nextPhaseLabel = timerState.value.phase.displayName
        val summaryState = FocusSessionSummaryDialogState(
            sessionId = event.sessionId,
            subject = event.subject,
            completedMinutes = event.completedMinutes,
            plannedMinutes = event.plannedMinutes,
            pointsEarned = event.xpEarned,
            gemsEarned = effectiveGemsEarned,
            streakDays = newStreak,
            todayTotalMinutes = updatedTodayMinutes,
            dailyGoalMinutes = current.dailyGoalMinutes,
            ambientSound = event.ambientSound,
            nextPhaseName = nextPhaseLabel,
            coachState = SessionCoachState.Loading
        )
        _sessionSummaryDialog.value = summaryState
        fetchSessionCoachSummary(summaryState, current.displayName)

        showGemReward("Focus Complete! (${event.completedMinutes} min)", effectiveGemsEarned, event.xpEarned)
    }

    private fun fetchSessionCoachSummary(
        summary: FocusSessionSummaryDialogState,
        displayName: String
    ) {
        sessionSummaryCoachJob?.cancel()
        sessionSummaryCoachJob = viewModelScope.launch {
            val result = geminiService.generateSessionSummary(
                displayName = displayName,
                subject = summary.subject,
                completedMinutes = summary.completedMinutes,
                plannedMinutes = summary.plannedMinutes,
                pointsEarned = summary.pointsEarned,
                gemsEarned = summary.gemsEarned,
                streakDays = summary.streakDays,
                todayTotalMinutes = summary.todayTotalMinutes,
                dailyGoalMinutes = summary.dailyGoalMinutes,
                ambientSound = summary.ambientSound,
                nextPhaseName = summary.nextPhaseName
            )
            result.onSuccess { coachRecap ->
                tactileEngine.playCardPop()
                _sessionSummaryDialog.update { currentDialog ->
                    if (currentDialog?.sessionId == summary.sessionId) {
                        currentDialog.copy(coachState = SessionCoachState.Success(coachRecap))
                    } else {
                        currentDialog
                    }
                }
            }.onFailure { err ->
                _sessionSummaryDialog.update { currentDialog ->
                    if (currentDialog?.sessionId == summary.sessionId) {
                        currentDialog.copy(
                            coachState = SessionCoachState.Error(
                                err.message ?: "Could not load Gemini AI coach summary."
                            )
                        )
                    } else {
                        currentDialog
                    }
                }
            }
        }
    }

    fun retrySessionCoachSummary() {
        val current = _sessionSummaryDialog.value ?: return
        val loadingState = current.copy(coachState = SessionCoachState.Loading)
        _sessionSummaryDialog.value = loadingState
        fetchSessionCoachSummary(loadingState, userProfile.value.displayName)
    }

    fun dismissSessionSummaryDialog() {
        sessionSummaryCoachJob?.cancel()
        _sessionSummaryDialog.value = null
    }

    fun startBreakFromSummary() {
        sessionSummaryCoachJob?.cancel()
        _sessionSummaryDialog.value = null
        startOrResumeTimer()
    }

    // --- Gemini Productivity Insights ---

    fun generateGeminiInsights() {
        if (_geminiInsightsState.value is GeminiInsightsUiState.Loading) return
        _geminiInsightsState.value = GeminiInsightsUiState.Loading
        viewModelScope.launch {
            val result = geminiService.generateStudyInsights(
                profile = userProfile.value,
                sessions = sessions.value,
                weeklyPoints = weeklyPoints.value,
                tasks = tasks.value,
                todayMinutes = todayMinutes.value
            )
            result.onSuccess { report ->
                tactileEngine.playCardPop()
                _geminiInsightsState.value = GeminiInsightsUiState.Success(report)
            }.onFailure { err ->
                _geminiInsightsState.value = GeminiInsightsUiState.Error(
                    err.message ?: "Unable to generate study insights right now."
                )
            }
        }
    }

    // --- Weekly Study PDF Export & Weekend Auto-Report ---

    fun exportAndShareWeeklyPdf() {
        if (_isExportingPdf.value) return
        _isExportingPdf.value = true
        tactileEngine.playTap()
        viewModelScope.launch {
            val aiReport = (_geminiInsightsState.value as? GeminiInsightsUiState.Success)?.report
            val result = withContext(Dispatchers.IO) {
                StudyReportPdfGenerator.generateWeeklyReportPdf(
                    context = appContext,
                    profile = userProfile.value,
                    sessions = sessions.value,
                    weeklyPoints = weeklyPoints.value,
                    aiReport = aiReport
                )
            }
            _isExportingPdf.value = false
            result.onSuccess { file ->
                tactileEngine.playCardPop()
                val shared = StudyReportPdfGenerator.sharePdf(appContext, file)
                if (shared) {
                    showBanner("Weekly Study PDF ready to send!")
                } else {
                    showBanner("Saved Weekly Study PDF report.")
                }
            }.onFailure {
                showBanner("Could not generate PDF report right now.")
            }
        }
    }

    fun setWeekendPdfReminderEnabled(enabled: Boolean) {
        tactileEngine.playToggle(enabled)
        viewModelScope.launch {
            prefsStore.setWeekendPdfReminderEnabled(enabled)
            WeekendReportScheduler.syncSchedule(appContext, enabled)
        }
        showBanner(
            if (enabled) "Weekend PDF Auto-Report scheduled for Sunday evening!"
            else "Weekend PDF Auto-Report paused."
        )
    }

    // --- Firebase Cloud Messaging Daily Streak Reminders ---

    fun refreshNotificationPermissionStatus() {
        _hasNotificationPermission.value = StreakNotificationManager.hasNotificationPermission(appContext)
    }

    fun setStreakReminderEnabled(enabled: Boolean) {
        tactileEngine.playToggle(enabled)
        refreshNotificationPermissionStatus()
        viewModelScope.launch {
            prefsStore.setStreakReminderEnabled(enabled)
            val pref = preferences.value
            DailyStreakReminderScheduler.syncSchedule(
                context = appContext,
                enabled = enabled,
                hour = pref.streakReminderHour,
                minute = pref.streakReminderMinute
            )
        }
        val formattedTime = StreakReminderPolicy.formatReminderTime(
            preferences.value.streakReminderHour,
            preferences.value.streakReminderMinute
        )
        showBanner(
            if (enabled) "Daily FCM streak alert armed for $formattedTime!"
            else "Daily streak push reminders paused."
        )
    }

    fun setStreakReminderTime(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        tactileEngine.playTap()
        viewModelScope.launch {
            prefsStore.setStreakReminderTime(safeHour, safeMinute)
            DailyStreakReminderScheduler.syncSchedule(
                context = appContext,
                enabled = preferences.value.streakReminderEnabled,
                hour = safeHour,
                minute = safeMinute
            )
        }
        val formatted = StreakReminderPolicy.formatReminderTime(safeHour, safeMinute)
        showBanner("Daily streak push alert set for $formatted")
    }

    fun sendTestStreakReminder() {
        refreshNotificationPermissionStatus()
        val status = streakReminderStatus.value
        if (!status.hasNotificationPermission) {
            showBanner("Allow notifications first to preview your daily streak alert.")
            return
        }
        val posted = StreakNotificationManager.showStreakReminderNotification(
            context = appContext,
            currentStreak = userProfile.value.currentStreak,
            reminderTimeLabel = status.formattedTime,
            isTest = true
        )
        if (posted) {
            tactileEngine.playCardPop()
            showBanner("Test streak alert sent! Check your notification shade.")
        } else {
            showBanner("Enable notifications in Android settings to receive streak alerts.")
        }
    }

    private suspend fun syncFcmStreakReminder(key: StreakReminderSyncKey) {
        _isSyncingFcm.value = true
        try {
            val pref = prefsStore.preferencesFlow.first()
            val outcome = StreakNotificationManager.syncFcmTokenAndTopics(
                context = appContext,
                prefsStore = prefsStore,
                firestoreRepo = firestoreRepo,
                enabled = key.enabled,
                hour = key.hour,
                minute = key.minute,
                currentStreak = key.currentStreak,
                lastFocusDate = key.lastFocusDate,
                previousTopic = pref.fcmSubscribedTopic,
                cachedToken = pref.fcmToken
            )
            _fcmCloudSynced.value = outcome.cloudSynced
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "FCM streak reminder sync failed: ${e.message}")
        } finally {
            _isSyncingFcm.value = false
        }
    }

    // --- Custom Completion Sound ---

    fun selectCompletionSound(preset: CompletionSoundPreset) {
        tactileEngine.completionSoundPreset = preset
        tactileEngine.previewCompletionSound(preset)
        viewModelScope.launch {
            prefsStore.setCompletionSound(preset)
        }
        showBanner("Completion chime set to ${preset.displayName}")
    }

    fun previewCompletionSound(preset: CompletionSoundPreset) {
        tactileEngine.previewCompletionSound(preset)
    }

    // --- Play Points Tier Rewards ---

    fun claimPlayPointsTierReward(tier: PlayPointsTier) {
        if (tier.gemBonus <= 0) return
        val profile = userProfile.value
        if (profile.focusXp < tier.minPoints) {
            showBanner("Earn ${tier.minPoints - profile.focusXp} more Play Points to unlock ${tier.displayName}!")
            return
        }
        if (tier.claimKey in profile.claimedMilestones) return

        tactileEngine.playPointsReward()
        val nextClaimed = (profile.claimedMilestones + tier.claimKey).distinct()
        val nextGems = profile.gems + tier.gemBonus
        val nextTotalGems = profile.totalGemsEarned + tier.gemBonus
        persistProfileMutation(
            updated = profile.copy(
                gems = nextGems,
                totalGemsEarned = nextTotalGems,
                claimedMilestones = nextClaimed
            ),
            cloudFields = mapOf(
                "gems" to nextGems,
                "totalGemsEarned" to nextTotalGems,
                "claimedMilestones" to nextClaimed
            )
        )
        showGemReward("${tier.displayName} Tier Bonus!", tier.gemBonus)
    }

    // --- Study Tasks ---

    fun addTask(title: String, subject: String = timerState.value.subject, estimatedPomodoros: Int = 1) {
        val clean = title.trim().take(120)
        if (clean.isEmpty()) return
        tactileEngine.playTap()
        viewModelScope.launch {
            if (firestoreRepo != null) {
                firestoreRepo.addTask(clean, subject, estimatedPomodoros)
            } else {
                val newTask = PomodoroTask(
                    id = UUID.randomUUID().toString(),
                    userId = userProfile.value.userId,
                    title = clean,
                    subject = subject,
                    estimatedPomodoros = estimatedPomodoros.coerceIn(1, 12),
                    completedPomodoros = 0,
                    completed = false,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                localTasks.update { listOf(newTask) + it }
            }
        }
    }

    fun toggleTaskCompletion(task: PomodoroTask) {
        val nextDone = !task.completed
        if (nextDone) {
            tactileEngine.playGemSparkle()
            awardBonusGems(5, "Task Completed!")
        } else {
            tactileEngine.playToggle(false)
        }
        viewModelScope.launch {
            updateTaskState(task, nextDone, task.completedPomodoros)
        }
    }

    fun deleteTask(taskId: String) {
        tactileEngine.playTap()
        if (timerState.value.activeTaskId == taskId) {
            FocusTimerRuntime.pinTask(null)
        }
        viewModelScope.launch {
            if (firestoreRepo != null) {
                firestoreRepo.deleteTask(taskId)
            } else {
                localTasks.update { list -> list.filterNot { it.id == taskId } }
            }
        }
    }

    private suspend fun updateTaskState(task: PomodoroTask, completed: Boolean, completedPomodoros: Int) {
        if (firestoreRepo != null) {
            firestoreRepo.updateTask(task.id, completed, completedPomodoros)
        } else {
            localTasks.update { list ->
                list.map {
                    if (it.id == task.id) it.copy(completed = completed, completedPomodoros = completedPomodoros)
                    else it
                }
            }
        }
    }

    // --- Ambient Sound Controls & Shop Unlocks ---

    fun isSoundUnlocked(preset: AmbientSoundPreset): Boolean {
        if (preset == AmbientSoundPreset.OFF || preset.gemCost == 0) return true
        return preset.name in userProfile.value.unlockedSounds
    }

    fun selectOrToggleAmbientSound(preset: AmbientSoundPreset) {
        if (preset == AmbientSoundPreset.OFF) {
            tactileEngine.playToggle(false)
            AmbientAudioService.stop(appContext)
            return
        }
        if (!isSoundUnlocked(preset)) {
            showBanner("Unlock ${preset.displayName} in the Rewards Shop (${preset.gemCost} Gems)")
            return
        }
        if (activeAmbientPreset.value == preset && isAmbientPlaying.value) {
            tactileEngine.playToggle(false)
            AmbientAudioService.pause(appContext, "Paused ${preset.displayName}")
        } else {
            tactileEngine.playToggle(true)
            viewModelScope.launch { prefsStore.setPreferredAmbientSound(preset) }
            AmbientAudioService.play(
                context = appContext,
                preset = preset,
                volume = preferences.value.ambientVolume,
                subtitle = "Try Again • ${preset.displayName}"
            )
        }
    }

    fun setAmbientVolume(volume: Float) {
        ambientEngine.setVolume(volume)
        viewModelScope.launch { prefsStore.setAmbientVolume(volume) }
    }

    fun unlockAmbientSound(preset: AmbientSoundPreset) {
        val profile = userProfile.value
        if (isSoundUnlocked(preset)) {
            selectOrToggleAmbientSound(preset)
            return
        }
        if (profile.gems < preset.gemCost) {
            showBanner("Need ${preset.gemCost - profile.gems} more Gems — complete a focus sprint!")
            return
        }
        tactileEngine.playUnlock()
        val updatedSounds = (profile.unlockedSounds + preset.name).distinct()
        val nextGems = profile.gems - preset.gemCost
        persistProfileMutation(
            updated = profile.copy(gems = nextGems, unlockedSounds = updatedSounds),
            cloudFields = mapOf("gems" to nextGems, "unlockedSounds" to updatedSounds)
        )
        viewModelScope.launch { prefsStore.setPreferredAmbientSound(preset) }
        AmbientAudioService.play(
            context = appContext,
            preset = preset,
            volume = preferences.value.ambientVolume,
            subtitle = "Unlocked • ${preset.displayName}"
        )
        showBanner("Unlocked ${preset.displayName}!")
    }

    fun unlockOrEquipTheme(themeId: GlassThemeId) {
        val profile = userProfile.value
        val owned = themeId.gemCost == 0 || themeId.name in profile.unlockedCosmetics
        if (owned) {
            tactileEngine.playCardPop()
            persistProfileMutation(
                updated = profile.copy(equippedTheme = themeId.name),
                cloudFields = mapOf("equippedTheme" to themeId.name)
            )
            return
        }
        if (profile.gems < themeId.gemCost) {
            showBanner("Need ${themeId.gemCost - profile.gems} more Gems to unlock ${themeId.displayName}")
            return
        }
        tactileEngine.playUnlock()
        val nextGems = profile.gems - themeId.gemCost
        val nextCosmetics = (profile.unlockedCosmetics + themeId.name).distinct()
        persistProfileMutation(
            updated = profile.copy(
                gems = nextGems,
                unlockedCosmetics = nextCosmetics,
                equippedTheme = themeId.name
            ),
            cloudFields = mapOf(
                "gems" to nextGems,
                "unlockedCosmetics" to nextCosmetics,
                "equippedTheme" to themeId.name
            )
        )
        showBanner("Equipped ${themeId.displayName}!")
    }

    fun unlockOrEquipBadge(badgeId: MascotBadgeId) {
        val profile = userProfile.value
        val owned = badgeId.gemCost == 0 || badgeId.name in profile.unlockedCosmetics
        if (owned) {
            tactileEngine.playCardPop()
            persistProfileMutation(
                updated = profile.copy(equippedBadge = badgeId.name),
                cloudFields = mapOf("equippedBadge" to badgeId.name)
            )
            return
        }
        if (profile.gems < badgeId.gemCost) {
            showBanner("Need ${badgeId.gemCost - profile.gems} more Gems to unlock ${badgeId.displayName}")
            return
        }
        tactileEngine.playUnlock()
        val nextGems = profile.gems - badgeId.gemCost
        val nextCosmetics = (profile.unlockedCosmetics + badgeId.name).distinct()
        persistProfileMutation(
            updated = profile.copy(
                gems = nextGems,
                unlockedCosmetics = nextCosmetics,
                equippedBadge = badgeId.name
            ),
            cloudFields = mapOf(
                "gems" to nextGems,
                "unlockedCosmetics" to nextCosmetics,
                "equippedBadge" to badgeId.name
            )
        )
        showBanner("Equipped ${badgeId.displayName} ring aura!")
    }

    fun claimMilestone(milestone: PathMilestone) {
        if (!milestone.isUnlocked || milestone.isClaimed) return
        val profile = userProfile.value
        if (milestone.id in profile.claimedMilestones) return
        tactileEngine.playGemSparkle()
        val nextClaimed = (profile.claimedMilestones + milestone.id).distinct()
        val nextGems = profile.gems + milestone.gemReward
        val nextTotalGems = profile.totalGemsEarned + milestone.gemReward
        persistProfileMutation(
            updated = profile.copy(
                gems = nextGems,
                totalGemsEarned = nextTotalGems,
                claimedMilestones = nextClaimed
            ),
            cloudFields = mapOf(
                "gems" to nextGems,
                "totalGemsEarned" to nextTotalGems,
                "claimedMilestones" to nextClaimed
            )
        )
        showGemReward("${milestone.title} Chest!", milestone.gemReward)
    }

    private fun awardBonusGems(amount: Int, reason: String) {
        val profile = userProfile.value
        val nextGems = profile.gems + amount
        val nextTotal = profile.totalGemsEarned + amount
        persistProfileMutation(
            updated = profile.copy(gems = nextGems, totalGemsEarned = nextTotal),
            cloudFields = mapOf("gems" to nextGems, "totalGemsEarned" to nextTotal)
        )
        showGemReward(reason, amount)
    }

    // --- Profile & Preferences ---

    fun updateDisplayName(name: String) {
        val clean = name.trim().take(60)
        if (clean.isEmpty()) return
        tactileEngine.playTap()
        val profile = userProfile.value
        persistProfileMutation(
            updated = profile.copy(displayName = clean),
            cloudFields = mapOf("displayName" to clean)
        )
        showBanner("Name updated to $clean")
    }

    fun updateDailyGoalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(15, 720)
        val profile = userProfile.value
        persistProfileMutation(
            updated = profile.copy(dailyGoalMinutes = clamped),
            cloudFields = mapOf("dailyGoalMinutes" to clamped)
        )
    }

    fun updateWeeklyGoalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(60, 3600)
        viewModelScope.launch {
            prefsStore.setWeeklyGoalMinutes(clamped)
        }
    }

    fun setSelectedGoalPeriod(period: StudyGoalPeriod) {
        viewModelScope.launch {
            prefsStore.setSelectedGoalPeriod(period)
        }
    }

    fun saveStudyGoals(
        period: StudyGoalPeriod,
        dailyMinutes: Int,
        weeklyMinutes: Int
    ) {
        val clampedDaily = dailyMinutes.coerceIn(15, 720)
        val clampedWeekly = weeklyMinutes.coerceIn(60, 3600)
        val profile = userProfile.value
        persistProfileMutation(
            updated = profile.copy(dailyGoalMinutes = clampedDaily),
            cloudFields = mapOf("dailyGoalMinutes" to clampedDaily)
        )
        viewModelScope.launch {
            prefsStore.setWeeklyGoalMinutes(clampedWeekly)
            prefsStore.setSelectedGoalPeriod(period)
        }
        tactileEngine.playCardPop()
        val weeklyHoursText = if (clampedWeekly % 60 == 0) {
            "${clampedWeekly / 60}h"
        } else {
            "${clampedWeekly / 60}h ${clampedWeekly % 60}m"
        }
        showBanner("Goals saved: ${clampedDaily}m/day • $weeklyHoursText/week")
    }

    fun setUiClickSoundsEnabled(enabled: Boolean) {
        tactileEngine.soundsEnabled = enabled
        if (enabled) tactileEngine.playToggle(true)
        viewModelScope.launch { prefsStore.setUiClickSoundsEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        tactileEngine.hapticsEnabled = enabled
        if (enabled) tactileEngine.playToggle(true)
        viewModelScope.launch { prefsStore.setHapticsEnabled(enabled) }
    }

    fun setAmbientAutoPlayWithTimer(enabled: Boolean) {
        tactileEngine.playToggle(enabled)
        viewModelScope.launch { prefsStore.setAmbientAutoPlayWithTimer(enabled) }
    }

    fun setHideStudyHistory(hide: Boolean) {
        tactileEngine.playToggle(!hide)
        viewModelScope.launch { prefsStore.setHideStudyHistory(hide) }
        showBanner(
            if (hide) "Recent Study History hidden — tap Show anytime."
            else "Recent Study History visible."
        )
    }

    fun setCustomAppIcon(iconId: CustomAppIconId) {
        tactileEngine.playCardPop()
        viewModelScope.launch { prefsStore.setCustomAppIconId(iconId) }
        showBanner("App icon switched to ${iconId.displayName}!")
    }

    fun setCustomIconShape(shapeId: AppIconShapeId) {
        tactileEngine.playTap()
        viewModelScope.launch { prefsStore.setCustomIconShapeId(shapeId) }
        showBanner("Icon shape set to ${shapeId.displayName}")
    }

    fun notifyCustomShortcutResult(message: String) {
        showBanner(message)
    }

    // --- Atmosphere Gradient Lighting, Haptics & Galaxy Study Launcher ---

    fun setAmbientLightingStyle(style: AmbientLightingStyle) {
        tactileEngine.playCardPop()
        viewModelScope.launch { prefsStore.setAmbientLightingStyle(style) }
        showBanner("Ambient lighting: ${style.displayName}")
    }

    fun setAmbientLightingIntensity(intensity: Float) {
        viewModelScope.launch { prefsStore.setAmbientLightingIntensity(intensity) }
    }

    fun setAmbientEdgeGlowEnabled(enabled: Boolean) {
        tactileEngine.playToggle(enabled)
        viewModelScope.launch { prefsStore.setAmbientEdgeGlowEnabled(enabled) }
    }

    fun setHapticIntensity(level: HapticIntensityLevel) {
        tactileEngine.previewHapticStrength(level.scale)
        viewModelScope.launch { prefsStore.setHapticIntensity(level) }
        showBanner("Vibration strength: ${level.displayName}")
    }

    // --- Screen & App Usage Tracking ---

    fun setAppInForeground(inForeground: Boolean) {
        if (inForeground) {
            refreshScreenUsage()
            if (appUsageTickerJob?.isActive == true) return
            appUsageTickerJob = viewModelScope.launch {
                var secondsSinceFlush = 0
                while (isActive) {
                    delay(1_000L)
                    unsavedForegroundSeconds += 1
                    secondsSinceFlush += 1
                    val liveTotalSec = persistedAppUsageSecondsToday + unsavedForegroundSeconds
                    _screenUsageState.update { state ->
                        val liveMs = liveTotalSec * 1000L
                        val updatedThisApp = maxOf(state.thisAppUsageMillis, liveMs)
                        val delta = (updatedThisApp - state.thisAppUsageMillis).coerceAtLeast(0L)
                        state.copy(
                            thisAppUsageMillis = updatedThisApp,
                            totalScreenTimeMillis = if (state.hasUsagePermission) {
                                state.totalScreenTimeMillis + delta
                            } else {
                                state.totalScreenTimeMillis
                            }
                        )
                    }
                    if (secondsSinceFlush >= 10) {
                        flushForegroundAppUsage()
                        secondsSinceFlush = 0
                    }
                }
            }
        } else {
            appUsageTickerJob?.cancel()
            appUsageTickerJob = null
            viewModelScope.launch { flushForegroundAppUsage() }
        }
    }

    fun refreshScreenUsage() {
        viewModelScope.launch {
            val todayKey = todayDateKey()
            val stored = prefsStore.appUsageSecondsForDateFlow(todayKey).first()
            if (stored > persistedAppUsageSecondsToday) {
                persistedAppUsageSecondsToday = stored
            }
            val liveSec = persistedAppUsageSecondsToday + unsavedForegroundSeconds
            val snapshot = withContext(Dispatchers.IO) {
                ScreenUsageTracker.queryTodayUsage(appContext, liveSec)
            }
            _screenUsageState.value = snapshot
        }
    }

    fun openUsageAccessSettings() {
        tactileEngine.playTap()
        ScreenUsageTracker.openUsageAccessSettings(appContext)
    }

    private suspend fun flushForegroundAppUsage() {
        val toSave = unsavedForegroundSeconds
        if (toSave <= 0) return
        unsavedForegroundSeconds = 0
        persistedAppUsageSecondsToday += toSave
        prefsStore.addAppUsageSeconds(todayDateKey(), toSave)
    }

    // --- Social Study Circle ---

    fun addFriendByHandle(rawInput: String) {
        val repo = firestoreRepo
        if (repo == null) {
            _socialError.value = "Sign in with Google to add study friends."
            return
        }
        if (rawInput.isBlank()) return
        _socialError.value = null
        _isAddingFriend.value = true
        viewModelScope.launch {
            val result = repo.addFriendByHandle(rawInput)
            _isAddingFriend.value = false
            result.onSuccess { friend ->
                tactileEngine.playUnlock()
                showBanner("Added ${friend.displayName} (@${friend.handle}) to Study Circle!")
            }.onFailure { err ->
                _socialError.value = err.message ?: "Could not add friend. Check the @handle and try again."
            }
        }
    }

    fun removeFriend(friendUid: String) {
        val repo = firestoreRepo ?: return
        viewModelScope.launch {
            repo.removeFriend(friendUid)
                .onSuccess { showBanner("Removed friend from Study Circle") }
                .onFailure { _socialError.value = "Could not remove friend right now." }
        }
    }

    fun setPrivateStudyMode(enabled: Boolean) {
        tactileEngine.playToggle(enabled)
        viewModelScope.launch {
            prefsStore.setPrivateStudyMode(enabled)
        }
        showBanner(
            if (enabled) "Private Study Mode ON — live status & subject hidden"
            else "Private Study Mode OFF — live focus presence resumed"
        )
    }

    fun blockUser(targetUid: String, targetHandle: String, targetDisplayName: String) {
        val repo = firestoreRepo ?: return
        tactileEngine.playTap()
        viewModelScope.launch {
            repo.blockUser(targetUid, targetHandle, targetDisplayName)
                .onSuccess {
                    showBanner("Blocked $targetDisplayName safely.")
                }
                .onFailure {
                    _socialError.value = "Could not block scholar right now."
                }
        }
    }

    fun unblockUser(targetUid: String) {
        val repo = firestoreRepo ?: return
        tactileEngine.playTap()
        viewModelScope.launch {
            repo.unblockUser(targetUid)
                .onSuccess { showBanner("Scholar unblocked.") }
                .onFailure { _socialError.value = "Could not unblock right now." }
        }
    }

    fun clearSocialError() {
        _socialError.value = null
    }

    fun notifyHandleCopied(handleWithAt: String) {
        showBanner("Copied $handleWithAt — share with friends!")
    }

    fun updateMyStudyHandle(rawHandle: String) {
        val repo = firestoreRepo
        if (repo == null) {
            _socialError.value = "Sign in with Google to customize your @handle."
            return
        }
        val trimmed = rawHandle.trim()
        if (trimmed.isBlank()) {
            _socialError.value = "Enter a handle (3–30 lowercase letters, numbers, or underscores)."
            return
        }
        _socialError.value = null
        _isUpdatingHandle.value = true
        viewModelScope.launch {
            val result = repo.updateStudyHandle(
                rawHandle = trimmed,
                displayName = userProfile.value.displayName
            )
            _isUpdatingHandle.value = false
            result.onSuccess { updatedHandle ->
                tactileEngine.playUnlock()
                showBanner("Your study handle is now @$updatedHandle!")
            }.onFailure { err ->
                _socialError.value = err.message ?: "Could not update @handle right now."
            }
        }
    }

    // --- Real-Time 1:1 Direct Messaging with Study Circle Friends ---

    fun openFriendChat(friend: PublicStudyProfile) {
        val repo = firestoreRepo
        if (repo == null) {
            _socialError.value = "Sign in with Google to message friends."
            return
        }
        tactileEngine.playTap()
        activeChatJob?.cancel()
        _activeChatFriend.value = friend
        _chatMessages.value = emptyList()
        _chatError.value = null
        _isChatLoading.value = true

        activeChatJob = viewModelScope.launch {
            repo.observeDirectMessages(friend.userId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    _isChatLoading.value = false
                    _chatError.value = error.message ?: "Could not load chat messages."
                }
                .collect { msgs ->
                    _isChatLoading.value = false
                    _chatMessages.value = msgs
                }
        }
    }

    fun closeFriendChat() {
        activeChatJob?.cancel()
        activeChatJob = null
        _activeChatFriend.value = null
        _chatMessages.value = emptyList()
        _chatError.value = null
        _isChatLoading.value = false
        _isChatSending.value = false
    }

    fun sendFriendMessage(rawText: String) {
        val repo = firestoreRepo ?: return
        val friend = _activeChatFriend.value ?: return
        val clean = rawText.trim()
        if (clean.isEmpty() || _isChatSending.value) return

        _isChatSending.value = true
        _chatError.value = null
        tactileEngine.playTap()
        viewModelScope.launch {
            val result = repo.sendDirectMessage(
                friendUid = friend.userId,
                senderDisplayName = userProfile.value.displayName,
                rawText = clean
            )
            _isChatSending.value = false
            result.onSuccess {
                tactileEngine.playCardPop()
            }.onFailure { err ->
                _chatError.value = err.message ?: "Message could not be sent right now."
            }
        }
    }

    fun clearChatError() {
        _chatError.value = null
    }

    fun refreshLeaderboard() {
        tactileEngine.playTap()
        _leaderboardRefreshTick.update { it + 1 }
    }

    // --- Group Study Challenges ---

    fun createStudyChallenge(
        title: String,
        subject: String,
        targetMinutes: Int,
        invitedFriendUids: List<String>
    ) {
        val repo = firestoreRepo
        if (repo == null) {
            _challengeError.value = "Sign in with Google to create a Study Challenge."
            return
        }
        _challengeError.value = null
        _isChallengeBusy.value = true
        val profile = userProfile.value
        val handle = myStudyProfileFlow.value?.handle.orEmpty()
        viewModelScope.launch {
            val result = repo.createStudyChallenge(
                title = title,
                subject = subject,
                targetMinutes = targetMinutes,
                creatorDisplayName = profile.displayName,
                creatorHandle = handle,
                invitedFriendUids = invitedFriendUids
            )
            _isChallengeBusy.value = false
            result.onSuccess { created ->
                tactileEngine.playUnlock()
                showBanner("Challenge created! Share code ${created.inviteCode} with friends.")
            }.onFailure { err ->
                _challengeError.value = err.message ?: "Could not create Study Challenge right now."
            }
        }
    }

    fun joinChallengeByCode(rawCode: String) {
        val repo = firestoreRepo
        if (repo == null) {
            _challengeError.value = "Sign in with Google to join a Study Challenge."
            return
        }
        _challengeError.value = null
        _isChallengeBusy.value = true
        val profile = userProfile.value
        val handle = myStudyProfileFlow.value?.handle.orEmpty()
        viewModelScope.launch {
            val result = repo.joinChallengeByCode(
                rawCode = rawCode,
                displayName = profile.displayName,
                handle = handle
            )
            _isChallengeBusy.value = false
            result.onSuccess { joined ->
                tactileEngine.playUnlock()
                showBanner("Joined \"${joined.title}\"!")
            }.onFailure { err ->
                _challengeError.value = err.message ?: "Could not find a challenge with that code."
            }
        }
    }

    fun acceptChallengeInvite(challengeId: String) {
        val repo = firestoreRepo ?: return
        _challengeError.value = null
        _isChallengeBusy.value = true
        val profile = userProfile.value
        val handle = myStudyProfileFlow.value?.handle.orEmpty()
        viewModelScope.launch {
            val result = repo.acceptChallengeInvite(
                challengeId = challengeId,
                displayName = profile.displayName,
                handle = handle
            )
            _isChallengeBusy.value = false
            result.onSuccess { joined ->
                tactileEngine.playUnlock()
                showBanner("Joined \"${joined.title}\"!")
            }.onFailure { err ->
                _challengeError.value = err.message ?: "Could not accept challenge invite."
            }
        }
    }

    fun declineChallengeInvite(challengeId: String) {
        val repo = firestoreRepo ?: return
        _challengeError.value = null
        _isChallengeBusy.value = true
        tactileEngine.playTap()
        viewModelScope.launch {
            val result = repo.declineChallengeInvite(challengeId)
            _isChallengeBusy.value = false
            result.onSuccess {
                showBanner("Challenge invite declined.")
            }.onFailure { err ->
                _challengeError.value = err.message ?: "Could not decline challenge invite."
            }
        }
    }

    fun inviteFriendsToChallenge(challengeId: String, friendUids: List<String>) {
        val repo = firestoreRepo ?: return
        if (friendUids.isEmpty()) return
        _challengeError.value = null
        _isChallengeBusy.value = true
        viewModelScope.launch {
            val result = repo.inviteFriendsToChallenge(challengeId, friendUids)
            _isChallengeBusy.value = false
            result.onSuccess { count ->
                tactileEngine.playCardPop()
                if (count > 0) {
                    showBanner("Sent challenge invite to $count ${if (count == 1) "friend" else "friends"}!")
                }
            }.onFailure { err ->
                _challengeError.value = err.message ?: "Could not invite friends right now."
            }
        }
    }

    fun leaveOrDeleteChallenge(challenge: StudyChallenge) {
        val repo = firestoreRepo ?: return
        _challengeError.value = null
        _isChallengeBusy.value = true
        viewModelScope.launch {
            val result = repo.leaveOrDeleteChallenge(challenge)
            _isChallengeBusy.value = false
            result.onSuccess { deleted ->
                showBanner(if (deleted) "Deleted \"${challenge.title}\"" else "Left \"${challenge.title}\"")
            }.onFailure { err ->
                _challengeError.value = err.message ?: "Could not update challenge membership."
            }
        }
    }

    fun clearChallengesError() {
        _challengeError.value = null
    }

    fun notifyChallengeCodeCopied(code: String) {
        showBanner("Copied challenge code $code!")
    }

    // --- Firestore 'admin' Role Dashboard Actions (Zero Personal User Data) ---

    fun refreshAdminUserCounts() {
        val repo = firestoreRepo ?: return
        if (!isAdminRole.value || _isRefreshingUserCounts.value) return
        _isRefreshingUserCounts.value = true
        viewModelScope.launch {
            val result = repo.fetchAdminUserCounts(
                todayDateKey = todayDateKey(),
                fallbackDirectory = adminPublicDirectoryFlow.value
            )
            _isRefreshingUserCounts.value = false
            result.onSuccess { snapshot ->
                _serverUserCounts.value = snapshot
            }.onFailure { err ->
                Log.w(TAG, "Admin user count refresh warning: ${err.message}")
            }
        }
    }

    fun adminPublishLiveConfig(updatedConfig: AppLiveConfig) {
        val repo = firestoreRepo ?: return
        if (!isAdminRole.value || _isAdminPushingUpdate.value) return
        _isAdminPushingUpdate.value = true
        _adminError.value = null
        _adminFeedback.value = null
        viewModelScope.launch {
            val result = repo.publishLiveAppConfig(updatedConfig)
            _isAdminPushingUpdate.value = false
            result.onSuccess { pushed ->
                _liveAppConfig.value = pushed
                prefsStore.saveCachedLiveConfig(pushed)
                tactileEngine.playUnlock()
                _adminFeedback.value = "App settings v${pushed.configVersion} synced silently to all devices!"
            }.onFailure { err ->
                _adminError.value = err.message ?: "Could not publish app settings right now."
            }
        }
    }

    fun adminSendNotice(
        targetUid: String?,
        targetLabel: String,
        title: String,
        body: String,
        kind: AdminNoticeKind
    ) {
        val repo = firestoreRepo ?: return
        if (!isAdminRole.value || _isAdminSendingNotice.value) return
        _isAdminSendingNotice.value = true
        _adminError.value = null
        _adminFeedback.value = null
        viewModelScope.launch {
            val result = repo.sendAdminNotice(
                targetUid = targetUid,
                targetLabel = targetLabel,
                title = title,
                body = body,
                kind = kind
            )
            _isAdminSendingNotice.value = false
            result.onSuccess {
                tactileEngine.playCardPop()
                _adminFeedback.value = if (targetUid.isNullOrBlank()) {
                    "Broadcast delivered silently to all scholars!"
                } else {
                    "Direct notice sent to $targetLabel!"
                }
            }.onFailure { err ->
                _adminError.value = err.message ?: "Could not send notice right now."
            }
        }
    }

    fun adminDeleteNotice(noticeId: String) {
        val repo = firestoreRepo ?: return
        if (!isAdminRole.value) return
        _adminError.value = null
        viewModelScope.launch {
            repo.deleteAdminNotice(noticeId)
                .onSuccess {
                    tactileEngine.playTap()
                    _adminFeedback.value = "Notice removed."
                }
                .onFailure { err ->
                    _adminError.value = err.message ?: "Could not delete notice."
                }
        }
    }

    fun adminDeleteUserAccount(
        profile: PublicStudyProfile,
        reason: String
    ) {
        val repo = firestoreRepo ?: return
        if (!isAdminRole.value || _adminDeletingUid.value != null) return
        _adminDeletingUid.value = profile.userId
        _adminError.value = null
        _adminFeedback.value = null
        viewModelScope.launch {
            val result = repo.adminDeleteUserAccount(
                targetUid = profile.userId,
                targetHandle = profile.handle,
                reason = reason
            )
            _adminDeletingUid.value = null
            result.onSuccess {
                tactileEngine.playTap()
                _adminFeedback.value = "Deleted public account @${profile.handle} (${profile.displayName})."
                refreshAdminUserCounts()
            }.onFailure { err ->
                _adminError.value = err.message ?: "Could not delete account."
            }
        }
    }

    fun clearAdminMessages() {
        _adminFeedback.value = null
        _adminError.value = null
    }

    // --- Multi-Task Subject Study Alarms ---

    fun addSubjectAlarm(
        subject: String,
        hour: Int,
        minute: Int,
        durationMinutes: Int = 25,
        note: String = ""
    ) {
        val cleanSubject = subject.trim().take(28)
        if (cleanSubject.isBlank()) return
        val newAlarm = SubjectStudyAlarm(
            id = UUID.randomUUID().toString().take(12),
            subject = cleanSubject,
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            durationMinutes = durationMinutes.coerceIn(10, 120),
            enabled = true,
            note = note.trim().take(80)
        )
        val updated = (subjectAlarms.value + newAlarm)
            .sortedWith(compareBy<SubjectStudyAlarm> { it.hour }.thenBy { it.minute })
        viewModelScope.launch {
            prefsStore.saveSubjectAlarms(updated)
            SubjectStudyAlarmScheduler.syncAllAlarms(appContext, updated)
            tactileEngine.playCardPop()
            showBanner("${newAlarm.subject} alarm set for ${newAlarm.formattedTime}!")
        }
    }

    fun toggleSubjectAlarm(alarmId: String, enabled: Boolean) {
        val current = subjectAlarms.value
        val target = current.firstOrNull { it.id == alarmId } ?: return
        val updated = current.map { if (it.id == alarmId) it.copy(enabled = enabled) else it }
        viewModelScope.launch {
            prefsStore.saveSubjectAlarms(updated)
            SubjectStudyAlarmScheduler.syncAllAlarms(appContext, updated)
            tactileEngine.playToggle(enabled)
            showBanner(
                if (enabled) "${target.subject} alarm ON (${target.formattedTime})"
                else "${target.subject} alarm paused"
            )
        }
    }

    fun deleteSubjectAlarm(alarmId: String) {
        val current = subjectAlarms.value
        val target = current.firstOrNull { it.id == alarmId } ?: return
        val updated = current.filterNot { it.id == alarmId }
        viewModelScope.launch {
            SubjectStudyAlarmScheduler.cancelAlarm(appContext, target.id)
            prefsStore.saveSubjectAlarms(updated)
            tactileEngine.playTap()
            showBanner("Removed ${target.subject} alarm (${target.formattedTime})")
        }
    }

    fun applyAlarmLaunch(subject: String, durationMinutes: Int) {
        if (subject.isBlank()) return
        selectSubject(subject)
        val cur = timerState.value
        if (!cur.isRunning && !cur.isPaused) {
            setTimerDuration(durationMinutes.coerceIn(10, 120))
        }
        showBanner("Ready to study $subject (${durationMinutes}m)!")
    }

    // --- Scholar Profile Details & Zero-Permission Avatar Photo ---

    fun updateScholarDetails(details: ScholarProfileDetails) {
        viewModelScope.launch {
            prefsStore.saveScholarDetails(details)
            tactileEngine.playCardPop()
            showBanner("Scholar profile details saved!")
        }
    }

    fun saveProfileAvatarFromUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val input = appContext.contentResolver.openInputStream(uri) ?: return@runCatching null
                    val original = input.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
                    val maxDim = 512
                    val scale = minOf(1f, maxDim.toFloat() / maxOf(original.width, original.height).coerceAtLeast(1))
                    val targetW = (original.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (original.height * scale).toInt().coerceAtLeast(1)
                    val scaled = if (scale < 1f) {
                        Bitmap.createScaledBitmap(original, targetW, targetH, true)
                    } else {
                        original
                    }
                    val destFile = File(appContext.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                    // Clean up older avatar files
                    appContext.filesDir.listFiles()?.forEach { f ->
                        if (f.name.startsWith("profile_avatar_") && f.name.endsWith(".jpg") && f.name != destFile.name) {
                            f.delete()
                        }
                    }
                    FileOutputStream(destFile).use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    }
                    if (scaled !== original) scaled.recycle()
                    original.recycle()
                    destFile.absolutePath
                }.getOrNull()
            }
            if (savedPath != null) {
                val updated = scholarDetails.value.copy(avatarLocalPath = savedPath)
                prefsStore.saveScholarDetails(updated)
                tactileEngine.playUnlock()
                showBanner("Profile photo updated!")
            } else {
                showBanner("Could not load selected photo. Please try another image.")
            }
        }
    }

    fun clearProfileAvatar() {
        viewModelScope.launch {
            val currentPath = scholarDetails.value.avatarLocalPath
            if (currentPath.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    runCatching { File(currentPath).delete() }
                }
            }
            prefsStore.saveScholarDetails(scholarDetails.value.copy(avatarLocalPath = ""))
            tactileEngine.playTap()
            showBanner("Profile photo reset to custom emblem.")
        }
    }

    // --- 500-Hour Pro Edition AI Studio (Strictly Gated, Zero Bypass) ---

    fun generateProStudyNotes(subject: String, topic: String) {
        if (!proUnlockStatus.value.isUnlocked) {
            _proAiState.value = ProAiStudioUiState.Error(
                "Pro Edition is strictly locked until you complete 500 verified study hours (${String.format(Locale.US, "%.1f", proUnlockStatus.value.remainingHours)} hrs remaining)."
            )
            return
        }
        val cleanTopic = topic.trim()
        if (cleanTopic.isBlank()) {
            _proAiState.value = ProAiStudioUiState.Error("Enter a chapter or topic to generate study notes & practice questions.")
            return
        }
        _proAiState.value = ProAiStudioUiState.Loading("Crafting Pro notes & practice Q&A for $cleanTopic…")
        viewModelScope.launch {
            val result = geminiService.generateProStudyPack(subject = subject, topic = cleanTopic)
            result.onSuccess { pack ->
                tactileEngine.playUnlock()
                _proAiState.value = ProAiStudioUiState.NotesReady(pack)
            }.onFailure { err ->
                _proAiState.value = ProAiStudioUiState.Error(
                    err.localizedMessage ?: "Could not generate Pro study notes right now. Please try again."
                )
            }
        }
    }

    fun askProAiTutor(subject: String, question: String) {
        if (!proUnlockStatus.value.isUnlocked) {
            _proAiState.value = ProAiStudioUiState.Error(
                "Pro AI Tutor unlocks strictly after 500 verified study hours (${String.format(Locale.US, "%.1f", proUnlockStatus.value.remainingHours)} hrs left)."
            )
            return
        }
        val cleanQ = question.trim()
        if (cleanQ.isBlank()) return
        _proAiState.value = ProAiStudioUiState.Loading("Thinking through your $subject question…")
        viewModelScope.launch {
            val result = geminiService.askProAiTutor(subject = subject, question = cleanQ)
            result.onSuccess { answer ->
                tactileEngine.playCardPop()
                _proAiState.value = ProAiStudioUiState.TutorAnswer(question = cleanQ, answer = answer)
            }.onFailure { err ->
                _proAiState.value = ProAiStudioUiState.Error(
                    err.localizedMessage ?: "Pro AI Tutor could not respond right now. Please retry."
                )
            }
        }
    }

    fun clearProAiState() {
        _proAiState.value = ProAiStudioUiState.Idle
    }

    override fun onCleared() {
        appUsageTickerJob?.cancel()
        activeChatJob?.cancel()
        super.onCleared()
    }

    // --- Helpers ---

    private fun persistProfileMutation(updated: UserProfile, cloudFields: Map<String, Any>) {
        viewModelScope.launch {
            prefsStore.saveLocalProfile(updated)
            firestoreRepo?.updateProfileFields(cloudFields)
        }
    }

    private fun showBanner(message: String) {
        _statusBanner.value = message
        viewModelScope.launch {
            delay(3200L)
            if (_statusBanner.value == message) _statusBanner.value = null
        }
    }

    private fun showGemReward(title: String, gemsDelta: Int, xpDelta: Int = 0) {
        val banner = GemRewardBanner(title = title, gemsDelta = gemsDelta, xpDelta = xpDelta)
        _gemRewardBanner.value = banner
        viewModelScope.launch {
            delay(3800L)
            if (_gemRewardBanner.value?.timestampMs == banner.timestampMs) {
                _gemRewardBanner.value = null
            }
        }
    }

    private fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    class Factory(
        private val application: Application,
        private val isCloudSignedIn: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TryAgainViewModel(application, isCloudSignedIn) as T
        }
    }
}

private data class SocialFlagsState(
    val isSyncingCircle: Boolean,
    val isAddingFriend: Boolean,
    val isUpdatingHandle: Boolean,
    val errorMessage: String?
)

private data class StudyPresenceSnapshot(
    val displayName: String,
    val status: StudyLiveStatus,
    val currentSubject: String,
    val todayFocusMinutes: Int,
    val dailyGoalMinutes: Int,
    val currentStreak: Int,
    val totalFocusMinutes: Int,
    val level: Int,
    val equippedBadge: String,
    val lastFocusDate: String
)

private data class StreakReminderSyncKey(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val currentStreak: Int,
    val lastFocusDate: String
)

private data class AdminActionFlags(
    val isPushingUpdate: Boolean,
    val isSendingNotice: Boolean,
    val deletingUserId: String?,
    val feedbackMessage: String?,
    val errorMessage: String?
)

