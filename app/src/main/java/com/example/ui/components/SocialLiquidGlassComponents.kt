package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.BlockedUser
import com.example.data.model.FriendLiveCardState
import com.example.data.model.PublicStudyProfile
import com.example.data.model.StudyChallenge
import com.example.data.model.StudyLiveStatus
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic Apple-inspired Liquid Glass optical lens container:
 * - Soft hardware-blurred caustic refraction plane behind content
 * - Curved upper optical specular dome highlight (liquid meniscus reflection)
 * - Iridescent chromatic dispersion rim (white -> cyan -> violet)
 * - Subtle inner bevel depth for a tactile glass-pane finish
 */
@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    accentTint: Color? = null,
    elevated: Boolean = true,
    animatedCaustics: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val phase = if (animatedCaustics) {
        val infinite = rememberInfiniteTransition(label = "liquidGlassPanel")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "causticDrift"
        ).value
    } else 0.9f

    val shimmerSweep = if (animatedCaustics && elevated) {
        val infinite = rememberInfiniteTransition(label = "liquidGlassShimmer")
        infinite.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerGlide"
        ).value
    } else -2f

    val baseTint = if (palette.isDark) {
        if (elevated) palette.glassFillElevated.copy(alpha = 0.50f) else palette.glassFill.copy(alpha = 0.42f)
    } else {
        if (elevated) Color.White.copy(alpha = 0.56f) else Color.White.copy(alpha = 0.46f)
    }

    val prismaticBorder = remember(palette.isDark, accentTint, palette.prismCyan, palette.prismViolet) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (palette.isDark) 0.58f else 0.98f),
                palette.prismCyan.copy(alpha = if (palette.isDark) 0.40f else 0.44f),
                (accentTint ?: palette.prismViolet).copy(alpha = if (palette.isDark) 0.36f else 0.34f),
                Color.White.copy(alpha = if (palette.isDark) 0.24f else 0.78f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 20.dp else 10.dp,
                shape = shape,
                ambientColor = palette.shadowAmbient,
                spotColor = (accentTint ?: palette.prismCyan).copy(alpha = if (palette.isDark) 0.32f else 0.18f)
            )
            .clip(shape)
    ) {
        // Layer 1: Blurred refractive liquid caustic backdrop
        val orbA = accentTint ?: palette.primary
        val orbB = palette.prismCyan
        val orbC = palette.prismViolet
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 22.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
        ) {
            val alphaMain = if (palette.isDark) 0.30f else 0.24f
            val alphaAlt = if (palette.isDark) 0.24f else 0.18f

            val p1 = Offset(
                x = size.width * (0.18f + 0.11f * cos(phase)),
                y = size.height * (0.14f + 0.08f * sin(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbA.copy(alpha = alphaMain), Color.Transparent),
                    center = p1,
                    radius = size.width * 0.68f
                ),
                radius = size.width * 0.68f,
                center = p1
            )

            val p2 = Offset(
                x = size.width * (0.85f + 0.09f * sin(phase * 0.8f)),
                y = size.height * (0.78f + 0.08f * cos(phase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orbB.copy(alpha = alphaAlt),
                        orbC.copy(alpha = alphaAlt * 0.6f),
                        Color.Transparent
                    ),
                    center = p2,
                    radius = size.width * 0.66f
                ),
                radius = size.width * 0.66f,
                center = p2
            )
        }

        // Layer 2: Translucent glass fill + curved specular liquid dome + crisp border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(baseTint)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.18f else 0.62f),
                            palette.prismCyan.copy(alpha = if (palette.isDark) 0.04f else 0.05f),
                            (accentTint ?: palette.prismViolet).copy(alpha = if (palette.isDark) 0.10f else 0.06f)
                        )
                    )
                )
                .drawWithContent {
                    // Upper liquid glass specular dome sheen behind content
                    val domeHeight = (size.height * 0.42f).coerceAtMost(78.dp.toPx())
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.56f),
                                Color.White.copy(alpha = 0f)
                            ),
                            startY = 0f,
                            endY = domeHeight
                        ),
                        topLeft = Offset(3.dp.toPx(), 2.dp.toPx()),
                        size = Size(
                            width = (size.width - 6.dp.toPx()).coerceAtLeast(0f),
                            height = domeHeight
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx() * 0.85f, cornerRadius.toPx() * 0.85f)
                    )

                    drawContent()

                    // Crisp specular top quartz rim
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (palette.isDark) 0.55f else 0.96f),
                                palette.prismCyan.copy(alpha = if (palette.isDark) 0.34f else 0.48f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.06f, 1.3.dp.toPx()),
                        end = Offset(size.width * 0.94f, 1.3.dp.toPx()),
                        strokeWidth = 1.6.dp.toPx()
                    )

                    if (shimmerSweep > -1.5f) {
                        val bandX = size.width * shimmerSweep
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    palette.prismCyan.copy(alpha = if (palette.isDark) 0.07f else 0.13f),
                                    Color.White.copy(alpha = if (palette.isDark) 0.11f else 0.30f),
                                    palette.prismViolet.copy(alpha = if (palette.isDark) 0.05f else 0.10f),
                                    Color.Transparent
                                ),
                                start = Offset(bandX - size.width * 0.25f, 0f),
                                end = Offset(bandX + size.width * 0.25f, size.height)
                            )
                        )
                    }
                }
                .border(width = 1.4.dp, brush = prismaticBorder, shape = shape)
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Signature Liquid Glass hero command card for the Social Study Circle tab:
 * Combines live presence orb, Private Study Mode indicator, @handle Copy & Share pills,
 * and a 3-column Liquid Glass live circle telemetry strip.
 */
