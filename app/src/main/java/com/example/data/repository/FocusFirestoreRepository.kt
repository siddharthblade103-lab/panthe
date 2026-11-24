package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FocusSession
import com.example.data.model.GlassThemeId
import com.example.data.model.MascotBadgeId
import com.example.data.model.PomodoroTask
import com.example.data.model.UserProfile
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FocusFirestoreRepository(context: Context) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )

    private fun requireUserId(): String =
        Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Authentication required for cloud sync")

    fun observeProfile(): Flow<UserProfile?> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(null)
        val path = "users/$uid"
        return db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeSessions(limit: Long = 100): Flow<List<FocusSession>> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        val path = "users/$uid/sessions"
        return db.collection("users").document(uid).collection("sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FocusSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeTasks(): Flow<List<PomodoroTask>> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        val path = "users/$uid/tasks"
        return db.collection("users").document(uid).collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PomodoroTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun ensureUserProfile(seed: UserProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid)
        val existing = ref.get().await()
        if (!existing.exists()) {
            val safeName = (Firebase.auth.currentUser?.displayName ?: seed.displayName)
                .trim().take(60).ifEmpty { "Scholar" }
            val payload = hashMapOf(
                "userId" to uid,
                "displayName" to safeName,
                "dailyGoalMinutes" to seed.dailyGoalMinutes.coerceIn(15, 720),
                "currentStreak" to seed.currentStreak.coerceIn(0, 10000),
                "longestStreak" to seed.longestStreak.coerceIn(0, 10000),
                "totalFocusMinutes" to seed.totalFocusMinutes.coerceIn(0, 1000000),
                "focusXp" to seed.focusXp.coerceIn(0, 10000000),
                "gems" to seed.gems.coerceIn(0, 1000000),
                "totalGemsEarned" to seed.totalGemsEarned.coerceIn(0, 1000000),
                "lastFocusDate" to seed.lastFocusDate.take(20),
                "subjects" to seed.subjects.take(20),
                "unlockedSounds" to seed.unlockedSounds.take(20),
                "unlockedCosmetics" to seed.unlockedCosmetics.take(30),
                "claimedMilestones" to seed.claimedMilestones.take(30),
                "equippedTheme" to GlassThemeId.fromWire(seed.equippedTheme).name,
                "equippedBadge" to MascotBadgeId.fromWire(seed.equippedBadge).name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/${Firebase.auth.currentUser?.uid}")
    }

    suspend fun updateProfileFields(fields: Map<String, Any>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val payload = fields.toMutableMap()
        payload["updatedAt"] = FieldValue.serverTimestamp()
        db.collection("users").document(uid).update(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${Firebase.auth.currentUser?.uid}")
    }

    suspend fun logSessionAndUpdateProfile(
        subject: String,
        plannedMinutes: Int,
        completedMinutes: Int,
        ambientSound: String,
        xpEarned: Int,
        gemsEarned: Int,
        dateKey: String,
        updatedProfileFields: Map<String, Any>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val sessionRef = userRef.collection("sessions").document()

        val sessionPayload = hashMapOf(
            "userId" to uid,
            "subject" to subject.trim().take(40).ifEmpty { "General" },
            "plannedMinutes" to plannedMinutes.coerceIn(1, 360),
            "completedMinutes" to completedMinutes.coerceIn(1, 360),
            "ambientSound" to ambientSound.trim().take(40),
            "xpEarned" to xpEarned.coerceIn(0, 5000),
            "gemsEarned" to gemsEarned.coerceIn(0, 2000),
            "dateKey" to dateKey.take(20),
            "createdAt" to FieldValue.serverTimestamp()
        )

        val profilePayload = updatedProfileFields.toMutableMap()
        profilePayload["updatedAt"] = FieldValue.serverTimestamp()

        val batch = db.batch()
        batch.set(sessionRef, sessionPayload)
        batch.update(userRef, profilePayload)
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${Firebase.auth.currentUser?.uid}/sessions")
    }

    suspend fun addTask(title: String, subject: String, estimatedPomodoros: Int): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(120)
        require(cleanTitle.isNotEmpty()) { "Task title cannot be empty" }
        val payload = hashMapOf(
            "userId" to uid,
            "title" to cleanTitle,
            "subject" to subject.trim().take(40).ifEmpty { "Math" },
            "estimatedPomodoros" to estimatedPomodoros.coerceIn(1, 12),
            "completedPomodoros" to 0,
            "completed" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("tasks").document().set(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/${Firebase.auth.currentUser?.uid}/tasks")
    }

    suspend fun updateTask(taskId: String, completed: Boolean, completedPomodoros: Int): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(taskId.isNotBlank())
        db.collection("users").document(uid).collection("tasks").document(taskId)
            .update(
                mapOf(
                    "completed" to completed,
                    "completedPomodoros" to completedPomodoros.coerceIn(0, 99),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/${Firebase.auth.currentUser?.uid}/tasks/$taskId")
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(taskId.isNotBlank())
        db.collection("users").document(uid).collection("tasks").document(taskId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${Firebase.auth.currentUser?.uid}/tasks/$taskId")
    }
}
