package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.FocusSession
import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.PomodoroPhase
import com.example.data.model.TimerUiState
import com.example.ui.components.PomodoroTimerController
import com.example.ui.components.formatClock
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "FocusTimerService"

data class CompletedTimerEvent(
    val sessionId: String,
    val phase: PomodoroPhase,
    val subject: String,
    val plannedMinutes: Int,
    val completedMinutes: Int,
    val ambientSound: String,
    val xpEarned: Int,
    val gemsEarned: Int,
    val dateKey: String,
    val pinnedTaskId: String?,
    val alreadyPersistedLocally: Boolean,
    val sessionStartedDateKey: String = ""
)

data class TimerHydrationResult(
    val didRestore: Boolean,
    val restoredState: TimerUiState,
    val completedWhileAway: Boolean = false,
    val finishedPhase: PomodoroPhase? = null,
    val finishedMinutes: Int = 0,
    val finishedPlannedMinutes: Int = 0,
    val finishedSubject: String = "Math",
    val finishedPinnedTaskId: String? = null,
    val finishedSessionStartedDateKey: String = ""
)

/**
 * Process-wide reactive timer state shared seamlessly between [FocusTimerService]
 * and [com.example.viewmodel.TryAgainViewModel] so countdown & pause states persist
 * accurately across pauses, background execution, and app process restarts.
 */
object FocusTimerRuntime {
    internal val controller = PomodoroTimerController()
    private val hydrationMutex = Mutex()
    @Volatile
    private var isHydrated: Boolean = false

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private val _completedEvents = MutableSharedFlow<CompletedTimerEvent>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val completedEvents: SharedFlow<CompletedTimerEvent> = _completedEvents.asSharedFlow()

    suspend fun hydrateIfNeeded(
        store: LocalPreferencesStore,
        nowEpochMs: Long = System.currentTimeMillis()
    ): TimerHydrationResult = hydrationMutex.withLock {
        if (isHydrated) {
            return@withLock TimerHydrationResult(
                didRestore = false,
                restoredState = _state.value
            )
        }
        isHydrated = true

        // If a session is already actively running or paused in memory, do not overwrite it.
        if (controller.isRunning || controller.isPaused) {
            return@withLock TimerHydrationResult(
                didRestore = false,
                restoredState = _state.value
            )
        }

        val saved = runCatching { store.loadTimerSnapshot() }.getOrNull()
            ?: return@withLock TimerHydrationResult(
                didRestore = false,
                restoredState = _state.value
            )

        val reconciled = FocusStreakCalculator.reconcilePersistedTimer(saved, nowEpochMs)
        val restoredUi = applyReconciled(reconciled)

        // Persist the reconciled snapshot immediately so completedWhileAway is never double-credited
        runCatching {
            store.saveTimerSnapshot(toPersistedSnapshot(nowEpochMs))
        }

        TimerHydrationResult(
            didRestore = reconciled.restoredFromSavedSession,
            restoredState = restoredUi,
            completedWhileAway = reconciled.completedWhileAway,
            finishedPhase = reconciled.finishedPhase,
            finishedMinutes = reconciled.finishedMinutes,
            finishedPlannedMinutes = reconciled.finishedPlannedMinutes,
            finishedSubject = reconciled.subject,
            finishedPinnedTaskId = reconciled.activeTaskId,
            finishedSessionStartedDateKey = reconciled.finishedSessionStartedDateKey
        )
    }

    @Synchronized
    private fun applyReconciled(reconciled: FocusStreakCalculator.ReconciledTimerState): TimerUiState {
        controller.restoreFromReconciled(reconciled)
        _state.update {
            it.copy(
                subject = reconciled.subject,
                activeTaskId = reconciled.activeTaskId
            )
        }
        return publishSnapshot()
    }

    @Synchronized
    fun selectSubject(subject: String) {
        _state.update { it.copy(subject = subject) }
    }

    @Synchronized
    fun pinTask(taskId: String?) {
        _state.update { it.copy(activeTaskId = taskId) }
    }

    @Synchronized
    fun selectPhase(phase: PomodoroPhase) {
        controller.selectPhase(phase)
        publishSnapshot()
    }

