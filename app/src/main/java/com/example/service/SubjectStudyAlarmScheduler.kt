package com.example.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.SubjectStudyAlarm
import java.util.Calendar

private const val TAG = "SubjectAlarmScheduler"

/**
 * Play-compliant per-subject study schedule alarm scheduler.
 * Allows students to schedule separate alarms for different subjects (e.g. Math at 6:00 AM,
 * Science at 7:00 AM) using AlarmManager without requiring restricted exact-alarm permissions.
 */
object SubjectStudyAlarmScheduler {

    const val CHANNEL_ID = "try_again_subject_alarms"
    const val ACTION_SUBJECT_STUDY_ALARM = "com.example.alarms.ACTION_SUBJECT_STUDY_ALARM"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_SUBJECT = "extra_alarm_subject"
    const val EXTRA_ALARM_DURATION = "extra_alarm_duration"
    const val EXTRA_ALARM_TIME_LABEL = "extra_alarm_time_label"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.subject_alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.subject_alarm_channel_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 280L, 180L, 280L, 180L, 420L)
            setSound(alarmSound, audioAttrs)
        }
        nm.createNotificationChannel(channel)
    }

    fun syncAllAlarms(context: Context, alarms: List<SubjectStudyAlarm>) {
        ensureNotificationChannel(context)
        alarms.forEach { alarm ->
            if (alarm.enabled) {
                scheduleAlarm(context, alarm)
            } else {
                cancelAlarm(context, alarm.id)
            }
        }
    }

    fun scheduleAlarm(context: Context, alarm: SubjectStudyAlarm) {
        val appCtx = context.applicationContext
        ensureNotificationChannel(appCtx)
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(appCtx, alarm)
        val triggerAt = nextTriggerMillis(alarm.hour, alarm.minute)

        runCatching {
            alarmManager.cancel(pendingIntent)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Scheduled ${alarm.subject} study alarm at ${alarm.formattedTime}")
        }.onFailure { err ->
            Log.w(TAG, "Could not schedule subject alarm for ${alarm.subject}: ${err.message}")
        }
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val dummy = SubjectStudyAlarm(id = alarmId, subject = "", hour = 0, minute = 0)
        runCatching {
            alarmManager.cancel(buildPendingIntent(appCtx, dummy))
        }
    }

    private fun requestCodeFor(alarmId: String): Int = 5200 + (alarmId.hashCode() and 0x0FFF)

    private fun buildPendingIntent(context: Context, alarm: SubjectStudyAlarm): PendingIntent {
        val intent = Intent(context, SubjectStudyAlarmReceiver::class.java).apply {
            action = ACTION_SUBJECT_STUDY_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_SUBJECT, alarm.subject)
            putExtra(EXTRA_ALARM_DURATION, alarm.durationMinutes)
            putExtra(EXTRA_ALARM_TIME_LABEL, alarm.formattedTime)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(alarm.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now + 15_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    fun postSubjectAlarmNotification(
        context: Context,
        alarmId: String,
        subject: String,
        durationMinutes: Int,
        timeLabel: String
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        ensureNotificationChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(StreakNotificationManager.EXTRA_OPEN_FOCUS_FROM_REMINDER, true)
            putExtra(EXTRA_ALARM_SUBJECT, subject)
            putExtra(EXTRA_ALARM_DURATION, durationMinutes)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            requestCodeFor(alarmId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time for $subject! ($timeLabel)")
            .setContentText("Your scheduled ${durationMinutes}-min $subject study session is ready. Tap to begin!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0L, 280L, 180L, 280L, 180L, 420L))
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .addAction(
                android.R.drawable.ic_media_play,
                "Start $subject",
                contentPi
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return runCatching {
            nm.notify(requestCodeFor(alarmId), notification)
            true
        }.getOrDefault(false)
    }
}

class SubjectStudyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val alarmId = intent?.getStringExtra(SubjectStudyAlarmScheduler.EXTRA_ALARM_ID).orEmpty()
        val subject = intent?.getStringExtra(SubjectStudyAlarmScheduler.EXTRA_ALARM_SUBJECT)
            ?.ifBlank { "Study" } ?: "Study"
        val duration = intent?.getIntExtra(SubjectStudyAlarmScheduler.EXTRA_ALARM_DURATION, 25) ?: 25
        val timeLabel = intent?.getStringExtra(SubjectStudyAlarmScheduler.EXTRA_ALARM_TIME_LABEL).orEmpty()

        SubjectStudyAlarmScheduler.postSubjectAlarmNotification(
            context = context.applicationContext,
            alarmId = alarmId.ifBlank { subject },
            subject = subject,
            durationMinutes = duration,
            timeLabel = timeLabel.ifBlank { "Now" }
        )
    }
}
