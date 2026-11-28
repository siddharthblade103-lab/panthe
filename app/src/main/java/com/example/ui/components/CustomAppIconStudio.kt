package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppIconShapeId
import com.example.data.model.CustomAppIconId
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

private data class IconPaletteSpec(
    val gradientStart: Color,
    val gradientEnd: Color,
    val glowColor: Color,
    val symbolTint: Color
)

private fun specForIcon(iconId: CustomAppIconId): IconPaletteSpec = when (iconId) {
    CustomAppIconId.MONKEY_CLASSIC -> IconPaletteSpec(
        gradientStart = Color(0xFF58CC02),
        gradientEnd = Color(0xFF19933B),
        glowColor = Color(0xFF7BE82E),
        symbolTint = Color.White
    )
    CustomAppIconId.CRYSTAL_PRISM -> IconPaletteSpec(
        gradientStart = Color(0xFF0EA5E9),
        gradientEnd = Color(0xFF6366F1),
        glowColor = Color(0xFF38BDF8),
        symbolTint = Color.White
    )
    CustomAppIconId.EMERALD_LEAF -> IconPaletteSpec(
        gradientStart = Color(0xFF10B981),
        gradientEnd = Color(0xFF047857),
        glowColor = Color(0xFF34D399),
        symbolTint = Color(0xFFECFDF5)
    )
    CustomAppIconId.SOLAR_CROWN -> IconPaletteSpec(
        gradientStart = Color(0xFFFFB703),
        gradientEnd = Color(0xFFFB5607),
        glowColor = Color(0xFFFFD166),
        symbolTint = Color(0xFFFFFBF0)
    )
    CustomAppIconId.MIDNIGHT_OWL -> IconPaletteSpec(
        gradientStart = Color(0xFF4338CA),
        gradientEnd = Color(0xFF0F172A),
        glowColor = Color(0xFF818CF8),
        symbolTint = Color(0xFFE0E7FF)
    )
    CustomAppIconId.SAKURA_FLAME -> IconPaletteSpec(
        gradientStart = Color(0xFFF43F5E),
        gradientEnd = Color(0xFFFB923C),
        glowColor = Color(0xFFFDA4AF),
        symbolTint = Color(0xFFFFF1F2)
    )
}

fun appIconShape(shapeId: AppIconShapeId, size: Dp): Shape = when (shapeId) {
    AppIconShapeId.CIRCLE -> CircleShape
    AppIconShapeId.SQUIRCLE -> RoundedCornerShape((size.value * 0.28f).dp)
    AppIconShapeId.PRISM -> CutCornerShape((size.value * 0.24f).dp)
}

/**
 * Reusable customizable Liquid-Glass App Icon emblem shown across:
 * - Main Focus Screen floating glass header
 * - Opening Splash Animation
 * - Profile Hero & Custom Icon Studio
 */
@Composable
fun CustomAppIconEmblem(
    iconId: CustomAppIconId,
    shapeId: AppIconShapeId,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String = "Try Again custom icon"
) {
    val palette = LocalTryAgainPalette.current
    val spec = specForIcon(iconId)
    val shape = appIconShape(shapeId, size)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(spec.gradientStart, spec.gradientEnd),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = if (size >= 64.dp) 2.2.dp else 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (palette.isDark) 0.65f else 0.96f),
                        spec.glowColor.copy(alpha = 0.60f),
                        Color.White.copy(alpha = 0.22f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (iconId == CustomAppIconId.MONKEY_CLASSIC) {
            Image(
                painter = painterResource(id = R.drawable.ic_tryagain_launcher_fg_1795336082633),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Subtle refractive caustic backdrop inside the icon
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f),
                            Color.Transparent
                        ),
                        center = Offset(this.size.width * 0.28f, this.size.height * 0.22f),
                        radius = this.size.minDimension * 0.65f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            spec.glowColor.copy(alpha = 0.36f),
                            Color.Transparent
                        ),
                        center = Offset(this.size.width * 0.78f, this.size.height * 0.82f),
                        radius = this.size.minDimension * 0.55f
                    )
                )
            }

            val symbolVector = when (iconId) {
                CustomAppIconId.MONKEY_CLASSIC -> Icons.Rounded.AutoAwesome
                CustomAppIconId.CRYSTAL_PRISM -> Icons.Rounded.Diamond
                CustomAppIconId.EMERALD_LEAF -> Icons.Rounded.Eco
                CustomAppIconId.SOLAR_CROWN -> Icons.Rounded.WorkspacePremium
                CustomAppIconId.MIDNIGHT_OWL -> Icons.Rounded.School
                CustomAppIconId.SAKURA_FLAME -> Icons.Rounded.LocalFireDepartment
            }
            Icon(
                imageVector = symbolVector,
                contentDescription = contentDescription,
                tint = spec.symbolTint,
                modifier = Modifier.size(size * 0.56f)
            )
        }

        // Top Liquid-Glass specular rim shine
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(this.size.width * 0.10f, -this.size.height * 0.14f),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width * 0.80f,
                    this.size.height * 0.44f
                )
            )
        }
    }
}

