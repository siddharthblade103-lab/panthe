package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.LocalPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "DailyStreakScheduler"
private const val ACTION_DAILY_STREAK_CHECK = "com.example.reminders.ACTION_DAILY_STREAK_CHECK"
private const val REQUEST_CODE_DAILY_STREAK = 4420

/**
 * Play-compliant on-device daily streak check scheduler using [AlarmManager.setInexactRepeating]
 * (no `SCHEDULE_EXACT_ALARM` special permission required). Complements Firebase Cloud Messaging
 * pushes so students always receive their daily streak alert at their chosen time even when
 * offline or before a server-side Cloud Function campaign is configured in Firebase Console.
 */
object DailyStreakReminderScheduler {

    fun syncSchedule(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(appCtx)

        if (!enabled) {
            runCatching { alarmManager.cancel(pendingIntent) }
            Log.d(TAG, "Daily streak reminder schedule paused.")
            return
        }

        val triggerAt = StreakReminderPolicy.nextTriggerMillis(
            nowMillis = System.currentTimeMillis(),
            hour = hour,
            minute = minute
        )
        runCatching {
            alarmManager.cancel(pendingIntent)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(
                TAG,
                "Daily streak reminder armed for ${StreakReminderPolicy.formatReminderTime(hour, minute)}."
            )
        }.onFailure { err ->
            Log.w(TAG, "Could not schedule daily streak reminder: ${err.message}")
        }
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyStreakReminderReceiver::class.java).apply {
            action = ACTION_DAILY_STREAK_CHECK
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_STREAK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class DailyStreakReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = LocalPreferencesStore(appCtx)
                val prefs = store.preferencesFlow.first()
                val profile = store.localProfileFlow.first()
                val dailyLogs = store.localDailyLogsFlow.first()
                val snapshot = store.loadTimerSnapshot()
                val today = StreakReminderPolicy.todayDateKey()
                val todayMinutes = dailyLogs[today]?.first ?: 0

                val shouldAlert = StreakReminderPolicy.shouldSendStreakReminder(
                    enabled = prefs.streakReminderEnabled,
                    todayDateKey = today,
                    lastSessionStartedDate = prefs.lastSessionStartedDate,
                    lastFocusDate = profile.lastFocusDate,
                    todayLoggedMinutes = todayMinutes,
                    activeTimerSnapshot = snapshot,
                    lastReminderSentDate = prefs.lastStreakReminderSentDate,
                    force = false
                )

                if (!shouldAlert) {
                    Log.d(TAG, "Skipping local daily streak alert: user already focused or already alerted today.")
                    return@launch
                }

                val posted = StreakNotificationManager.showStreakReminderNotification(
                    context = appCtx,
                    currentStreak = profile.currentStreak,
                    reminderTimeLabel = StreakReminderPolicy.formatReminderTime(
                        prefs.streakReminderHour,
                        prefs.streakReminderMinute
                    )
                )
                if (posted) {
                    store.markStreakReminderSent(today)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Daily streak reminder check failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
