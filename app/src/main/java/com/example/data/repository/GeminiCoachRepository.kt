package com.example.data.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Gemini AI Study Coach powered by Firebase AI Logic (gemini-3.8-flash) with App Check attestation.
 * Uses lazy initialization so Firebase.ai is only evaluated upon first inference.
 */
class GeminiCoachRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Try Again, an upbeat, warm, Duolingo-inspired study monkey coach for students. " +
                        "Give crisp, practical, science-backed study tips or short energetic motivational boosts. " +
                        "Keep responses concise (2-4 short lines, under 65 words), clear, encouraging, and directly actionable. " +
                        "No markdown headers or heavy formatting."
                )
            }
        )
    }

    fun streamStudyTip(
        subject: String,
        plannedMinutes: Int,
        streakDays: Int,
        activeTaskTitle: String?
    ): Flow<String> {
        val taskClause = activeTaskTitle?.takeIf { it.isNotBlank() }
            ?.let { " Active task: \"$it\"." } ?: ""
        val prompt = "Give one crisp, high-impact study technique for a $plannedMinutes-minute " +
            "$subject focus session.$taskClause Current streak: $streakDays days."
        return model.generateContentStream(prompt).mapNotNull { it.text }
    }

    fun streamMotivation(
        subject: String,
        isFocusing: Boolean,
        streakDays: Int,
        gems: Int
    ): Flow<String> {
        val stateLabel = if (isFocusing) "currently mid-session focusing on $subject" else "about to start a $subject focus sprint"
        val prompt = "The student is $stateLabel (streak: $streakDays days, gems: $gems). " +
            "Give a fresh, warm, punchy 2-sentence motivational boost in the playful spirit of 'Try Again'!"
        return model.generateContentStream(prompt).mapNotNull { it.text }
    }

    fun streamCustomQuestion(
        subject: String,
        question: String
    ): Flow<String> {
        val prompt = "Student studying $subject asks during a focus session: \"$question\". " +
            "Answer clearly in 3 concise sentences so they can get right back into flow."
        return model.generateContentStream(prompt).mapNotNull { it.text }
    }
}
