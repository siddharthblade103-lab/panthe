package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.StreakReminderUiStatus
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

private data class ReminderPresetSlot(
    val hour: Int,
    val minute: Int,
    val label: String
)

private val DEFAULT_REMINDER_PRESETS = listOf(
    ReminderPresetSlot(18, 0, "6:00 PM"),
    ReminderPresetSlot(19, 30, "7:30 PM"),
    ReminderPresetSlot(20, 0, "8:00 PM"),
    ReminderPresetSlot(21, 0, "9:00 PM"),
    ReminderPresetSlot(22, 0, "10:00 PM")
)

/**
 * Liquid-Glass configuration card for Firebase Cloud Messaging daily study streak push alerts.
 * Allows enabling/disabling daily reminders, picking any custom alert time, checking live
 * streak safety status, granting Android 13+ notification permission, and sending a test alert.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStreakReminderCard(
    status: StreakReminderUiStatus,
    currentStreak: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectTime: (hour: Int, minute: Int) -> Unit,
    onRequestPermission: () -> Unit,
    onSendTestAlert: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    var showTimePickerDialog by remember { mutableStateOf(false) }

    if (showTimePickerDialog) {
        StreakReminderTimePickerDialog(
            initialHour = status.hour,
            initialMinute = status.minute,
            onConfirm = { h, m ->
                showTimePickerDialog = false
                onSelectTime(h, m)
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    val accentColor = when {
        !status.enabled -> palette.textMuted
        status.hasStartedSessionToday -> palette.primary
        else -> palette.streakColor
    }

    val statusPillText = when {
        !status.enabled -> stringResource(R.string.streak_reminder_status_paused)
        status.hasStartedSessionToday -> stringResource(R.string.streak_reminder_status_safe)
        else -> stringResource(R.string.streak_reminder_status_armed, status.formattedTime)
    }

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (compact) "focus_streak_reminder_card" else "profile_streak_reminder_card"),
        cornerRadius = 26.dp,
        elevated = true,
        accentTint = accentColor,
        shimmer = status.enabled && !status.hasStartedSessionToday,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header Row: Glowing Bell + Title + Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.28f),
                                    accentColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.2.dp, accentColor.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (status.enabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                        contentDescription = stringResource(R.string.streak_reminder_title),
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.streak_reminder_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (status.hasStartedSessionToday) {
                            stringResource(R.string.streak_reminder_subtitle_safe, status.formattedTime)
                        } else {
                            stringResource(
                                R.string.streak_reminder_subtitle_armed,
                                status.formattedTime,
                                currentStreak.coerceAtLeast(1)
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = status.enabled,
                    onCheckedChange = { enabled ->
                        tactile?.playToggle(enabled)
                        onToggleEnabled(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.streakColor,
                        uncheckedThumbColor = palette.textMuted,
                        uncheckedTrackColor = palette.chipUnselected
                    ),
                    modifier = Modifier.testTag(if (compact) "focus_toggle_streak_reminder" else "toggle_streak_reminder")
                )
            }

            // 2. Live Status Pill + FCM Push Sync Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (status.hasStartedSessionToday) {
                                Icons.Rounded.VerifiedUser
                            } else {
                                Icons.Rounded.AccessTime
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = statusPillText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }

                if (status.enabled) {
                    val fcmBadgeColor = if (status.fcmConnected || status.cloudSynced) palette.gemColor else palette.textSecondary
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = fcmBadgeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, fcmBadgeColor.copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = fcmBadgeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = when {
                                    status.isSyncingFcm -> stringResource(R.string.streak_reminder_fcm_syncing)
                                    status.cloudSynced -> stringResource(R.string.streak_reminder_fcm_cloud_synced)
                                    status.fcmConnected -> stringResource(R.string.streak_reminder_fcm_ready)
                                    else -> stringResource(R.string.streak_reminder_fcm_device_ready)
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = fcmBadgeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Permission Prompt Banner (Android 13+ when POST_NOTIFICATIONS is not granted)
            AnimatedVisibility(
                visible = status.enabled && !status.hasNotificationPermission,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.coralAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = palette.coralAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.streak_reminder_permission_needed),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.coralAccent,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    tactile?.playTap()
                                    onRequestPermission()
                                }
                                .testTag("streak_reminder_permission_button")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.streak_reminder_allow_cta),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 4. Time Picker Row + Quick Presets + Test Alert Action
            AnimatedVisibility(
                visible = status.enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Time selector glass bar
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.glassFill,
                        border = BorderStroke(1.dp, palette.glassBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                tactile?.playTap()
                                showTimePickerDialog = true
                            }
                            .testTag(if (compact) "focus_streak_reminder_time_button" else "streak_reminder_time_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = palette.streakColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.streak_reminder_time_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = status.formattedTime,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = NunitoDisplayFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = palette.textPrimary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.primarySoft
                            ) {
                                Text(
                                    text = stringResource(R.string.streak_reminder_change_time),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.primaryLip,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Quick Time Presets
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DEFAULT_REMINDER_PRESETS) { preset ->
                            MorphismChip(
                                label = preset.label,
                                selected = status.hour == preset.hour && status.minute == preset.minute,
                                onClick = {
                                    tactile?.playTap()
                                    onSelectTime(preset.hour, preset.minute)
                                },
                                testTag = "streak_preset_${preset.hour}_${preset.minute}"
                            )
                        }
                    }

                    if (!compact) {
                        // FCM Topic info & Test Push Alert Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.streak_reminder_fcm_topic_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                                Text(
                                    text = status.fcmTopic.ifBlank { "daily_streak_reminders" },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            MorphismActionButton(
                                text = stringResource(R.string.streak_reminder_test_cta),
                                icon = Icons.Rounded.Send,
                                onClick = {
                                    tactile?.playTap()
                                    onSendTestAlert()
                                },
                                containerColor = palette.streakColor,
                                lipColor = palette.streakLip,
                                height = 44.dp,
                                cornerRadius = 14.dp,
                                modifier = Modifier.width(148.dp),
                                testTag = "streak_reminder_test_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreakReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val pickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        MorphismSurface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("streak_reminder_time_picker_dialog"),
            cornerRadius = 28.dp,
            elevated = true,
            accentTint = palette.streakColor,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.streak_reminder_picker_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = palette.textPrimary
                )
                Text(
                    text = stringResource(R.string.streak_reminder_picker_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )

                TimePicker(
                    state = pickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = palette.glassFill,
                        selectorColor = palette.streakColor,
                        containerColor = palette.glassFillElevated,
                        periodSelectorBorderColor = palette.glassBorderHighlight,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = palette.textPrimary,
                        periodSelectorSelectedContainerColor = palette.streakColor.copy(alpha = 0.22f),
                        periodSelectorUnselectedContainerColor = palette.glassFill,
                        periodSelectorSelectedContentColor = palette.streakColor,
                        periodSelectorUnselectedContentColor = palette.textSecondary,
                        timeSelectorSelectedContainerColor = palette.streakColor.copy(alpha = 0.20f),
                        timeSelectorUnselectedContainerColor = palette.glassFill,
                        timeSelectorSelectedContentColor = palette.textPrimary,
                        timeSelectorUnselectedContentColor = palette.textSecondary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("streak_picker_cancel")
                    ) {
                        Text(
                            text = "Cancel",
                            color = palette.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    MorphismActionButton(
                        text = stringResource(R.string.streak_reminder_save_time),
                        onClick = { onConfirm(pickerState.hour, pickerState.minute) },
                        containerColor = palette.streakColor,
                        lipColor = palette.streakLip,
                        height = 46.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.width(142.dp),
                        testTag = "streak_picker_save"
                    )
                }
            }
        }
    }
}
