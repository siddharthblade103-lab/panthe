package com.example

import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.PomodoroPhase
import com.example.service.FocusStreakCalculator
import com.example.ui.components.PomodoroTimerController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusPauseResumeAndStreakTest {

    @Test
    fun pauseAndResume_preservesExactElapsedRemainingAndTracksPauseDuration() {
        val controller = PomodoroTimerController(initialFocusMinutes = 25)
        val t0 = 1_700_000_000_000L

        controller.start(nowEpochMs = t0, todayDateKey = "2026-11-27")
        assertEquals("2026-11-27", controller.sessionStartedDateKey)
        assertEquals(0, controller.pauseCount)

        // Focus for 5 minutes (300 seconds)
        assertNull(controller.advanceBySeconds(300))
        assertEquals(1200, controller.remainingSeconds)
        assertEquals(300, controller.elapsedSeconds)

        // Pause for 2 minutes (120 seconds)
        val tPause = t0 + 300_000L
        controller.pause(nowEpochMs = tPause)
        assertTrue(controller.isPaused)
        assertFalse(controller.isRunning)
        assertEquals(1, controller.pauseCount)
        assertEquals(tPause, controller.pausedAtEpochMs)

        // Ticking while paused must NEVER change remaining or elapsed seconds
        assertNull(controller.advanceBySeconds(60))
        assertEquals(1200, controller.remainingSeconds)
        assertEquals(300, controller.elapsedSeconds)

        // Resume across midnight on the next day
        val tResume = tPause + 120_000L
        controller.start(nowEpochMs = tResume, todayDateKey = "2026-11-28")
        assertTrue(controller.isRunning)
        assertFalse(controller.isPaused)
        assertNull(controller.pausedAtEpochMs)
        assertEquals(120, controller.totalPausedSeconds)
        // Original session start date stays preserved for midnight streak protection!
        assertEquals("2026-11-27", controller.sessionStartedDateKey)
        assertEquals(1200, controller.remainingSeconds)
        assertEquals(300, controller.elapsedSeconds)
    }

    @Test
    fun reconcilePersistedTimer_pausedSessionHasZeroDriftAfterHoursClosed() {
        val pausedAt = 1_700_000_000_000L
        val snapshot = PersistedTimerSnapshot(
            phase = PomodoroPhase.FOCUS,
            subject = "Coding",
            plannedMinutes = 45,
            focusMinutesSetting = 45,
            shortBreakMinutesSetting = 5,
            longBreakMinutesSetting = 15,
            remainingSeconds = 1800,
            elapsedSeconds = 900,
            isRunning = false,
            isPaused = true,
            completedPomodorosInCycle = 2,
            activeTaskId = "task_42",
            sessionStartedDateKey = "2026-11-27",
            lastPersistedEpochMs = pausedAt,
            pausedAtEpochMs = pausedAt,
            totalPausedSeconds = 45,
            pauseCount = 2
        )

        // Reopen app 3 hours later
        val now = pausedAt + (3 * 3600 * 1000L)
        val restored = FocusStreakCalculator.reconcilePersistedTimer(snapshot, nowEpochMs = now)

        assertTrue(restored.isPaused)
        assertFalse(restored.isRunning)
        assertFalse(restored.completedWhileAway)
        assertTrue(restored.restoredFromSavedSession)
        assertEquals(1800, restored.remainingSeconds)
        assertEquals(900, restored.elapsedSeconds)
        assertEquals("Coding", restored.subject)
        assertEquals("task_42", restored.activeTaskId)
        assertEquals("2026-11-27", restored.sessionStartedDateKey)
        assertEquals(2, restored.pauseCount)

        // Restore into controller and verify exact match
        val controller = PomodoroTimerController()
        controller.restoreFromReconciled(restored)
        assertEquals(1800, controller.remainingSeconds)
        assertEquals(900, controller.elapsedSeconds)
        assertTrue(controller.isPaused)
        assertFalse(controller.isRunning)
    }

    @Test
    fun reconcilePersistedTimer_runningSessionDeductsWallClockDeltaAndDetectsCompletion() {
        val persistedAt = 1_700_000_000_000L
        val runningSnapshot = PersistedTimerSnapshot(
            phase = PomodoroPhase.FOCUS,
            subject = "Math",
            plannedMinutes = 25,
            focusMinutesSetting = 25,
            remainingSeconds = 600,
            elapsedSeconds = 900,
            isRunning = true,
            isPaused = false,
            completedPomodorosInCycle = 1,
            sessionStartedDateKey = "2026-11-27",
            lastPersistedEpochMs = persistedAt
        )

        // Reopen app 90 seconds later -> still running with 510s remaining
        val midRestored = FocusStreakCalculator.reconcilePersistedTimer(
            runningSnapshot,
            nowEpochMs = persistedAt + 90_000L
        )
        assertTrue(midRestored.isRunning)
        assertFalse(midRestored.isPaused)
        assertFalse(midRestored.completedWhileAway)
        assertEquals(510, midRestored.remainingSeconds)
        assertEquals(990, midRestored.elapsedSeconds)

        // Reopen app 15 minutes later -> completed while closed!
        val finishedRestored = FocusStreakCalculator.reconcilePersistedTimer(
            runningSnapshot,
            nowEpochMs = persistedAt + 900_000L
        )
        assertTrue(finishedRestored.completedWhileAway)
        assertFalse(finishedRestored.isRunning)
        assertFalse(finishedRestored.isPaused)
        assertEquals(PomodoroPhase.FOCUS, finishedRestored.finishedPhase)
        assertEquals(25, finishedRestored.finishedMinutes)
        assertEquals("2026-11-27", finishedRestored.finishedSessionStartedDateKey)
        // Automatically advanced to Short Break & incremented cycle
        assertEquals(PomodoroPhase.SHORT_BREAK, finishedRestored.phase)
        assertEquals(2, finishedRestored.completedPomodorosInCycle)
    }

    @Test
    fun calculateNextStreak_preventsUnfairLossAcrossMidnightPauses() {
        // 1. Same day completion keeps streak intact
        assertEquals(
            7,
            FocusStreakCalculator.calculateNextStreak(
                currentStreak = 7,
                lastFocusDate = "2026-11-27",
                completionDateKey = "2026-11-27",
                sessionStartedDateKey = "2026-11-27"
            )
        )

        // 2. Next consecutive day increments streak
        assertEquals(
            8,
            FocusStreakCalculator.calculateNextStreak(
                currentStreak = 7,
                lastFocusDate = "2026-11-27",
                completionDateKey = "2026-11-28",
                sessionStartedDateKey = "2026-11-28"
            )
        )

        // 3. Midnight-spanning paused session:
        // Last completed on Nov 26, started & paused sprint on Nov 27 at 11:50 PM, resumed & finished Nov 28 at 12:10 AM.
        // MUST NOT reset to 1!
        assertEquals(
            8,
            FocusStreakCalculator.calculateNextStreak(
                currentStreak = 7,
                lastFocusDate = "2026-11-26",
                completionDateKey = "2026-11-28",
                sessionStartedDateKey = "2026-11-27"
            )
        )

        // 4. Midnight-spanning session when user already focused on Nov 27:
        // Last completed Nov 27, started another sprint Nov 27 night, paused, finished Nov 28 -> streak increments to 8
        assertEquals(
            8,
            FocusStreakCalculator.calculateNextStreak(
                currentStreak = 7,
                lastFocusDate = "2026-11-27",
                completionDateKey = "2026-11-28",
                sessionStartedDateKey = "2026-11-27"
            )
        )

        // 5. Clock skew / timezone travel (lastFocusDate ahead of completionDateKey) never unfairly resets streak
        assertEquals(
            12,
            FocusStreakCalculator.calculateNextStreak(
                currentStreak = 12,
                lastFocusDate = "2026-11-29",
                completionDateKey = "2026-11-28",
                sessionStartedDateKey = "2026-11-28"
            )
        )

        // 6. Genuine multi-day break without bridging session resets to Day 1
        assertEquals(
            1,
            FocusStreakCalculator.calculateNextStreak(
                currentStreak = 7,
                lastFocusDate = "2026-11-20",
                completionDateKey = "2026-11-28",
                sessionStartedDateKey = "2026-11-28"
            )
        )
    }
}
