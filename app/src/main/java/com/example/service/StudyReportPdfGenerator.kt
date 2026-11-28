package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.DayFocusPoint
import com.example.data.model.FocusSession
import com.example.data.model.GeminiInsightReport
import com.example.data.model.PlayPointsTier
import com.example.data.model.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "StudyReportPdfGenerator"
private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842

/**
 * Generates a crisp, multi-section A4 Weekly Study Report PDF using Android's native
 * [PdfDocument] and shares it via [FileProvider] with zero storage permissions.
 */
object StudyReportPdfGenerator {

    fun generateWeeklyReportPdf(
        context: Context,
        profile: UserProfile,
        sessions: List<FocusSession>,
        weeklyPoints: List<DayFocusPoint>,
        aiReport: GeminiInsightReport? = null
    ): Result<File> {
        return runCatching {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val outFile = File(reportsDir, "TryAgain_Weekly_Study_Report_$dateStamp.pdf")

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Palette paints
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F4FAF5") }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#163020") }
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D5E8D8")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            val accentGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#58CC02") }
            val accentBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1CB0F6") }
            val accentGold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF9600") }
            val accentTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E6F2E8") }

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 20f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C8E6CE")
                textSize = 11f
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#142214")
                textSize = 13.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3B4F3D")
                textSize = 10.5f
            }
            val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#142214")
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val metricValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#142214")
                textSize = 16.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6B7F6E")
                textSize = 9.5f
            }

            // Full page soft background
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            // Header Banner
            val headerRect = RectF(28f, 26f, PAGE_WIDTH - 28f, 114f)
            canvas.drawRoundRect(headerRect, 16f, 16f, headerPaint)
            val tier = PlayPointsTier.forPoints(profile.focusXp)
            val generatedDate = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US).format(Date())
            canvas.drawText("Try Again — Weekly Study Report", 46f, 58f, titlePaint)
            canvas.drawText(
                "Scholar: ${profile.displayName}  •  Level ${profile.level} (${tier.displayName} Tier)  •  $generatedDate",
                46f,
                80f,
                subtitlePaint
            )
            canvas.drawText(
                "Daily Goal: ${profile.dailyGoalMinutes} min  •  Play Points: ${profile.focusXp} pts  •  Gems: ${profile.gems}",
                46f,
                98f,
                subtitlePaint
            )

            // 4 Summary Metric Tiles
            val weeklyTotalMin = weeklyPoints.sumOf { it.minutes }
            val weeklySessionsCount = sessions.count { s -> weeklyPoints.any { it.dateKey == s.dateKey } }
                .let { if (it == 0 && sessions.isNotEmpty()) sessions.size else it }
            val metrics = listOf(
                Triple("7-Day Study", "${weeklyTotalMin}m", accentGreen),
                Triple("Sessions", "$weeklySessionsCount", accentBlue),
                Triple("Active Streak", "${profile.currentStreak}d", accentGold),
                Triple("Play Points", "${profile.focusXp}", accentBlue)
            )
            val tileTop = 128f
            val tileBottom = 188f
            val gap = 10f
            val tileW = (PAGE_WIDTH - 56f - gap * 3) / 4f
            metrics.forEachIndexed { idx, (label, value, barColor) ->
                val left = 28f + idx * (tileW + gap)
                val rect = RectF(left, tileTop, left + tileW, tileBottom)
                canvas.drawRoundRect(rect, 12f, 12f, cardPaint)
                canvas.drawRoundRect(rect, 12f, 12f, borderPaint)
                canvas.drawRoundRect(RectF(left + 12f, tileTop + 10f, left + 36f, tileTop + 14f), 2f, 2f, barColor)
                canvas.drawText(value, left + 12f, tileTop + 37f, metricValPaint)
                canvas.drawText(label, left + 12f, tileTop + 52f, mutedPaint)
            }

            // 7-Day Daily Focus Bar Chart
            val chartTop = 202f
            val chartBottom = 352f
            val chartRect = RectF(28f, chartTop, PAGE_WIDTH - 28f, chartBottom)
            canvas.drawRoundRect(chartRect, 14f, 14f, cardPaint)
            canvas.drawRoundRect(chartRect, 14f, 14f, borderPaint)
            canvas.drawText("7-Day Focus Rhythm", 44f, chartTop + 24f, sectionPaint)

            val maxDayMin = (weeklyPoints.maxOfOrNull { it.minutes } ?: 30).coerceAtLeast(30)
            val plotLeft = 48f
            val plotWidth = PAGE_WIDTH - 96f
            val barSlot = plotWidth / weeklyPoints.size.coerceAtLeast(1)
            val barMaxH = 74f
            val baseLineY = chartBottom - 28f
            weeklyPoints.forEachIndexed { idx, pt ->
                val cx = plotLeft + idx * barSlot + barSlot / 2f
                val barW = 24f
                val trackRect = RectF(cx - barW / 2f, baseLineY - barMaxH, cx + barW / 2f, baseLineY)
                canvas.drawRoundRect(trackRect, 6f, 6f, accentTrack)
                val fillH = ((pt.minutes.toFloat() / maxDayMin) * barMaxH).coerceIn(if (pt.minutes > 0) 6f else 0f, barMaxH)
                if (fillH > 0f) {
                    val fillRect = RectF(cx - barW / 2f, baseLineY - fillH, cx + barW / 2f, baseLineY)
                    canvas.drawRoundRect(fillRect, 6f, 6f, accentGreen)
                }
                if (pt.minutes > 0) {
                    canvas.drawText("${pt.minutes}m", cx - 10f, baseLineY - fillH - 5f, mutedPaint)
                }
                canvas.drawText(pt.dayLabel, cx - 10f, chartBottom - 12f, boldBodyPaint)
            }

            // Subject Breakdown Card
            val subjTop = 366f
            val subjBottom = 492f
            val subjRect = RectF(28f, subjTop, PAGE_WIDTH - 28f, subjBottom)
            canvas.drawRoundRect(subjRect, 14f, 14f, cardPaint)
            canvas.drawRoundRect(subjRect, 14f, 14f, borderPaint)
            canvas.drawText("Subject Time Breakdown", 44f, subjTop + 24f, sectionPaint)

            val bySubject = sessions
                .groupBy { it.subject }
                .mapValues { (_, list) -> list.sumOf { it.completedMinutes } }
                .entries
                .sortedByDescending { it.value }
                .take(4)

            if (bySubject.isEmpty()) {
                canvas.drawText("Complete focus sessions to populate your subject breakdown.", 44f, subjTop + 56f, bodyPaint)
            } else {
                val maxSubj = bySubject.maxOf { it.value }.coerceAtLeast(1)
                bySubject.forEachIndexed { idx, (subject, mins) ->
                    val rowY = subjTop + 48f + idx * 20f
                    canvas.drawText(subject.take(18), 44f, rowY, boldBodyPaint)
                    val barStart = 152f
                    val barFull = 340f
                    canvas.drawRoundRect(RectF(barStart, rowY - 9f, barStart + barFull, rowY - 1f), 4f, 4f, accentTrack)
                    val fillW = (mins.toFloat() / maxSubj) * barFull
                    canvas.drawRoundRect(RectF(barStart, rowY - 9f, barStart + fillW, rowY - 1f), 4f, 4f, accentBlue)
                    canvas.drawText("${mins} min", barStart + barFull + 10f, rowY, boldBodyPaint)
                }
            }

            // Gemini AI Insights Summary (if available)
            var nextSectionTop = 506f
            if (aiReport != null) {
                val aiBottom = 612f
                val aiRect = RectF(28f, nextSectionTop, PAGE_WIDTH - 28f, aiBottom)
                canvas.drawRoundRect(aiRect, 14f, 14f, cardPaint)
                canvas.drawRoundRect(aiRect, 14f, 14f, borderPaint)
                canvas.drawText("Personalized Study Insights · ${aiReport.focusScoreLabel}", 44f, nextSectionTop + 22f, sectionPaint)
                drawWrappedText(
                    canvas = canvas,
                    text = aiReport.encouragingMessage,
                    x = 44f,
                    startY = nextSectionTop + 40f,
                    maxWidth = PAGE_WIDTH - 88f,
                    lineHeight = 14f,
                    maxLines = 2,
                    paint = bodyPaint
                )
                val tip = aiReport.nextSessionTip
                drawWrappedText(
                    canvas = canvas,
                    text = "Next Sprint Tip: $tip",
                    x = 44f,
                    startY = nextSectionTop + 74f,
                    maxWidth = PAGE_WIDTH - 88f,
                    lineHeight = 14f,
                    maxLines = 2,
                    paint = boldBodyPaint
                )
                nextSectionTop = aiBottom + 14f
            }

            // Recent Study History Table
            val histBottom = PAGE_HEIGHT - 36f
            val histRect = RectF(28f, nextSectionTop, PAGE_WIDTH - 28f, histBottom)
            canvas.drawRoundRect(histRect, 14f, 14f, cardPaint)
            canvas.drawRoundRect(histRect, 14f, 14f, borderPaint)
            canvas.drawText("Study Session History", 44f, nextSectionTop + 24f, sectionPaint)

            val headerY = nextSectionTop + 44f
            canvas.drawText("Date", 44f, headerY, mutedPaint)
            canvas.drawText("Subject", 136f, headerY, mutedPaint)
            canvas.drawText("Duration", 268f, headerY, mutedPaint)
            canvas.drawText("Ambient Sound", 342f, headerY, mutedPaint)
            canvas.drawText("Rewards", 464f, headerY, mutedPaint)

            if (sessions.isEmpty()) {
                canvas.drawText("No completed sessions logged yet.", 44f, headerY + 24f, bodyPaint)
            } else {
                val maxRows = ((histBottom - headerY - 18f) / 18f).toInt().coerceAtLeast(3)
                sessions.take(maxRows).forEachIndexed { idx, s ->
                    val y = headerY + 20f + idx * 18f
                    canvas.drawText(s.dateKey.ifBlank { dateStamp }, 44f, y, bodyPaint)
                    canvas.drawText(s.subject.take(18), 136f, y, boldBodyPaint)
                    canvas.drawText("${s.completedMinutes} min", 268f, y, bodyPaint)
                    canvas.drawText(s.ambientSound.take(16), 342f, y, bodyPaint)
                    canvas.drawText("+${s.gemsEarned}g / +${s.xpEarned}p", 464f, y, bodyPaint)
                }
            }

            // Footer
            canvas.drawText(
                "Generated by Try Again Study Tracker • Keep your streak alive!",
                28f,
                PAGE_HEIGHT - 16f,
                mutedPaint
            )

            document.finishPage(page)
            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            outFile
        }.onFailure { err ->
            Log.e(TAG, "Failed to generate Weekly Study Report PDF", err)
        }
    }

    fun buildSharePdfIntent(context: Context, pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Try Again — Weekly Study Report PDF")
            putExtra(
                Intent.EXTRA_TEXT,
                "Here is my Weekly Study Report PDF generated by Try Again!"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Send Weekly Study PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun sharePdf(context: Context, pdfFile: File): Boolean {
        return runCatching {
            context.startActivity(buildSharePdfIntent(context, pdfFile))
            true
        }.onFailure { err ->
            Log.e(TAG, "Failed to open share sheet for PDF", err)
        }.getOrDefault(false)
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        lineHeight: Float,
        maxLines: Int,
        paint: Paint
    ) {
        val words = text.split(Regex("\\s+"))
        var currentLine = ""
        var lineCount = 0
        var y = startY

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                canvas.drawText(currentLine, x, y, paint)
                lineCount++
                y += lineHeight
                if (lineCount >= maxLines) return
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty() && lineCount < maxLines) {
            canvas.drawText(currentLine, x, y, paint)
        }
    }
}
