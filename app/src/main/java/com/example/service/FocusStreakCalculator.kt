package com.example.service

import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.PomodoroPhase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure Kotlin helper for fair streak calculation and wall-clock-accurate
 * timer restoration after pauses, background execution, or app process restarts.
 */
object FocusStreakCalculator {

    private const val DATE_PATTERN = "yyyy-MM-dd"

    fun formatDateKey(epochMs: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
        return sdf.format(Date(epochMs))
    }

    fun previousDateKey(dateKey: String): String {
        return shiftDateKey(dateKey, -1)
    }

    fun shiftDateKey(dateKey: String, deltaDays: Int): String {
        val parsed = parseDateKey(dateKey) ?: return ""
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        return SimpleDateFormat(DATE_PATTERN, Locale.US).format(cal.time)
    }

    private fun parseDateKey(dateKey: String): Date? {
        if (dateKey.isBlank()) return null
        return runCatching {
            val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
                isLenient = false
            }
            sdf.parse(dateKey)
        }.getOrNull()
    }

    /**
     * Computes the next focus streak fairly without penalizing paused or
     * midnight-spanning focus sessions:
     * - Same day completion preserves current streak (>= 1).
     * - Consecutive day completion increments streak by 1.
     * - If a focus session started before midnight (e.g. yesterday), was paused,
     *   and resumed/completed after midnight (today), the session bridges the day
     *   boundary so an active learner never suffers an unfair streak reset.
     * - Clock skew (lastFocusDate > completionDateKey) never unfairly drops streak.
     */
    fun calculateNextStreak(
        currentStreak: Int,
        lastFocusDate: String,
        completionDateKey: String,
        sessionStartedDateKey: String? = null
    ): Int {
        if (completionDateKey.isBlank()) return currentStreak.coerceAtLeast(1)
        if (lastFocusDate.isBlank()) return 1

        val safeCurrent = currentStreak.coerceAtLeast(1)
        val yesterdayOfCompletion = previousDateKey(completionDateKey)

        // Same day completion -> streak already lit today
        if (lastFocusDate == completionDateKey) {
            return safeCurrent
        }

        // Standard consecutive day completion
        if (yesterdayOfCompletion.isNotBlank() && lastFocusDate == yesterdayOfCompletion) {
            return safeCurrent + 1
        }

        // Midnight-spanning session protection (e.g. started & paused before midnight, finished after)
        val startKey = sessionStartedDateKey?.trim().orEmpty()
        if (startKey.isNotBlank() && startKey != completionDateKey) {
            val dayBeforeStart = previousDateKey(startKey)
            if (lastFocusDate == startKey) {
                return safeCurrent + 1
            }
            if (dayBeforeStart.isNotBlank() && lastFocusDate == dayBeforeStart) {
                // Started studying on the consecutive day after lastFocusDate, paused past midnight, finished today
                return safeCurrent + 1
            }
        }

        // Protect against device timezone / clock skew where lastFocusDate is ahead of completionDateKey
        if (lastFocusDate > completionDateKey) {
            return safeCurrent
        }

        return 1
    }

    /**
     * Tracks consecutive calendar days where the student met their daily study goal
     * (`minutes >= dailyGoalMinutes`). If today hasn't met the goal yet, gracefully checks
     * starting from yesterday so an active streak remains visible throughout the day.
     */
    fun calculateConsecutiveGoalStreak(
        dailyMinutesByDate: Map<String, Int>,
        dailyGoalMinutes: Int,
        todayDateKey: String = formatDateKey()
    ): Int {
        val goal = dailyGoalMinutes.coerceAtLeast(1)
        val todayMet = (dailyMinutesByDate[todayDateKey] ?: 0) >= goal
        var cursor = if (todayMet) todayDateKey else previousDateKey(todayDateKey)
        var consecutive = 0
        var guard = 0
        while (cursor.isNotBlank() && guard < 3650) {
            val mins = dailyMinutesByDate[cursor] ?: 0
            if (mins >= goal) {
                consecutive += 1
                cursor = previousDateKey(cursor)
            } else {
                break
            }
            guard += 1
        }
        return consecutive
    }

    data class ReconciledTimerState(
        val phase: PomodoroPhase,
        val subject: String,
        val plannedMinutes: Int,
        val focusMinutesSetting: Int,
        val shortBreakMinutesSetting: Int,
        val longBreakMinutesSetting: Int,
        val remainingSeconds: Int,
        val elapsedSeconds: Int,
        val isRunning: Boolean,
        val isPaused: Boolean,
        val completedPomodorosInCycle: Int,
        val activeTaskId: String?,
        val sessionStartedDateKey: String,
        val pausedAtEpochMs: Long?,
        val totalPausedSeconds: Int,
        val pauseCount: Int,
        val restoredFromSavedSession: Boolean,
        val completedWhileAway: Boolean = false,
        val finishedPhase: PomodoroPhase? = null,
        val finishedMinutes: Int = 0,
        val finishedPlannedMinutes: Int = 0,
        val finishedSessionStartedDateKey: String = ""
    )

    /**
     * Reconciles a persisted [PersistedTimerSnapshot] against current wall-clock time.
     * - Paused sessions restore with ZERO time loss regardless of how long the app was closed.
     * - Running sessions account for elapsed wall-clock seconds and detect completion if 00:00 passed.
     */
    fun reconcilePersistedTimer(
        snapshot: PersistedTimerSnapshot,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ReconciledTimerState {
        val focusMins = snapshot.focusMinutesSetting.coerceIn(
            PomodoroPhase.FOCUS.minMinutes,
            PomodoroPhase.FOCUS.maxMinutes
        )
        val shortMins = snapshot.shortBreakMinutesSetting.coerceIn(
            PomodoroPhase.SHORT_BREAK.minMinutes,
            PomodoroPhase.SHORT_BREAK.maxMinutes
        )
        val longMins = snapshot.longBreakMinutesSetting.coerceIn(
            PomodoroPhase.LONG_BREAK.minMinutes,
            PomodoroPhase.LONG_BREAK.maxMinutes
        )
        val plannedMins = snapshot.plannedMinutes.coerceIn(
            snapshot.phase.minMinutes,
            snapshot.phase.maxMinutes
        )
        val totalSecs = (plannedMins * 60).coerceAtLeast(60)
        val safeCycle = snapshot.completedPomodorosInCycle.coerceIn(0, 3)

        // Case 1: Idle snapshot (preserve user's settings & phase cleanly)
        if (!snapshot.isRunning && !snapshot.isPaused) {
            return ReconciledTimerState(
                phase = snapshot.phase,
                subject = snapshot.subject.ifBlank { "Math" },
                plannedMinutes = plannedMins,
                focusMinutesSetting = focusMins,
                shortBreakMinutesSetting = shortMins,
                longBreakMinutesSetting = longMins,
                remainingSeconds = totalSecs,
                elapsedSeconds = 0,
                isRunning = false,
                isPaused = false,
                completedPomodorosInCycle = safeCycle,
                activeTaskId = snapshot.activeTaskId,
                sessionStartedDateKey = "",
                pausedAtEpochMs = null,
                totalPausedSeconds = 0,
                pauseCount = 0,
                restoredFromSavedSession = false
            )
        }

        // Case 2: Paused session -> exact preservation with zero drift!
        if (snapshot.isPaused && !snapshot.isRunning) {
            val clampedRemaining = snapshot.remainingSeconds.coerceIn(1, totalSecs)
            val clampedElapsed = snapshot.elapsedSeconds.coerceIn(0, totalSecs)
            return ReconciledTimerState(
                phase = snapshot.phase,
                subject = snapshot.subject.ifBlank { "Math" },
                plannedMinutes = plannedMins,
                focusMinutesSetting = focusMins,
                shortBreakMinutesSetting = shortMins,
                longBreakMinutesSetting = longMins,
                remainingSeconds = clampedRemaining,
                elapsedSeconds = clampedElapsed,
                isRunning = false,
                isPaused = true,
                completedPomodorosInCycle = safeCycle,
                activeTaskId = snapshot.activeTaskId,
                sessionStartedDateKey = snapshot.sessionStartedDateKey.ifBlank {
                    formatDateKey(snapshot.pausedAtEpochMs ?: snapshot.lastPersistedEpochMs.takeIf { it > 0L } ?: nowEpochMs)
                },
                pausedAtEpochMs = snapshot.pausedAtEpochMs ?: snapshot.lastPersistedEpochMs.takeIf { it > 0L } ?: nowEpochMs,
                totalPausedSeconds = snapshot.totalPausedSeconds.coerceAtLeast(0),
                pauseCount = snapshot.pauseCount.coerceAtLeast(1),
                restoredFromSavedSession = true
            )
        }

        // Case 3: Running session -> compute exact wall-clock delta since last persistence
        val deltaSeconds = if (snapshot.lastPersistedEpochMs > 0L) {
            ((nowEpochMs - snapshot.lastPersistedEpochMs) / 1000L).coerceAtLeast(0L).toInt()
        } else {
            0
        }
        val safeRemainingBefore = snapshot.remainingSeconds.coerceIn(0, totalSecs)
        val safeElapsedBefore = snapshot.elapsedSeconds.coerceIn(0, totalSecs)

        if (deltaSeconds >= safeRemainingBefore) {
            // Completed while the process was stopped! Advance to next phase cleanly.
            val (nextPhase, nextCycle) = when (snapshot.phase) {
                PomodoroPhase.FOCUS -> {
                    val nextCount = safeCycle + 1
                    if (nextCount >= 4) PomodoroPhase.LONG_BREAK to 0 else PomodoroPhase.SHORT_BREAK to nextCount
                }
                PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS to safeCycle
            }
            val nextMinutes = when (nextPhase) {
                PomodoroPhase.FOCUS -> focusMins
                PomodoroPhase.SHORT_BREAK -> shortMins
                PomodoroPhase.LONG_BREAK -> longMins
            }
            return ReconciledTimerState(
                phase = nextPhase,
                subject = snapshot.subject.ifBlank { "Math" },
                plannedMinutes = nextMinutes,
                focusMinutesSetting = focusMins,
                shortBreakMinutesSetting = shortMins,
                longBreakMinutesSetting = longMins,
                remainingSeconds = nextMinutes * 60,
                elapsedSeconds = 0,
                isRunning = false,
                isPaused = false,
                completedPomodorosInCycle = nextCycle,
                activeTaskId = snapshot.activeTaskId,
                sessionStartedDateKey = "",
                pausedAtEpochMs = null,
                totalPausedSeconds = 0,
                pauseCount = 0,
                restoredFromSavedSession = true,
                completedWhileAway = true,
                finishedPhase = snapshot.phase,
                finishedMinutes = plannedMins,
                finishedPlannedMinutes = plannedMins,
                finishedSessionStartedDateKey = snapshot.sessionStartedDateKey.ifBlank {
                    formatDateKey(snapshot.lastPersistedEpochMs.takeIf { it > 0L } ?: nowEpochMs)
                }
            )
        }

        val nextRemaining = (safeRemainingBefore - deltaSeconds).coerceAtLeast(1)
        val nextElapsed = (safeElapsedBefore + deltaSeconds).coerceAtMost(totalSecs)
        return ReconciledTimerState(
            phase = snapshot.phase,
            subject = snapshot.subject.ifBlank { "Math" },
            plannedMinutes = plannedMins,
            focusMinutesSetting = focusMins,
            shortBreakMinutesSetting = shortMins,
            longBreakMinutesSetting = longMins,
            remainingSeconds = nextRemaining,
            elapsedSeconds = nextElapsed,
            isRunning = true,
            isPaused = false,
            completedPomodorosInCycle = safeCycle,
            activeTaskId = snapshot.activeTaskId,
            sessionStartedDateKey = snapshot.sessionStartedDateKey.ifBlank {
                formatDateKey(snapshot.lastPersistedEpochMs.takeIf { it > 0L } ?: nowEpochMs)
            },
            pausedAtEpochMs = null,
            totalPausedSeconds = snapshot.totalPausedSeconds.coerceAtLeast(0),
            pauseCount = snapshot.pauseCount.coerceAtLeast(0),
            restoredFromSavedSession = true
        )
    }
}