@Composable
fun SocialCircleLiquidHero(
    myHandle: String,
    myLiveStatus: StudyLiveStatus,
    privateStudyMode: Boolean,
    isSyncing: Boolean,
    todayFocusMinutes: Int,
    streakDays: Int,
    friendsCount: Int,
    focusingFriendsCount: Int,
    circleTodayMinutes: Int,
    onCopyHandle: () -> Unit,
    onShareHandle: () -> Unit,
    onEditHandle: () -> Unit,
    onOpenSafetyShield: () -> Unit,
    isUpdatingHandle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val presenceColor = when {
        privateStudyMode -> palette.prismViolet
        myLiveStatus == StudyLiveStatus.FOCUSING -> palette.primary
        myLiveStatus == StudyLiveStatus.BREAK -> palette.gemColor
        else -> palette.prismCyan
    }

    LiquidGlassPanel(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 30.dp,
        accentTint = presenceColor,
        elevated = true,
        animatedCaustics = true,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Identity & Privacy Status Row
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        presenceColor.copy(alpha = 0.28f),
                                        palette.prismCyan.copy(alpha = 0.20f)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Color.White.copy(alpha = if (palette.isDark) 0.45f else 0.94f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (privateStudyMode) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = "Private Study Mode active",
                                tint = palette.prismViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            LiquidCrystalSyncDot(
                                color = presenceColor,
                                isLivePulse = myLiveStatus == StudyLiveStatus.FOCUSING,
                                isSyncing = isSyncing,
                                dotSize = 9.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (privateStudyMode) {
                                "Private Study Mode Active"
                            } else {
                                stringResource(R.string.social_your_card_label)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (privateStudyMode) palette.prismViolet else palette.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isUpdatingHandle) {
                                    tactile?.playTap()
                                    onEditHandle()
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isUpdatingHandle) "Updating @handle…"
                                else if (myHandle.isNotBlank()) "@$myHandle"
                                else "Tap to set @handle",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .testTag("my_study_handle_text")
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.social_edit_handle_cta),
                                tint = palette.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Privacy Shield pill
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                        .testTag("hero_safety_shield_pill")
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            tactile?.playTap()
                            onOpenSafetyShield()
                        },
                    shape = RoundedCornerShape(50),
                    color = if (privateStudyMode) {
                        palette.prismViolet.copy(alpha = if (palette.isDark) 0.28f else 0.16f)
                    } else {
                        palette.primarySoft
                    },
                    border = BorderStroke(
                        1.dp,
                        if (privateStudyMode) palette.prismViolet.copy(alpha = 0.48f) else palette.primary.copy(alpha = 0.36f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = stringResource(R.string.social_safety_button_desc),
                            tint = if (privateStudyMode) palette.prismViolet else palette.primaryLip,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (privateStudyMode) {
                                stringResource(R.string.social_safety_private_pill)
                            } else {
                                stringResource(R.string.social_safety_pill)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (privateStudyMode) palette.prismViolet else palette.primaryLip
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Liquid Glass Circle Telemetry Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidTelemetryTile(
                    label = "Live Now",
                    value = "$focusingFriendsCount / $friendsCount",
                    accent = palette.primary,
                    modifier = Modifier.weight(1f)
                )
                LiquidTelemetryTile(
                    label = "Circle Today",
                    value = formatStudyDurationCompact(circleTodayMinutes + todayFocusMinutes),
                    accent = palette.gemColor,
                    modifier = Modifier.weight(1f)
                )
                LiquidTelemetryTile(
                    label = "Your Streak",
                    value = "${streakDays}d",
                    accent = palette.streakColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tactile Liquid Glass Edit, Copy & Share Actions (48dp height for accessibility)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("edit_handle_button")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !isUpdatingHandle) {
                            tactile?.playTap()
                            onEditHandle()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = palette.primary.copy(alpha = if (palette.isDark) 0.24f else 0.14f),
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.42f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.social_edit_handle_cta),
                            tint = palette.primaryLip,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.social_edit_handle_cta),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.primaryLip,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_handle_button")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = myHandle.isNotBlank()) {
                            tactile?.playTap()
                            onCopyHandle()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = palette.glassFillElevated,
                    border = BorderStroke(1.dp, palette.glassBorderHighlight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.social_copy_handle),
                            tint = palette.textPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.social_copy_handle),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_handle_button")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = myHandle.isNotBlank()) {
                            tactile?.playTap()
                            onShareHandle()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = palette.gemColor.copy(alpha = if (palette.isDark) 0.24f else 0.14f),
                    border = BorderStroke(1.dp, palette.gemColor.copy(alpha = 0.42f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.social_share_handle),
                            tint = palette.gemColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.social_share_handle),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.gemColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidTelemetryTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = palette.glassFill.copy(alpha = if (palette.isDark) 0.56f else 0.70f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (palette.isDark) 0.28f else 0.90f),
                    accent.copy(alpha = 0.34f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = NunitoDisplayFamily,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Upgraded Liquid Glass Friend Card for the responsive Social grid:
 * - Optical liquid sheen, glowing presence ring, animated sync dot, smooth goal progress bar
 * - Safe 48dp overflow button that opens FriendSafetyActionDialog (Remove / Block) instead of accidental 1-tap delete
 */
@Composable
fun LiquidGlassFriendCard(
    friendState: FriendLiveCardState,
    onMessageFriend: () -> Unit,
    onOpenFriendActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val friend: PublicStudyProfile = friendState.profile
    val liveStatus = friend.liveStatus
    val isFocusing = liveStatus == StudyLiveStatus.FOCUSING

    val statusColor by animateColorAsState(
        targetValue = when (liveStatus) {
            StudyLiveStatus.FOCUSING -> palette.primary
            StudyLiveStatus.BREAK -> palette.gemColor
            StudyLiveStatus.READY -> palette.prismViolet
        },
        animationSpec = tween(320),
        label = "liquidFriendStatusColor"
    )

    val animatedGoalProgress by animateFloatAsState(
        targetValue = friend.goalProgress,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "liquidFriendGoalProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "liquidFriendCardScale"
    )

    val initials = remember(friend.displayName) {
        friend.displayName.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "S" }
    }

    val totalHoursLabel = remember(friend.totalFocusMinutes) {
        val hours = friend.totalFocusMinutes / 60f
        if (hours >= 1f) String.format(Locale.US, "%.1fh", hours) else "${friend.totalFocusMinutes}m"
    }

    LiquidGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("friend_card_${friend.handle}")
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                tactile?.playTap()
                onMessageFriend()
            },
        cornerRadius = 26.dp,
        elevated = isFocusing,
        accentTint = statusColor,
        animatedCaustics = isFocusing,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Avatar + Live Glowing Sync Dot + Safe Actions Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.BottomEnd) {
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
                                .border(
                                    width = if (isFocusing) 1.8.dp else 1.2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (palette.isDark) 0.55f else 0.95f),
                                            statusColor.copy(alpha = 0.72f)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
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

                        LiquidCrystalSyncDot(
                            color = if (friendState.isSyncing) palette.prismCyan else statusColor,
                            isLivePulse = isFocusing || liveStatus == StudyLiveStatus.BREAK,
                            isSyncing = friendState.isSyncing,
                            syncPulseKey = friendState.syncPulseCount,
                            dotSize = 7.dp,
                            modifier = Modifier.testTag("friend_sync_dot_${friend.handle}")
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (friendState.isSyncing) "Sync" else "Live",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (friendState.isSyncing) palette.prismCyan else statusColor
                    )
                }

                IconButton(
                    onClick = {
                        tactile?.playTap()
                        onOpenFriendActions()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("friend_actions_${friend.handle}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.social_friend_options_desc),
                        tint = palette.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
            Text(
                text = "@${friend.handle}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Live Status & Current Subject Liquid Glass Capsule
            Surface(
                shape = RoundedCornerShape(50),
                color = statusColor.copy(alpha = if (palette.isDark) 0.24f else 0.14f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.38f))
            ) {
                Text(
                    text = when (liveStatus) {
                        StudyLiveStatus.FOCUSING -> "${liveStatus.label} · ${friend.currentSubject}"
                        StudyLiveStatus.BREAK -> liveStatus.label
                        StudyLiveStatus.READY -> "Ready · ${friend.currentSubject}"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (palette.isDark) Color.White else statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted
                )
                Text(
                    text = "${friend.todayFocusMinutes}m / ${friend.dailyGoalMinutes}m",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = palette.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Liquid glass goal track + glossy fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.chipUnselected)
                    .border(0.6.dp, palette.glassBorderSubtle, RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedGoalProgress.coerceAtLeast(if (friend.todayFocusMinutes > 0) 0.08f else 0f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(statusColor, palette.prismCyan)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.streakColor.copy(alpha = if (palette.isDark) 0.18f else 0.12f),
                    border = BorderStroke(0.8.dp, palette.streakColor.copy(alpha = 0.30f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Friend streak",
                            tint = palette.streakColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${friend.currentStreak}d",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.streakColor,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.glassFill.copy(alpha = 0.62f),
                    border = BorderStroke(0.8.dp, palette.glassBorderSubtle)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lv ${friend.level} · $totalHoursLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct 1:1 Study Message CTA Button (48dp accessible height)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("message_friend_${friend.handle}")
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        tactile?.playTap()
                        onMessageFriend()
                    },
                shape = RoundedCornerShape(14.dp),
                color = palette.primary.copy(alpha = if (palette.isDark) 0.24f else 0.14f),
                border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.42f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubble,
                        contentDescription = stringResource(R.string.social_message_friend_cta),
                        tint = palette.primaryLip,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.social_message_friend_cta),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.primaryLip,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Liquid Glass Social Safety & Privacy Shield Modal:
 * - Shows verified Study-Only Privacy protections (screen time never leaves device)
 * - Toggle for Private Study Mode (hides live focusing status & subject)
 * - Manage Blocked Scholars with 1-tap Unblock
 */
@Composable
fun SocialSafetyShieldDialog(
    privateStudyMode: Boolean,
    blockedUsers: List<BlockedUser>,
    onTogglePrivateStudyMode: (Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .testTag("social_safety_shield_dialog"),
                cornerRadius = 30.dp,
                accentTint = if (privateStudyMode) palette.prismViolet else palette.primary,
                elevated = true,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(palette.primarySoft)
                                    .border(1.dp, palette.primary.copy(alpha = 0.45f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = palette.primaryLip,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.social_safety_shield_title),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = NunitoDisplayFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.social_safety_shield_subtitle),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                tactile?.playTap()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_safety_shield_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Safety Shield",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    // Verified Study-Only Protection Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = palette.primarySoft.copy(alpha = if (palette.isDark) 0.45f else 0.72f),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SafetyCheckLine(
                                icon = Icons.Rounded.Lock,
                                text = "Screen time & app usage stay strictly on this phone — never uploaded or visible to friends."
                            )
                            SafetyCheckLine(
                                icon = Icons.Rounded.Shield,
                                text = "Only consensual study totals (minutes, streaks, and challenges) sync with your signed-in account."
                            )
                        }
                    }

                    // Private Study Mode Toggle Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = palette.glassFillElevated,
                        border = BorderStroke(
                            1.dp,
                            if (privateStudyMode) palette.prismViolet.copy(alpha = 0.50f) else palette.glassBorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.social_private_mode_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.social_private_mode_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = privateStudyMode,
                                onCheckedChange = onTogglePrivateStudyMode,
                                modifier = Modifier.testTag("private_study_mode_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.prismViolet,
                                    uncheckedThumbColor = palette.textMuted,
                                    uncheckedTrackColor = palette.chipUnselected
                                )
                            )
                        }
                    }

                    // Blocked Scholars List
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.social_blocked_title),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary
                            )
                            Text(
                                text = "${blockedUsers.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textMuted
                            )
                        }

                        if (blockedUsers.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = palette.glassFill.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, palette.glassBorderSubtle)
                            ) {
                                Text(
                                    text = stringResource(R.string.social_blocked_empty),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            blockedUsers.forEach { blocked ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.glassFillElevated,
                                    border = BorderStroke(1.dp, palette.glassBorderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = blocked.blockedDisplayName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (blocked.blockedHandle.isNotBlank()) {
                                                Text(
                                                    text = "@${blocked.blockedHandle}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textMuted
                                                )
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 72.dp, minHeight = 40.dp)
                                                .testTag("unblock_user_${blocked.blockedUid}")
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    tactile?.playTap()
                                                    onUnblockUser(blocked.blockedUid)
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = palette.primarySoft,
                                            border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.social_unblock_cta),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = palette.primaryLip
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
        }
    }
}

@Composable
private fun SafetyCheckLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    val palette = LocalTryAgainPalette.current
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.primaryLip,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textPrimary
        )
    }
}

