package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.AppPreferencesState
import com.example.data.model.GlassThemeId
import com.example.data.model.MascotBadgeId
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tryAgainDataStore by preferencesDataStore(name = "try_again_preferences")

class LocalPreferencesStore(private val context: Context) {

    private object Keys {
        val UI_CLICK_SOUNDS = booleanPreferencesKey("ui_click_sounds")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
        val AMBIENT_AUTO_PLAY = booleanPreferencesKey("ambient_auto_play")
        val PREFERRED_AMBIENT = stringPreferencesKey("preferred_ambient")

        // Local persistence for instant on-device mode (zero barriers on launch)
        val LOCAL_DISPLAY_NAME = stringPreferencesKey("local_display_name")
        val LOCAL_DAILY_GOAL = intPreferencesKey("local_daily_goal")
        val LOCAL_STREAK = intPreferencesKey("local_streak")
        val LOCAL_LONGEST_STREAK = intPreferencesKey("local_longest_streak")
        val LOCAL_TOTAL_MINUTES = intPreferencesKey("local_total_minutes")
        val LOCAL_FOCUS_XP = intPreferencesKey("local_focus_xp")
        val LOCAL_GEMS = intPreferencesKey("local_gems")
        val LOCAL_TOTAL_GEMS = intPreferencesKey("local_total_gems")
        val LOCAL_LAST_DATE = stringPreferencesKey("local_last_date")
        val LOCAL_UNLOCKED_SOUNDS = stringSetPreferencesKey("local_unlocked_sounds")
        val LOCAL_UNLOCKED_COSMETICS = stringSetPreferencesKey("local_unlocked_cosmetics")
        val LOCAL_CLAIMED_MILESTONES = stringSetPreferencesKey("local_claimed_milestones")
        val LOCAL_EQUIPPED_THEME = stringPreferencesKey("local_equipped_theme")
        val LOCAL_EQUIPPED_BADGE = stringPreferencesKey("local_equipped_badge")
        val LOCAL_DAILY_FOCUS_LOGS = stringSetPreferencesKey("local_daily_focus_logs")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.tryAgainDataStore.data.map { prefs ->
        AppPreferencesState(
            uiClickSoundsEnabled = prefs[Keys.UI_CLICK_SOUNDS] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            ambientVolume = (prefs[Keys.AMBIENT_VOLUME] ?: 0.65f).coerceIn(0.05f, 1f),
            ambientAutoPlayWithTimer = prefs[Keys.AMBIENT_AUTO_PLAY] ?: true,
            preferredAmbientSound = AmbientSoundPreset.fromWire(prefs[Keys.PREFERRED_AMBIENT])
                .let { if (it == AmbientSoundPreset.OFF) AmbientSoundPreset.RAIN else it }
        )
    }

    val localProfileFlow: Flow<UserProfile> = context.tryAgainDataStore.data.map { prefs ->
        UserProfile(
            userId = "local_scholar",
            displayName = prefs[Keys.LOCAL_DISPLAY_NAME] ?: "Scholar",
            dailyGoalMinutes = (prefs[Keys.LOCAL_DAILY_GOAL] ?: 90).coerceIn(15, 720),
            currentStreak = prefs[Keys.LOCAL_STREAK] ?: 1,
            longestStreak = prefs[Keys.LOCAL_LONGEST_STREAK] ?: 1,
            totalFocusMinutes = prefs[Keys.LOCAL_TOTAL_MINUTES] ?: 0,
            focusXp = prefs[Keys.LOCAL_FOCUS_XP] ?: 0,
            gems = prefs[Keys.LOCAL_GEMS] ?: 65,
            totalGemsEarned = prefs[Keys.LOCAL_TOTAL_GEMS] ?: 65,
            lastFocusDate = prefs[Keys.LOCAL_LAST_DATE] ?: "",
            subjects = UserProfile.DEFAULT_SUBJECTS,
            unlockedSounds = prefs[Keys.LOCAL_UNLOCKED_SOUNDS]?.toList()
                ?.ifEmpty { UserProfile.DEFAULT_UNLOCKED_SOUNDS }
                ?: UserProfile.DEFAULT_UNLOCKED_SOUNDS,
            unlockedCosmetics = prefs[Keys.LOCAL_UNLOCKED_COSMETICS]?.toList()
                ?.ifEmpty { UserProfile.DEFAULT_UNLOCKED_COSMETICS }
                ?: UserProfile.DEFAULT_UNLOCKED_COSMETICS,
            claimedMilestones = prefs[Keys.LOCAL_CLAIMED_MILESTONES]?.toList() ?: emptyList(),
            equippedTheme = prefs[Keys.LOCAL_EQUIPPED_THEME] ?: GlassThemeId.DUO_EMERALD.name,
            equippedBadge = prefs[Keys.LOCAL_EQUIPPED_BADGE] ?: MascotBadgeId.CLASSIC_MONKEY.name
        )
    }

