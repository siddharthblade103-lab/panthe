package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class GlassThemeId(
    val displayName: String,
    val tagline: String,
    val gemCost: Int,
    val isDark: Boolean
) {
    DUO_EMERALD("Duo Emerald", "Crisp sunlit green & frosted glass", 0, false),
    LIQUID_CRYSTAL("Liquid Crystal", "High-contrast quartz & refractive crystal glass", 0, false),
    GLASS_DESIGN("Glass Design", "Pure frosted translucent glassmorphism", 0, false),
    GALAXY_UI("Galaxy UI", "Luminous One UI cobalt & starlit indigo glass", 0, true),
    UNIVERSE_COSMOS("Universe", "Deep space nebula, stardust gold & violet glass", 0, true),
    EARTH_TERRA("Earth", "Oceanic azure, emerald coast & living sky glass", 0, false),
    NATURE_CANOPY("Nature", "Botanical sage, leaf emerald & morning mist glass", 0, false),
    SAPPHIRE_SKY("Sapphire Glass", "Cool sky sapphire & crystalline mist", 0, false),
    GOLDEN_AMBER("Sunburst Gold", "Warm amber light & honey glass", 60, false),
    CORAL_SUNSET("Coral Prism", "Sunlit peach & rose quartz glass", 75, false),
    AURORA_BOREALIS("Aurora Prism", "Northern mint & violet dark glass", 85, true),
    MIDNIGHT_FOREST("Midnight Pine", "Deep obsidian glass & emerald glow", 95, true),
    AMETHYST_DUSK("Amethyst Dusk", "Velvet indigo & crystal lavender", 105, true),
    SAKURA_QUARTZ("Rose Quartz", "Soft sakura pearl & coral glass", 115, false),
    OBSIDIAN_PRISM("Obsidian Ice", "Sleek carbon glass & glacial cyan", 130, true);

    companion object {
        fun fromWire(value: String?): GlassThemeId =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DUO_EMERALD
    }
}

enum class CompletionSoundPreset(
    val displayName: String,
    val subtitle: String
) {
    CRYSTAL_CHIME("Crystal Chime", "Bright cascading glass arpeggio"),
    VICTORY_FANFARE("Victory Fanfare", "Triumphant major celebration chord"),
    ZEN_GONG("Zen Temple Bell", "Warm resonant bronze bell decay"),
    HARP_BLOOM("Harp Bloom", "Gentle sparkling acoustic harp gliss"),
    SOFT_BELL("Soft Marimba", "Calm rounded wood-bell double tap");

    companion object {
        fun fromWire(value: String?): CompletionSoundPreset =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CRYSTAL_CHIME
    }
}

enum class PlayPointsTier(
    val displayName: String,
    val minPoints: Int,
    val gemBonus: Int,
    val claimKey: String,
    val perkLabel: String
) {
    BRONZE("Bronze", 0, 0, "tier_bronze", "Starter study rewards active"),
    SILVER("Silver", 150, 35, "tier_silver", "+35 Bonus Gems & Silver crest"),
    GOLD("Gold", 450, 75, "tier_gold", "+75 Bonus Gems & Gold crest"),
    CRYSTAL("Crystal", 900, 120, "tier_crystal", "+120 Bonus Gems & Prism badge"),
    DIAMOND("Diamond", 1600, 200, "tier_diamond", "+200 Bonus Gems & Elite glow"),
    PRISM_LEGEND("Prism Legend", 2800, 350, "tier_prism_legend", "+350 Bonus Gems & Legend rank");

    fun nextTier(): PlayPointsTier? {
        val idx = entries.indexOf(this)
        return entries.getOrNull(idx + 1)
    }

    companion object {
        fun forPoints(points: Int): PlayPointsTier =
            entries.lastOrNull { points >= it.minPoints } ?: BRONZE
    }
}

enum class MascotBadgeId(
    val displayName: String,
    val subtitle: String,
    val gemCost: Int
) {
    CLASSIC_MONKEY("Leaf Explorer", "Fresh sprout focus aura", 0),
    SCHOLAR_OWL("Honor Scholar", "Sapphire shimmer timer halo", 0),
    ROYAL_CROWN("Streak Sovereign", "Golden crown prism ring", 90),
    COSMIC_AURA("Metamorphosis", "Iridescent liquid-glass aurora", 140);

    companion object {
        fun fromWire(value: String?): MascotBadgeId =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CLASSIC_MONKEY
    }
}

