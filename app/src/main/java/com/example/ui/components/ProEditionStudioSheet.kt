package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ProAiStudioUiState
import com.example.data.model.ProUnlockStatus
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.util.Locale

/**
 * Un-Bypassable 500-Hour Pro Edition Vault Card for Rewards Shop:
 * Strictly checks verified accumulated study hours >= 500.0 (30,000 minutes).
 * Cannot be purchased with Gems or bypassed by any toggle.
 */
@Composable
fun ProEditionVaultCard(
    status: ProUnlockStatus,
    onOpenProStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val context = LocalContext.current
    val goldAccent = Color(0xFFF59E0B)
    val goldLip = Color(0xFFD97706)

    MorphismSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pro_edition_vault_card"),
        cornerRadius = 28.dp,
        elevated = true,
        shimmer = status.isUnlocked,
        frostedBlur = true,
        accentTint = if (status.isUnlocked) goldAccent else palette.prismViolet,
        contentPadding = PaddingValues(18.dp)
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
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = if (status.isUnlocked) {
                                        listOf(goldAccent, Color(0xFFF97316))
                                    } else {
                                        listOf(palette.prismViolet, palette.gemColor)
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (status.isUnlocked) Icons.Rounded.WorkspacePremium else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.pro_edition_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (status.isUnlocked) {
                                "500+ Verified Study Hours Mastered!"
                            } else {
                                String.format(
                                    Locale.US,
                                    "%.1f / %d.0 hrs completed (%.1f hrs left)",
                                    status.completedHours,
                                    status.requiredHours,
                                    status.remainingHours
                                )
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (status.isUnlocked) goldAccent else palette.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (status.isUnlocked) goldAccent.copy(alpha = 0.20f) else palette.chipUnselected,
                    border = BorderStroke(
                        1.dp,
                        if (status.isUnlocked) goldAccent.copy(alpha = 0.55f) else palette.glassBorderSubtle
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (status.isUnlocked) R.string.pro_edition_unlocked_badge
                            else R.string.pro_edition_locked_badge
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp
                        ),
                        color = if (status.isUnlocked) goldLip else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bar toward 500 Verified Study Hours
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.chipUnselected)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(status.progressFraction.coerceAtLeast(if (status.verifiedStudyMinutes > 0) 0.02f else 0f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(palette.prismViolet, goldAccent)
                            )
                        )
                )
            }

            Text(
                text = stringResource(R.string.pro_edition_unlock_rule),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )

            // Pro Perks Preview Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProPerkPill(
                    icon = Icons.Rounded.AutoAwesome,
                    label = "AI Tutor",
                    unlocked = status.isUnlocked,
                    modifier = Modifier.weight(1f)
                )
                ProPerkPill(
                    icon = Icons.Rounded.MenuBook,
                    label = "Notes & Q&A",
                    unlocked = status.isUnlocked,
                    modifier = Modifier.weight(1f)
                )
                ProPerkPill(
                    icon = Icons.Rounded.Share,
                    label = "Share App",
                    unlocked = status.isUnlocked,
                    modifier = Modifier.weight(1f)
                )
            }

            if (status.isUnlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MorphismActionButton(
                        text = stringResource(R.string.pro_edition_open_studio),
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = onOpenProStudio,
                        containerColor = goldAccent,
                        lipColor = goldLip,
                        height = 46.dp,
                        cornerRadius = 15.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "open_pro_studio_button"
                    )
                    MorphismActionButton(
                        text = "SHARE",
                        icon = Icons.Rounded.Share,
                        onClick = { shareTryAgainApp(context) },
                        containerColor = palette.primary,
                        lipColor = palette.primaryLip,
                        height = 46.dp,
                        cornerRadius = 15.dp,
                        modifier = Modifier.width(116.dp),
                        testTag = "pro_share_app_button"
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = palette.chipUnselected.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, palette.glassBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = palette.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Keep logging real focus sprints — Pro features unlock automatically at 500.0 hours.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProPerkPill(
    icon: ImageVector,
    label: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (unlocked) palette.primarySoft else palette.glassFillElevated,
        border = BorderStroke(
            1.dp,
            if (unlocked) palette.primary.copy(alpha = 0.42f) else palette.glassBorderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (unlocked) palette.primaryLip else palette.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (unlocked) palette.textPrimary else palette.textSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Pro AI Notes, Practice Questions, AI Tutor & App Share Modal.
 * Hard-guarded: renders only if `status.isUnlocked == true`.
 */
@Composable
fun ProEditionStudioDialog(
    status: ProUnlockStatus,
    subjects: List<String>,
    aiState: ProAiStudioUiState,
    onGenerateNotes: (subject: String, topic: String) -> Unit,
    onAskTutor: (subject: String, question: String) -> Unit,
    onClearState: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!status.isUnlocked) return

    val palette = LocalTryAgainPalette.current
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Notes & Q&A, 1 = AI Tutor
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: UserProfile.DEFAULT_SUBJECTS.first()) }
    var topicInput by remember { mutableStateOf("") }
    var questionInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        MorphismSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 660.dp)
                .testTag("pro_edition_studio_dialog"),
            cornerRadius = 28.dp,
            elevated = true,
            frostedBlur = true,
            accentTint = Color(0xFFF59E0B),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "500h Pro AI Studio",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = NunitoDisplayFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = palette.textPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Pro Studio",
                            tint = palette.textSecondary
                        )
                    }
                }

                SlidingGlassSegmentedBar(
                    labels = listOf("Notes & Q&A", "AI Tutor"),
                    selectedIndex = activeTab,
                    onSelect = {
                        activeTab = it
                        onClearState()
                    },
                    tagPrefix = "pro_studio_tab"
                )

                // Subject quick pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.take(4).forEach { subj ->
                        MorphismChip(
                            label = subj,
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj }
                        )
                    }
                }

                if (activeTab == 0) {
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        label = { Text("Chapter or topic (e.g. Quadratic Equations, Photosynthesis)") },
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
                    MorphismActionButton(
                        text = "GENERATE NOTES & QUESTIONS",
                        icon = Icons.Rounded.Quiz,
                        onClick = { onGenerateNotes(selectedSubject, topicInput) },
                        enabled = aiState !is ProAiStudioUiState.Loading,
                        containerColor = Color(0xFFF59E0B),
                        lipColor = Color(0xFFD97706),
                        height = 46.dp,
                        cornerRadius = 15.dp,
                        testTag = "pro_generate_notes_button"
                    )
                } else {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        label = { Text("Ask your Pro AI Tutor anything…") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.glassBorderSubtle,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        )
                    )
                    MorphismActionButton(
                        text = "ASK PRO AI TUTOR",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = { onAskTutor(selectedSubject, questionInput) },
                        enabled = aiState !is ProAiStudioUiState.Loading,
                        containerColor = palette.primary,
                        lipColor = palette.primaryLip,
                        height = 46.dp,
                        cornerRadius = 15.dp,
                        testTag = "pro_ask_tutor_button"
                    )
                }

                when (aiState) {
                    is ProAiStudioUiState.Loading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.4.dp,
                                color = palette.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = aiState.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }

                    is ProAiStudioUiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.coralAccent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.coralAccent,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    is ProAiStudioUiState.NotesReady -> {
                        val pack = aiState.result
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.glassFillElevated,
                            border = BorderStroke(1.dp, palette.glassBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${pack.subject}: ${pack.topic}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = pack.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                                if (pack.keyPoints.isNotEmpty()) {
                                    Text(
                                        text = "Key Revision Points",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = palette.primaryLip
                                    )
                                    pack.keyPoints.forEach { pt ->
                                        Text(
                                            text = "• $pt",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                                if (pack.practiceQuestions.isNotEmpty()) {
                                    Text(
                                        text = "Ready-Made Practice Questions",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFFF59E0B)
                                    )
                                    pack.practiceQuestions.forEachIndexed { idx, qa ->
                                        Text(
                                            text = "Q${idx + 1}. ${qa.question}\nAns: ${qa.answer}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is ProAiStudioUiState.TutorAnswer -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.glassFillElevated,
                            border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = aiState.question,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = palette.primaryLip
                                )
                                Text(
                                    text = aiState.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }

                    ProAiStudioUiState.Idle -> Unit
                }

                MorphismActionButton(
                    text = stringResource(R.string.pro_edition_share_app).uppercase(),
                    icon = Icons.Rounded.Share,
                    onClick = { shareTryAgainApp(context) },
                    containerColor = palette.gemColor,
                    lipColor = palette.gemLip,
                    height = 44.dp,
                    cornerRadius = 14.dp,
                    testTag = "pro_dialog_share_app_button"
                )
            }
        }
    }
}

fun shareTryAgainApp(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Study with me on Try Again!")
        putExtra(
            Intent.EXTRA_TEXT,
            "I unlocked the 500-Hour Pro Edition on Try Again! Join me for deep Pomodoro study sessions, live study circles, and daily streak challenges."
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Try Again"))
}