    suspend fun saveLocalProfile(profile: UserProfile) {
        context.tryAgainDataStore.edit { prefs ->
            prefs[Keys.LOCAL_DISPLAY_NAME] = profile.displayName
            prefs[Keys.LOCAL_DAILY_GOAL] = profile.dailyGoalMinutes
            prefs[Keys.LOCAL_STREAK] = profile.currentStreak
            prefs[Keys.LOCAL_LONGEST_STREAK] = profile.longestStreak
            prefs[Keys.LOCAL_TOTAL_MINUTES] = profile.totalFocusMinutes
            prefs[Keys.LOCAL_FOCUS_XP] = profile.focusXp
            prefs[Keys.LOCAL_GEMS] = profile.gems
            prefs[Keys.LOCAL_TOTAL_GEMS] = profile.totalGemsEarned
            prefs[Keys.LOCAL_LAST_DATE] = profile.lastFocusDate
            prefs[Keys.LOCAL_UNLOCKED_SOUNDS] = profile.unlockedSounds.toSet()
            prefs[Keys.LOCAL_UNLOCKED_COSMETICS] = profile.unlockedCosmetics.toSet()
            prefs[Keys.LOCAL_CLAIMED_MILESTONES] = profile.claimedMilestones.toSet()
            prefs[Keys.LOCAL_EQUIPPED_THEME] = profile.equippedTheme
            prefs[Keys.LOCAL_EQUIPPED_BADGE] = profile.equippedBadge
        }
    }

    /**
     * Map of dateKey ("yyyy-MM-dd") -> Pair(totalMinutes, sessionsCount) persisted locally.
     */
    val localDailyLogsFlow: Flow<Map<String, Pair<Int, Int>>> = context.tryAgainDataStore.data.map { prefs ->
        val entries = prefs[Keys.LOCAL_DAILY_FOCUS_LOGS].orEmpty()
        buildMap {
            entries.forEach { raw ->
                val parts = raw.split("|")
                if (parts.size == 3) {
                    val dateKey = parts[0]
                    val mins = parts[1].toIntOrNull() ?: 0
                    val count = parts[2].toIntOrNull() ?: 0
                    if (dateKey.isNotBlank() && (mins > 0 || count > 0)) {
                        put(dateKey, mins to count)
                    }
                }
            }
        }
    }

    suspend fun recordLocalDayFocus(dateKey: String, addedMinutes: Int) {
        if (dateKey.isBlank() || addedMinutes <= 0) return
        context.tryAgainDataStore.edit { prefs ->
            val currentMap = mutableMapOf<String, Pair<Int, Int>>()
            prefs[Keys.LOCAL_DAILY_FOCUS_LOGS].orEmpty().forEach { raw ->
                val parts = raw.split("|")
                if (parts.size == 3) {
                    val mins = parts[1].toIntOrNull() ?: 0
                    val count = parts[2].toIntOrNull() ?: 0
                    currentMap[parts[0]] = mins to count
                }
            }
            val (prevMins, prevCount) = currentMap[dateKey] ?: (0 to 0)
            currentMap[dateKey] = (prevMins + addedMinutes) to (prevCount + 1)
            // Keep most recent 45 days to stay lightweight
            val trimmed = currentMap.entries
                .sortedByDescending { it.key }
                .take(45)
                .map { (k, v) -> "$k|${v.first}|${v.second}" }
                .toSet()
            prefs[Keys.LOCAL_DAILY_FOCUS_LOGS] = trimmed
        }
    }

    suspend fun setUiClickSoundsEnabled(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.UI_CLICK_SOUNDS] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setAmbientVolume(volume: Float) {
        context.tryAgainDataStore.edit { it[Keys.AMBIENT_VOLUME] = volume.coerceIn(0.05f, 1f) }
    }

    suspend fun setAmbientAutoPlayWithTimer(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.AMBIENT_AUTO_PLAY] = enabled }
    }

    suspend fun setPreferredAmbientSound(preset: AmbientSoundPreset) {
        context.tryAgainDataStore.edit { it[Keys.PREFERRED_AMBIENT] = preset.name }
    }
}