    @Synchronized
    fun setDuration(minutes: Int) {
        controller.setDurationMinutes(minutes)
        publishSnapshot()
    }

    @Synchronized
    fun adjustDuration(delta: Int) {
        controller.adjustDurationMinutes(delta)
        publishSnapshot()
    }

    @Synchronized
    internal fun startController(nowEpochMs: Long = System.currentTimeMillis()): TimerUiState {
        isHydrated = true
        controller.start(
            nowEpochMs = nowEpochMs,
            todayDateKey = FocusStreakCalculator.formatDateKey(nowEpochMs)
        )
        return publishSnapshot()
    }

    @Synchronized
    internal fun pauseController(nowEpochMs: Long = System.currentTimeMillis()): TimerUiState {
        isHydrated = true
        controller.pause(nowEpochMs = nowEpochMs)
        return publishSnapshot()
    }

    @Synchronized
    internal fun resetController(): TimerUiState {
        isHydrated = true
        controller.reset()
        return publishSnapshot()
    }

    @Synchronized
    internal fun advanceSeconds(seconds: Int): Pair<PomodoroPhase, Int>? {
        val result = controller.advanceBySeconds(seconds)
        publishSnapshot()
        return result
    }

    @Synchronized
    internal fun completePhase(autoFinished: Boolean): Pair<PomodoroPhase, Int> {
        val result = controller.completeCurrentPhase(autoFinished = autoFinished)
        publishSnapshot()
        return result
    }

    @Synchronized
    fun toPersistedSnapshot(nowEpochMs: Long = System.currentTimeMillis()): PersistedTimerSnapshot {
        val cur = _state.value
        return PersistedTimerSnapshot(
            phase = cur.phase,
            subject = cur.subject,
            plannedMinutes = cur.plannedMinutes,
            focusMinutesSetting = cur.focusMinutesSetting,
            shortBreakMinutesSetting = cur.shortBreakMinutesSetting,
            longBreakMinutesSetting = cur.longBreakMinutesSetting,
            remainingSeconds = cur.remainingSeconds,
            elapsedSeconds = cur.elapsedSeconds,
            isRunning = cur.isRunning,
            isPaused = cur.isPaused,
            completedPomodorosInCycle = cur.completedPomodorosInCycle,
            activeTaskId = cur.activeTaskId,
            sessionStartedDateKey = cur.sessionStartedDateKey,
            lastPersistedEpochMs = nowEpochMs,
            pausedAtEpochMs = cur.pausedAtEpochMs,
            totalPausedSeconds = cur.totalPausedSeconds,
            pauseCount = cur.pauseCount
        )
    }

    suspend fun persistNow(store: LocalPreferencesStore, nowEpochMs: Long = System.currentTimeMillis()) {
        runCatching {
            store.saveTimerSnapshot(toPersistedSnapshot(nowEpochMs))
        }.onFailure { err ->
            Log.w(TAG, "Failed to persist timer snapshot: ${err.message}")
        }
    }

    @Synchronized
    internal fun publishSnapshot(): TimerUiState {
        val cur = _state.value
        val next = TimerUiState(
            phase = controller.phase,
            subject = cur.subject,
            plannedMinutes = controller.plannedMinutes,
            focusMinutesSetting = controller.focusMinutes,
            shortBreakMinutesSetting = controller.shortBreakMinutes,
            longBreakMinutesSetting = controller.longBreakMinutes,
            remainingSeconds = controller.remainingSeconds,
            elapsedSeconds = controller.elapsedSeconds,
            isRunning = controller.isRunning,
            isPaused = controller.isPaused,
            completedPomodorosInCycle = controller.completedPomodorosInCycle,
            activeTaskId = cur.activeTaskId,
            sessionStartedDateKey = controller.sessionStartedDateKey,
            pausedAtEpochMs = controller.pausedAtEpochMs,
            totalPausedSeconds = controller.totalPausedSeconds,
            pauseCount = controller.pauseCount,
            restoredFromSavedSession = controller.restoredFromSavedSession
        )
        _state.value = next
        return next
    }

    internal fun emitCompleted(event: CompletedTimerEvent) {
        _completedEvents.tryEmit(event)
    }
}

