package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.DayFocusPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "WeekendReportScheduler"
private const val ACTION_WEEKEND_STUDY_PDF = "com.example.reports.ACTION_WEEKEND_STUDY_PDF"
private const val REQUEST_CODE_WEEKEND_ALARM = 4301
private const val NOTIFICATION_ID_WEEKEND_PDF = 4302

/**
 * Play-compliant weekly scheduler using [AlarmManager.setInexactRepeating] (no exact alarm
 * special permission required) to prepare the student's Weekly Study PDF every Sunday evening
 * and notify them with a 1-tap Send/Share PDF action.
 */
object WeekendReportScheduler {

    fun syncSchedule(context: Context, enabled: Boolean) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(appCtx)

        if (!enabled) {
            runCatching { alarmManager.cancel(pendingIntent) }
            Log.d(TAG, "Weekend PDF auto-report cancelled.")
            return
        }

        val triggerAt = nextSundayEveningMillis()
        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                AlarmManager.INTERVAL_DAY * 7L,
                pendingIntent
            )
            Log.d(TAG, "Weekend PDF auto-report scheduled for next Sunday 18:00.")
        }.onFailure { err ->
            Log.w(TAG, "Could not schedule weekend report alarm: ${err.message}")
        }
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WeekendReportReceiver::class.java).apply {
            action = ACTION_WEEKEND_STUDY_PDF
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WEEKEND_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextSundayEveningMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis + 60_000L) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}

class WeekendReportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = LocalPreferencesStore(appCtx)
                val prefs = store.preferencesFlow.first()
                if (!prefs.weekendPdfReminderEnabled) return@launch

                val profile = store.localProfileFlow.first()
                val sessions = store.localSessionHistoryFlow.first()
                val dailyLogs = store.localDailyLogsFlow.first()

                val dayFmt = SimpleDateFormat("EEE", Locale.US)
                val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val cal = Calendar.getInstance()
                val weeklyPoints = (6 downTo 0).map { daysAgo ->
                    val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
                    val key = keyFmt.format(c.time)
                    val fromSessions = sessions.filter { it.dateKey == key }.sumOf { it.completedMinutes }
                    val mins = maxOf(fromSessions, dailyLogs[key]?.first ?: 0)
                    DayFocusPoint(
                        dayLabel = dayFmt.format(c.time).take(3),
                        dateKey = key,
                        minutes = mins
                    )
                }

                val pdfResult = StudyReportPdfGenerator.generateWeeklyReportPdf(
                    context = appCtx,
                    profile = profile,
                    sessions = sessions,
                    weeklyPoints = weeklyPoints,
                    aiReport = null
                )

                pdfResult.onSuccess { pdfFile ->
                    postWeekendPdfNotification(appCtx, pdfFile, weeklyPoints.sumOf { it.minutes })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Weekend report generation failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postWeekendPdfNotification(context: Context, pdfFile: java.io.File, weeklyMinutes: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FocusTimerService.CHANNEL_COMPLETE_ID,
                "Session Complete & Weekly Reports",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val shareIntent = StudyReportPdfGenerator.buildSharePdfIntent(context, pdfFile)
        val sharePending = PendingIntent.getActivity(
            context,
            4303,
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FocusTimerService.CHANNEL_COMPLETE_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("Your Weekend Study PDF Report is Ready!")
            .setContentText("You focused $weeklyMinutes min this week. Tap to send or save your PDF report.")
            .setContentIntent(sharePending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_share, "Send PDF", sharePending)
            .build()

        runCatching { nm.notify(NOTIFICATION_ID_WEEKEND_PDF, notification) }
    }
}
