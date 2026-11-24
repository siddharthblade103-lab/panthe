package com.example

import com.example.data.model.PomodoroPhase
import com.example.ui.components.PomodoroTimerController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroTimerControllerTest {

    @Test
    fun startPauseResumeAndReset_manageTimerStateCorrectly() {
        val controller = PomodoroTimerController(initialFocusMinutes = 25)
        assertEquals(25, controller.plannedMinutes)
        assertEquals(1500, controller.remainingSeconds)
        assertFalse(controller.isRunning)
        assertFalse(controller.isPaused)

        controller.start()
        assertTrue(controller.isRunning)
        assertFalse(controller.isPaused)

        repeat(10) { assertNull(controller.tickOneSecond()) }
        assertEquals(1490, controller.remainingSeconds)
        assertEquals(10, controller.elapsedSeconds)

        controller.pause()
        assertFalse(controller.isRunning)
        assertTrue(controller.isPaused)
        assertNull(controller.tickOneSecond())
        assertEquals(1490, controller.remainingSeconds)

        controller.start()
        assertTrue(controller.isRunning)
        assertFalse(controller.isPaused)
        controller.tickOneSecond()
        assertEquals(1489, controller.remainingSeconds)

        controller.reset()
        assertFalse(controller.isRunning)
        assertFalse(controller.isPaused)
        assertEquals(1500, controller.remainingSeconds)
        assertEquals(0, controller.elapsedSeconds)
    }

    @Test
    fun durationControls_clampAndPersistPerPhase() {
        val controller = PomodoroTimerController()

        controller.setDurationMinutes(40)
        assertEquals(40, controller.plannedMinutes)
        assertEquals(40 * 60, controller.remainingSeconds)

        controller.adjustDurationMinutes(5)
        assertEquals(45, controller.plannedMinutes)

        controller.adjustDurationMinutes(-200)
        assertEquals(PomodoroPhase.FOCUS.minMinutes, controller.plannedMinutes)

        controller.setDurationMinutes(30)
        controller.selectPhase(PomodoroPhase.SHORT_BREAK)
        assertEquals(PomodoroPhase.SHORT_BREAK, controller.phase)
        assertEquals(5, controller.plannedMinutes)

        controller.setDurationMinutes(8)
        assertEquals(8, controller.shortBreakMinutes)
        assertEquals(8 * 60, controller.remainingSeconds)

        // Switching back to Focus restores the customized 30-minute focus duration
        controller.selectPhase(PomodoroPhase.FOCUS)
        assertEquals(30, controller.plannedMinutes)
        assertEquals(30 * 60, controller.remainingSeconds)
    }

    @Test
    fun completingFourFocusSprints_advancesToLongBreakAndResetsCycle() {
        val controller = PomodoroTimerController(
            initialFocusMinutes = 1,
            initialShortBreakMinutes = 5,
            initialLongBreakMinutes = 15
        )

        // Complete 3 focus sprints -> Short Break after each
        repeat(3) { sprintIndex ->
            assertEquals(PomodoroPhase.FOCUS, controller.phase)
            controller.start()
            var finished: Pair<PomodoroPhase, Int>? = null
            repeat(60) { finished = controller.tickOneSecond() }
            assertEquals(PomodoroPhase.FOCUS to 1, finished)
            assertEquals(PomodoroPhase.SHORT_BREAK, controller.phase)
            assertEquals(sprintIndex + 1, controller.completedPomodorosInCycle)
            assertEquals(5 * 60, controller.remainingSeconds)

            // Skip short break back to Focus
            controller.completeCurrentPhase(autoFinished = false)
            assertEquals(PomodoroPhase.FOCUS, controller.phase)
        }

        // 4th sprint -> Long Break & cycle counter resets to 0
        controller.start()
        var fourthFinished: Pair<PomodoroPhase, Int>? = null
        repeat(60) { fourthFinished = controller.tickOneSecond() }
        assertEquals(PomodoroPhase.FOCUS to 1, fourthFinished)
        assertEquals(PomodoroPhase.LONG_BREAK, controller.phase)
        assertEquals(0, controller.completedPomodorosInCycle)
        assertEquals(15 * 60, controller.remainingSeconds)
    }
}
