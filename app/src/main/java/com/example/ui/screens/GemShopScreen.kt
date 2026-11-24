package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.GlassThemeId
import com.example.data.model.MascotBadgeId
import com.example.data.model.UserProfile
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.MetamorphosisContainer
import com.example.ui.components.MorphismActionButton
import com.example.ui.components.MorphismSurface
import com.example.ui.components.SlidingGlassSegmentedBar
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import com.example.ui.theme.paletteForTheme

private enum class ShopCategory(val label: String) {
    SOUNDS("Sounds"),
    THEMES("Themes"),
    AURAS("Ring Auras")
}

@Composable
fun GemShopScreen(
    profile: UserProfile,
    activeAmbient: AmbientSoundPreset,
    isAmbientPlaying: Boolean,
    onUnlockOrPlaySound: (AmbientSoundPreset) -> Unit,
    onUnlockOrEquipTheme: (GlassThemeId) -> Unit,
    onUnlockOrEquipBadge: (MascotBadgeId) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    var selectedCategory by remember { mutableStateOf(ShopCategory.SOUNDS) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            StaggeredEntrance(index = 0) {
                Column {
                    Text(
                        text = stringResource(R.string.shop_title),
                        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = NunitoDisplayFamily),
                        color = palette.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.shop_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // Shimmering Hero Gem Vault Balance Card with Rolling Counter
        item {
            StaggeredEntrance(index = 1) {
                MorphismSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    elevated = true,
                    shimmer = true,
                    accentTint = palette.gemColor,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GEM BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = palette.gemColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Diamond,
                                    contentDescription = "Gems",
                                    tint = palette.gemColor,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                AnimatedCounterText(
                                    count = profile.gems,
                                    style = MaterialTheme.typography.displayLarge.copy(fontFamily = NunitoDisplayFamily),
                                    color = palette.textPrimary,
                                    modifier = Modifier.testTag("shop_gem_balance")
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Earn +2 Gems/min + 15 bonus every Pomodoro",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.gemColor.copy(alpha = 0.14f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${profile.totalGemsEarned}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = NunitoDisplayFamily),
                                    color = palette.gemColor
                                )
                                Text(
                                    text = "Lifetime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Apple-Style Fluid Sliding Glass Category Switcher
        item {
            StaggeredEntrance(index = 2) {
                SlidingGlassSegmentedBar(
                    labels = ShopCategory.entries.map { it.label },
                    selectedIndex = ShopCategory.entries.indexOf(selectedCategory),
                    onSelect = { idx -> selectedCategory = ShopCategory.entries[idx] },
                    tagPrefix = "shop_cat"
                )
            }
        }

        // Animated Category Content
        item {
            MetamorphosisContainer(targetState = selectedCategory, label = "shopCategory") { category ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (category) {
                        ShopCategory.SOUNDS -> {
                            AmbientSoundPreset.unlockableEntries.forEachIndexed { idx, preset ->
                                val owned = preset.gemCost == 0 || preset.name in profile.unlockedSounds
                                val playing = activeAmbient == preset && isAmbientPlaying
                                StaggeredEntrance(index = idx) {
                                    SoundShopItemCard(
                                        preset = preset,
                                        owned = owned,
                                        playing = playing,
                                        canAfford = profile.gems >= preset.gemCost,
                                        onAction = { onUnlockOrPlaySound(preset) }
                                    )
                                }
                            }
                        }

                        ShopCategory.THEMES -> {
                            GlassThemeId.entries.forEachIndexed { idx, themeId ->
                                val owned = themeId.gemCost == 0 || themeId.name in profile.unlockedCosmetics
                                val equipped = profile.equippedTheme.equals(themeId.name, ignoreCase = true)
                                StaggeredEntrance(index = idx) {
                                    ThemeShopItemCard(
                                        themeId = themeId,
                                        owned = owned,
                                        equipped = equipped,
                                        canAfford = profile.gems >= themeId.gemCost,
                                        onAction = { onUnlockOrEquipTheme(themeId) }
                                    )
                                }
                            }
                        }

                        ShopCategory.AURAS -> {
                            MascotBadgeId.entries.forEachIndexed { idx, badge ->
                                val owned = badge.gemCost == 0 || badge.name in profile.unlockedCosmetics
                                val equipped = profile.equippedBadge.equals(badge.name, ignoreCase = true)
                                StaggeredEntrance(index = idx) {
                                    BadgeShopItemCard(
                                        badge = badge,
                                        owned = owned,
                                        equipped = equipped,
                                        canAfford = profile.gems >= badge.gemCost,
                                        onAction = { onUnlockOrEquipBadge(badge) }
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
private fun SoundShopItemCard(
    preset: AmbientSoundPreset,
    owned: Boolean,
    playing: Boolean,
    canAfford: Boolean,
    onAction: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = playing,
        shimmer = playing,
        accentTint = if (playing) palette.primary else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (playing) palette.primarySoft else palette.chipUnselected),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = if (playing) palette.primaryLip else palette.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    text = preset.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }

            MorphismActionButton(
                text = when {
                    playing -> "PAUSE"
                    owned -> "PLAY"
                    else -> "${preset.gemCost}"
                },
                icon = when {
                    playing -> Icons.Rounded.Pause
                    owned -> Icons.Rounded.PlayArrow
                    else -> Icons.Rounded.Diamond
                },
                onClick = onAction,
                containerColor = when {
                    playing -> palette.streakColor
                    owned -> palette.primary
                    canAfford -> palette.gemColor
                    else -> palette.textMuted
                },
                lipColor = when {
                    playing -> palette.streakLip
                    owned -> palette.primaryLip
                    canAfford -> palette.gemLip
                    else -> palette.glassBorderSubtle
                },
                height = 42.dp,
                cornerRadius = 14.dp,
                modifier = Modifier.width(108.dp),
                testTag = "shop_sound_${preset.name.lowercase()}"
            )
        }
    }
}

@Composable
private fun ThemeShopItemCard(
    themeId: GlassThemeId,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onAction: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val preview = remember(themeId) { paletteForTheme(themeId) }

    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = equipped,
        shimmer = equipped,
        accentTint = if (equipped) preview.primary else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(preview.canvasTop, preview.primary)))
                    .border(1.5.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = themeId.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    text = themeId.tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }

            if (equipped) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.primarySoft,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Equipped",
                            tint = palette.primaryLip,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.primaryLip
                        )
                    }
                }
            } else {
                MorphismActionButton(
                    text = if (owned) "EQUIP" else "${themeId.gemCost}",
                    icon = if (owned) null else Icons.Rounded.Diamond,
                    onClick = onAction,
                    containerColor = when {
                        owned -> palette.primary
                        canAfford -> palette.gemColor
                        else -> palette.textMuted
                    },
                    lipColor = when {
                        owned -> palette.primaryLip
                        canAfford -> palette.gemLip
                        else -> palette.glassBorderSubtle
                    },
                    height = 42.dp,
                    cornerRadius = 14.dp,
                    modifier = Modifier.width(104.dp),
                    testTag = "shop_theme_${themeId.name.lowercase()}"
                )
            }
        }
    }
}

@Composable
private fun BadgeShopItemCard(
    badge: MascotBadgeId,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onAction: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val auraColors = when (badge) {
        MascotBadgeId.CLASSIC_MONKEY -> listOf(Color(0xFF58CC02), Color(0xFF89E219))
        MascotBadgeId.SCHOLAR_OWL -> listOf(Color(0xFF1CB0F6), Color(0xFF6366F1))
        MascotBadgeId.ROYAL_CROWN -> listOf(Color(0xFFFFB020), Color(0xFFFF7A00))
        MascotBadgeId.COSMIC_AURA -> listOf(Color(0xFF58CC02), Color(0xFF1CB0F6), Color(0xFFEC4899))
    }

    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = equipped,
        shimmer = equipped,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(auraColors))
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(palette.glassFillElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (badge == MascotBadgeId.COSMIC_AURA) Icons.Rounded.AutoAwesome else Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = auraColors.first(),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    text = badge.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }

            if (equipped) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.primarySoft
                ) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.primaryLip,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            } else {
                MorphismActionButton(
                    text = if (owned) "EQUIP" else "${badge.gemCost}",
                    icon = if (owned) null else Icons.Rounded.Diamond,
                    onClick = onAction,
                    containerColor = when {
                        owned -> palette.primary
                        canAfford -> palette.gemColor
                        else -> palette.textMuted
                    },
                    lipColor = when {
                        owned -> palette.primaryLip
                        canAfford -> palette.gemLip
                        else -> palette.glassBorderSubtle
                    },
                    height = 42.dp,
                    cornerRadius = 14.dp,
                    modifier = Modifier.width(104.dp),
                    testTag = "shop_badge_${badge.name.lowercase()}"
                )
            }
        }
    }
}
