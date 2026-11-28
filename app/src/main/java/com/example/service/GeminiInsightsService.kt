package com.example.service

import android.util.Log
import com.example.data.model.DayFocusPoint
import com.example.data.model.FocusSession
import com.example.data.model.GeminiInsightReport
import com.example.data.model.PomodoroTask
import com.example.data.model.ProAiNoteResult
import com.example.data.model.ProPracticeQuestion
import com.example.data.model.SessionCoachSummary
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiInsightsService"
private const val GEMINI_MODEL_NAME = "gemini-3.8-flash"

/**
 * Dedicated Gemini API service module powered by Firebase AI Logic (`Firebase.ai`).
 * Sends the user's real focus session data to Gemini and returns structured
 * personalized productivity insights, post-session coach recaps, and encouraging messages.
 */
class GeminiInsightsService {

    private val insightSchema = Schema.obj(
        mapOf(
            "headline" to Schema.string(
                description = "Short, punchy 3-6 word headline summarizing the user's current study momentum."
            ),
            "encouragingMessage" to Schema.string(
                description = "Warm, personal 2-sentence encouraging note referencing the student's actual subjects, streak, or study minutes."
            ),
            "productivityInsights" to Schema.array(
                items = Schema.string(
                    description = "One concrete, actionable productivity insight tailored to the user's session patterns (under 22 words)."
                ),
                description = "Exactly 3 personalized productivity insights."
            ),
            "nextSessionTip" to Schema.string(
                description = "One specific recommendation for their very next Pomodoro sprint (subject, duration, or ambient focus tip)."
            ),
            "focusScoreLabel" to Schema.string(
                description = "A concise 2-3 word momentum badge like 'Steady Climber', 'Deep Flow Pro', or 'Fresh Spark'."
            )
        )
    )

    private val sessionSummarySchema = Schema.obj(
        mapOf(
            "celebrationTag" to Schema.string(
                description = "Short, energetic 2-3 word badge celebrating this specific sprint (e.g. 'Deep Flow Sprint', 'Streak Builder')."
            ),
            "quickSummary" to Schema.string(
                description = "Warm, concise 2-sentence AI coach recap referencing the exact minutes spent, subject studied, points earned, and daily goal progress."
            ),
            "nextMoveTip" to Schema.string(
                description = "One short, practical tip (under 18 words) for their break or next study round."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.65f
                responseMimeType = "application/json"
                responseSchema = insightSchema
            }
        )
    }

    private val proStudyPackSchema = Schema.obj(
        mapOf(
            "summary" to Schema.string(description = "Clear, concise 2-3 sentence concept overview of the topic."),
            "keyPoints" to Schema.array(
                items = Schema.string(description = "High-yield study bullet point or formula."),
                description = "4 to 6 crisp revision notes."
            ),
            "practiceQuestions" to Schema.array(
                items = Schema.obj(
                    mapOf(
                        "question" to Schema.string(description = "Exam-style practice question."),
                        "answer" to Schema.string(description = "Clear step-by-step answer."),
                        "hint" to Schema.string(description = "Short memory or formula hint.")
                    )
                ),
                description = "4 ready-made practice questions with answers."
            )
        )
    )

