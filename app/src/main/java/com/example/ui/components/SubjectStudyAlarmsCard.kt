package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.SubjectStudyAlarm
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

/**
 * Multi-Task Subject Study Alarms Card on the Focus tab:
 * Lets scholars schedule separate daily alarms for Math (e.g. 6:00 AM), Science (e.g. 7:00 AM),
 * Coding, Reading, etc., toggle each on/off, or tap any alarm to load that subject directly into the timer.
 */
@Composable
fun SubjectStudyAlarmsCard(
    alarms: List<SubjectStudyAlarm>,
    subjects: List<String>,
    onAddAlarm: (subject: String, hour: Int, minute: Int, durationMinutes: Int, note: String) -> Unit,
    onToggleAlarm: (alarmId: String, enabled: Boolean) -> Unit,
    onDeleteAlarm: (alarmId: String) -> Unit,
    onLoadSubjectNow: (subject: String, durationMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current
    var showAddDialog by remember { mutableStateOf(false) }

    val activeCount = remember(alarms) { alarms.count { it.enabled } }

    if (showAddDialog) {
        AddSubjectAlarmDialog(
            subjects = subjects.ifEmpty { UserProfile.DEFAULT_SUBJECTS },
            onSave = { subject, hour, minute, duration, note ->
                showAddDialog = false
                onAddAlarm(subject, hour, minute, duration, note)
            },
            onDismiss = { showAddDialog = false }
        )
    }

    LiquidGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_study_alarms_card"),
        cornerRadius = 26.dp,
        elevated = activeCount > 0,
        accentTint = if (activeCount > 0) palette.streakColor else palette.prismCyan,
        animatedCaustics = false,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.streakColor.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                        border = BorderStroke(1.dp, palette.streakColor.copy(alpha = 0.35f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (activeCount > 0) Icons.Rounded.AlarmOn else Icons.Rounded.Alarm,
                                contentDescription = null,
                                tint = palette.streakColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.subject_alarms_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (activeCount > 0) {
                                "$activeCount active daily subject ${if (activeCount == 1) "alarm" else "alarms"}"
                            } else {
                                stringResource(R.string.subject_alarms_subtitle)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("add_subject_alarm_button")
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            systemHaptics.buttonTap()
                            tactile?.playTap()
                            showAddDialog = true
                        },
                    shape = RoundedCornerShape(50),
                    color = palette.primary,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.subject_alarms_add_cta),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.subject_alarms_add_cta),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                    }
                }
            }

            if (alarms.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = palette.chipUnselected.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, palette.glassBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.subject_alarms_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickPresetAlarmChip(
                                label = "Math · 6:00 AM",
                                onClick = { onAddAlarm("Math", 6, 0, 30, "Morning math sprint") }
                            )
                            QuickPresetAlarmChip(
                                label = "Science · 7:00 AM",
                                onClick = { onAddAlarm("Science", 7, 0, 30, "Science concepts") }
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    alarms.forEach { alarm ->
                        SubjectAlarmItemRow(
                            alarm = alarm,
                            onToggle = { enabled -> onToggleAlarm(alarm.id, enabled) },
                            onDelete = { onDeleteAlarm(alarm.id) },
                            onStartNow = { onLoadSubjectNow(alarm.subject, alarm.durationMinutes) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPresetAlarmChip(
    label: String,
    onClick: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable {
                tactile?.playTap()
                onClick()
            },
        shape = RoundedCornerShape(50),
        color = palette.primarySoft,
        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = palette.primaryLip,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = palette.primaryLip
            )
        }
    }
}

@Composable
private fun SubjectAlarmItemRow(
    alarm: SubjectStudyAlarm,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onStartNow: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_alarm_row_${alarm.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (alarm.enabled) palette.glassFillElevated else palette.chipUnselected.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            if (alarm.enabled) palette.streakColor.copy(alpha = 0.36f) else palette.glassBorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        tactile?.playTap()
                        onStartNow()
                    }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = alarm.formattedTime,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = if (alarm.enabled) palette.textPrimary else palette.textMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (alarm.enabled) palette.primarySoft else palette.chipUnselected
                    ) {
                        Text(
                            text = alarm.subject,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (alarm.enabled) palette.primaryLip else palette.textMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = buildString {
                        append("Daily · ${alarm.durationMinutes}m sprint")
                        if (alarm.note.isNotBlank()) append(" · ${alarm.note}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    tactile?.playTap()
                    onStartNow()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Load ${alarm.subject} on focus timer",
                    tint = palette.primaryLip,
                    modifier = Modifier.size(20.dp)
                )
            }

            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = palette.streakColor,
                    uncheckedThumbColor = palette.textMuted,
                    uncheckedTrackColor = palette.chipUnselected
                ),
                modifier = Modifier.testTag("subject_alarm_switch_${alarm.id}")
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("subject_alarm_delete_${alarm.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${alarm.subject} alarm",
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddSubjectAlarmDialog(
    subjects: List<String>,
    onSave: (subject: String, hour: Int, minute: Int, durationMinutes: Int, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "Math") }
    var customSubject by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(6) }
    var minute by remember { mutableIntStateOf(0) }
    var durationMinutes by remember { mutableIntStateOf(30) }
    var note by remember { mutableStateOf("") }

    val formattedPreview = remember(hour, minute) {
        SubjectStudyAlarm(hour = hour, minute = minute).formattedTime
    }

    Dialog(onDismissRequest = onDismiss) {
        MorphismSurface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_subject_alarm_dialog"),
            cornerRadius = 28.dp,
            elevated = true,
            accentTint = palette.streakColor,
            frostedBlur = true,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = palette.streakColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Schedule Subject Alarm",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }

                Text(
                    text = "Pick Subject",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textSecondary
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects) { subj ->
                        MorphismChip(
                            label = subj,
                            selected = customSubject.isBlank() && selectedSubject == subj,
                            onClick = {
                                selectedSubject = subj
                                customSubject = ""
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = customSubject,
                    onValueChange = { customSubject = it.take(24) },
                    label = { Text("Or custom subject (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.glassBorderSubtle,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    )
                )

                // Time Picker Stepper Row
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.chipUnselected.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, palette.glassBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formattedPreview,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    tactile?.playTap()
                                    hour = (hour + 23) % 24
                                }) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease hour", tint = palette.textPrimary)
                                }
                                Text(
                                    text = "Hour",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textSecondary
                                )
                                IconButton(onClick = {
                                    tactile?.playTap()
                                    hour = (hour + 1) % 24
                                }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase hour", tint = palette.textPrimary)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    tactile?.playTap()
                                    minute = (minute + 55) % 60
                                }) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease minute", tint = palette.textPrimary)
                                }
                                Text(
                                    text = "Min",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textSecondary
                                )
                                IconButton(onClick = {
                                    tactile?.playTap()
                                    minute = (minute + 5) % 60
                                }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase minute", tint = palette.textPrimary)
                                }
                            }
                        }
                    }
                }

                // Sprint Duration Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sprint:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textSecondary
                    )
                    listOf(25, 30, 45, 60).forEach { mins ->
                        MorphismChip(
                            label = "${mins}m",
                            selected = durationMinutes == mins,
                            onClick = { durationMinutes = mins }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(60) },
                    label = { Text("Study reminder note (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.glassBorderSubtle,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Cancel", color = palette.textSecondary, fontWeight = FontWeight.Bold)
                    }
                    MorphismActionButton(
                        text = "SAVE ALARM",
                        icon = Icons.Rounded.AlarmOn,
                        onClick = {
                            val finalSubject = customSubject.trim().ifBlank { selectedSubject }
                            onSave(finalSubject, hour, minute, durationMinutes, note)
                        },
                        containerColor = palette.streakColor,
                        lipColor = palette.streakLip,
                        height = 46.dp,
                        cornerRadius = 15.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "confirm_save_subject_alarm"
                    )
                }
            }
        }
    }
}