/**
 * Play-compliant Foreground Service (`specialUse` timer countdown) that keeps the active
 * Pomodoro session ticking accurately using monotonic/wall-clock anchors, persists state
 * across pauses and process restarts, protects focus streaks across midnight pauses,
 * plays the user's selected custom completion sound when finished, and logs study history
 * directly to persistent storage.
 */
class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private lateinit var prefsStore: LocalPreferencesStore
    private lateinit var tactileEngine: TactileSoundEngine

    override fun onCreate() {
        super.onCreate()
        prefsStore = LocalPreferencesStore(applicationContext)
        tactileEngine = TactileSoundEngine(applicationContext)
        ensureChannels()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        // Immediately satisfy startForegroundService contract when starting or resuming
        if (action == ACTION_START || action == ACTION_TOGGLE_PAUSE || action == null) {
            promoteToForeground(buildOngoingNotification(FocusTimerRuntime.state.value))
        }

        serviceScope.launch {
            val hydration = FocusTimerRuntime.hydrateIfNeeded(prefsStore)
            if (hydration.completedWhileAway && hydration.finishedPhase != null) {
                handlePhaseCompleted(
                    phase = hydration.finishedPhase,
                    completedMinutes = hydration.finishedMinutes,
                    plannedMinutesOverride = hydration.finishedPlannedMinutes,
                    subjectOverride = hydration.finishedSubject,
                    pinnedTaskIdOverride = hydration.finishedPinnedTaskId,
                    sessionStartedDateKeyOverride = hydration.finishedSessionStartedDateKey
                )
                return@launch
            }

            when (action) {
                ACTION_START -> handleStart()
                ACTION_PAUSE -> handlePause()
                ACTION_TOGGLE_PAUSE -> {
                    if (FocusTimerRuntime.state.value.isRunning) handlePause() else handleStart()
                }
                ACTION_COMPLETE -> handleCompleteNow()
                ACTION_RESET -> handleReset()
                else -> {
                    // System restarted sticky service after process kill
                    val restored = FocusTimerRuntime.state.value
                    when {
                        restored.isRunning -> handleStart()
                        restored.isPaused -> {
                            promoteToForeground(buildOngoingNotification(restored))
                        }
                        else -> stopForegroundAndSelf()
                    }
                }
            }
        }
        return START_STICKY
    }

    private suspend fun handleStart() {
        val snapshot = FocusTimerRuntime.startController()
        promoteToForeground(buildOngoingNotification(snapshot))
        FocusTimerRuntime.persistNow(prefsStore)

        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            var lastAnchorElapsedMs = SystemClock.elapsedRealtime()
            var secondsSincePersist = 0
            while (isActive && FocusTimerRuntime.controller.isRunning) {
                delay(1_000L)
                val nowElapsedMs = SystemClock.elapsedRealtime()
                val deltaSeconds = ((nowElapsedMs - lastAnchorElapsedMs) / 1_000L)
                    .toInt()
                    .coerceAtLeast(1)
                lastAnchorElapsedMs += deltaSeconds * 1_000L

                val stateBeforeTick = FocusTimerRuntime.state.value
                val finished = FocusTimerRuntime.advanceSeconds(deltaSeconds)
                val current = FocusTimerRuntime.state.value

                if (finished != null) {
                    handlePhaseCompleted(
                        phase = finished.first,
                        completedMinutes = finished.second,
                        plannedMinutesOverride = stateBeforeTick.plannedMinutes,
                        subjectOverride = stateBeforeTick.subject,
                        pinnedTaskIdOverride = stateBeforeTick.activeTaskId,
                        sessionStartedDateKeyOverride = stateBeforeTick.sessionStartedDateKey
                    )
                    break
                } else {
                    updateOngoingNotification(current)
                    secondsSincePersist += deltaSeconds
                    if (secondsSincePersist >= 5) {
                        secondsSincePersist = 0
                        FocusTimerRuntime.persistNow(prefsStore)
                    }
                }
            }
        }
    }

    private suspend fun handlePause() {
        tickerJob?.cancel()
        val snapshot = FocusTimerRuntime.pauseController()
        FocusTimerRuntime.persistNow(prefsStore)
        promoteToForeground(buildOngoingNotification(snapshot))

        val prefs = prefsStore.preferencesFlow.first()
        if (prefs.ambientAutoPlayWithTimer && AmbientSoundEngine.shared.isPlaying.value) {
            AmbientAudioService.pause(applicationContext, "Focus timer paused")
        }
    }

    private suspend fun handleReset() {
        tickerJob?.cancel()
        FocusTimerRuntime.resetController()
        FocusTimerRuntime.persistNow(prefsStore)

        val prefs = prefsStore.preferencesFlow.first()
        if (prefs.ambientAutoPlayWithTimer && AmbientSoundEngine.shared.isPlaying.value) {
            AmbientAudioService.pause(applicationContext, "Focus timer reset")
        }
        stopForegroundAndSelf()
    }

    private suspend fun handleCompleteNow() {
        tickerJob?.cancel()
        val stateBefore = FocusTimerRuntime.state.value
        val (phase, mins) = FocusTimerRuntime.completePhase(autoFinished = false)
        handlePhaseCompleted(
            phase = phase,
            completedMinutes = mins,
            plannedMinutesOverride = stateBefore.plannedMinutes,
            subjectOverride = stateBefore.subject,
            pinnedTaskIdOverride = stateBefore.activeTaskId,
            sessionStartedDateKeyOverride = stateBefore.sessionStartedDateKey
        )
    }

    private suspend fun handlePhaseCompleted(
        phase: PomodoroPhase,
        completedMinutes: Int,
        plannedMinutesOverride: Int? = null,
        subjectOverride: String? = null,
        pinnedTaskIdOverride: String? = null,
        sessionStartedDateKeyOverride: String? = null
    ) {
        // Persist post-completion idle state right away so app restart starts cleanly in the next phase
        FocusTimerRuntime.persistNow(prefsStore)

        val prefs = prefsStore.preferencesFlow.first()
        tactileEngine.soundsEnabled = prefs.uiClickSoundsEnabled
        tactileEngine.hapticsEnabled = prefs.hapticsEnabled
        tactileEngine.completionSoundPreset = prefs.completionSound

        if (prefs.ambientAutoPlayWithTimer && AmbientSoundEngine.shared.isPlaying.value) {
            AmbientAudioService.pause(applicationContext, "Session complete — take a breather")
        }

        val stateCurrent = FocusTimerRuntime.state.value
        val subject = subjectOverride?.ifBlank { null } ?: stateCurrent.subject
        val pinnedTaskId = pinnedTaskIdOverride ?: stateCurrent.activeTaskId
        val plannedMinutes = plannedMinutesOverride ?: FocusTimerRuntime.controller.focusMinutes
        val soundLabel = AmbientSoundEngine.shared.selectedPreset.value.displayName
        val today = todayDateKey()
        val yesterday = yesterdayDateKey()
        val startedDateKey = sessionStartedDateKeyOverride?.ifBlank { null } ?: today

        if (phase != PomodoroPhase.FOCUS) {
            tactileEngine.playToggle(true)
            postCompletionNotification(
                title = "Break Complete!",
                body = "Refreshed and ready for your next $subject focus sprint."
            )
            FocusTimerRuntime.emitCompleted(
                CompletedTimerEvent(
                    sessionId = UUID.randomUUID().toString(),
                    phase = phase,
                    subject = subject,
                    plannedMinutes = plannedMinutes,
                    completedMinutes = completedMinutes,
                    ambientSound = soundLabel,
                    xpEarned = 0,
                    gemsEarned = 0,
                    dateKey = today,
                    pinnedTaskId = pinnedTaskId,
                    alreadyPersistedLocally = true,
                    sessionStartedDateKey = startedDateKey
                )
            )
            stopForegroundAndSelf()
            return
        }

        // Play the user's selected custom completion sound even in background!
        tactileEngine.playSessionComplete(preset = prefs.completionSound, forcePlay = true)

        val gemsEarned = (completedMinutes * 2) + 15
        val xpEarned = completedMinutes * 3
        val sessionId = UUID.randomUUID().toString()
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

        // Always persist locally right away with midnight-safe streak continuity
        runCatching {
            prefsStore.recordBackgroundCompletedSession(
                session = session,
                todayDateKey = today,
                yesterdayDateKey = yesterday,
                sessionStartedDateKey = startedDateKey
            )
        }.onFailure { err ->
            Log.e(TAG, "Failed to persist background session locally", err)
        }

        postCompletionNotification(
            title = "Focus Complete! ($completedMinutes min · $subject)",
            body = "+$gemsEarned Gems & +$xpEarned Play Points earned • Sound: ${prefs.completionSound.displayName}"
        )

        FocusTimerRuntime.emitCompleted(
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
                sessionStartedDateKey = startedDateKey
            )
        )

        stopForegroundAndSelf()
    }

    private fun promoteToForeground(notification: Notification) {
        try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID_ONGOING, notification, fgsType)
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed; countdown continues safely: ${e.message}")
        }
    }

    private fun updateOngoingNotification(state: TimerUiState) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            nm.notify(NOTIFICATION_ID_ONGOING, buildOngoingNotification(state))
        }
    }

    private fun buildOngoingNotification(state: TimerUiState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            this,
            200,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val togglePending = PendingIntent.getService(
            this,
            201,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_COMPLETE
        }
        val completePending = PendingIntent.getService(
            this,
            202,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = formatClock(state.displaySeconds)
        val statusPrefix = when {
            state.isPaused -> "Paused"
            state.phase == PomodoroPhase.FOCUS -> "Focusing"
            else -> state.phase.displayName
        }

        return NotificationCompat.Builder(this, CHANNEL_ONGOING_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$statusPrefix • ${state.subject} ($timeText)")
            .setContentText(
                if (state.isPaused) "Streak protected • Progress saved — tap Resume anytime"
                else "Running in background — custom chime plays at 00:00"
            )
            .setContentIntent(openAppPending)
            .setOngoing(state.isRunning || state.isPaused)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(state.totalSeconds, state.totalSeconds - state.displaySeconds, false)
            .addAction(
                if (state.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (state.isRunning) "Pause" else "Resume",
                togglePending
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                if (state.phase == PomodoroPhase.FOCUS) "Save & Finish" else "Skip Break",
                completePending
            )
            .build()
    }

    private fun postCompletionNotification(title: String, body: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            205,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        runCatching {
            nm.notify(NOTIFICATION_ID_COMPLETE, notification)
        }
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val ongoing = NotificationChannel(
                CHANNEL_ONGOING_ID,
                getString(R.string.timer_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.timer_notification_channel_desc)
                setShowBadge(false)
            }
            val complete = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                getString(R.string.timer_complete_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.timer_complete_channel_desc)
                enableVibration(true)
            }
            nm.createNotificationChannel(ongoing)
            nm.createNotificationChannel(complete)
        }
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    companion object {
        const val CHANNEL_ONGOING_ID = "try_again_bg_timer"
        const val CHANNEL_COMPLETE_ID = "try_again_timer_complete"
        const val NOTIFICATION_ID_ONGOING = 4201
        const val NOTIFICATION_ID_COMPLETE = 4202

        const val ACTION_START = "com.example.timer.ACTION_START"
        const val ACTION_PAUSE = "com.example.timer.ACTION_PAUSE"
        const val ACTION_TOGGLE_PAUSE = "com.example.timer.ACTION_TOGGLE_PAUSE"
        const val ACTION_COMPLETE = "com.example.timer.ACTION_COMPLETE"
        const val ACTION_RESET = "com.example.timer.ACTION_RESET"

        fun startOrResume(context: Context) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, FocusTimerService::class.java).apply {
                action = ACTION_START
            }
            val launched = runCatching {
                ContextCompat.startForegroundService(appCtx, intent)
            }.isSuccess
            if (!launched) {
                runCatching { appCtx.startService(intent) }
            }
        }

        fun pause(context: Context) {
            FocusTimerRuntime.pauseController()
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, FocusTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            runCatching { appCtx.startService(intent) }
        }

        fun completeNow(context: Context) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, FocusTimerService::class.java).apply {
                action = ACTION_COMPLETE
            }
            runCatching { appCtx.startService(intent) }
        }

        fun reset(context: Context) {
            FocusTimerRuntime.resetController()
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, FocusTimerService::class.java).apply {
                action = ACTION_RESET
            }
            runCatching { appCtx.startService(intent) }
        }
    }
}
