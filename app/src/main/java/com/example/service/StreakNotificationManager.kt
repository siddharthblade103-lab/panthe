package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.PomodoroPhase
import com.example.data.repository.FocusFirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure, deterministic policy evaluating whether a daily streak alert should be shown,
 * how FCM reminder topics are named, and when the next local reminder check should trigger.
 */
object StreakReminderPolicy {

    const val GLOBAL_STREAK_TOPIC = "daily_streak_reminders"

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = timeZone
        return fmt.format(Date(nowMillis))
    }

    /**
     * Returns true if the student has already started or completed a focus session today
     * (including an active or paused focus timer started today).
     */
    fun hasActivityToday(
        todayDateKey: String,
        lastSessionStartedDate: String,
        lastFocusDate: String,
        todayLoggedMinutes: Int,
        activeTimerSnapshot: PersistedTimerSnapshot?
    ): Boolean {
        if (todayDateKey.isBlank()) return false
        if (todayLoggedMinutes > 0) return true
        if (lastSessionStartedDate == todayDateKey) return true
        if (lastFocusDate == todayDateKey) return true
        if (activeTimerSnapshot != null &&
            activeTimerSnapshot.phase == PomodoroPhase.FOCUS &&
            (activeTimerSnapshot.isRunning || activeTimerSnapshot.isPaused || activeTimerSnapshot.elapsedSeconds >= 60) &&
            activeTimerSnapshot.sessionStartedDateKey == todayDateKey
        ) {
            return true
        }
        return false
    }

    /**
     * Evaluates whether a streak reminder notification should fire right now.
     * Prevents nagging once a focus session has been started today and avoids duplicate alerts
     * on the same calendar day unless [force] is explicitly requested (e.g., test alert).
     */
    fun shouldSendStreakReminder(
        enabled: Boolean,
        todayDateKey: String,
        lastSessionStartedDate: String,
        lastFocusDate: String,
        todayLoggedMinutes: Int,
        activeTimerSnapshot: PersistedTimerSnapshot?,
        lastReminderSentDate: String,
        force: Boolean = false
    ): Boolean {
        if (force) return true
        if (!enabled) return false
        if (lastReminderSentDate == todayDateKey) return false
        return !hasActivityToday(
            todayDateKey = todayDateKey,
            lastSessionStartedDate = lastSessionStartedDate,
            lastFocusDate = lastFocusDate,
            todayLoggedMinutes = todayLoggedMinutes,
            activeTimerSnapshot = activeTimerSnapshot
        )
    }

    fun formatReminderTime(hour: Int, minute: Int): String {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val amPm = if (safeHour >= 12) "PM" else "AM"
        val displayHour = when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, safeMinute, amPm)
    }

    /**
     * Valid FCM topic name matching `[a-zA-Z0-9-_.~%]+` for server-side scheduled campaigns.
     */
    fun buildReminderSlotTopic(hour: Int, minute: Int): String {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        return String.format(Locale.US, "streak_reminder_%02d_%02d", safeHour, safeMinute)
    }

    /**
     * Computes the next trigger timestamp (in epoch millis) for the requested local hour and minute.
     * Rolls forward by 1 day if the time has already passed (within a 30-second buffer).
     */
    fun nextTriggerMillis(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis + 30_000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}

data class FcmSyncOutcome(
    val token: String,
    val subscribedTopic: String,
    val cloudSynced: Boolean
)

/**
 * Centralized manager for Firebase Cloud Messaging registration, topic subscription,
 * Firestore schedule synchronization, and rich Android streak reminder notifications.
 */
object StreakNotificationManager {
    private const val TAG = "StreakNotificationMgr"

