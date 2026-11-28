package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AmbientLightingStyle
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.AppIconShapeId
import com.example.data.model.AppLiveConfig
import com.example.data.model.AppPreferencesState
import com.example.data.model.CompletionSoundPreset
import com.example.data.model.CustomAppIconId
import com.example.data.model.FocusSession
import com.example.data.model.GlassThemeId
import com.example.data.model.HapticIntensityLevel
import com.example.data.model.LauncherGridStyle
import com.example.data.model.MascotBadgeId
import com.example.data.model.PersistedTimerSnapshot
import com.example.data.model.PomodoroPhase
import com.example.data.model.ScholarProfileDetails
import com.example.data.model.StudyGoalPeriod
import com.example.data.model.SubjectStudyAlarm
import com.example.data.model.UserProfile
import com.example.service.FocusStreakCalculator
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

private val Context.tryAgainDataStore by preferencesDataStore(name = "try_again_preferences")

class LocalPreferencesStore(private val context: Context) {

    private object Keys {
        val UI_CLICK_SOUNDS = booleanPreferencesKey("ui_click_sounds")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
        val AMBIENT_AUTO_PLAY = booleanPreferencesKey("ambient_auto_play")
        val PREFERRED_AMBIENT = stringPreferencesKey("preferred_ambient")
        val COMPLETION_SOUND = stringPreferencesKey("completion_sound")
        val WEEKEND_PDF_ENABLED = booleanPreferencesKey("weekend_pdf_enabled")
        val BACKGROUND_TIMER_ENABLED = booleanPreferencesKey("background_timer_enabled")
        val WEEKLY_GOAL_MINUTES = intPreferencesKey("weekly_goal_minutes")
        val SELECTED_GOAL_PERIOD = stringPreferencesKey("selected_goal_period")
        val PRIVATE_STUDY_MODE = booleanPreferencesKey("private_study_mode")
        val STREAK_REMINDER_ENABLED = booleanPreferencesKey("streak_reminder_enabled")
        val STREAK_REMINDER_HOUR = intPreferencesKey("streak_reminder_hour")
        val STREAK_REMINDER_MINUTE = intPreferencesKey("streak_reminder_minute")
        val FCM_TOKEN = stringPreferencesKey("fcm_registration_token")
        val FCM_SUBSCRIBED_TOPIC = stringPreferencesKey("fcm_subscribed_topic")
        val LAST_SESSION_STARTED_DATE = stringPreferencesKey("last_session_started_date")
        val LAST_STREAK_REMINDER_SENT_DATE = stringPreferencesKey("last_streak_reminder_sent_date")
        val HIDE_STUDY_HISTORY = booleanPreferencesKey("hide_study_history")
        val CUSTOM_APP_ICON_ID = stringPreferencesKey("custom_app_icon_id")
        val CUSTOM_ICON_SHAPE_ID = stringPreferencesKey("custom_icon_shape_id")
        val AMBIENT_LIGHTING_STYLE = stringPreferencesKey("ambient_lighting_style")
        val AMBIENT_LIGHTING_INTENSITY = floatPreferencesKey("ambient_lighting_intensity")
        val AMBIENT_EDGE_GLOW = booleanPreferencesKey("ambient_edge_glow_enabled")
        val HAPTIC_INTENSITY = stringPreferencesKey("haptic_intensity_level")
        val LAUNCHER_PINNED_PACKAGES = stringSetPreferencesKey("launcher_pinned_packages")
        val LAUNCHER_GRID_STYLE = stringPreferencesKey("launcher_grid_style")
        val LAUNCHER_STRICT_EDU_ONLY = booleanPreferencesKey("launcher_strict_edu_only")
        val LAUNCHER_CLOCK_LARGE = booleanPreferencesKey("launcher_clock_large")

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
        val LOCAL_SESSION_HISTORY_JSON = stringPreferencesKey("local_session_history_json")
        val LOCAL_APP_USAGE_DATE = stringPreferencesKey("local_app_usage_date")
        val LOCAL_APP_USAGE_SECONDS = intPreferencesKey("local_app_usage_seconds")
        val PERSISTED_TIMER_JSON = stringPreferencesKey("persisted_timer_snapshot_json")
        val CACHED_LIVE_CONFIG_JSON = stringPreferencesKey("cached_live_app_config_json")
        val SUBJECT_ALARMS_JSON = stringPreferencesKey("subject_study_alarms_json")
        val SCHOLAR_DETAILS_JSON = stringPreferencesKey("scholar_profile_details_json")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.tryAgainDataStore.data.map { prefs ->
        AppPreferencesState(
            uiClickSoundsEnabled = prefs[Keys.UI_CLICK_SOUNDS] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            ambientVolume = (prefs[Keys.AMBIENT_VOLUME] ?: 0.65f).coerceIn(0.05f, 1f),
            ambientAutoPlayWithTimer = prefs[Keys.AMBIENT_AUTO_PLAY] ?: true,
            preferredAmbientSound = AmbientSoundPreset.fromWire(prefs[Keys.PREFERRED_AMBIENT])
                .let { if (it == AmbientSoundPreset.OFF) AmbientSoundPreset.RAIN else it },
            completionSound = CompletionSoundPreset.fromWire(prefs[Keys.COMPLETION_SOUND]),
            weekendPdfReminderEnabled = prefs[Keys.WEEKEND_PDF_ENABLED] ?: true,
            backgroundTimerEnabled = prefs[Keys.BACKGROUND_TIMER_ENABLED] ?: true,
            weeklyGoalMinutes = (prefs[Keys.WEEKLY_GOAL_MINUTES] ?: 300).coerceIn(60, 3600),
            selectedGoalPeriod = StudyGoalPeriod.fromWire(prefs[Keys.SELECTED_GOAL_PERIOD]),
            privateStudyMode = prefs[Keys.PRIVATE_STUDY_MODE] ?: false,
            streakReminderEnabled = prefs[Keys.STREAK_REMINDER_ENABLED] ?: true,
            streakReminderHour = (prefs[Keys.STREAK_REMINDER_HOUR] ?: 20).coerceIn(0, 23),
            streakReminderMinute = (prefs[Keys.STREAK_REMINDER_MINUTE] ?: 0).coerceIn(0, 59),
            fcmToken = prefs[Keys.FCM_TOKEN].orEmpty(),
            fcmSubscribedTopic = prefs[Keys.FCM_SUBSCRIBED_TOPIC].orEmpty(),
            lastSessionStartedDate = prefs[Keys.LAST_SESSION_STARTED_DATE].orEmpty(),
            lastStreakReminderSentDate = prefs[Keys.LAST_STREAK_REMINDER_SENT_DATE].orEmpty(),
            hideStudyHistory = prefs[Keys.HIDE_STUDY_HISTORY] ?: false,
            customAppIconId = CustomAppIconId.fromWire(prefs[Keys.CUSTOM_APP_ICON_ID]),
            customIconShapeId = AppIconShapeId.fromWire(prefs[Keys.CUSTOM_ICON_SHAPE_ID]),
            ambientLightingStyle = AmbientLightingStyle.fromWire(prefs[Keys.AMBIENT_LIGHTING_STYLE]),
            ambientLightingIntensity = (prefs[Keys.AMBIENT_LIGHTING_INTENSITY] ?: 0.72f).coerceIn(0.15f, 1f),
            ambientEdgeGlowEnabled = prefs[Keys.AMBIENT_EDGE_GLOW] ?: true,
            hapticIntensity = HapticIntensityLevel.fromWire(prefs[Keys.HAPTIC_INTENSITY]),
            launcherPinnedPackages = prefs[Keys.LAUNCHER_PINNED_PACKAGES]?.toList()?.sorted() ?: emptyList(),
            launcherGridStyle = LauncherGridStyle.fromWire(prefs[Keys.LAUNCHER_GRID_STYLE]),
            launcherStrictEduOnly = prefs[Keys.LAUNCHER_STRICT_EDU_ONLY] ?: true,
            launcherClockStyleLarge = prefs[Keys.LAUNCHER_CLOCK_LARGE] ?: true
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

    /**
     * Persisted detailed study session history (latest 120 sessions) so study logs,
     * Gemini insights, and Weekly PDF reports persist reliably across app restarts & background finishes.
     */
    val localSessionHistoryFlow: Flow<List<FocusSession>> = context.tryAgainDataStore.data.map { prefs ->
        parseSessionsJson(prefs[Keys.LOCAL_SESSION_HISTORY_JSON])
    }

    val subjectAlarmsFlow: Flow<List<SubjectStudyAlarm>> = context.tryAgainDataStore.data.map { prefs ->
        parseSubjectAlarmsJson(prefs[Keys.SUBJECT_ALARMS_JSON])
    }

    val scholarDetailsFlow: Flow<ScholarProfileDetails> = context.tryAgainDataStore.data.map { prefs ->
        parseScholarDetailsJson(prefs[Keys.SCHOLAR_DETAILS_JSON])
    }

    suspend fun saveSubjectAlarms(alarms: List<SubjectStudyAlarm>) {
        context.tryAgainDataStore.edit { prefs ->
            prefs[Keys.SUBJECT_ALARMS_JSON] = encodeSubjectAlarmsJson(alarms)
        }
    }

    suspend fun saveScholarDetails(details: ScholarProfileDetails) {
        context.tryAgainDataStore.edit { prefs ->
            prefs[Keys.SCHOLAR_DETAILS_JSON] = encodeScholarDetailsJson(details)
        }
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

    suspend fun appendLocalSession(session: FocusSession) {
        context.tryAgainDataStore.edit { prefs ->
            val existing = parseSessionsJson(prefs[Keys.LOCAL_SESSION_HISTORY_JSON])
            if (existing.any { it.id == session.id }) return@edit
            val updated = (listOf(session) + existing).take(120)
            prefs[Keys.LOCAL_SESSION_HISTORY_JSON] = encodeSessionsJson(updated)
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
            val trimmed = currentMap.entries
                .sortedByDescending { it.key }
                .take(45)
                .map { (k, v) -> "$k|${v.first}|${v.second}" }
                .toSet()
            prefs[Keys.LOCAL_DAILY_FOCUS_LOGS] = trimmed
        }
    }

    /**
     * Atomically records a completed session from background timer service even if UI is closed.
     * Uses [FocusStreakCalculator] so paused/midnight-spanning sessions never cause unfair streak loss.
     */
    suspend fun recordBackgroundCompletedSession(
        session: FocusSession,
        todayDateKey: String,
        yesterdayDateKey: String,
        sessionStartedDateKey: String? = null
    ) {
        context.tryAgainDataStore.edit { prefs ->
            val existing = parseSessionsJson(prefs[Keys.LOCAL_SESSION_HISTORY_JSON])
            if (existing.any { it.id == session.id }) return@edit

            val updatedHistory = (listOf(session) + existing).take(120)
            prefs[Keys.LOCAL_SESSION_HISTORY_JSON] = encodeSessionsJson(updatedHistory)

            val lastDate = prefs[Keys.LOCAL_LAST_DATE] ?: ""
            val curStreak = prefs[Keys.LOCAL_STREAK] ?: 1
            val curLongest = prefs[Keys.LOCAL_LONGEST_STREAK] ?: 1
            val newStreak = FocusStreakCalculator.calculateNextStreak(
                currentStreak = curStreak,
                lastFocusDate = lastDate,
                completionDateKey = todayDateKey,
                sessionStartedDateKey = sessionStartedDateKey
            )
            prefs[Keys.LOCAL_STREAK] = newStreak
            prefs[Keys.LOCAL_LONGEST_STREAK] = maxOf(curLongest, newStreak)
            prefs[Keys.LOCAL_TOTAL_MINUTES] = (prefs[Keys.LOCAL_TOTAL_MINUTES] ?: 0) + session.completedMinutes
            prefs[Keys.LOCAL_FOCUS_XP] = (prefs[Keys.LOCAL_FOCUS_XP] ?: 0) + session.xpEarned
            prefs[Keys.LOCAL_GEMS] = (prefs[Keys.LOCAL_GEMS] ?: 65) + session.gemsEarned
            prefs[Keys.LOCAL_TOTAL_GEMS] = (prefs[Keys.LOCAL_TOTAL_GEMS] ?: 65) + session.gemsEarned
            prefs[Keys.LOCAL_LAST_DATE] = todayDateKey
            prefs[Keys.LAST_SESSION_STARTED_DATE] = todayDateKey

            val currentMap = mutableMapOf<String, Pair<Int, Int>>()
            prefs[Keys.LOCAL_DAILY_FOCUS_LOGS].orEmpty().forEach { raw ->
                val parts = raw.split("|")
                if (parts.size == 3) {
                    currentMap[parts[0]] = (parts[1].toIntOrNull() ?: 0) to (parts[2].toIntOrNull() ?: 0)
                }
            }
            val (prevMins, prevCount) = currentMap[todayDateKey] ?: (0 to 0)
            currentMap[todayDateKey] = (prevMins + session.completedMinutes) to (prevCount + 1)
            prefs[Keys.LOCAL_DAILY_FOCUS_LOGS] = currentMap.entries
                .sortedByDescending { it.key }
                .take(45)
                .map { (k, v) -> "$k|${v.first}|${v.second}" }
                .toSet()
        }
    }

    /**
     * Persisted Try Again foreground usage seconds for the given dateKey ("yyyy-MM-dd").
     */
    fun appUsageSecondsForDateFlow(dateKey: String): Flow<Int> = context.tryAgainDataStore.data.map { prefs ->
        if (prefs[Keys.LOCAL_APP_USAGE_DATE] == dateKey) {
            (prefs[Keys.LOCAL_APP_USAGE_SECONDS] ?: 0).coerceAtLeast(0)
        } else {
            0
        }
    }

    suspend fun addAppUsageSeconds(dateKey: String, deltaSeconds: Int) {
        if (dateKey.isBlank() || deltaSeconds <= 0) return
        context.tryAgainDataStore.edit { prefs ->
            val current = if (prefs[Keys.LOCAL_APP_USAGE_DATE] == dateKey) {
                (prefs[Keys.LOCAL_APP_USAGE_SECONDS] ?: 0).coerceAtLeast(0)
            } else {
                0
            }
            prefs[Keys.LOCAL_APP_USAGE_DATE] = dateKey
            prefs[Keys.LOCAL_APP_USAGE_SECONDS] = current + deltaSeconds
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

    suspend fun setCompletionSound(preset: CompletionSoundPreset) {
        context.tryAgainDataStore.edit { it[Keys.COMPLETION_SOUND] = preset.name }
    }

    suspend fun setWeekendPdfReminderEnabled(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.WEEKEND_PDF_ENABLED] = enabled }
    }

    suspend fun setBackgroundTimerEnabled(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.BACKGROUND_TIMER_ENABLED] = enabled }
    }

    suspend fun setWeeklyGoalMinutes(minutes: Int) {
        context.tryAgainDataStore.edit { it[Keys.WEEKLY_GOAL_MINUTES] = minutes.coerceIn(60, 3600) }
    }

    suspend fun setSelectedGoalPeriod(period: StudyGoalPeriod) {
        context.tryAgainDataStore.edit { it[Keys.SELECTED_GOAL_PERIOD] = period.name }
    }

    suspend fun setPrivateStudyMode(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.PRIVATE_STUDY_MODE] = enabled }
    }

