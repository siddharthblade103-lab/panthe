package com.example

import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.PomodoroPhase
import com.example.service.StreakReminderPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StreakReminderPolicyTest {

    @Test
    fun alertsWhenEnabledAndNoSessionStartedToday() {
        val shouldAlert = StreakReminderPolicy.shouldSendStreakReminder(
            enabled = true,
            todayDateKey = "2026-11-27",
            lastSessionStartedDate = "2026-11-26",
            lastFocusDate = "2026-11-26",
            todayLoggedMinutes = 0,
            activeTimerSnapshot = null,
            lastReminderSentDate = "2026-11-26"
        )
        assertTrue(shouldAlert)
    }

    @Test
    fun suppressesAlertWhenSessionWasStartedToday() {
        val shouldAlert = StreakReminderPolicy.shouldSendStreakReminder(
            enabled = true,
            todayDateKey = "2026-11-27",
            lastSessionStartedDate = "2026-11-27",
            lastFocusDate = "2026-11-26",
            todayLoggedMinutes = 0,
            activeTimerSnapshot = null,
            lastReminderSentDate = ""
        )
        assertFalse(shouldAlert)
    }

    @Test
    fun suppressesAlertWhenFocusMinutesLoggedToday() {
        val shouldAlert = StreakReminderPolicy.shouldSendStreakReminder(
            enabled = true,
            todayDateKey = "2026-11-27",
            lastSessionStartedDate = "",
            lastFocusDate = "",
            todayLoggedMinutes = 25,
            activeTimerSnapshot = null,
            lastReminderSentDate = ""
        )
        assertFalse(shouldAlert)
    }

    @Test
    fun suppressesAlertWhenPausedFocusTimerStartedToday() {
        val pausedToday = PersistedTimerSnapshot(
            phase = PomodoroPhase.FOCUS,
            isRunning = false,
            isPaused = true,
            elapsedSeconds = 420,
            sessionStartedDateKey = "2026-11-27"
        )
        val shouldAlert = StreakReminderPolicy.shouldSendStreakReminder(
            enabled = true,
            todayDateKey = "2026-11-27",
            lastSessionStartedDate = "",
            lastFocusDate = "2026-11-26",
            todayLoggedMinutes = 0,
            activeTimerSnapshot = pausedToday,
            lastReminderSentDate = ""
        )
        assertFalse(shouldAlert)
    }

    @Test
    fun suppressesDuplicateAlertOnSameCalendarDayUnlessForced() {
        val duplicate = StreakReminderPolicy.shouldSendStreakReminder(
            enabled = true,
            todayDateKey = "2026-11-27",
            lastSessionStartedDate = "2026-11-26",
            lastFocusDate = "2026-11-26",
            todayLoggedMinutes = 0,
            activeTimerSnapshot = null,
            lastReminderSentDate = "2026-11-27",
            force = false
        )
        assertFalse(duplicate)

        val forcedTest = StreakReminderPolicy.shouldSendStreakReminder(
            enabled = false,
            todayDateKey = "2026-11-27",
            lastSessionStartedDate = "2026-11-27",
            lastFocusDate = "2026-11-27",
            todayLoggedMinutes = 50,
            activeTimerSnapshot = null,
            lastReminderSentDate = "2026-11-27",
            force = true
        )
        assertTrue(forcedTest)
    }

    @Test
    fun formatsTimeAndTopicNamesCorrectly() {
        assertEquals("12:00 AM", StreakReminderPolicy.formatReminderTime(0, 0))
        assertEquals("8:05 AM", StreakReminderPolicy.formatReminderTime(8, 5))
        assertEquals("12:30 PM", StreakReminderPolicy.formatReminderTime(12, 30))
        assertEquals("8:00 PM", StreakReminderPolicy.formatReminderTime(20, 0))
        assertEquals("streak_reminder_20_00", StreakReminderPolicy.buildReminderSlotTopic(20, 0))
        assertEquals("streak_reminder_07_30", StreakReminderPolicy.buildReminderSlotTopic(7, 30))
    }

    @Test
    fun nextTriggerRollsForwardToTomorrowWhenTimeAlreadyPassed() {
        val tz = TimeZone.getTimeZone("UTC")
        val nowCal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 27, 21, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMillis = StreakReminderPolicy.nextTriggerMillis(
            nowMillis = nowCal.timeInMillis,
            hour = 20,
            minute = 0,
            timeZone = tz
        )
        val resultCal = Calendar.getInstance(tz).apply { timeInMillis = nextMillis }
        assertEquals(28, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
    }
}