enum class AmbientSoundPreset(
    val displayName: String,
    val subtitle: String,
    val gemCost: Int
) {
    OFF("Silent", "Pure quiet focus", 0),
    RAIN("Rainfall", "Gentle raindrops on frosted glass", 0),
    CAFE_NOISE("Cafe Noise", "Warm coffeehouse murmur & soft clinks", 0),
    WHITE_NOISE("White Noise", "Clean velvet noise blanket", 0),
    FOREST_WIND("Forest Breeze", "Rustling canopy & distant songbirds", 0),
    OCEAN_WAVES("Ocean Tide", "Slow rhythmic coastal surf", 0),
    BROWN_NOISE("Deep Brown", "Warm low-frequency study rumble", 65),
    ALPHA_WAVES("Alpha 10Hz", "Binaural calm concentration pulse", 90),
    FIREPLACE("Cozy Hearth", "Warm glowing embers & soft crackle", 110);

    companion object {
        val unlockableEntries: List<AmbientSoundPreset> = entries.filter { it != OFF }

        fun fromWire(value: String?): AmbientSoundPreset =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OFF
    }
}

enum class PomodoroPhase(
    val displayName: String,
    val defaultMinutes: Int,
    val minMinutes: Int,
    val maxMinutes: Int
) {
    FOCUS("Focus", 25, 1, 120),
    SHORT_BREAK("Short Break", 5, 1, 30),
    LONG_BREAK("Long Break", 15, 5, 60)
}

enum class StudyGoalPeriod(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly");

    companion object {
        fun fromWire(value: String?): StudyGoalPeriod =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DAILY
    }
}

enum class CustomAppIconId(
    val displayName: String,
    val subtitle: String
) {
    MONKEY_CLASSIC("Mascot", "Try Again monkey"),
    CRYSTAL_PRISM("Prism", "Liquid glass gem"),
    EMERALD_LEAF("Sprout", "Emerald focus leaf"),
    SOLAR_CROWN("Crown", "Golden streak crest"),
    MIDNIGHT_OWL("Scholar", "Sapphire wisdom orb"),
    SAKURA_FLAME("Flame", "Radiant streak spark");

    companion object {
        fun fromWire(value: String?): CustomAppIconId =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONKEY_CLASSIC
    }
}

enum class AppIconShapeId(val displayName: String) {
    SQUIRCLE("Squircle"),
    CIRCLE("Circle"),
    PRISM("Diamond");

    companion object {
        fun fromWire(value: String?): AppIconShapeId =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SQUIRCLE
    }
}

enum class AmbientLightingStyle(
    val displayName: String,
    val subtitle: String
) {
    AURORA_GLASS("Aurora Glass", "Living emerald & prism mist"),
    GALAXY_NEBULA("Galaxy Nebula", "Deep cosmic sapphire & indigo glow"),
    SUNSET_AMBER("Solar Halo", "Warm golden peach & coral wash"),
    MIDNIGHT_PULSE("Cyber Pulse", "Electric cyan & violet edge aura"),
    CALM_MINIMAL("Pure Calm", "Ultra-quiet frosted studio light");

    companion object {
        fun fromWire(value: String?): AmbientLightingStyle =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AURORA_GLASS
    }
}

enum class HapticIntensityLevel(
    val displayName: String,
    val scale: Float
) {
    OFF("Off", 0f),
    SOFT("Soft", 0.55f),
    BALANCED("Crisp", 1.0f),
    STRONG("Galaxy Strong", 1.45f);

    companion object {
        fun fromWire(value: String?): HapticIntensityLevel =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BALANCED
    }
}

