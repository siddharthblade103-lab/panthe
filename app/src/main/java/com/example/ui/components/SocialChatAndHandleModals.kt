package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.PublicStudyProfile
import com.example.data.model.StudyChatMessage
import com.example.data.model.StudyChatUiState
import com.example.data.model.StudyLiveStatus
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.text.SimpleDateFormat
import java.util.Locale

private val HANDLE_PREVIEW_REGEX = Regex("^[a-z0-9_]{3,30}$")

private val QUICK_STUDY_PROMPTS = listOf(
    "Let's start a 25m focus sprint!",
    "Keep that streak burning!",
    "Taking a quick break — join soon?",
    "Great focus progress today!"
)

/**
 * Liquid Glass dialog letting users customize their unique @handle directly from the Social tab.
 */
@Composable
fun EditStudyHandleDialog(
    currentHandle: String,
    isUpdating: Boolean,
    onSaveHandle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    var draft by remember(currentHandle) { mutableStateOf(currentHandle) }
    val normalized = remember(draft) {
        draft.trim().removePrefix("@").lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]"), "")
            .take(30)
    }
    val isValid = HANDLE_PREVIEW_REGEX.matches(normalized)
    val isChanged = normalized != currentHandle.lowercase(Locale.US)

    Dialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .testTag("edit_study_handle_dialog"),
                cornerRadius = 30.dp,
                accentTint = palette.prismCyan,
                elevated = true,
                animatedCaustics = true,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(palette.primarySoft)
                                    .border(1.dp, palette.primary.copy(alpha = 0.42f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AlternateEmail,
                                    contentDescription = null,
                                    tint = palette.primaryLip,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.social_edit_handle_title),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.social_edit_handle_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                tactile?.playTap()
                                if (!isUpdating) onDismiss()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close edit handle dialog",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = draft,
                        onValueChange = { raw ->
                            draft = raw.lowercase(Locale.US)
                                .removePrefix("@")
                                .replace(Regex("[^a-z0-9_]"), "")
                                .take(30)
                        },
                        prefix = {
                            Text(
                                text = "@",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.primary
                            )
                        },
                        placeholder = { Text("your_handle") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValid) palette.primary else palette.coralAccent,
                            unfocusedBorderColor = palette.glassBorderHighlight,
                            focusedContainerColor = palette.glassFillElevated,
                            unfocusedContainerColor = palette.glassFillElevated,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isValid && !isUpdating) {
                                    onSaveHandle(normalized)
                                    onDismiss()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_handle_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isValid) {
                                "Preview: @$normalized"
                            } else {
                                "Use 3–30 lowercase letters, numbers, or _"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isValid) palette.primaryLip else palette.coralAccent
                        )
                        Text(
                            text = "${normalized.length}/30",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !isUpdating) {
                                    tactile?.playTap()
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = palette.glassFillElevated,
                            border = BorderStroke(1.dp, palette.glassBorderSubtle)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                            }
                        }

                        MorphismActionButton(
                            text = if (isUpdating) "SAVING…" else stringResource(R.string.social_save_handle_cta),
                            icon = Icons.Rounded.CheckCircle,
                            onClick = {
                                onSaveHandle(normalized)
                                onDismiss()
                            },
                            enabled = isValid && isChanged && !isUpdating,
                            containerColor = palette.primary,
                            lipColor = palette.primaryLip,
                            height = 48.dp,
                            cornerRadius = 16.dp,
                            modifier = Modifier.weight(1.35f),
                            testTag = "save_study_handle_button"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-height Liquid-Glass 1:1 Study Friend Direct Chat modal synced live with Firestore.
 */
@Composable
fun FriendDirectChatSheet(
    chatState: StudyChatUiState,
    currentUid: String,
    onSendMessage: (String) -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit
) {
    val friend: PublicStudyProfile = chatState.activeFriend ?: return
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    var draft by remember(friend.userId) { mutableStateOf("") }
    val listState = rememberLazyListState()

    BackHandler(onBack = onDismiss)

    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.lastIndex)
        }
    }

    val statusColor = when (friend.liveStatus) {
        StudyLiveStatus.FOCUSING -> palette.primary
        StudyLiveStatus.BREAK -> palette.gemColor
        StudyLiveStatus.READY -> palette.prismViolet
    }

    val initials = remember(friend.displayName) {
        friend.displayName.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "S" }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (palette.isDark) 0.62f else 0.36f))
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 22.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LiquidGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .fillMaxHeight(0.86f)
                    .testTag("friend_direct_chat_sheet"),
                cornerRadius = 30.dp,
                elevated = true,
                accentTint = statusColor,
                animatedCaustics = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                statusColor.copy(alpha = 0.30f),
                                                palette.prismCyan.copy(alpha = 0.24f)
                                            )
                                        )
                                    )
                                    .border(1.4.dp, statusColor.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "@${friend.handle}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = palette.streakColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${friend.currentStreak}d · ${friend.liveStatus.label}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                tactile?.playTap()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_friend_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close study chat",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Error notice if present
                    AnimatedVisibility(
                        visible = !chatState.errorMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        chatState.errorMessage?.let { err ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clickable { onClearError() },
                                shape = RoundedCornerShape(14.dp),
                                color = palette.coralAccent.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.coralAccent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Live Messages Stream
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(palette.glassFill.copy(alpha = if (palette.isDark) 0.42f else 0.56f))
                            .border(1.dp, palette.glassBorderSubtle, RoundedCornerShape(22.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        when {
                            chatState.isLoading && chatState.messages.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = palette.primary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            chatState.messages.isEmpty() -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = palette.primary.copy(alpha = 0.72f),
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(R.string.social_chat_empty_title, friend.displayName),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = NunitoDisplayFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = palette.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.social_chat_empty_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(
                                        items = chatState.messages,
                                        key = { it.id.ifBlank { "${it.senderUid}_${it.createdAt?.seconds}_${it.text.hashCode()}" } }
                                    ) { message ->
                                        StudyChatBubble(
                                            message = message,
                                            isMine = message.senderUid == currentUid
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Study Cheer Prompts
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(QUICK_STUDY_PROMPTS) { prompt ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = !chatState.isSending) {
                                        onSendMessage(prompt)
                                    },
                                shape = RoundedCornerShape(50),
                                color = palette.glassFillElevated,
                                border = BorderStroke(1.dp, palette.glassBorderHighlight)
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Composer Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(500) },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.social_chat_input_placeholder, friend.displayName),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.glassBorderHighlight,
                                focusedContainerColor = palette.glassFillElevated,
                                unfocusedContainerColor = palette.glassFillElevated,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (draft.isNotBlank() && !chatState.isSending) {
                                        onSendMessage(draft)
                                        draft = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("friend_chat_message_input")
                        )

                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("friend_chat_send_button")
                                .clip(CircleShape)
                                .clickable(enabled = draft.isNotBlank() && !chatState.isSending) {
                                    val toSend = draft
                                    draft = ""
                                    onSendMessage(toSend)
                                },
                            shape = CircleShape,
                            color = if (draft.isNotBlank() && !chatState.isSending) {
                                palette.primary
                            } else {
                                palette.chipUnselected
                            },
                            border = BorderStroke(
                                1.dp,
                                if (draft.isNotBlank()) Color.White.copy(alpha = 0.5f) else palette.glassBorderSubtle
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (chatState.isSending) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = stringResource(R.string.social_chat_send_desc),
                                        tint = if (draft.isNotBlank()) Color.White else palette.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyChatBubble(
    message: StudyChatMessage,
    isMine: Boolean
) {
    val palette = LocalTryAgainPalette.current
    val timeLabel = remember(message.createdAt) {
        val date = message.createdAt?.toDate() ?: return@remember "Sending…"
        SimpleDateFormat("h:mm a", Locale.US).format(date)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 6.dp,
                bottomEnd = if (isMine) 6.dp else 18.dp
            ),
            color = if (isMine) {
                palette.primary
            } else {
                palette.glassFillElevated
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (isMine) {
                    Color.White.copy(alpha = 0.35f)
                } else {
                    palette.glassBorderHighlight
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) Color.White else palette.textPrimary
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isMine) Color.White.copy(alpha = 0.78f) else palette.textMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