/**
 * Safe Liquid Glass action modal when tapping a friend card's options menu:
 * Prevents accidental removal and lets the user safely Remove or Block a scholar.
 */
@Composable
fun FriendSafetyActionDialog(
    friend: PublicStudyProfile,
    onMessageFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
    onBlockFriend: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
                    .testTag("friend_safety_action_dialog"),
                cornerRadius = 28.dp,
                accentTint = palette.prismCyan,
                elevated = true,
                contentPadding = PaddingValues(20.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = friend.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${friend.handle} • ${friend.currentStreak}d streak",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                tactile?.playTap()
                                onDismiss()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close friend options",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    // Send Direct Message
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("dialog_message_friend_button")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                tactile?.playTap()
                                onMessageFriend()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = palette.primary.copy(alpha = if (palette.isDark) 0.24f else 0.13f),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubble,
                                contentDescription = null,
                                tint = palette.primaryLip,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.social_message_friend_cta),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = palette.primaryLip
                                )
                                Text(
                                    text = "Chat live & cheer on each other's focus streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    // Remove from Study Circle
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("confirm_remove_friend_button")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                tactile?.playTap()
                                onRemoveFriend()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = palette.glassFillElevated,
                        border = BorderStroke(1.dp, palette.glassBorderHighlight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonRemove,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.social_remove_friend),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Stop syncing their live study card in your Circle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    // Block Scholar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("confirm_block_friend_button")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                tactile?.playTap()
                                onBlockFriend()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = palette.coralAccent.copy(alpha = if (palette.isDark) 0.20f else 0.11f),
                        border = BorderStroke(1.dp, palette.coralAccent.copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null,
                                tint = palette.coralAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.social_block_friend_cta),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = palette.coralAccent
                                )
                                Text(
                                    text = stringResource(R.string.social_block_friend_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Safe Liquid Glass confirmation dialog before leaving or deleting a Study Challenge.
 */
@Composable
fun ConfirmLeaveOrDeleteChallengeDialog(
    challenge: StudyChallenge,
    isCreator: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .testTag("confirm_challenge_exit_dialog"),
                cornerRadius = 26.dp,
                accentTint = palette.coralAccent,
                elevated = true,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isCreator) "Delete Study Challenge?" else "Leave Study Challenge?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (isCreator) {
                            "Deleting \"${challenge.title}\" removes the shared group goal and its invite code ${challenge.inviteCode} for everyone."
                        } else {
                            "You will leave \"${challenge.title}\" and your future focus sessions will no longer count toward this group goal."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    tactile?.playTap()
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(14.dp),
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

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("confirm_leave_or_delete_challenge_cta")
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    tactile?.playTap()
                                    onConfirm()
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = palette.coralAccent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isCreator) "Delete" else "Leave",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