/**
 * Interactive Liquid-Glass Custom Icon Studio Card for ProfileScreen:
 * - Pick from 6 distinctive icon styles + 3 shapes (Squircle / Circle / Diamond)
 * - Instantly updates the app emblem on Main Screen, Opening Animation, and Profile
 * - Optional 1-tap "Pin Custom Icon to Home Screen" shortcut via ShortcutManagerCompat
 */
@Composable
fun CustomAppIconStudioCard(
    selectedIcon: CustomAppIconId,
    selectedShape: AppIconShapeId,
    onSelectIcon: (CustomAppIconId) -> Unit,
    onSelectShape: (AppIconShapeId) -> Unit,
    onShortcutFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val context = LocalContext.current
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    LiquidGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("custom_icon_studio_card"),
        cornerRadius = if (isExpanded) 24.dp else 20.dp,
        elevated = isExpanded,
        accentTint = specForIcon(selectedIcon).glowColor,
        animatedCaustics = isExpanded,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = if (isExpanded) 14.dp else 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("custom_icon_studio_toggle")
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        tactile?.playTap()
                        isExpanded = !isExpanded
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CustomAppIconEmblem(
                        iconId = selectedIcon,
                        shapeId = selectedShape,
                        size = if (isExpanded) 42.dp else 32.dp,
                        modifier = Modifier.testTag("custom_icon_live_preview")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.custom_icon_studio_title),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary
                            )
                        }
                        Text(
                            text = "${selectedIcon.displayName} • ${selectedShape.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.primarySoft,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (isExpanded) R.string.custom_icon_hide_cta else R.string.custom_icon_expand_cta
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.primaryLip
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "Hide custom app icon studio" else "Open custom app icon studio",
                            tint = palette.primaryLip,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 6 Custom Icon Emblems Strip
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(CustomAppIconId.entries) { iconOption ->
                            val isSelected = iconOption == selectedIcon
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.04f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "iconOptionScale"
                            )
                            Surface(
                                modifier = Modifier
                                    .width(74.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .testTag("custom_icon_option_${iconOption.name.lowercase()}")
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        tactile?.playTap()
                                        onSelectIcon(iconOption)
                                    },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) {
                                    palette.primary.copy(alpha = if (palette.isDark) 0.24f else 0.14f)
                                } else {
                                    palette.glassFillElevated
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) palette.primary else palette.glassBorderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        CustomAppIconEmblem(
                                            iconId = iconOption,
                                            shapeId = selectedShape,
                                            size = 38.dp
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.primary)
                                                    .border(1.2.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected icon",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = iconOption.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                        ),
                                        color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Icon Shape Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppIconShapeId.entries.forEach { shapeOption ->
                            val active = shapeOption == selectedShape
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 40.dp)
                                    .testTag("custom_icon_shape_${shapeOption.name.lowercase()}")
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        tactile?.playTap()
                                        onSelectShape(shapeOption)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) palette.primarySoft else palette.glassFill,
                                border = BorderStroke(
                                    1.dp,
                                    if (active) palette.primary.copy(alpha = 0.52f) else palette.glassBorderSubtle
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shapeOption.displayName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold
                                        ),
                                        color = if (active) palette.primaryLip else palette.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Pin Custom Icon Shortcut to Home Screen
                    MorphismActionButton(
                        text = stringResource(R.string.custom_icon_pin_shortcut_cta),
                        icon = Icons.Rounded.AddToHomeScreen,
                        onClick = {
                            val pinned = requestPinnedCustomShortcut(context, selectedIcon, selectedShape)
                            onShortcutFeedback(
                                if (pinned) "Confirm the prompt to add your custom Try Again icon to Home!"
                                else "Your custom icon is active across Try Again! (Launcher pinning unavailable)"
                            )
                        },
                        containerColor = specForIcon(selectedIcon).gradientStart,
                        lipColor = specForIcon(selectedIcon).gradientEnd,
                        height = 44.dp,
                        cornerRadius = 14.dp,
                        testTag = "pin_custom_icon_shortcut_button"
                    )
                }
            }
        }
    }
}

