package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.AiCoachUiState
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.AppPreferencesState
import com.example.data.model.DayFocusPoint
import com.example.data.model.FocusSession
import com.example.data.model.GemRewardBanner
import com.example.data.model.GlassThemeId
import com.example.data.model.MascotBadgeId
import com.example.data.model.PathMilestone
import com.example.data.model.PomodoroPhase
import com.example.data.model.PomodoroTask
import com.example.data.model.StreakDayProgress
import com.example.data.model.TimerUiState
import com.example.data.model.UserProfile
import com.example.data.repository.FocusFirestoreRepository
import com.example.data.repository.GeminiCoachRepository
import com.example.service.AmbientAudioService
import com.example.service.AmbientSoundEngine
import com.example.service.TactileSoundEngine
import com.example.ui.components.PomodoroTimerController
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    private val geminiCoachRepo = GeminiCoachRepository()
    val tactileEngine = TactileSoundEngine(appContext)
    private val ambientEngine: AmbientSoundEngine = AmbientSoundEngine.shared

    private val timerController = PomodoroTimerController()
    private var tickerJob: Job? = null
    private var aiJob: Job? = null

    val preferences: StateFlow<AppPreferencesState> = prefsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferencesState())

    private val localProfileFlow = prefsStore.localProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    private val localDailyLogsFlow = prefsStore.localDailyLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val localSessions = MutableStateFlow<List<FocusSession>>(emptyList())
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

    val sessions: StateFlow<List<FocusSession>> = if (firestoreRepo != null) {
        firestoreRepo.observeSessions()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Sessions stream error", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    } else {
        localSessions.asStateFlow()
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

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    val activeAmbientPreset: StateFlow<AmbientSoundPreset> = ambientEngine.selectedPreset
    val isAmbientPlaying: StateFlow<Boolean> = ambientEngine.isPlaying
    val ambientWaveAmplitude: StateFlow<Float> = ambientEngine.waveAmplitude

    private val _aiCoachState = MutableStateFlow<AiCoachUiState>(AiCoachUiState.Idle)
    val aiCoachState: StateFlow<AiCoachUiState> = _aiCoachState.asStateFlow()

    private val dailyStatsByDate: StateFlow<Map<String, Pair<Int, Int>>> = combine(
        sessions,
        localDailyLogsFlow
    ) { sessList, persistedLocal ->
        val fromSessions = sessList.groupBy { it.dateKey }.mapValues { (_, list) ->
            list.sumOf { it.completedMinutes } to list.size
        }
        if (firestoreRepo != null) {
            fromSessions
        } else {
            val merged = persistedLocal.toMutableMap()
            fromSessions.forEach { (dateKey, pair) ->
                val existing = merged[dateKey]
                if (existing == null || pair.first > existing.first || pair.second > existing.second) {
                    merged[dateKey] = pair
                }
            }
            merged
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val todayMinutes: StateFlow<Int> = dailyStatsByDate.map { byDate ->
        byDate[todayDateKey()]?.first ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val weeklyPoints: StateFlow<List<DayFocusPoint>> = dailyStatsByDate.map { byDate ->
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val key = keyFormat.format(c.time)
            DayFocusPoint(
                dayLabel = dayFormat.format(c.time).take(3),
                dateKey = key,
                minutes = byDate[key]?.first ?: 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
                tactileEngine.hapticsEnabled = pref.hapticsEnabled
                ambientEngine.setVolume(pref.ambientVolume)
            }
        }
        if (firestoreRepo != null) {
            viewModelScope.launch {
                val seed = prefsStore.localProfileFlow.first()
                firestoreRepo.ensureUserProfile(seed)
            }
        }
    }

    // --- Timer Controls ---

    fun selectSubject(subject: String) {
        _timerState.update { it.copy(subject = subject) }
    }

    fun selectPhase(phase: PomodoroPhase) {
        timerController.selectPhase(phase)
        _timerState.value = snapshotTimerState()
    }

    fun setTimerDuration(minutes: Int) {
        timerController.setDurationMinutes(minutes)
        _timerState.value = snapshotTimerState()
    }

    fun adjustTimerDuration(delta: Int) {
        timerController.adjustDurationMinutes(delta)
        _timerState.value = snapshotTimerState()
    }

    fun pinTaskToTimer(taskId: String?) {
        val nextId = if (_timerState.value.activeTaskId == taskId) null else taskId
        _timerState.update { it.copy(activeTaskId = nextId) }
    }

    fun startOrResumeTimer() {
        timerController.start()
        _timerState.value = snapshotTimerState()

        val prefs = preferences.value
        if (timerController.phase == PomodoroPhase.FOCUS && prefs.ambientAutoPlayWithTimer) {
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
                    subtitle = "Focusing on ${_timerState.value.subject}"
                )
            }
        }

        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive && timerController.isRunning) {
                delay(1000L)
                val finished = timerController.tickOneSecond()
                _timerState.value = snapshotTimerState()
                if (finished != null) {
                    onPhaseCompleted(finished.first, finished.second)
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        tickerJob?.cancel()
        timerController.pause()
        _timerState.value = snapshotTimerState()
        if (preferences.value.ambientAutoPlayWithTimer && isAmbientPlaying.value) {
            AmbientAudioService.pause(appContext, "Focus timer paused")
        }
    }

    fun resetTimer() {
        tickerJob?.cancel()
        timerController.reset()
        _timerState.value = snapshotTimerState()
        if (preferences.value.ambientAutoPlayWithTimer && isAmbientPlaying.value) {
            AmbientAudioService.pause(appContext, "Focus timer reset")
        }
    }

    fun completeOrSkipPhase() {
        tickerJob?.cancel()
        val (finishedPhase, completedMins) = timerController.completeCurrentPhase(autoFinished = false)
        _timerState.value = snapshotTimerState()
        onPhaseCompleted(finishedPhase, completedMins)
    }

    private fun onPhaseCompleted(phase: PomodoroPhase, completedMinutes: Int) {
        if (preferences.value.ambientAutoPlayWithTimer && isAmbientPlaying.value) {
            AmbientAudioService.pause(appContext, "Session complete — take a breather")
        }

        if (phase != PomodoroPhase.FOCUS) {
            tactileEngine.playToggle(true)
            showBanner("Break finished! Ready to focus again.")
            return
        }

        tactileEngine.playSessionComplete()
        val subject = _timerState.value.subject
        val soundLabel = activeAmbientPreset.value.displayName
        val gemsEarned = (completedMinutes * 2) + 15
        val xpEarned = completedMinutes * 3
        val today = todayDateKey()
        val current = userProfile.value

        val newStreak = when {
            current.lastFocusDate == today -> current.currentStreak.coerceAtLeast(1)
            current.lastFocusDate == yesterdayDateKey() -> current.currentStreak + 1
            else -> 1
        }
        val newLongest = maxOf(current.longestStreak, newStreak)
        val newTotalMinutes = current.totalFocusMinutes + completedMinutes
        val newXp = current.focusXp + xpEarned
        val newGems = current.gems + gemsEarned
        val newTotalGems = current.totalGemsEarned + gemsEarned

        val pinnedTaskId = _timerState.value.activeTaskId

        viewModelScope.launch {
            if (firestoreRepo != null) {
                firestoreRepo.logSessionAndUpdateProfile(
                    subject = subject,
                    plannedMinutes = timerController.focusMinutes,
                    completedMinutes = completedMinutes,
                    ambientSound = soundLabel,
                    xpEarned = xpEarned,
                    gemsEarned = gemsEarned,
                    dateKey = today,
                    updatedProfileFields = mapOf(
                        "currentStreak" to newStreak,
                        "longestStreak" to newLongest,
                        "totalFocusMinutes" to newTotalMinutes,
                        "focusXp" to newXp,
                        "gems" to newGems,
                        "totalGemsEarned" to newTotalGems,
                        "lastFocusDate" to today
                    )
                )
            } else {
                val newSession = FocusSession(
                    id = UUID.randomUUID().toString(),
                    userId = current.userId,
                    subject = subject,
                    plannedMinutes = timerController.focusMinutes,
                    completedMinutes = completedMinutes,
                    ambientSound = soundLabel,
                    xpEarned = xpEarned,
                    gemsEarned = gemsEarned,
                    dateKey = today,
                    createdAt = Timestamp.now()
                )
                localSessions.update { listOf(newSession) + it }
                prefsStore.recordLocalDayFocus(today, completedMinutes)
                prefsStore.saveLocalProfile(
                    current.copy(
                        currentStreak = newStreak,
                        longestStreak = newLongest,
                        totalFocusMinutes = newTotalMinutes,
                        focusXp = newXp,
                        gems = newGems,
                        totalGemsEarned = newTotalGems,
                        lastFocusDate = today
                    )
                )
            }

            if (pinnedTaskId != null) {
                val targetTask = tasks.value.firstOrNull { it.id == pinnedTaskId }
                if (targetTask != null) {
                    val nextCount = targetTask.completedPomodoros + 1
                    val done = targetTask.completed || nextCount >= targetTask.estimatedPomodoros
                    updateTaskState(targetTask, done, nextCount)
                }
            }
        }

        showGemReward("Focus Complete! ($completedMinutes min)", gemsEarned, xpEarned)
    }

    // --- Study Tasks ---

    fun addTask(title: String, subject: String = _timerState.value.subject, estimatedPomodoros: Int = 1) {
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
        if (_timerState.value.activeTaskId == taskId) {
            _timerState.update { it.copy(activeTaskId = null) }
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
            showBanner("Unlock ${preset.displayName} in the Gem Shop (${preset.gemCost} Gems)")
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
            tactileEngine.playTap()
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
            tactileEngine.playTap()
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

    // --- Gemini AI Study Coach (Firebase AI Logic) ---

    fun requestStudyTip() {
        tactileEngine.playTap()
        val timer = _timerState.value
        val profile = userProfile.value
        val activeTask = tasks.value.firstOrNull { it.id == timer.activeTaskId }?.title
        launchCoachStream {
            geminiCoachRepo.streamStudyTip(
                subject = timer.subject,
                plannedMinutes = timer.plannedMinutes,
                streakDays = profile.currentStreak,
                activeTaskTitle = activeTask
            )
        }
    }

    fun requestMotivation() {
        tactileEngine.playTap()
        val timer = _timerState.value
        val profile = userProfile.value
        launchCoachStream {
            geminiCoachRepo.streamMotivation(
                subject = timer.subject,
                isFocusing = timer.isRunning,
                streakDays = profile.currentStreak,
                gems = profile.gems
            )
        }
    }

    fun askCoachQuestion(question: String) {
        val clean = question.trim()
        if (clean.isEmpty()) return
        tactileEngine.playTap()
        val subject = _timerState.value.subject
        launchCoachStream {
            geminiCoachRepo.streamCustomQuestion(subject = subject, question = clean)
        }
    }

    private fun launchCoachStream(streamFactory: () -> kotlinx.coroutines.flow.Flow<String>) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _aiCoachState.value = AiCoachUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    streamFactory().collect { chunk ->
                        accumulated.append(chunk)
                        _aiCoachState.value = AiCoachUiState.Streaming(accumulated.toString().trim())
                    }
                }
                val final = accumulated.toString().trim()
                if (final.isNotBlank()) {
                    _aiCoachState.value = AiCoachUiState.Success(final)
                } else {
                    _aiCoachState.value = AiCoachUiState.Error("Coach returned an empty response. Tap to retry.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini Coach timed out", e)
                _aiCoachState.value = AiCoachUiState.Error("Gemini Coach timed out after 60s. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Coach error", e)
                _aiCoachState.value = AiCoachUiState.Error(
                    e.localizedMessage ?: "Unable to reach Gemini Study Coach right now."
                )
            }
        }
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

    private fun snapshotTimerState(): TimerUiState = TimerUiState(
        phase = timerController.phase,
        subject = _timerState.value.subject,
        plannedMinutes = timerController.plannedMinutes,
        focusMinutesSetting = timerController.focusMinutes,
        shortBreakMinutesSetting = timerController.shortBreakMinutes,
        longBreakMinutesSetting = timerController.longBreakMinutes,
        remainingSeconds = timerController.remainingSeconds,
        elapsedSeconds = timerController.elapsedSeconds,
        isRunning = timerController.isRunning,
        isPaused = timerController.isPaused,
        completedPomodorosInCycle = timerController.completedPomodorosInCycle,
        activeTaskId = _timerState.value.activeTaskId
    )

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
