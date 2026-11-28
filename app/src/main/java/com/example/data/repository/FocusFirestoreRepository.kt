package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountModerationRecord
import com.example.data.model.AdminIdentityPolicy
import com.example.data.model.AdminNotice
import com.example.data.model.AdminNoticeKind
import com.example.data.model.AdminUserCountsSnapshot
import com.example.data.model.AppLiveConfig
import com.example.data.model.BlockedUser
import com.example.data.model.ChallengeCodeLookup
import com.example.data.model.ChallengeParticipant
import com.example.data.model.FocusSession
import com.example.data.model.FriendLink
import com.example.data.model.FriendLiveCardState
import com.example.data.model.GlassThemeId
import com.example.data.model.GlobalLeaderboardSnapshot
import com.example.data.model.MascotBadgeId
import com.example.data.model.PomodoroTask
import com.example.data.model.PublicStudyProfile
import com.example.data.model.StreakReminderSettings
import com.example.data.model.StudyChallenge
import com.example.data.model.StudyChallengeWithProgress
import com.example.data.model.StudyChatMessage
import com.example.data.model.StudyHandleLookup
import com.example.data.model.StudyLiveStatus
import com.example.data.model.UserProfile
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

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
        val currentUser = Firebase.auth.currentUser
        val isBootstrapOwner = AdminIdentityPolicy.isVerifiedAdmin(
            currentUser?.email,
            currentUser?.isEmailVerified == true
        )
        val ref = db.collection("users").document(uid)
        val existing = ref.get().await()
        if (!existing.exists()) {
            val safeName = (currentUser?.displayName ?: seed.displayName)
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
                "role" to if (isBootstrapOwner) "admin" else "user",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
        } else if (isBootstrapOwner && !existing.getString("role").equals("admin", ignoreCase = true)) {
            ref.update(
                mapOf(
                    "role" to "admin",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
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

    // --- Social Study Circle (Consensual, PII-Free Study Stats) ---

    fun observeMyStudyProfile(): Flow<PublicStudyProfile?> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(null)
        val path = "studyProfiles/$uid"
        return db.collection("studyProfiles").document(uid)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(PublicStudyProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeFriendsLiveStates(): Flow<List<FriendLiveCardState>> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        val friendsPath = "users/$uid/friends"
        return db.collection("users").document(uid).collection("friends")
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FriendLink::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .flatMapLatest { links ->
                if (links.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val perFriendFlows: List<Flow<FriendLiveCardState>> = links.map { link ->
                        var pulseCount = 0
                        db.collection("studyProfiles").document(link.friendUid)
                            .snapshots(MetadataChanges.INCLUDE)
                            .map { snap ->
                                val liveProfile = if (snap.exists()) {
                                    snap.toObject(
                                        PublicStudyProfile::class.java,
                                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                    )
                                } else null
                                val fallback = PublicStudyProfile(
                                    userId = link.friendUid,
                                    handle = link.friendHandle,
                                    displayName = link.friendDisplayName,
                                    status = StudyLiveStatus.READY.name
                                )
                                pulseCount += 1
                                FriendLiveCardState(
                                    profile = liveProfile ?: fallback,
                                    isSyncing = snap.metadata.isFromCache || snap.metadata.hasPendingWrites(),
                                    lastSyncedAtMs = System.currentTimeMillis(),
                                    syncPulseCount = pulseCount
                                )
                            }
                            .catch { error ->
                                if (error is CancellationException) throw error
                                handleFirestoreError(error, OperationType.GET, "studyProfiles/${link.friendUid}")
                                emit(
                                    FriendLiveCardState(
                                        profile = PublicStudyProfile(
                                            userId = link.friendUid,
                                            handle = link.friendHandle,
                                            displayName = link.friendDisplayName,
                                            status = StudyLiveStatus.READY.name
                                        ),
                                        isSyncing = false,
                                        lastSyncedAtMs = System.currentTimeMillis(),
                                        syncPulseCount = pulseCount
                                    )
                                )
                            }
                    }
                    combine(perFriendFlows) { states ->
                        states.toList().sortedWith(
                            compareByDescending<FriendLiveCardState> {
                                when (it.profile.liveStatus) {
                                    StudyLiveStatus.FOCUSING -> 2
                                    StudyLiveStatus.BREAK -> 1
                                    StudyLiveStatus.READY -> 0
                                }
                            }.thenByDescending { it.profile.todayFocusMinutes }
                        )
                    }
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, friendsPath)
                emit(emptyList())
            }
    }

    /**
     * Real-time Global Study Streaks Leaderboard from Firestore `studyProfiles`.
     * Bounded to <= 50 documents to align 1:1 with `firestore.rules` (`request.query.limit <= 50`).
     * Single-field orderBy avoids custom composite index requirements; client tie-breaks by totalFocusMinutes.
     */
    fun observeGlobalStreakLeaderboard(limit: Long = 50L): Flow<GlobalLeaderboardSnapshot> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: return flowOf(GlobalLeaderboardSnapshot())
        val boundedLimit = limit.coerceIn(1L, 50L)
        val path = "studyProfiles"
        return db.collection(path)
            .orderBy("currentStreak", Query.Direction.DESCENDING)
            .limit(boundedLimit)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val ranked = snapshot.documents
                    .mapNotNull { doc ->
                        doc.toObject(
                            PublicStudyProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                    .sortedWith(
                        compareByDescending<PublicStudyProfile> { it.currentStreak }
                            .thenByDescending { it.totalFocusMinutes }
                            .thenByDescending { it.todayFocusMinutes }
                    )
                GlobalLeaderboardSnapshot(
                    profiles = ranked,
                    isSyncing = snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites(),
                    lastSyncedAtMs = System.currentTimeMillis()
                )
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, "$path (uid=$uid)")
                throw error
            }
    }

    suspend fun publishMyStudyProfile(
        displayName: String,
        status: StudyLiveStatus,
        currentSubject: String,
        todayFocusMinutes: Int,
        dailyGoalMinutes: Int,
        currentStreak: Int,
        totalFocusMinutes: Int,
        level: Int,
        equippedBadge: String,
        lastFocusDate: String
    ): Result<PublicStudyProfile> = runCatching {
        val uid = requireUserId()
        val safeName = displayName.trim().take(60).ifEmpty { "Scholar" }
        val safeSubject = currentSubject.trim().take(40).ifEmpty { "Math" }
        val safeBadge = MascotBadgeId.fromWire(equippedBadge).name
        val profileRef = db.collection("studyProfiles").document(uid)
        val existingSnap = profileRef.get().await()
        val existingHandle = existingSnap.getString("handle")?.takeIf { HANDLE_REGEX.matches(it) }
        val handle = existingHandle ?: deriveDefaultHandle(safeName, uid)

        if (!existingSnap.exists()) {
            val createPayload = hashMapOf(
                "userId" to uid,
                "handle" to handle,
                "displayName" to safeName,
                "status" to status.name,
                "currentSubject" to safeSubject,
                "todayFocusMinutes" to todayFocusMinutes.coerceIn(0, 1440),
                "dailyGoalMinutes" to dailyGoalMinutes.coerceIn(15, 720),
                "currentStreak" to currentStreak.coerceIn(0, 10000),
                "totalFocusMinutes" to totalFocusMinutes.coerceIn(0, 1000000),
                "level" to level.coerceIn(1, 1000),
                "equippedBadge" to safeBadge,
                "lastFocusDate" to lastFocusDate.take(20),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            profileRef.set(createPayload).await()
        } else {
            val updatePayload = hashMapOf<String, Any>(
                "handle" to handle,
                "displayName" to safeName,
                "status" to status.name,
                "currentSubject" to safeSubject,
                "todayFocusMinutes" to todayFocusMinutes.coerceIn(0, 1440),
                "dailyGoalMinutes" to dailyGoalMinutes.coerceIn(15, 720),
                "currentStreak" to currentStreak.coerceIn(0, 10000),
                "totalFocusMinutes" to totalFocusMinutes.coerceIn(0, 1000000),
                "level" to level.coerceIn(1, 1000),
                "equippedBadge" to safeBadge,
                "lastFocusDate" to lastFocusDate.take(20),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            profileRef.update(updatePayload).await()
        }

        // Ensure O(1) handle lookup document is registered and synced for this owner
        val handleRef = db.collection("studyHandles").document(handle)
        val handleSnap = handleRef.get().await()
        if (!handleSnap.exists()) {
            handleRef.set(
                hashMapOf(
                    "userId" to uid,
                    "handle" to handle,
                    "displayName" to safeName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else if (handleSnap.getString("userId") == uid && handleSnap.getString("displayName") != safeName) {
            handleRef.update(
                mapOf(
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        PublicStudyProfile(
            userId = uid,
            handle = handle,
            displayName = safeName,
            status = status.name,
            currentSubject = safeSubject,
            todayFocusMinutes = todayFocusMinutes.coerceIn(0, 1440),
            dailyGoalMinutes = dailyGoalMinutes.coerceIn(15, 720),
            currentStreak = currentStreak.coerceIn(0, 10000),
            totalFocusMinutes = totalFocusMinutes.coerceIn(0, 1000000),
            level = level.coerceIn(1, 1000),
            equippedBadge = safeBadge,
            lastFocusDate = lastFocusDate.take(20)
        )
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "studyProfiles/${Firebase.auth.currentUser?.uid}")
    }

    suspend fun addFriendByHandle(rawInput: String): Result<PublicStudyProfile> = runCatching {
        val uid = requireUserId()
        val cleanHandle = normalizeHandleInput(rawInput)
        require(cleanHandle.length in 3..30 && HANDLE_REGEX.matches(cleanHandle)) {
            "Enter a valid @handle (3-30 letters, numbers, or underscores)"
        }

        val handleSnap = db.collection("studyHandles").document(cleanHandle).get().await()
        if (!handleSnap.exists()) {
            throw IllegalArgumentException("No scholar found with @$cleanHandle")
        }

        val lookup = handleSnap.toObject(
            StudyHandleLookup::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: throw IllegalArgumentException("Handle @$cleanHandle could not be read")

        if (lookup.userId == uid) {
            throw IllegalArgumentException("That's your own study handle!")
        }

        val blockedSnap = db.collection("users").document(uid)
            .collection("blockedUsers").document(lookup.userId)
            .get().await()
        if (blockedSnap.exists()) {
            throw IllegalArgumentException("You blocked @$cleanHandle. Unblock them in Safety Shield first.")
        }

        val friendProfileSnap = db.collection("studyProfiles").document(lookup.userId).get().await()
        val friendProfile = friendProfileSnap.toObject(
            PublicStudyProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: throw IllegalArgumentException("Friend's study card is not active yet")

        val friendLinkPayload = hashMapOf(
            "userId" to uid,
            "friendUid" to friendProfile.userId,
            "friendHandle" to friendProfile.handle,
            "friendDisplayName" to friendProfile.displayName.trim().take(60).ifEmpty { "Scholar" },
            "addedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
            .collection("friends").document(friendProfile.userId)
            .set(friendLinkPayload)
            .await()

        friendProfile
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${Firebase.auth.currentUser?.uid}/friends")
    }

    suspend fun removeFriend(friendUid: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(friendUid.isNotBlank())
        db.collection("users").document(uid)
            .collection("friends").document(friendUid)
            .delete()
            .await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${Firebase.auth.currentUser?.uid}/friends/$friendUid")
    }

    /**
     * Updates the signed-in scholar's unique @handle cleanly:
     * 1. Validates format (`^[a-z0-9_]{3,30}$`)
     * 2. Verifies availability in `studyHandles/{newHandle}`
     * 3. Claims `studyHandles/{newHandle}` and updates `studyProfiles/{uid}.handle`
     * 4. Releases the old `studyHandles/{oldHandle}` owned by this user
     */
    suspend fun updateStudyHandle(
        rawHandle: String,
        displayName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val newHandle = normalizeHandleInput(rawHandle)
        require(newHandle.length in 3..30 && HANDLE_REGEX.matches(newHandle)) {
            "Handle must be 3–30 lowercase letters, numbers, or underscores."
        }
        val safeName = displayName.trim().take(60).ifEmpty { "Scholar" }

        val profileRef = db.collection("studyProfiles").document(uid)
        val profileSnap = profileRef.get().await()
        val oldHandle = profileSnap.getString("handle")?.takeIf { HANDLE_REGEX.matches(it) }

        if (oldHandle == newHandle) {
            return@runCatching newHandle
        }

        val newHandleRef = db.collection("studyHandles").document(newHandle)
        val existingHandleSnap = newHandleRef.get().await()
        if (existingHandleSnap.exists() && existingHandleSnap.getString("userId") != uid) {
            throw IllegalArgumentException("@$newHandle is already taken. Try a different handle!")
        }

        if (!existingHandleSnap.exists()) {
            newHandleRef.set(
                hashMapOf(
                    "userId" to uid,
                    "handle" to newHandle,
                    "displayName" to safeName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            newHandleRef.update(
                mapOf(
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        if (profileSnap.exists()) {
            profileRef.update(
                mapOf(
                    "handle" to newHandle,
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            profileRef.set(
                hashMapOf(
                    "userId" to uid,
                    "handle" to newHandle,
                    "displayName" to safeName,
                    "status" to StudyLiveStatus.READY.name,
                    "currentSubject" to "Math",
                    "todayFocusMinutes" to 0,
                    "dailyGoalMinutes" to 90,
                    "currentStreak" to 1,
                    "totalFocusMinutes" to 0,
                    "level" to 1,
                    "equippedBadge" to MascotBadgeId.CLASSIC_MONKEY.name,
                    "lastFocusDate" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        if (!oldHandle.isNullOrBlank() && oldHandle != newHandle) {
            runCatching {
                val oldRef = db.collection("studyHandles").document(oldHandle)
                val oldSnap = oldRef.get().await()
                if (oldSnap.exists() && oldSnap.getString("userId") == uid) {
                    oldRef.delete().await()
                }
            }
        }

        newHandle
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "studyHandles/${normalizeHandleInput(rawHandle)}")
    }

    // --- Real-Time 1:1 Study Circle Direct Messaging ---

    fun observeDirectMessages(friendUid: String): Flow<List<StudyChatMessage>> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: return flowOf(emptyList())
        if (friendUid.isBlank() || friendUid == uid) return flowOf(emptyList())
        val chatId = buildDeterministicChatId(uid, friendUid)
        val path = "studyChats/$chatId/messages"
        return db.collection("studyChats")
            .document(chatId)
            .collection("messages")
            .whereArrayContains("participantUids", uid)
            .limit(100)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        StudyChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendDirectMessage(
        friendUid: String,
        senderDisplayName: String,
        rawText: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(friendUid.isNotBlank() && friendUid != uid) {
            "Choose a valid study friend to message."
        }
        val cleanText = rawText.trim().take(500)
        require(cleanText.isNotEmpty()) { "Message cannot be empty." }
        val safeSenderName = senderDisplayName.trim().take(60).ifEmpty { "Scholar" }
        val participants = listOf(uid, friendUid).sorted()
        val chatId = "${participants[0]}_${participants[1]}"

        val chatRef = db.collection("studyChats").document(chatId)
        val chatSnap = chatRef.get().await()
        if (chatSnap.exists()) {
            chatRef.update(
                mapOf(
                    "lastMessageText" to cleanText,
                    "lastSenderUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            chatRef.set(
                hashMapOf(
                    "participantUids" to participants,
                    "lastMessageText" to cleanText,
                    "lastSenderUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val messagePayload = hashMapOf(
            "chatId" to chatId,
            "participantUids" to participants,
            "senderUid" to uid,
            "senderDisplayName" to safeSenderName,
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp()
        )
        chatRef.collection("messages").add(messagePayload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(
            e,
            OperationType.WRITE,
            "studyChats/${buildDeterministicChatId(Firebase.auth.currentUser?.uid.orEmpty(), friendUid)}/messages"
        )
    }

    private fun buildDeterministicChatId(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")

    // --- Social Safety: Owner-Scoped Blocked Scholars ---

    fun observeBlockedUsers(): Flow<List<BlockedUser>> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        val path = "users/$uid/blockedUsers"
        return db.collection("users").document(uid)
            .collection("blockedUsers")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun blockUser(
        targetUid: String,
        targetHandle: String,
        targetDisplayName: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(targetUid.isNotBlank() && targetUid != uid) { "Cannot block this account" }
        val safeHandle = targetHandle.trim().lowercase(Locale.US).take(30)
        val safeName = targetDisplayName.trim().take(60).ifEmpty { "Scholar" }

        val blockPayload = hashMapOf(
            "userId" to uid,
            "blockedUid" to targetUid,
            "blockedHandle" to safeHandle,
            "blockedDisplayName" to safeName,
            "blockedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
            .collection("blockedUsers").document(targetUid)
            .set(blockPayload)
            .await()

        // Also remove from Study Circle friends if currently added
        runCatching {
            db.collection("users").document(uid)
                .collection("friends").document(targetUid)
                .delete()
                .await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${Firebase.auth.currentUser?.uid}/blockedUsers/$targetUid")
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(targetUid.isNotBlank())
        db.collection("users").document(uid)
            .collection("blockedUsers").document(targetUid)
            .delete()
            .await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${Firebase.auth.currentUser?.uid}/blockedUsers/$targetUid")
    }

    // --- Group Study Challenges (Shared Focus Goal & Live Aggregated Progress) ---

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeJoinedChallengesWithProgress(): Flow<List<StudyChallengeWithProgress>> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        val path = "studyChallenges"
        return db.collection(path)
            .whereArrayContains("memberUids", uid)
            .limit(30)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val isSyncingParent = snapshot.metadata.isFromCache || snapshot.metadata.hasPendingWrites()
                val challenges = snapshot.documents
                    .mapNotNull { doc ->
                        doc.toObject(StudyChallenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                    .sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                challenges to isSyncingParent
            }
            .flatMapLatest { (challenges, parentSyncing) ->
                if (challenges.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val perChallengeFlows = challenges.map { challenge ->
                        db.collection(path).document(challenge.id)
                            .collection("participants")
                            .limit(30)
                            .snapshots(MetadataChanges.INCLUDE)
                            .map { partSnap ->
                                val participants = partSnap.documents
                                    .mapNotNull { doc ->
                                        doc.toObject(
                                            ChallengeParticipant::class.java,
                                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                        )
                                    }
                                    .sortedWith(
                                        compareByDescending<ChallengeParticipant> { it.contributedMinutes }
                                            .thenByDescending { it.sessionsCompleted }
                                            .thenBy { it.displayName.lowercase(Locale.US) }
                                    )
                                StudyChallengeWithProgress(
                                    challenge = challenge,
                                    participants = participants,
                                    isSyncing = parentSyncing || partSnap.metadata.isFromCache || partSnap.metadata.hasPendingWrites()
                                )
                            }
                            .catch { err ->
                                if (err is CancellationException) throw err
                                handleFirestoreError(err, OperationType.LIST, "$path/${challenge.id}/participants")
                                emit(
                                    StudyChallengeWithProgress(
                                        challenge = challenge,
                                        participants = emptyList(),
                                        isSyncing = false
                                    )
                                )
                            }
                    }
                    combine(perChallengeFlows) { array -> array.toList() }
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observePendingChallengeInvites(): Flow<List<StudyChallenge>> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        val path = "studyChallenges"
        return db.collection(path)
            .whereArrayContains("invitedUids", uid)
            .limit(20)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { doc ->
                        doc.toObject(StudyChallenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                    .filter { uid !in it.memberUids }
                    .sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, "$path (invites)")
                emit(emptyList())
            }
    }

    suspend fun createStudyChallenge(
        title: String,
        subject: String,
        targetMinutes: Int,
        creatorDisplayName: String,
        creatorHandle: String,
        invitedFriendUids: List<String>
    ): Result<StudyChallenge> = runCatching {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(80)
        require(cleanTitle.length >= 2) { "Enter a challenge title (at least 2 characters)" }
        val cleanSubject = subject.trim().take(40).ifEmpty { "All Subjects" }
        val clampedTarget = targetMinutes.coerceIn(30, 10000)
        val safeCreatorName = creatorDisplayName.trim().take(60).ifEmpty { "Scholar" }
        val safeHandle = creatorHandle.trim().lowercase(Locale.US).take(30)
        val cleanInvites = invitedFriendUids
            .map { it.trim() }
            .filter { it.isNotBlank() && it != uid }
            .distinct()
            .take(19)

        val challengeRef = db.collection("studyChallenges").document()
        val challengeId = challengeRef.id
        val inviteCode = generateUniqueChallengeCode()

        // 1. Create parent StudyChallenge document FIRST so participant rules see uid in memberUids
        val challengePayload = hashMapOf(
            "creatorUid" to uid,
            "creatorName" to safeCreatorName,
            "title" to cleanTitle,
            "subject" to cleanSubject,
            "targetMinutes" to clampedTarget,
            "inviteCode" to inviteCode,
            "memberUids" to listOf(uid),
            "invitedUids" to cleanInvites,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        challengeRef.set(challengePayload).await()

        // 2. Create O(1) invite code lookup
        val codePayload = hashMapOf(
            "code" to inviteCode,
            "challengeId" to challengeId,
            "creatorUid" to uid,
            "title" to cleanTitle,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("challengeCodes").document(inviteCode).set(codePayload).await()

        // 3. Create creator's initial participant progress document
        val participantPayload = hashMapOf(
            "challengeId" to challengeId,
            "userId" to uid,
            "displayName" to safeCreatorName,
            "handle" to safeHandle,
            "contributedMinutes" to 0,
            "sessionsCompleted" to 0,
            "joinedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        challengeRef.collection("participants").document(uid).set(participantPayload).await()

        StudyChallenge(
            id = challengeId,
            creatorUid = uid,
            creatorName = safeCreatorName,
            title = cleanTitle,
            subject = cleanSubject,
            targetMinutes = clampedTarget,
            inviteCode = inviteCode,
            memberUids = listOf(uid),
            invitedUids = cleanInvites
        )
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "studyChallenges")
    }

    suspend fun joinChallengeByCode(
        rawCode: String,
        displayName: String,
        handle: String
    ): Result<StudyChallenge> = runCatching {
        val cleanCode = rawCode.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "")
        require(CHALLENGE_CODE_REGEX.matches(cleanCode)) {
            "Enter a valid 6-character challenge code (e.g., TRY48X)"
        }
        val codeSnap = db.collection("challengeCodes").document(cleanCode).get().await()
        if (!codeSnap.exists()) {
            throw IllegalArgumentException("No Study Challenge found for code $cleanCode")
        }
        val lookup = codeSnap.toObject(
            ChallengeCodeLookup::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: throw IllegalArgumentException("Could not read invite code $cleanCode")

        joinChallengeInternal(lookup.challengeId, displayName, handle)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "challengeCodes")
    }

    suspend fun acceptChallengeInvite(
        challengeId: String,
        displayName: String,
        handle: String
    ): Result<StudyChallenge> = runCatching {
        joinChallengeInternal(challengeId, displayName, handle)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "studyChallenges/$challengeId")
    }

    suspend fun declineChallengeInvite(challengeId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(challengeId.isNotBlank()) { "Invalid challenge ID" }
        val ref = db.collection("studyChallenges").document(challengeId)
        val snap = ref.get().await()
        if (!snap.exists()) return@runCatching Unit
        val challenge = snap.toObject(
            StudyChallenge::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: return@runCatching Unit
        if (uid in challenge.invitedUids && uid !in challenge.memberUids) {
            val nextInvites = challenge.invitedUids.filterNot { it == uid }
            ref.update(
                mapOf(
                    "invitedUids" to nextInvites,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "studyChallenges/$challengeId (decline)")
    }

    private suspend fun joinChallengeInternal(
        challengeId: String,
        displayName: String,
        handle: String
    ): StudyChallenge {
        val uid = requireUserId()
        require(challengeId.isNotBlank()) { "Invalid challenge ID" }
        val challengeRef = db.collection("studyChallenges").document(challengeId)
        val snap = challengeRef.get().await()
        if (!snap.exists()) {
            throw IllegalArgumentException("This Study Challenge no longer exists")
        }
        val challenge = snap.toObject(
            StudyChallenge::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: throw IllegalArgumentException("Unable to open Study Challenge")

        val safeName = displayName.trim().take(60).ifEmpty { "Scholar" }
        val safeHandle = handle.trim().lowercase(Locale.US).take(30)

        if (uid !in challenge.memberUids) {
            if (challenge.memberUids.size >= 20) {
                throw IllegalStateException("This challenge already has the maximum 20 participants")
            }
            val nextMembers = (challenge.memberUids + uid).distinct()
            val nextInvites = challenge.invitedUids.filterNot { it == uid }
            challengeRef.update(
                mapOf(
                    "memberUids" to nextMembers,
                    "invitedUids" to nextInvites,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val participantRef = challengeRef.collection("participants").document(uid)
        val existingPart = participantRef.get().await()
        if (!existingPart.exists()) {
            participantRef.set(
                hashMapOf(
                    "challengeId" to challengeId,
                    "userId" to uid,
                    "displayName" to safeName,
                    "handle" to safeHandle,
                    "contributedMinutes" to 0,
                    "sessionsCompleted" to 0,
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return challenge.copy(
            memberUids = (challenge.memberUids + uid).distinct(),
            invitedUids = challenge.invitedUids.filterNot { it == uid }
        )
    }

    suspend fun inviteFriendsToChallenge(
        challengeId: String,
        friendUids: List<String>
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("studyChallenges").document(challengeId)
        val snap = ref.get().await()
        val challenge = snap.toObject(
            StudyChallenge::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: throw IllegalArgumentException("Challenge not found")
        require(uid in challenge.memberUids) { "Only members can invite friends" }

        val toAdd = friendUids
            .map { it.trim() }
            .filter { it.isNotBlank() && it !in challenge.memberUids && it !in challenge.invitedUids }
            .distinct()
        if (toAdd.isEmpty()) return@runCatching 0

        val nextInvites = (challenge.invitedUids + toAdd).distinct().take(20)
        ref.update(
            mapOf(
                "invitedUids" to nextInvites,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        toAdd.size
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "studyChallenges/$challengeId")
    }

    suspend fun leaveOrDeleteChallenge(challenge: StudyChallenge): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("studyChallenges").document(challenge.id)
        if (challenge.creatorUid == uid) {
            runCatching { ref.collection("participants").document(uid).delete().await() }
            if (challenge.inviteCode.isNotBlank()) {
                runCatching { db.collection("challengeCodes").document(challenge.inviteCode).delete().await() }
            }
            ref.delete().await()
            true // deleted
        } else {
            runCatching { ref.collection("participants").document(uid).delete().await() }
            val remaining = challenge.memberUids.filterNot { it == uid }
            if (remaining.isNotEmpty()) {
                ref.update(
                    mapOf(
                        "memberUids" to remaining,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            false // left
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "studyChallenges/${challenge.id}")
    }

    /**
     * Automatically credits completed focus minutes to all active Study Challenges
     * the user has joined (matching either "All Subjects" or the completed session's subject).
     */
    suspend fun contributeFocusToActiveChallenges(
        completedMinutes: Int,
        subject: String,
        displayName: String,
        handle: String
    ): Result<Int> = runCatching {
        if (completedMinutes <= 0) return@runCatching 0
        val uid = requireUserId()
        val safeName = displayName.trim().take(60).ifEmpty { "Scholar" }
        val safeHandle = handle.trim().lowercase(Locale.US).take(30)

        val joinedSnap = db.collection("studyChallenges")
            .whereArrayContains("memberUids", uid)
            .limit(30)
            .get()
            .await()

        var updatedCount = 0
        for (doc in joinedSnap.documents) {
            val challenge = doc.toObject(
                StudyChallenge::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: continue
            val matchesSubject = challenge.subject.equals("All Subjects", ignoreCase = true) ||
                challenge.subject.equals(subject, ignoreCase = true)
            if (!matchesSubject) continue

            val partRef = doc.reference.collection("participants").document(uid)
            val partSnap = partRef.get().await()
            if (partSnap.exists()) {
                partRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "handle" to safeHandle,
                        "contributedMinutes" to FieldValue.increment(completedMinutes.toLong()),
                        "sessionsCompleted" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                partRef.set(
                    hashMapOf(
                        "challengeId" to challenge.id,
                        "userId" to uid,
                        "displayName" to safeName,
                        "handle" to safeHandle,
                        "contributedMinutes" to completedMinutes.coerceIn(0, 100000),
                        "sessionsCompleted" to 1,
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            updatedCount += 1
        }
        updatedCount
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "studyChallenges/participants")
    }

    // --- Firebase Cloud Messaging Daily Streak Reminder Settings ---

    fun observeStreakReminderSettings(): Flow<StreakReminderSettings?> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(null)
        val path = "users/$uid/notificationSettings/streakReminder"
        return db.collection("users")
            .document(uid)
            .collection("notificationSettings")
            .document(STREAK_REMINDER_DOC_ID)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    StreakReminderSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun upsertStreakReminderSettings(
        enabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        timezoneId: String,
        fcmToken: String,
        fcmTopic: String,
        lastFocusDate: String,
        currentStreak: Int
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("users")
            .document(uid)
            .collection("notificationSettings")
            .document(STREAK_REMINDER_DOC_ID)

        val safeHour = reminderHour.coerceIn(0, 23)
        val safeMinute = reminderMinute.coerceIn(0, 59)
        val safeTimezone = timezoneId.trim().take(64).ifEmpty { "UTC" }
        val safeToken = fcmToken.trim().take(512)
        val safeTopic = fcmTopic.trim().take(80)
        val safeLastFocus = lastFocusDate.trim().take(20)
        val safeStreak = currentStreak.coerceIn(0, 10000)

        val existingSnap = ref.get().await()
        if (existingSnap.exists()) {
            ref.update(
                mapOf(
                    "enabled" to enabled,
                    "reminderHour" to safeHour,
                    "reminderMinute" to safeMinute,
                    "timezoneId" to safeTimezone,
                    "fcmToken" to safeToken,
                    "fcmTopic" to safeTopic,
                    "lastFocusDate" to safeLastFocus,
                    "currentStreak" to safeStreak,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.set(
                hashMapOf(
                    "userId" to uid,
                    "enabled" to enabled,
                    "reminderHour" to safeHour,
                    "reminderMinute" to safeMinute,
                    "timezoneId" to safeTimezone,
                    "fcmToken" to safeToken,
                    "fcmTopic" to safeTopic,
                    "lastFocusDate" to safeLastFocus,
                    "currentStreak" to safeStreak,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(
            e,
            OperationType.WRITE,
            "users/${Firebase.auth.currentUser?.uid}/notificationSettings/$STREAK_REMINDER_DOC_ID"
        )
    }

    private suspend fun generateUniqueChallengeCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(6) {
            val candidate = buildString(6) {
                repeat(6) { append(alphabet.random()) }
            }
            val exists = runCatching {
                db.collection("challengeCodes").document(candidate).get().await().exists()
            }.getOrDefault(false)
            if (!exists) return candidate
        }
        return buildString(6) { repeat(6) { append(alphabet.random()) } }
    }

    // --- Silent Live Background App Updates & Firestore 'admin' Role Operations (Zero Personal User Data) ---

    private suspend fun requireFirestoreAdminRole() {
        val uid = requireUserId()
        val user = Firebase.auth.currentUser
        if (AdminIdentityPolicy.isVerifiedAdmin(user?.email, user?.isEmailVerified == true)) {
            return
        }
        val snap = db.collection("users").document(uid).get().await()
        val role = snap.getString("role")
        if (!role.equals("admin", ignoreCase = true)) {
            throw SecurityException("Firestore 'admin' role required")
        }
    }

    /**
     * Streams live remote app config silently in the background whenever an admin pushes a change.
     */
    fun observeLiveAppConfig(): Flow<AppLiveConfig?> {
        if (Firebase.auth.currentUser == null) return flowOf(null)
        return db.collection("appConfig")
            .document(LIVE_CONFIG_DOC_ID)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    AppLiveConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(null)
            }
    }

    suspend fun publishLiveAppConfig(config: AppLiveConfig): Result<AppLiveConfig> = runCatching {
        requireFirestoreAdminRole()
        val nextVersion = (config.configVersion.coerceAtLeast(0) + 1).coerceIn(1, 1_000_000)
        val sanitized = config.copy(
            configVersion = nextVersion,
            liveTagline = config.liveTagline.trim().take(100),
            announcementTitle = config.announcementTitle.trim().take(80),
            announcementBody = config.announcementBody.trim().take(240),
            updateNoticeLabel = config.updateNoticeLabel.trim().take(60),
            defaultFocusMinutes = config.defaultFocusMinutes.coerceIn(5, 120),
            gemMultiplierPercent = config.gemMultiplierPercent.coerceIn(100, 300)
        )
        val payload = hashMapOf(
            "configVersion" to sanitized.configVersion,
            "liveTagline" to sanitized.liveTagline,
            "announcementTitle" to sanitized.announcementTitle,
            "announcementBody" to sanitized.announcementBody,
            "announcementActive" to sanitized.announcementActive,
            "updateNoticeLabel" to sanitized.updateNoticeLabel,
            "defaultFocusMinutes" to sanitized.defaultFocusMinutes,
            "gemMultiplierPercent" to sanitized.gemMultiplierPercent,
            "socialEnabled" to sanitized.socialEnabled,
            "launcherEnabled" to sanitized.launcherEnabled,
            "ambientDrawerEnabled" to sanitized.ambientDrawerEnabled,
            "maintenanceBannerEnabled" to sanitized.maintenanceBannerEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("appConfig").document(LIVE_CONFIG_DOC_ID).set(payload).await()
        sanitized
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "appConfig/$LIVE_CONFIG_DOC_ID")
    }

    /**
     * Streams official Admin broadcasts and direct notices silently in real time.
     */
    fun observeAdminNotices(isAdmin: Boolean): Flow<List<AdminNotice>> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(emptyList())
        val query: Query = if (isAdmin) {
            db.collection("adminNotices").limit(30)
        } else {
            db.collection("adminNotices")
                .whereArrayContainsAny("audienceUids", listOf("ALL", uid))
                .limit(20)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        AdminNotice::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
    }

    suspend fun sendAdminNotice(
        targetUid: String?,
        targetLabel: String,
        title: String,
        body: String,
        kind: AdminNoticeKind
    ): Result<Unit> = runCatching {
        requireFirestoreAdminRole()
        val cleanTitle = title.trim().take(80)
        val cleanBody = body.trim().take(400)
        require(cleanTitle.isNotEmpty()) { "Enter a notice title." }
        require(cleanBody.isNotEmpty()) { "Enter a message body." }
        val audience = if (targetUid.isNullOrBlank()) listOf("ALL") else listOf(targetUid.trim().take(128))
        val safeTargetLabel = targetLabel.trim().take(60).ifEmpty {
            if (targetUid.isNullOrBlank()) "All Scholars" else "Direct Scholar"
        }
        val ref = db.collection("adminNotices").document()
        ref.set(
            hashMapOf(
                "audienceUids" to audience,
                "targetLabel" to safeTargetLabel,
                "title" to cleanTitle,
                "body" to cleanBody,
                "kind" to kind.name,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "adminNotices")
    }

    suspend fun deleteAdminNotice(noticeId: String): Result<Unit> = runCatching {
        requireFirestoreAdminRole()
        require(noticeId.isNotBlank()) { "Invalid notice ID" }
        db.collection("adminNotices").document(noticeId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "adminNotices/$noticeId")
    }

    /**
     * Admin-only public study directory stream (strictly PII-free: reads ONLY /studyProfiles,
     * never touches /users/{uid} private data, emails, tasks, sessions, or private chats).
     */
    fun observePublicDirectoryForAdmin(): Flow<List<PublicStudyProfile>> {
        if (Firebase.auth.currentUser == null) return flowOf(emptyList())
        return db.collection("studyProfiles")
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        PublicStudyProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.sortedByDescending { it.totalFocusMinutes }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
    }

    /**
     * Fetches exact server-side Firestore Aggregation Counts (zero PII) for the Admin Dashboard.
     */
    suspend fun fetchAdminUserCounts(
        todayDateKey: String,
        fallbackDirectory: List<PublicStudyProfile>
    ): Result<AdminUserCountsSnapshot> = runCatching {
        requireFirestoreAdminRole()
        val profilesCol = db.collection("studyProfiles")
        val totalUsers = runCatching {
            profilesCol.count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrElse { fallbackDirectory.size }

        val focusingNow = runCatching {
            profilesCol.whereEqualTo("status", StudyLiveStatus.FOCUSING.name)
                .count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrElse { fallbackDirectory.count { it.liveStatus == StudyLiveStatus.FOCUSING } }

        val onBreakNow = runCatching {
            profilesCol.whereEqualTo("status", StudyLiveStatus.BREAK.name)
                .count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrElse { fallbackDirectory.count { it.liveStatus == StudyLiveStatus.BREAK } }

        val activeToday = runCatching {
            profilesCol.whereEqualTo("lastFocusDate", todayDateKey)
                .count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrElse { fallbackDirectory.count { it.lastFocusDate == todayDateKey || it.todayFocusMinutes > 0 } }

        val onStreakCount = runCatching {
            profilesCol.whereGreaterThan("currentStreak", 0)
                .count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrElse { fallbackDirectory.count { it.currentStreak > 0 } }

        val removedAccounts = runCatching {
            db.collection("accountModeration")
                .whereEqualTo("status", "REMOVED")
                .count().get(AggregateSource.SERVER).await().count.toInt()
        }.getOrDefault(0)

        val readyNow = (totalUsers - focusingNow - onBreakNow).coerceAtLeast(0)
        val totalFocusMinutes = fallbackDirectory.sumOf { it.totalFocusMinutes.toLong().coerceAtLeast(0L) }

        AdminUserCountsSnapshot(
            totalUsers = totalUsers,
            focusingNow = focusingNow,
            onBreakNow = onBreakNow,
            readyNow = readyNow,
            activeToday = activeToday,
            onStreakCount = onStreakCount,
            removedAccounts = removedAccounts,
            totalFocusMinutesAcrossDirectory = totalFocusMinutes,
            isRefreshing = false,
            lastUpdatedMs = System.currentTimeMillis()
        )
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.LIST, "studyProfiles (count)")
    }

    /**
     * Deletes a user's public study account (/studyProfiles/{uid} & /studyHandles/{handle})
     * and records a non-PII moderation notice in /accountModeration/{uid} so the client updates silently.
     * Never reads or accesses private /users/{uid} personal data.
     */
    suspend fun adminDeleteUserAccount(
        targetUid: String,
        targetHandle: String,
        reason: String
    ): Result<Unit> = runCatching {
        requireFirestoreAdminRole()
        val cleanUid = targetUid.trim()
        require(cleanUid.isNotEmpty()) { "Invalid account ID" }
        require(cleanUid != Firebase.auth.currentUser?.uid) { "You cannot delete the active Admin account." }
        val cleanReason = reason.trim().take(160).ifEmpty { "Account removed by administrator." }

        runCatching {
            db.collection("studyProfiles").document(cleanUid).delete().await()
        }
        val cleanHandle = targetHandle.trim().lowercase(Locale.US)
        if (HANDLE_REGEX.matches(cleanHandle)) {
            runCatching {
                db.collection("studyHandles").document(cleanHandle).delete().await()
            }
        }
        db.collection("accountModeration").document(cleanUid).set(
            hashMapOf(
                "userId" to cleanUid,
                "status" to "REMOVED",
                "reason" to cleanReason,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "studyProfiles/$targetUid")
    }

    fun observeMyAccountModeration(): Flow<AccountModerationRecord?> {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return flowOf(null)
        return db.collection("accountModeration")
            .document(uid)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    AccountModerationRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(null)
            }
    }

    companion object {
        const val STREAK_REMINDER_DOC_ID = "streakReminder"
        const val LIVE_CONFIG_DOC_ID = "live"
        private val HANDLE_REGEX = Regex("^[a-z0-9_]{3,30}$")
        private val MENTION_REGEX = Regex("@([a-zA-Z0-9_]{3,30})")
        private val CHALLENGE_CODE_REGEX = Regex("^[A-Z0-9]{6}$")

        fun deriveDefaultHandle(displayName: String, uid: String): String {
            val baseName = displayName.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "")
                .take(14)
                .ifEmpty { "scholar" }
            val uidSuffix = uid.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "")
                .take(6)
                .ifEmpty { "101" }
            val combined = "${baseName}_$uidSuffix".take(30)
            return if (HANDLE_REGEX.matches(combined)) combined else "scholar_$uidSuffix".take(30)
        }

        fun normalizeHandleInput(raw: String): String {
            val trimmed = raw.trim()
            val mentionMatch = MENTION_REGEX.find(trimmed)
            val candidate = mentionMatch?.groupValues?.getOrNull(1) ?: trimmed.removePrefix("@")
            return candidate.trim().lowercase(Locale.US)
        }
    }
}