private fun requestPinnedCustomShortcut(
    context: Context,
    iconId: CustomAppIconId,
    shapeId: AppIconShapeId
): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
    return runCatching {
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val bitmap = renderCustomIconBitmap(iconId, shapeId)
        val shortcut = ShortcutInfoCompat.Builder(
            context,
            "try_again_custom_${iconId.name.lowercase()}_${shapeId.name.lowercase()}"
        )
            .setShortLabel("Try Again")
            .setLongLabel("Try Again (${iconId.displayName})")
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setIntent(shortcutIntent)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }.getOrDefault(false)
}

private fun renderCustomIconBitmap(
    iconId: CustomAppIconId,
    shapeId: AppIconShapeId
): Bitmap {
    val sizePx = 192
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val spec = specForIcon(iconId)
    val rect = RectF(8f, 8f, sizePx - 8f, sizePx - 8f)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
            spec.gradientStart.toArgb(),
            spec.gradientEnd.toArgb(),
            Shader.TileMode.CLAMP
        )
    }

    when (shapeId) {
        AppIconShapeId.CIRCLE -> canvas.drawOval(rect, bgPaint)
        AppIconShapeId.SQUIRCLE -> canvas.drawRoundRect(rect, 48f, 48f, bgPaint)
        AppIconShapeId.PRISM -> {
            val cut = 38f
            val path = Path().apply {
                moveTo(rect.left + cut, rect.top)
                lineTo(rect.right - cut, rect.top)
                lineTo(rect.right, rect.top + cut)
                lineTo(rect.right, rect.bottom - cut)
                lineTo(rect.right - cut, rect.bottom)
                lineTo(rect.left + cut, rect.bottom)
                lineTo(rect.left, rect.bottom - cut)
                lineTo(rect.left, rect.top + cut)
                close()
            }
            canvas.drawPath(path, bgPaint)
        }
    }

    // Specular sheen highlight
    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(72, 255, 255, 255)
    }
    canvas.drawOval(RectF(28f, 12f, sizePx - 28f, sizePx * 0.42f), sheenPaint)

    // Crisp central monogram emblem
    val emblemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = spec.symbolTint.toArgb()
        textAlign = Paint.Align.CENTER
        textSize = 72f
        isFakeBoldText = true
    }
    val glyph = when (iconId) {
        CustomAppIconId.MONKEY_CLASSIC -> "TA"
        CustomAppIconId.CRYSTAL_PRISM -> "◆"
        CustomAppIconId.EMERALD_LEAF -> "✦"
        CustomAppIconId.SOLAR_CROWN -> "♛"
        CustomAppIconId.MIDNIGHT_OWL -> "★"
        CustomAppIconId.SAKURA_FLAME -> "▲"
    }
    val baseline = (sizePx / 2f) - ((emblemPaint.descent() + emblemPaint.ascent()) / 2f)
    canvas.drawText(glyph, sizePx / 2f, baseline, emblemPaint)
    return bitmap
}
