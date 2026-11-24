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
    SAPPHIRE_SKY("Sapphire Glass", "Cool sky sapphire & crystalline mist", 50, false),
    GOLDEN_AMBER("Sunburst Gold", "Warm amber light & honey glass", 75, false),
    MIDNIGHT_FOREST("Midnight Pine", "Deep obsidian glass & emerald glow", 100, true),
    SAKURA_QUARTZ("Rose Quartz", "Soft sakura pearl & coral glass", 120, false);

    companion object {
        fun fromWire(value: String?): GlassThemeId =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DUO_EMERALD
    }
}

enum class MascotBadgeId(
    val displayName: String,
    val subtitle: String,
    val gemCost: Int
) {
    CLASSIC_MONKEY("Leaf Explorer", "Fresh sprout focus aura", 0),
    SCHOLAR_OWL("Honor Scholar", "Sapphire shimmer timer halo", 60),
    ROYAL_CROWN("Streak Sovereign", "Golden crown prism ring", 100),
    COSMIC_AURA("Metamorphosis", "Iridescent liquid-glass aurora", 150);

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
    RAIN("Soft Rain", "Gentle raindrops on frosted glass", 0),
    WHITE_NOISE("White Noise", "Clean velvet noise blanket", 0),
    FOREST_WIND("Forest Breeze", "Rustling canopy & distant songbirds", 40),
    OCEAN_WAVES("Ocean Tide", "Slow rhythmic coastal surf", 60),
    BROWN_NOISE("Deep Brown", "Warm low-frequency study rumble", 80),
    ALPHA_WAVES("Alpha 10Hz", "Binaural calm concentration pulse", 100),
    FIREPLACE("Cozy Hearth", "Warm glowing embers & soft crackle", 120);

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
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
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
            AmbientSoundPreset.WHITE_NOISE.name
        )
        val DEFAULT_UNLOCKED_COSMETICS = listOf(
            GlassThemeId.DUO_EMERALD.name,
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

sealed interface AiCoachUiState {
    data object Idle : AiCoachUiState
    data object Loading : AiCoachUiState
    data class Streaming(val text: String) : AiCoachUiState
    data class Success(val text: String) : AiCoachUiState
    data class Error(val message: String) : AiCoachUiState
}

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
    val activeTaskId: String? = null
) {
    val totalSeconds: Int get() = (plannedMinutes * 60).coerceAtLeast(60)
    val progress: Float
        get() = ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val displaySeconds: Int get() = remainingSeconds.coerceAtLeast(0)
}

data class AppPreferencesState(
    val uiClickSoundsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val ambientVolume: Float = 0.65f,
    val ambientAutoPlayWithTimer: Boolean = true,
    val preferredAmbientSound: AmbientSoundPreset = AmbientSoundPreset.RAIN
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
    val minutes: Int
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
