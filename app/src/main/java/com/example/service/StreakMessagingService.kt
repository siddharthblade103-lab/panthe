package com.example.service

import android.util.Log
import com.example.data.local.LocalPreferencesStore
import com.example.data.repository.FocusFirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "StreakMessagingService"

/**
 * Firebase Cloud Messaging receiver for daily study streak push notifications.
 *
 * - Persists rotated FCM registration tokens locally and syncs them (along with the student's
 *   preferred daily alert time, timezone, and latest focus date) to Firestore when signed in.
 * - Filters incoming `streak_reminder` FCM pushes through [StreakReminderPolicy] so students who
 *   already started or finished a focus session today are never unnecessarily disturbed.
 */
class StreakMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Received refreshed FCM registration token.")
        val appCtx = applicationContext
        serviceScope.launch {
            runCatching {
                val store = LocalPreferencesStore(appCtx)
                store.saveFcmTokenOnly(token)
                val prefs = store.preferencesFlow.first()
                val profile = store.localProfileFlow.first()
                val latestActivityDate = maxOf(prefs.lastSessionStartedDate, profile.lastFocusDate)
                val repo = if (Firebase.auth.currentUser != null) FocusFirestoreRepository(appCtx) else null

                StreakNotificationManager.syncFcmTokenAndTopics(
                    context = appCtx,
                    prefsStore = store,
                    firestoreRepo = repo,
                    enabled = prefs.streakReminderEnabled,
                    hour = prefs.streakReminderHour,
                    minute = prefs.streakReminderMinute,
                    currentStreak = profile.currentStreak,
                    lastFocusDate = latestActivityDate,
                    previousTopic = prefs.fcmSubscribedTopic,
                    cachedToken = token,
                    explicitToken = token
                )
            }.onFailure { err ->
                Log.w(TAG, "Failed to sync refreshed FCM token: ${err.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val appCtx = applicationContext
        val data = message.data
        val messageType = data["type"]?.trim()?.lowercase().orEmpty()
        val force = data["force"].equals("true", ignoreCase = true)
        val customTitle = data["title"] ?: message.notification?.title
        val customBody = data["body"] ?: message.notification?.body

        val isStreakPayload = messageType.isEmpty() ||
            messageType == "streak_reminder" ||
            messageType == "daily_streak"

        serviceScope.launch {
            runCatching {
                val store = LocalPreferencesStore(appCtx)
                val prefs = store.preferencesFlow.first()
                val profile = store.localProfileFlow.first()
                val dailyLogs = store.localDailyLogsFlow.first()
                val snapshot = store.loadTimerSnapshot()
                val today = StreakReminderPolicy.todayDateKey()
                val todayMinutes = dailyLogs[today]?.first ?: 0

                val shouldAlert = if (isStreakPayload) {
                    StreakReminderPolicy.shouldSendStreakReminder(
                        enabled = prefs.streakReminderEnabled,
                        todayDateKey = today,
                        lastSessionStartedDate = prefs.lastSessionStartedDate,
                        lastFocusDate = profile.lastFocusDate,
                        todayLoggedMinutes = todayMinutes,
                        activeTimerSnapshot = snapshot,
                        lastReminderSentDate = prefs.lastStreakReminderSentDate,
                        force = force
                    )
                } else {
                    true
                }

                if (!shouldAlert) {
                    Log.d(TAG, "Suppressing FCM streak alert: user already started studying today or reminders paused.")
                    return@launch
                }

                val posted = StreakNotificationManager.showStreakReminderNotification(
                    context = appCtx,
                    currentStreak = profile.currentStreak,
                    reminderTimeLabel = StreakReminderPolicy.formatReminderTime(
                        prefs.streakReminderHour,
                        prefs.streakReminderMinute
                    ),
                    customTitle = customTitle,
                    customBody = customBody,
                    isTest = force
                )
                if (posted && isStreakPayload && !force) {
                    store.markStreakReminderSent(today)
                }
            }.onFailure { err ->
                Log.e(TAG, "Error handling incoming FCM message", err)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
