package com.example.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.data.model.AppUsageEntry
import com.example.data.model.ScreenUsageUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Real Android UsageStatsManager & AppOpsManager integration for tracking daily
 * device screen time and per-app foreground usage time.
 */
object ScreenUsageTracker {

    private val excludedSystemPrefixes = listOf(
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.android.permissioncontroller"
    )

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Queries today's real foreground app usage and a 7-day daily distraction-time breakdown
     * via Android UsageStatsManager when Usage Access is granted, seamlessly merging Try Again's
     * live in-app foreground timer.
     */
    fun queryTodayUsage(
        context: Context,
        liveThisAppSecondsToday: Int
    ): ScreenUsageUiState {
        val now = System.currentTimeMillis()
        val liveAppMillis = liveThisAppSecondsToday.coerceAtLeast(0) * 1000L
        val hasPermission = hasUsageStatsPermission(context)

        if (!hasPermission) {
            return ScreenUsageUiState(
                hasUsagePermission = false,
                totalScreenTimeMillis = 0L,
                thisAppUsageMillis = liveAppMillis,
                topApps = emptyList(),
                dailyDistractionMinutesByDate = emptyMap(),
                lastUpdatedMillis = now
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ScreenUsageUiState(
                hasUsagePermission = true,
                totalScreenTimeMillis = liveAppMillis,
                thisAppUsageMillis = liveAppMillis,
                topApps = emptyList(),
                dailyDistractionMinutesByDate = emptyMap(),
                lastUpdatedMillis = now
            )

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val rawMap = runCatching {
            usageStatsManager.queryAndAggregateUsageStats(startOfDay, now)
        }.getOrDefault(emptyMap())

        val ownPkg = context.packageName
        val pm = context.packageManager

        val perAppMillis = mutableMapOf<String, Long>()
        rawMap.forEach { (pkg, stats) ->
            val timeMs = stats.totalTimeInForeground
            val isLauncherOrSystemUi = excludedSystemPrefixes.any { pkg.startsWith(it) }
            if (timeMs >= 5_000L && !isLauncherOrSystemUi) {
                perAppMillis[pkg] = timeMs
            }
        }

        // Ensure Try Again reflects the freshest live foreground duration
        val recordedOwn = perAppMillis[ownPkg] ?: 0L
        val effectiveOwn = maxOf(recordedOwn, liveAppMillis)
        if (effectiveOwn > 0L) {
            perAppMillis[ownPkg] = effectiveOwn
        }

        val totalMs = perAppMillis.values.sum()
        val maxEntryMs = (perAppMillis.values.maxOrNull() ?: 1L).coerceAtLeast(1L)

        val topEntries = perAppMillis.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (pkg, ms) ->
                val isOwn = pkg == ownPkg
                val label = if (isOwn) {
                    "Try Again"
                } else {
                    resolveAppLabel(pm, pkg)
                }
                AppUsageEntry(
                    packageName = pkg,
                    appName = label,
                    usageMillis = ms,
                    shareFraction = (ms.toFloat() / maxEntryMs.toFloat()).coerceIn(0.08f, 1f),
                    isThisApp = isOwn
                )
            }

        val dailyDistractionMap = querySevenDayDistractionMinutes(
            usageStatsManager = usageStatsManager,
            ownPkg = ownPkg,
            nowMillis = now
        )

        return ScreenUsageUiState(
            hasUsagePermission = true,
            totalScreenTimeMillis = totalMs,
            thisAppUsageMillis = effectiveOwn,
            topApps = topEntries,
            dailyDistractionMinutesByDate = dailyDistractionMap,
            lastUpdatedMillis = now
        )
    }

    /**
     * Computes real non-focus (other app) foreground minutes per day across the last 7 days.
     */
    private fun querySevenDayDistractionMinutes(
        usageStatsManager: UsageStatsManager,
        ownPkg: String,
        nowMillis: Long
    ): Map<String, Int> {
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val result = mutableMapOf<String, Int>()
        val baseCal = Calendar.getInstance()

        for (daysAgo in 6 downTo 0) {
            val dayStartCal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = dayStartCal.timeInMillis
            val endMs = if (daysAgo == 0) {
                nowMillis
            } else {
                (dayStartCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis - 1L
            }

            val dayMap = runCatching {
                usageStatsManager.queryAndAggregateUsageStats(startMs, endMs)
            }.getOrDefault(emptyMap())

            var otherAppsMs = 0L
            dayMap.forEach { (pkg, stats) ->
                val isExcluded = pkg == ownPkg || excludedSystemPrefixes.any { pkg.startsWith(it) }
                if (!isExcluded && stats.totalTimeInForeground >= 5_000L) {
                    otherAppsMs += stats.totalTimeInForeground
                }
            }
            val key = keyFormat.format(dayStartCal.time)
            result[key] = (otherAppsMs / 60_000L).toInt().coerceAtLeast(0)
        }
        return result
    }

    private fun resolveAppLabel(pm: PackageManager, packageName: String): String {
        val label = runCatching {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo)?.toString()?.trim()
        }.getOrNull()

        if (!label.isNullOrBlank()) return label

        // Clean fallback from last package segment if package isn't launcher-visible
        val lastSegment = packageName.substringAfterLast('.').ifBlank { packageName }
        return lastSegment.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}m"
            minutes > 0L -> "${minutes}m"
            totalSeconds > 0L -> "${seconds}s"
            else -> "0m"
        }
    }
}