    suspend fun setStreakReminderEnabled(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.STREAK_REMINDER_ENABLED] = enabled }
    }

    suspend fun setStreakReminderTime(hour: Int, minute: Int) {
        context.tryAgainDataStore.edit {
            it[Keys.STREAK_REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.STREAK_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun saveFcmRegistration(token: String, subscribedTopic: String) {
        context.tryAgainDataStore.edit {
            if (token.isNotBlank()) {
                it[Keys.FCM_TOKEN] = token.take(512)
            }
            it[Keys.FCM_SUBSCRIBED_TOPIC] = subscribedTopic.take(80)
        }
    }

    suspend fun saveFcmTokenOnly(token: String) {
        if (token.isBlank()) return
        context.tryAgainDataStore.edit {
            it[Keys.FCM_TOKEN] = token.take(512)
        }
    }

    suspend fun markSessionStartedToday(dateKey: String) {
        if (dateKey.isBlank()) return
        context.tryAgainDataStore.edit {
            it[Keys.LAST_SESSION_STARTED_DATE] = dateKey.take(20)
        }
    }

    suspend fun markStreakReminderSent(dateKey: String) {
        if (dateKey.isBlank()) return
        context.tryAgainDataStore.edit {
            it[Keys.LAST_STREAK_REMINDER_SENT_DATE] = dateKey.take(20)
        }
    }

    suspend fun setHideStudyHistory(hide: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.HIDE_STUDY_HISTORY] = hide }
    }

    suspend fun setCustomAppIconId(iconId: CustomAppIconId) {
        context.tryAgainDataStore.edit { it[Keys.CUSTOM_APP_ICON_ID] = iconId.name }
    }

    suspend fun setCustomIconShapeId(shapeId: AppIconShapeId) {
        context.tryAgainDataStore.edit { it[Keys.CUSTOM_ICON_SHAPE_ID] = shapeId.name }
    }

    suspend fun setAmbientLightingStyle(style: AmbientLightingStyle) {
        context.tryAgainDataStore.edit { it[Keys.AMBIENT_LIGHTING_STYLE] = style.name }
    }

    suspend fun setAmbientLightingIntensity(intensity: Float) {
        context.tryAgainDataStore.edit { it[Keys.AMBIENT_LIGHTING_INTENSITY] = intensity.coerceIn(0.15f, 1f) }
    }

    suspend fun setAmbientEdgeGlowEnabled(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.AMBIENT_EDGE_GLOW] = enabled }
    }

    suspend fun setHapticIntensity(level: HapticIntensityLevel) {
        context.tryAgainDataStore.edit {
            it[Keys.HAPTIC_INTENSITY] = level.name
            it[Keys.HAPTICS] = level != HapticIntensityLevel.OFF
        }
    }

    suspend fun setLauncherPinnedPackages(packages: List<String>) {
        context.tryAgainDataStore.edit {
            it[Keys.LAUNCHER_PINNED_PACKAGES] = packages.filter { pkg -> pkg.isNotBlank() }.toSet()
        }
    }

    suspend fun setLauncherGridStyle(style: LauncherGridStyle) {
        context.tryAgainDataStore.edit { it[Keys.LAUNCHER_GRID_STYLE] = style.name }
    }

    suspend fun setLauncherStrictEduOnly(enabled: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.LAUNCHER_STRICT_EDU_ONLY] = enabled }
    }

    suspend fun setLauncherClockStyleLarge(large: Boolean) {
        context.tryAgainDataStore.edit { it[Keys.LAUNCHER_CLOCK_LARGE] = large }
    }

    suspend fun saveTimerSnapshot(snapshot: PersistedTimerSnapshot) {
        context.tryAgainDataStore.edit { prefs ->
            prefs[Keys.PERSISTED_TIMER_JSON] = encodeTimerSnapshot(snapshot)
        }
    }

    suspend fun loadTimerSnapshot(): PersistedTimerSnapshot? {
        val raw = context.tryAgainDataStore.data.map { it[Keys.PERSISTED_TIMER_JSON] }.first()
        return parseTimerSnapshot(raw)
    }

    suspend fun clearTimerSnapshot() {
        context.tryAgainDataStore.edit { prefs ->
            prefs.remove(Keys.PERSISTED_TIMER_JSON)
        }
    }

    /**
     * Cached live remote config hydrated immediately on launch with zero loading spinner,
     * silently updated in the background whenever the admin publishes a change.
     */
    val cachedLiveConfigFlow: Flow<AppLiveConfig> = context.tryAgainDataStore.data.map { prefs ->
        parseLiveConfigJson(prefs[Keys.CACHED_LIVE_CONFIG_JSON]) ?: AppLiveConfig()
    }

    suspend fun saveCachedLiveConfig(config: AppLiveConfig) {
        context.tryAgainDataStore.edit { prefs ->
            prefs[Keys.CACHED_LIVE_CONFIG_JSON] = encodeLiveConfigJson(config)
        }
    }

    private fun encodeLiveConfigJson(config: AppLiveConfig): String {
        return JSONObject().apply {
            put("configVersion", config.configVersion)
            put("liveTagline", config.liveTagline)
            put("announcementTitle", config.announcementTitle)
            put("announcementBody", config.announcementBody)
            put("announcementActive", config.announcementActive)
            put("updateNoticeLabel", config.updateNoticeLabel)
            put("defaultFocusMinutes", config.defaultFocusMinutes)
            put("gemMultiplierPercent", config.gemMultiplierPercent)
            put("socialEnabled", config.socialEnabled)
            put("launcherEnabled", config.launcherEnabled)
            put("ambientDrawerEnabled", config.ambientDrawerEnabled)
            put("maintenanceBannerEnabled", config.maintenanceBannerEnabled)
        }.toString()
    }

    private fun parseLiveConfigJson(raw: String?): AppLiveConfig? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            AppLiveConfig(
                configVersion = obj.optInt("configVersion", 1).coerceIn(1, 1_000_000),
                liveTagline = obj.optString("liveTagline", "").take(100),
                announcementTitle = obj.optString("announcementTitle", "").take(80),
                announcementBody = obj.optString("announcementBody", "").take(240),
                announcementActive = obj.optBoolean("announcementActive", false),
                updateNoticeLabel = obj.optString("updateNoticeLabel", "").take(60),
                defaultFocusMinutes = obj.optInt("defaultFocusMinutes", 25).coerceIn(5, 120),
                gemMultiplierPercent = obj.optInt("gemMultiplierPercent", 100).coerceIn(100, 300),
                socialEnabled = obj.optBoolean("socialEnabled", true),
                launcherEnabled = obj.optBoolean("launcherEnabled", true),
                ambientDrawerEnabled = obj.optBoolean("ambientDrawerEnabled", true),
                maintenanceBannerEnabled = obj.optBoolean("maintenanceBannerEnabled", false)
            )
        }.getOrNull()
    }

    private fun encodeTimerSnapshot(snapshot: PersistedTimerSnapshot): String {
        return JSONObject().apply {
            put("phase", snapshot.phase.name)
            put("subject", snapshot.subject)
            put("plannedMinutes", snapshot.plannedMinutes)
            put("focusMinutesSetting", snapshot.focusMinutesSetting)
            put("shortBreakMinutesSetting", snapshot.shortBreakMinutesSetting)
            put("longBreakMinutesSetting", snapshot.longBreakMinutesSetting)
            put("remainingSeconds", snapshot.remainingSeconds)
            put("elapsedSeconds", snapshot.elapsedSeconds)
            put("isRunning", snapshot.isRunning)
            put("isPaused", snapshot.isPaused)
            put("completedPomodorosInCycle", snapshot.completedPomodorosInCycle)
            put("activeTaskId", snapshot.activeTaskId ?: JSONObject.NULL)
            put("sessionStartedDateKey", snapshot.sessionStartedDateKey)
            put("lastPersistedEpochMs", snapshot.lastPersistedEpochMs)
            put("pausedAtEpochMs", snapshot.pausedAtEpochMs ?: JSONObject.NULL)
            put("totalPausedSeconds", snapshot.totalPausedSeconds)
            put("pauseCount", snapshot.pauseCount)
        }.toString()
    }

    private fun parseTimerSnapshot(raw: String?): PersistedTimerSnapshot? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val phaseName = obj.optString("phase", PomodoroPhase.FOCUS.name)
            val phase = PomodoroPhase.entries.firstOrNull { it.name == phaseName } ?: PomodoroPhase.FOCUS
            val activeTask = if (obj.isNull("activeTaskId")) null else obj.optString("activeTaskId").ifBlank { null }
            val pausedAt = if (obj.isNull("pausedAtEpochMs")) null else obj.optLong("pausedAtEpochMs", 0L).takeIf { it > 0L }
            PersistedTimerSnapshot(
                phase = phase,
                subject = obj.optString("subject", "Math").ifBlank { "Math" },
                plannedMinutes = obj.optInt("plannedMinutes", phase.defaultMinutes),
                focusMinutesSetting = obj.optInt("focusMinutesSetting", PomodoroPhase.FOCUS.defaultMinutes),
                shortBreakMinutesSetting = obj.optInt("shortBreakMinutesSetting", PomodoroPhase.SHORT_BREAK.defaultMinutes),
                longBreakMinutesSetting = obj.optInt("longBreakMinutesSetting", PomodoroPhase.LONG_BREAK.defaultMinutes),
                remainingSeconds = obj.optInt("remainingSeconds", phase.defaultMinutes * 60),
                elapsedSeconds = obj.optInt("elapsedSeconds", 0),
                isRunning = obj.optBoolean("isRunning", false),
                isPaused = obj.optBoolean("isPaused", false),
                completedPomodorosInCycle = obj.optInt("completedPomodorosInCycle", 0),
                activeTaskId = activeTask,
                sessionStartedDateKey = obj.optString("sessionStartedDateKey", ""),
                lastPersistedEpochMs = obj.optLong("lastPersistedEpochMs", 0L),
                pausedAtEpochMs = pausedAt,
                totalPausedSeconds = obj.optInt("totalPausedSeconds", 0),
                pauseCount = obj.optInt("pauseCount", 0)
            )
        }.getOrNull()
    }

    private fun parseSessionsJson(raw: String?): List<FocusSession> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val createdMs = obj.optLong("createdAtMs", 0L)
                    add(
                        FocusSession(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", "local_scholar"),
                            subject = obj.optString("subject", "Math"),
                            plannedMinutes = obj.optInt("plannedMinutes", 25),
                            completedMinutes = obj.optInt("completedMinutes", 25),
                            ambientSound = obj.optString("ambientSound", "Silent"),
                            xpEarned = obj.optInt("xpEarned", 50),
                            gemsEarned = obj.optInt("gemsEarned", 35),
                            dateKey = obj.optString("dateKey", ""),
                            createdAt = if (createdMs > 0L) Timestamp(Date(createdMs)) else null
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeSessionsJson(sessions: List<FocusSession>): String {
        val arr = JSONArray()
        sessions.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("userId", s.userId)
            obj.put("subject", s.subject)
            obj.put("plannedMinutes", s.plannedMinutes)
            obj.put("completedMinutes", s.completedMinutes)
            obj.put("ambientSound", s.ambientSound)
            obj.put("xpEarned", s.xpEarned)
            obj.put("gemsEarned", s.gemsEarned)
            obj.put("dateKey", s.dateKey)
            obj.put("createdAtMs", s.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseSubjectAlarmsJson(raw: String?): List<SubjectStudyAlarm> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").ifBlank { continue }
                    add(
                        SubjectStudyAlarm(
                            id = id,
                            subject = obj.optString("subject", "Math").ifBlank { "Math" },
                            hour = obj.optInt("hour", 6).coerceIn(0, 23),
                            minute = obj.optInt("minute", 0).coerceIn(0, 59),
                            durationMinutes = obj.optInt("durationMinutes", 25).coerceIn(10, 180),
                            enabled = obj.optBoolean("enabled", true),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }.sortedWith(compareBy<SubjectStudyAlarm> { it.hour }.thenBy { it.minute })
        }.getOrDefault(emptyList())
    }

    private fun encodeSubjectAlarmsJson(alarms: List<SubjectStudyAlarm>): String {
        val arr = JSONArray()
        alarms.forEach { alarm ->
            arr.put(
                JSONObject().apply {
                    put("id", alarm.id)
                    put("subject", alarm.subject)
                    put("hour", alarm.hour)
                    put("minute", alarm.minute)
                    put("durationMinutes", alarm.durationMinutes)
                    put("enabled", alarm.enabled)
                    put("note", alarm.note)
                }
            )
        }
        return arr.toString()
    }

    private fun parseScholarDetailsJson(raw: String?): ScholarProfileDetails {
        if (raw.isNullOrBlank()) return ScholarProfileDetails()
        return runCatching {
            val obj = JSONObject(raw)
            ScholarProfileDetails(
                avatarLocalPath = obj.optString("avatarLocalPath", ""),
                bio = obj.optString("bio", ""),
                schoolOrCourse = obj.optString("schoolOrCourse", ""),
                targetExamOrGoal = obj.optString("targetExamOrGoal", ""),
                favoriteSubject = obj.optString("favoriteSubject", "Math").ifBlank { "Math" },
                preferredStudyShift = obj.optString("preferredStudyShift", obj.optString("studyShift", "Morning")).ifBlank { "Morning" },
                phoneNumber = obj.optString("phoneNumber", ""),
                preferredEmail = obj.optString("preferredEmail", "")
            )
        }.getOrDefault(ScholarProfileDetails())
    }

    private fun encodeScholarDetailsJson(details: ScholarProfileDetails): String {
        return JSONObject().apply {
            put("avatarLocalPath", details.avatarLocalPath)
            put("bio", details.bio)
            put("schoolOrCourse", details.schoolOrCourse)
            put("targetExamOrGoal", details.targetExamOrGoal)
            put("favoriteSubject", details.favoriteSubject)
            put("preferredStudyShift", details.preferredStudyShift)
            put("phoneNumber", details.phoneNumber)
            put("preferredEmail", details.preferredEmail)
        }.toString()
    }
}