    private val sessionSummaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.7f
                responseMimeType = "application/json"
                responseSchema = sessionSummarySchema
            }
        )
    }

    private val proStudyPackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.55f
                responseMimeType = "application/json"
                responseSchema = proStudyPackSchema
            }
        )
    }

    private val proTutorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.6f
            }
        )
    }

    suspend fun generateSessionSummary(
        displayName: String,
        subject: String,
        completedMinutes: Int,
        plannedMinutes: Int,
        pointsEarned: Int,
        gemsEarned: Int,
        streakDays: Int,
        todayTotalMinutes: Int,
        dailyGoalMinutes: Int,
        ambientSound: String,
        nextPhaseName: String
    ): Result<SessionCoachSummary> {
        return try {
            val prompt = """
                You are an upbeat, concise Gemini AI study coach inside the Try Again focus app.
                The student just finished a focus session! Give them a quick, encouraging post-session recap in JSON.

                Completed Session:
                - Student: $displayName
                - Subject: $subject
                - Time Spent: $completedMinutes min (planned: $plannedMinutes min)
                - Play Points Earned: +$pointsEarned pts
                - Gems Earned: +$gemsEarned gems
                - Ambient Sound: $ambientSound
                - Today's Total Study: $todayTotalMinutes / $dailyGoalMinutes min goal
                - Active Streak: $streakDays days
                - Up Next: $nextPhaseName

                Keep it punchy, warm, and genuinely tailored to these exact numbers.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                sessionSummaryModel.generateContent(prompt)
            }

            val rawJson = response.text?.trim()
            if (rawJson.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Gemini AI coach returned an empty summary."))
            }

            val root = JSONObject(rawJson)
            Result.success(
                SessionCoachSummary(
                    celebrationTag = root.optString("celebrationTag").trim().ifBlank { "Focus Sprint Done" },
                    quickSummary = root.optString("quickSummary").trim()
                        .ifBlank { "Solid $completedMinutes-minute $subject session! You earned +$pointsEarned Play Points." },
                    nextMoveTip = root.optString("nextMoveTip").trim()
                        .ifBlank { "Take a quick stretch during your $nextPhaseName and hydrate." }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini session summary timed out", e)
            Result.failure(Exception("AI coach recap timed out. Tap Retry to try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Gemini session summary failed", e)
            val friendly = e.message?.take(140)?.ifBlank { null }
                ?: "Could not reach Gemini AI coach right now."
            Result.failure(Exception(friendly, e))
        }
    }

    suspend fun generateStudyInsights(
        profile: UserProfile,
        sessions: List<FocusSession>,
        weeklyPoints: List<DayFocusPoint>,
        tasks: List<PomodoroTask>,
        todayMinutes: Int
    ): Result<GeminiInsightReport> {
        return try {
            val prompt = buildStudyPrompt(
                profile = profile,
                sessions = sessions,
                weeklyPoints = weeklyPoints,
                tasks = tasks,
                todayMinutes = todayMinutes
            )

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }

            val rawJson = response.text?.trim()
            if (rawJson.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Gemini returned an empty response. Please try again."))
            }

            val parsed = parseInsightReport(rawJson)
            Result.success(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini insights request timed out", e)
            Result.failure(Exception("Insights request timed out. Check your connection and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Gemini insights generation failed", e)
            val friendly = e.message?.take(140)?.ifBlank { null }
                ?: "Could not generate insights right now. Please try again shortly."
            Result.failure(Exception(friendly, e))
        }
    }

    private fun buildStudyPrompt(
        profile: UserProfile,
        sessions: List<FocusSession>,
        weeklyPoints: List<DayFocusPoint>,
        tasks: List<PomodoroTask>,
        todayMinutes: Int
    ): String {
        val recentSessions = sessions.take(15)
        val weeklyTotal = weeklyPoints.sumOf { it.minutes }
        val activeDays = weeklyPoints.count { it.minutes > 0 }
        val avgSprint = if (recentSessions.isNotEmpty()) {
            recentSessions.map { it.completedMinutes }.average().toInt()
        } else {
            25
        }
        val subjectBreakdown = recentSessions
            .groupBy { it.subject }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.completedMinutes } }
            .joinToString(", ") { (subject, list) ->
                "$subject: ${list.sumOf { it.completedMinutes }}m (${list.size} sessions)"
            }
            .ifBlank { "No completed sessions recorded yet" }

        val dailyBreakdown = weeklyPoints.joinToString(", ") { "${it.dayLabel}=${it.minutes}m" }
        val completedTasks = tasks.count { it.completed }
        val openTasks = tasks.filterNot { it.completed }.take(4).joinToString("; ") { "${it.title} (${it.subject})" }
            .ifBlank { "None" }

        return """
            You are an encouraging, sharp academic focus coach inside a Pomodoro study app called Try Again.
            Analyze this student's real study telemetry and return personalized, uplifting productivity insights as JSON.

            Student Profile:
            - Name: ${profile.displayName}
            - Level: ${profile.level} (${profile.focusXp} Play Points)
            - Daily Focus Goal: ${profile.dailyGoalMinutes} min
            - Today's Focus Minutes: $todayMinutes min
            - Current Streak: ${profile.currentStreak} days (Best: ${profile.longestStreak} days)
            - Lifetime Focus Minutes: ${profile.totalFocusMinutes} min

            Last 7 Days Summary:
            - Weekly Total: $weeklyTotal min across $activeDays active days
            - Daily Minutes: $dailyBreakdown
            - Average Sprint Length: $avgSprint min
            - Subject Breakdown: $subjectBreakdown
            - Tasks Completed: $completedTasks / ${tasks.size}
            - Upcoming Open Tasks: $openTasks

            Guidelines:
            - Be genuinely warm, specific to their real numbers and subjects, never generic.
            - If they have 0 sessions so far, encourage them warmly to start their first 15-25 min sprint and suggest a concrete first step using their open tasks or subjects.
            - Keep each insight crisp and immediately actionable.
        """.trimIndent()
    }

    private fun parseInsightReport(jsonText: String): GeminiInsightReport {
        val root = JSONObject(jsonText)
        val headline = root.optString("headline").trim().ifBlank { "Your Focus Rhythm" }
        val encouraging = root.optString("encouragingMessage").trim()
            .ifBlank { "Every focused minute builds lasting momentum." }
        val nextTip = root.optString("nextSessionTip").trim()
            .ifBlank { "Start a 25-minute sprint on your top priority subject." }
        val scoreLabel = root.optString("focusScoreLabel").trim()
            .ifBlank { "Focus Builder" }

        val insightsArray = root.optJSONArray("productivityInsights")
        val insights = buildList {
            if (insightsArray != null) {
                for (i in 0 until insightsArray.length()) {
                    val item = insightsArray.optString(i).trim()
                    if (item.isNotEmpty()) add(item)
                }
            }
        }

        return GeminiInsightReport(
            headline = headline,
            encouragingMessage = encouraging,
            productivityInsights = insights,
            nextSessionTip = nextTip,
            focusScoreLabel = scoreLabel,
            generatedAtMs = System.currentTimeMillis()
        )
    }

    /**
     * Pro Edition (500-Hour Unlock): Generates ready-made revision notes & practice questions
     * using Firebase AI Logic (`gemini-3.8-flash`).
     */
    suspend fun generateProStudyPack(
        subject: String,
        topic: String
    ): Result<ProAiNoteResult> {
        return try {
            val cleanSubject = subject.trim().ifBlank { "General Study" }
            val cleanTopic = topic.trim().ifBlank { "Core Fundamentals" }
            val prompt = """
                You are Try Again Pro AI Tutor for top scholars who have studied 500+ hours.
                Generate crisp, ready-made study notes and 4 exam-ready practice questions with clear answers in JSON.
                Subject: $cleanSubject
                Topic: $cleanTopic
                Keep explanations clear, accurate, and easy to revise quickly.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                proStudyPackModel.generateContent(prompt)
            }
            val rawJson = response.text?.trim()
            if (rawJson.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Pro AI returned an empty study pack."))
            }
            val root = JSONObject(rawJson)
            val summary = root.optString("summary").trim().ifBlank { "Focused revision notes for $cleanTopic." }
            val pointsArr = root.optJSONArray("keyPoints")
            val keyPoints = buildList {
                if (pointsArr != null) {
                    for (i in 0 until pointsArr.length()) {
                        val item = pointsArr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }
            val qArr = root.optJSONArray("practiceQuestions")
            val questions = buildList {
                if (qArr != null) {
                    for (i in 0 until qArr.length()) {
                        val qObj = qArr.optJSONObject(i) ?: continue
                        val q = qObj.optString("question").trim()
                        val a = qObj.optString("answer").trim()
                        val h = qObj.optString("hint").trim()
                        val fullAnswer = if (h.isNotEmpty()) "$a\nHint: $h" else a
                        if (q.isNotEmpty() && a.isNotEmpty()) {
                            add(ProPracticeQuestion(question = q, answer = fullAnswer))
                        }
                    }
                }
            }
            Result.success(
                ProAiNoteResult(
                    subject = cleanSubject,
                    topic = cleanTopic,
                    summary = summary,
                    keyPoints = keyPoints,
                    practiceQuestions = questions
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Pro AI notes generation timed out", e)
            Result.failure(Exception("Pro AI timed out. Please try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Pro AI notes generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not generate Pro study notes.", e))
        }
    }

    /**
     * Pro Edition (500-Hour Unlock): Interactive AI Tutor Q&A via Firebase AI Logic.
     */
    suspend fun askProAiTutor(
        subject: String,
        question: String
    ): Result<String> {
        return try {
            val prompt = """
                You are Try Again Pro AI Study Tutor.
                Subject: ${subject.ifBlank { "General Study" }}
                Student Question: ${question.trim()}
                Give a clear, well-structured, step-by-step explanation with a brief takeaway at the end.
            """.trimIndent()
            val response = withTimeout(60_000L) {
                proTutorModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Pro AI Tutor returned an empty answer."))
            } else {
                Result.success(text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Pro AI Tutor timed out", e)
            Result.failure(Exception("AI Tutor request timed out. Please try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Pro AI Tutor failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not reach Pro AI Tutor right now.", e))
        }
    }
}