enum class LauncherGridStyle(
    val displayName: String,
    val columns: Int
) {
    GALAXY_4X("4× Grid", 4),
    COMPACT_5X("5× Compact", 5),
    FOCUS_CARDS("Study Cards", 2);

    companion object {
        fun fromWire(value: String?): LauncherGridStyle =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GALAXY_4X
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "Scholar",
    val dailyGoalMinutes: Int = 90,
    val currentStreak: Int = 1,
    val longestStreak: Int = 1,
    val totalFocusMinutes: Int = 0,
    val focusXp: Int = 0,
    val gems: Int = 65,
    val totalGemsEarned: Int = 65,
    val lastFocusDate: String = "",
    val subjects: List<String> = DEFAULT_SUBJECTS,
    val unlockedSounds: List<String> = DEFAULT_UNLOCKED_SOUNDS,
    val unlockedCosmetics: List<String> = DEFAULT_UNLOCKED_COSMETICS,
    val claimedMilestones: List<String> = emptyList(),
    val equippedTheme: String = GlassThemeId.DUO_EMERALD.name,
    val equippedBadge: String = MascotBadgeId.CLASSIC_MONKEY.name,
    val role: String = "user",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val hasAdminRole: Boolean
        get() = role.equals("admin", ignoreCase = true)

    val level: Int
        get() = (focusXp / 250) + 1

    val xpInCurrentLevel: Int
        get() = focusXp % 250

    val levelProgress: Float
        get() = (xpInCurrentLevel / 250f).coerceIn(0f, 1f)

    companion object {
        val DEFAULT_SUBJECTS = listOf("Math", "Science", "English", "Coding", "History")
        val DEFAULT_UNLOCKED_SOUNDS = listOf(
            AmbientSoundPreset.RAIN.name,
            AmbientSoundPreset.CAFE_NOISE.name,
            AmbientSoundPreset.WHITE_NOISE.name
        )
        val DEFAULT_UNLOCKED_COSMETICS = listOf(
            GlassThemeId.DUO_EMERALD.name,
            GlassThemeId.LIQUID_CRYSTAL.name,
            MascotBadgeId.CLASSIC_MONKEY.name
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FocusSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "Math",
    val plannedMinutes: Int = 25,
    val completedMinutes: Int = 25,
    val ambientSound: String = "Silent",
    val xpEarned: Int = 50,
    val gemsEarned: Int = 35,
    val dateKey: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PomodoroTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "Math",
    val estimatedPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    val completed: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class PathMilestone(
    val id: String,
    val title: String,
    val description: String,
    val gemReward: Int,
    val targetLabel: String,
    val progress: Float,
    val isUnlocked: Boolean,
    val isClaimed: Boolean
)

data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val usageMillis: Long,
    val shareFraction: Float,
    val isThisApp: Boolean = false
)

data class DailyUsagePoint(
    val dateKey: String,
    val dayLabel: String,
    val monthDayLabel: String,
    val focusMinutes: Int,
    val distractionMinutes: Int,
    val isToday: Boolean
) {
    val totalMinutes: Int get() = focusMinutes + distractionMinutes
    val focusPercent: Int
        get() = if (totalMinutes > 0) ((focusMinutes * 100f) / totalMinutes).toInt().coerceIn(0, 100) else 0
}

data class ScreenUsageUiState(
    val hasUsagePermission: Boolean = false,
    val totalScreenTimeMillis: Long = 0L,
    val thisAppUsageMillis: Long = 0L,
    val topApps: List<AppUsageEntry> = emptyList(),
    val dailyDistractionMinutesByDate: Map<String, Int> = emptyMap(),
    val lastUpdatedMillis: Long = 0L
)

data class TimerUiState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val subject: String = "Math",
    val plannedMinutes: Int = 25,
    val focusMinutesSetting: Int = 25,
    val shortBreakMinutesSetting: Int = 5,
    val longBreakMinutesSetting: Int = 15,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val completedPomodorosInCycle: Int = 0,
    val activeTaskId: String? = null,
    val sessionStartedDateKey: String = "",
    val pausedAtEpochMs: Long? = null,
    val totalPausedSeconds: Int = 0,
    val pauseCount: Int = 0,
    val restoredFromSavedSession: Boolean = false
) {
    val totalSeconds: Int get() = (plannedMinutes * 60).coerceAtLeast(60)
    val progress: Float
        get() = ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val displaySeconds: Int get() = remainingSeconds.coerceAtLeast(0)
    val elapsedMinutes: Int get() = elapsedSeconds / 60
    val hasSignificantFocusProgress: Boolean
        get() = phase == PomodoroPhase.FOCUS && elapsedSeconds >= 60
}

data class PersistedTimerSnapshot(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val subject: String = "Math",
    val plannedMinutes: Int = 25,
    val focusMinutesSetting: Int = 25,
    val shortBreakMinutesSetting: Int = 5,
    val longBreakMinutesSetting: Int = 15,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val completedPomodorosInCycle: Int = 0,
    val activeTaskId: String? = null,
    val sessionStartedDateKey: String = "",
    val lastPersistedEpochMs: Long = 0L,
    val pausedAtEpochMs: Long? = null,
    val totalPausedSeconds: Int = 0,
    val pauseCount: Int = 0
)

data class AppPreferencesState(
    val uiClickSoundsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val ambientVolume: Float = 0.65f,
    val ambientAutoPlayWithTimer: Boolean = true,
    val preferredAmbientSound: AmbientSoundPreset = AmbientSoundPreset.RAIN,
    val completionSound: CompletionSoundPreset = CompletionSoundPreset.CRYSTAL_CHIME,
    val weekendPdfReminderEnabled: Boolean = true,
    val backgroundTimerEnabled: Boolean = true,
    val weeklyGoalMinutes: Int = 300,
    val selectedGoalPeriod: StudyGoalPeriod = StudyGoalPeriod.DAILY,
    val privateStudyMode: Boolean = false,
    val streakReminderEnabled: Boolean = true,
    val streakReminderHour: Int = 20,
    val streakReminderMinute: Int = 0,
    val fcmToken: String = "",
    val fcmSubscribedTopic: String = "",
    val lastSessionStartedDate: String = "",
    val lastStreakReminderSentDate: String = "",
    val hideStudyHistory: Boolean = false,
    val customAppIconId: CustomAppIconId = CustomAppIconId.MONKEY_CLASSIC,
    val customIconShapeId: AppIconShapeId = AppIconShapeId.SQUIRCLE,
    val ambientLightingStyle: AmbientLightingStyle = AmbientLightingStyle.AURORA_GLASS,
    val ambientLightingIntensity: Float = 0.72f,
    val ambientEdgeGlowEnabled: Boolean = true,
    val hapticIntensity: HapticIntensityLevel = HapticIntensityLevel.BALANCED,
    val launcherPinnedPackages: List<String> = emptyList(),
    val launcherGridStyle: LauncherGridStyle = LauncherGridStyle.GALAXY_4X,
    val launcherStrictEduOnly: Boolean = true,
    val launcherClockStyleLarge: Boolean = true
)

data class GeminiInsightReport(
    val headline: String,
    val encouragingMessage: String,
    val productivityInsights: List<String>,
    val nextSessionTip: String,
    val focusScoreLabel: String,
    val generatedAtMs: Long = System.currentTimeMillis()
)

sealed interface GeminiInsightsUiState {
    data object Idle : GeminiInsightsUiState
    data object Loading : GeminiInsightsUiState
    data class Success(val report: GeminiInsightReport) : GeminiInsightsUiState
    data class Error(val message: String) : GeminiInsightsUiState
}

data class SessionCoachSummary(
    val celebrationTag: String,
    val quickSummary: String,
    val nextMoveTip: String
)

sealed interface SessionCoachState {
    data object Loading : SessionCoachState
    data class Success(val summary: SessionCoachSummary) : SessionCoachState
    data class Error(val message: String) : SessionCoachState
}

data class FocusSessionSummaryDialogState(
    val sessionId: String,
    val subject: String,
    val completedMinutes: Int,
    val plannedMinutes: Int,
    val pointsEarned: Int,
    val gemsEarned: Int,
    val streakDays: Int,
    val todayTotalMinutes: Int,
    val dailyGoalMinutes: Int,
    val ambientSound: String,
    val nextPhaseName: String,
    val coachState: SessionCoachState = SessionCoachState.Loading
)

data class GemRewardBanner(
    val title: String,
    val gemsDelta: Int,
    val xpDelta: Int = 0,
    val timestampMs: Long = System.currentTimeMillis()
)

data class DayFocusPoint(
    val dayLabel: String,
    val dateKey: String,
    val minutes: Int,
    val monthDayLabel: String = "",
    val pointsEarned: Int = 0,
    val gemsEarned: Int = 0,
    val sessionsCount: Int = 0,
    val isToday: Boolean = false
)

data class StreakDayProgress(
    val dateKey: String,
    val dayShortLabel: String,
    val dayFullLabel: String,
    val monthDayLabel: String,
    val dayOfMonth: Int,
    val focusedMinutes: Int,
    val sessionsCount: Int,
    val goalMinutes: Int,
    val isToday: Boolean
) {
    val hasFocused: Boolean get() = focusedMinutes > 0 || sessionsCount > 0
    val goalMet: Boolean get() = focusedMinutes >= goalMinutes.coerceAtLeast(15)
    val progress: Float get() = (focusedMinutes.toFloat() / goalMinutes.coerceAtLeast(15)).coerceIn(0f, 1f)
}

enum class StudyLiveStatus(val label: String) {
    FOCUSING("Focusing"),
    BREAK("On Break"),
    READY("Ready");

    companion object {
        fun fromWire(value: String?): StudyLiveStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: READY
    }
}

@Keep
@IgnoreExtraProperties
data class PublicStudyProfile(
    val userId: String = "",
    val handle: String = "",
    val displayName: String = "Scholar",
    val status: String = StudyLiveStatus.READY.name,
    val currentSubject: String = "Math",
    val todayFocusMinutes: Int = 0,
    val dailyGoalMinutes: Int = 90,
    val currentStreak: Int = 1,
    val totalFocusMinutes: Int = 0,
    val level: Int = 1,
    val equippedBadge: String = MascotBadgeId.CLASSIC_MONKEY.name,
    val lastFocusDate: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val liveStatus: StudyLiveStatus get() = StudyLiveStatus.fromWire(status)
    val goalProgress: Float
        get() = (todayFocusMinutes.toFloat() / dailyGoalMinutes.coerceAtLeast(15)).coerceIn(0f, 1f)
}

@Keep
@IgnoreExtraProperties
data class StudyHandleLookup(
    val userId: String = "",
    val handle: String = "",
    val displayName: String = "Scholar",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendLink(
    val userId: String = "",
    val friendUid: String = "",
    val friendHandle: String = "",
    val friendDisplayName: String = "Scholar",
    val addedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    val userId: String = "",
    val blockedUid: String = "",
    val blockedHandle: String = "",
    val blockedDisplayName: String = "Scholar",
    val blockedAt: Timestamp? = null
)

data class FriendLiveCardState(
    val profile: PublicStudyProfile,
    val isSyncing: Boolean = false,
    val lastSyncedAtMs: Long = 0L,
    val syncPulseCount: Int = 0
)

data class SocialDashboardUiState(
    val isSignedIn: Boolean = false,
    val myStudyProfile: PublicStudyProfile? = null,
    val friends: List<FriendLiveCardState> = emptyList(),
    val blockedUsers: List<BlockedUser> = emptyList(),
    val privateStudyMode: Boolean = false,
    val isSyncingCircle: Boolean = false,
    val isAddingFriend: Boolean = false,
    val isUpdatingHandle: Boolean = false,
    val errorMessage: String? = null,
    val lastCircleSyncMs: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class StudyChat(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastSenderUid: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val participantUids: List<String> = emptyList(),
    val senderUid: String = "",
    val senderDisplayName: String = "Scholar",
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class StudyChatUiState(
    val activeFriend: PublicStudyProfile? = null,
    val messages: List<StudyChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

data class GlobalLeaderboardSnapshot(
    val profiles: List<PublicStudyProfile> = emptyList(),
    val isSyncing: Boolean = false,
    val lastSyncedAtMs: Long = 0L
)

data class GlobalLeaderboardEntry(
    val rank: Int,
    val profile: PublicStudyProfile,
    val isCurrentUser: Boolean,
    val isFriend: Boolean
)

sealed interface GlobalLeaderboardUiState {
    data object Loading : GlobalLeaderboardUiState
    data class Success(
        val entries: List<GlobalLeaderboardEntry>,
        val myEntry: GlobalLeaderboardEntry?,
        val nextAboveEntry: GlobalLeaderboardEntry?,
        val isSyncing: Boolean,
        val lastSyncedAtMs: Long
    ) : GlobalLeaderboardUiState
    data class Error(val message: String) : GlobalLeaderboardUiState
}

@Keep
@IgnoreExtraProperties
data class StudyChallenge(
    @DocumentId val id: String = "",
    val creatorUid: String = "",
    val creatorName: String = "Scholar",
    val title: String = "",
    val subject: String = "All Subjects",
    val targetMinutes: Int = 300,
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val invitedUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChallengeParticipant(
    val challengeId: String = "",
    val userId: String = "",
    val displayName: String = "Scholar",
    val handle: String = "",
    val contributedMinutes: Int = 0,
    val sessionsCompleted: Int = 0,
    val joinedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChallengeCodeLookup(
    val code: String = "",
    val challengeId: String = "",
    val creatorUid: String = "",
    val title: String = "",
    val createdAt: Timestamp? = null
)

data class StudyChallengeWithProgress(
    val challenge: StudyChallenge,
    val participants: List<ChallengeParticipant> = emptyList(),
    val isSyncing: Boolean = false
) {
    val totalContributedMinutes: Int
        get() = participants.sumOf { it.contributedMinutes.coerceAtLeast(0) }

    val totalSessionsCompleted: Int
        get() = participants.sumOf { it.sessionsCompleted.coerceAtLeast(0) }

    val progressFraction: Float
        get() = (totalContributedMinutes.toFloat() / challenge.targetMinutes.coerceAtLeast(30).toFloat())
            .coerceIn(0f, 1f)

    val isCompleted: Boolean
        get() = totalContributedMinutes >= challenge.targetMinutes.coerceAtLeast(30)
}

data class StudyChallengesUiState(
    val activeChallenges: List<StudyChallengeWithProgress> = emptyList(),
    val pendingInvites: List<StudyChallenge> = emptyList(),
    val isSyncing: Boolean = false,
    val isBusy: Boolean = false,
    val errorMessage: String? = null
)

@Keep
@IgnoreExtraProperties
data class StreakReminderSettings(
    val userId: String = "",
    val enabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val timezoneId: String = "UTC",
    val fcmToken: String = "",
    val fcmTopic: String = "",
    val lastFocusDate: String = "",
    val currentStreak: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class StreakReminderUiStatus(
    val enabled: Boolean = true,
    val hour: Int = 20,
    val minute: Int = 0,
    val formattedTime: String = "8:00 PM",
    val hasStartedSessionToday: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val fcmConnected: Boolean = false,
    val fcmTokenPreview: String = "",
    val fcmTopic: String = "",
    val cloudSynced: Boolean = false,
    val isSyncingFcm: Boolean = false
)

// --- Verified Admin & Silent Live Background App Updates (Zero Personal User Data) ---

object AdminIdentityPolicy {
    const val VERIFIED_ADMIN_EMAIL = "siddharthbade104@gmail.com"

    fun isVerifiedAdmin(email: String?, isEmailVerified: Boolean = true): Boolean {
        if (email.isNullOrBlank() || !isEmailVerified) return false
        return email.trim().equals(VERIFIED_ADMIN_EMAIL, ignoreCase = true)
    }
}

enum class AdminNoticeKind(val displayName: String) {
    UPDATE("Live Update"),
    BROADCAST("Broadcast"),
    DIRECT("Direct"),
    ALERT("Alert");

    companion object {
        fun fromWire(value: String?): AdminNoticeKind =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BROADCAST
    }
}

@Keep
@IgnoreExtraProperties
data class AppLiveConfig(
    val configVersion: Int = 1,
    val liveTagline: String = "",
    val announcementTitle: String = "",
    val announcementBody: String = "",
    val announcementActive: Boolean = false,
    val updateNoticeLabel: String = "",
    val defaultFocusMinutes: Int = 25,
    val gemMultiplierPercent: Int = 100,
    val socialEnabled: Boolean = true,
    val launcherEnabled: Boolean = true,
    val ambientDrawerEnabled: Boolean = true,
    val maintenanceBannerEnabled: Boolean = false,
    val updatedAt: Timestamp? = null
) {
    val gemMultiplier: Float
        get() = (gemMultiplierPercent.coerceIn(100, 300) / 100f)

    val hasVisibleAnnouncement: Boolean
        get() = announcementActive && (announcementTitle.isNotBlank() || announcementBody.isNotBlank())
}

@Keep
@IgnoreExtraProperties
data class AdminNotice(
    @DocumentId val id: String = "",
    val audienceUids: List<String> = listOf("ALL"),
    val targetLabel: String = "All Scholars",
    val title: String = "",
    val body: String = "",
    val kind: String = AdminNoticeKind.BROADCAST.name,
    val createdAt: Timestamp? = null
) {
    val noticeKind: AdminNoticeKind get() = AdminNoticeKind.fromWire(kind)
    val isBroadcast: Boolean get() = "ALL" in audienceUids
}

@Keep
@IgnoreExtraProperties
data class AccountModerationRecord(
    val userId: String = "",
    val status: String = "ACTIVE",
    val reason: String = "",
    val updatedAt: Timestamp? = null
) {
    val isRemoved: Boolean get() = status.equals("REMOVED", ignoreCase = true)
}

data class AdminUserCountsSnapshot(
    val totalUsers: Int = 0,
    val focusingNow: Int = 0,
    val onBreakNow: Int = 0,
    val readyNow: Int = 0,
    val activeToday: Int = 0,
    val onStreakCount: Int = 0,
    val removedAccounts: Int = 0,
    val totalFocusMinutesAcrossDirectory: Long = 0L,
    val isRefreshing: Boolean = false,
    val lastUpdatedMs: Long = 0L
)

data class AdminPanelUiState(
    val isVerifiedAdmin: Boolean = false,
    val firestoreRole: String = "user",
    val userCounts: AdminUserCountsSnapshot = AdminUserCountsSnapshot(),
    val publicDirectory: List<PublicStudyProfile> = emptyList(),
    val notices: List<AdminNotice> = emptyList(),
    val liveConfig: AppLiveConfig = AppLiveConfig(),
    val isPushingUpdate: Boolean = false,
    val isSendingNotice: Boolean = false,
    val deletingUserId: String? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

// --- Multi-Task Subject Study Alarms ---

data class SubjectStudyAlarm(
    val id: String = "",
    val subject: String = "Math",
    val hour: Int = 6,
    val minute: Int = 0,
    val durationMinutes: Int = 25,
    val enabled: Boolean = true,
    val note: String = ""
) {
    val formattedTime: String
        get() {
            val h12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val amPm = if (hour < 12) "AM" else "PM"
            return "%d:%02d %s".format(h12, minute, amPm)
        }
}

// --- Detailed Scholar Profile & Custom Avatar ---

data class ScholarProfileDetails(
    val avatarLocalPath: String = "",
    val bio: String = "",
    val schoolOrCourse: String = "",
    val targetExamOrGoal: String = "",
    val favoriteSubject: String = "Math",
    val preferredStudyShift: String = "Morning",
    val phoneNumber: String = "",
    val preferredEmail: String = ""
) {
    val hasAnyDetails: Boolean
        get() = bio.isNotBlank() ||
            schoolOrCourse.isNotBlank() ||
            targetExamOrGoal.isNotBlank() ||
            phoneNumber.isNotBlank() ||
            preferredEmail.isNotBlank()
}

// --- 500-Hour Pro Edition (Strict, Un-Bypassable Verified Study Gate) ---

data class ProUnlockStatus(
    val verifiedStudyMinutes: Int = 0,
    val requiredStudyMinutes: Int = PRO_REQUIRED_STUDY_MINUTES
) {
    val isUnlocked: Boolean
        get() = verifiedStudyMinutes >= requiredStudyMinutes

    val completedHours: Float
        get() = verifiedStudyMinutes / 60f

    val requiredHours: Int
        get() = requiredStudyMinutes / 60

    val remainingHours: Float
        get() = ((requiredStudyMinutes - verifiedStudyMinutes).coerceAtLeast(0)) / 60f

    val progressFraction: Float
        get() = (verifiedStudyMinutes.toFloat() / requiredStudyMinutes.toFloat()).coerceIn(0f, 1f)

    companion object {
        const val PRO_REQUIRED_STUDY_MINUTES: Int = 30_000 // Strictly 500.0 verified study hours
    }
}

data class ProPracticeQuestion(
    val question: String,
    val answer: String
)

data class ProAiNoteResult(
    val subject: String,
    val topic: String,
    val summary: String,
    val keyPoints: List<String>,
    val practiceQuestions: List<ProPracticeQuestion>
)

sealed interface ProAiStudioUiState {
    data object Idle : ProAiStudioUiState
    data class Loading(val label: String) : ProAiStudioUiState
    data class NotesReady(val result: ProAiNoteResult) : ProAiStudioUiState
    data class TutorAnswer(val question: String, val answer: String) : ProAiStudioUiState
    data class Error(val message: String) : ProAiStudioUiState
}