    const val CHANNEL_STREAK_REMINDERS_ID = "try_again_streak_reminders"
    const val NOTIFICATION_ID_STREAK_REMINDER = 4410
    const val EXTRA_OPEN_FOCUS_FROM_REMINDER = "com.example.extra.OPEN_FOCUS_FROM_REMINDER"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val existing = nm.getNotificationChannel(CHANNEL_STREAK_REMINDERS_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_STREAK_REMINDERS_ID,
            context.getString(R.string.streak_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.streak_reminder_channel_desc)
            enableVibration(true)
            enableLights(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showStreakReminderNotification(
        context: Context,
        currentStreak: Int,
        reminderTimeLabel: String,
        customTitle: String? = null,
        customBody: String? = null,
        isTest: Boolean = false
    ): Boolean {
        val appCtx = context.applicationContext
        if (!hasNotificationPermission(appCtx)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping streak alert.")
            return false
        }

        ensureNotificationChannel(appCtx)
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false

        val title = customTitle?.takeIf { it.isNotBlank() } ?: when {
            isTest -> "Test Alert: Keep your ${currentStreak.coerceAtLeast(1)}-day streak alive!"
            currentStreak >= 2 -> "Protect your $currentStreak-day study streak!"
            else -> "Time to start today's study sprint!"
        }

        val body = customBody?.takeIf { it.isNotBlank() } ?: when {
            isTest -> "Daily FCM streak reminders are armed for $reminderTimeLabel. Start any focus sprint before then to stay streak-safe!"
            currentStreak >= 2 -> "You haven't started a focus session yet today. Jump into a quick sprint now to keep your $currentStreak-day flame burning!"
            else -> "You haven't started studying yet today. Tap to launch a calm focus sprint and earn Play Points & Gems."
        }

        val launchIntent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(EXTRA_OPEN_FOCUS_FROM_REMINDER, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appCtx,
            4411,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appCtx, CHANNEL_STREAK_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_play,
                appCtx.getString(R.string.streak_reminder_notification_action),
                contentPendingIntent
            )
            .build()

        return runCatching {
            nm.notify(NOTIFICATION_ID_STREAK_REMINDER, notification)
            true
        }.onFailure { err ->
            Log.e(TAG, "Failed to post streak reminder notification", err)
        }.getOrDefault(false)
    }

    fun cancelStreakReminderNotification(context: Context) {
        val nm = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { nm.cancel(NOTIFICATION_ID_STREAK_REMINDER) }
    }

    /**
     * Fetches the real Firebase Cloud Messaging registration token, aligns FCM topic subscriptions
     * (`daily_streak_reminders` + time-slot topic `streak_reminder_HH_MM`), persists locally, and
     * syncs the full schedule + token + latest focus date to Firestore when signed in.
     */
    suspend fun syncFcmTokenAndTopics(
        context: Context,
        prefsStore: LocalPreferencesStore,
        firestoreRepo: FocusFirestoreRepository?,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        currentStreak: Int,
        lastFocusDate: String,
        previousTopic: String,
        cachedToken: String,
        explicitToken: String? = null
    ): FcmSyncOutcome {
        ensureNotificationChannel(context.applicationContext)

        val messaging = runCatching { Firebase.messaging }.getOrNull()
        val fetchedToken = explicitToken?.takeIf { it.isNotBlank() }
            ?: runCatching { messaging?.token?.await() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: cachedToken

        val desiredTopic = if (enabled) StreakReminderPolicy.buildReminderSlotTopic(hour, minute) else ""

        if (messaging != null) {
            if (enabled) {
                runCatching { messaging.subscribeToTopic(StreakReminderPolicy.GLOBAL_STREAK_TOPIC).await() }
                    .onFailure { Log.w(TAG, "Could not subscribe to global FCM topic: ${it.message}") }
                if (previousTopic.isNotBlank() && previousTopic != desiredTopic) {
                    runCatching { messaging.unsubscribeFromTopic(previousTopic).await() }
                        .onFailure { Log.w(TAG, "Could not unsubscribe previous FCM topic $previousTopic: ${it.message}") }
                }
                if (desiredTopic.isNotBlank()) {
                    runCatching { messaging.subscribeToTopic(desiredTopic).await() }
                        .onFailure { Log.w(TAG, "Could not subscribe to slot FCM topic $desiredTopic: ${it.message}") }
                }
            } else {
                runCatching { messaging.unsubscribeFromTopic(StreakReminderPolicy.GLOBAL_STREAK_TOPIC).await() }
                if (previousTopic.isNotBlank()) {
                    runCatching { messaging.unsubscribeFromTopic(previousTopic).await() }
                }
            }
        }

        prefsStore.saveFcmRegistration(
            token = fetchedToken,
            subscribedTopic = desiredTopic
        )

        var cloudSynced = false
        if (firestoreRepo != null) {
            val result = firestoreRepo.upsertStreakReminderSettings(
                enabled = enabled,
                reminderHour = hour,
                reminderMinute = minute,
                timezoneId = TimeZone.getDefault().id,
                fcmToken = fetchedToken,
                fcmTopic = desiredTopic,
                lastFocusDate = lastFocusDate,
                currentStreak = currentStreak
            )
            cloudSynced = result.isSuccess
        }

        return FcmSyncOutcome(
            token = fetchedToken,
            subscribedTopic = desiredTopic,
            cloudSynced = cloudSynced
        )
    }
}
