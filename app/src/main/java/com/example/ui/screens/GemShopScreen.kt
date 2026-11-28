package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.CompletionSoundPreset
import com.example.data.model.GlassThemeId
import com.example.data.model.MascotBadgeId
import com.example.data.model.PlayPointsTier
import com.example.data.model.ProAiStudioUiState
import com.example.data.model.ProUnlockStatus
import com.example.data.model.UserProfile
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.MetamorphosisContainer
import com.example.ui.components.MorphismActionButton
import com.example.ui.components.MorphismSurface
import com.example.ui.components.ProEditionStudioDialog
import com.example.ui.components.ProEditionVaultCard
import com.example.ui.components.SlidingGlassSegmentedBar
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily
import com.example.ui.theme.paletteForTheme

private enum class ShopCategory(val label: String) {
    PALETTES("Themes"),
    POINTS("Play Points"),
    SOUNDS("Sounds"),
    AURAS("Auras")
}

@Composable
fun GemShopScreen(
    profile: UserProfile,
    activeAmbient: AmbientSoundPreset,
    isAmbientPlaying: Boolean,
    completionSound: CompletionSoundPreset,
    proUnlockStatus: ProUnlockStatus = ProUnlockStatus(),
    proAiState: ProAiStudioUiState = ProAiStudioUiState.Idle,
    onGenerateProNotes: (String, String) -> Unit = { _, _ -> },
    onAskProTutor: (String, String) -> Unit = { _, _ -> },
    onClearProAiState: () -> Unit = {},
    onUnlockOrPlaySound: (AmbientSoundPreset) -> Unit,
    onSelectCompletionSound: (CompletionSoundPreset) -> Unit,
    onUnlockOrEquipTheme: (GlassThemeId) -> Unit,
    onUnlockOrEquipBadge: (MascotBadgeId) -> Unit,
    onClaimPlayPointsTier: (PlayPointsTier) -> Unit
) {
    val palette = LocalTryAgainPalette.current
    var selectedCategory by remember { mutableStateOf(ShopCategory.PALETTES) }
    var showProStudioDialog by remember { mutableStateOf(false) }
    val currentTier = remember(profile.focusXp) { PlayPointsTier.forPoints(profile.focusXp) }
    val nextTier = remember(currentTier) { currentTier.nextTier() }

    if (showProStudioDialog && proUnlockStatus.isUnlocked) {
        ProEditionStudioDialog(
            status = proUnlockStatus,
            subjects = profile.subjects.ifEmpty { UserProfile.DEFAULT_SUBJECTS },
            aiState = proAiState,
            onGenerateNotes = onGenerateProNotes,
            onAskTutor = onAskProTutor,
            onClearState = onClearProAiState,
            onDismiss = { showProStudioDialog = false }
        )
    }

    val totalUnlockables = remember {
        AmbientSoundPreset.unlockableEntries.size + GlassThemeId.entries.size + MascotBadgeId.entries.size
    }
    val ownedCount = remember(profile.unlockedSounds, profile.unlockedCosmetics) {
        val soundsOwned = AmbientSoundPreset.unlockableEntries.count {
            it.gemCost == 0 || it.name in profile.unlockedSounds
        }
        val palettesOwned = GlassThemeId.entries.count {
            it.gemCost == 0 || it.name in profile.unlockedCosmetics
        }
        val aurasOwned = MascotBadgeId.entries.count {
            it.gemCost == 0 || it.name in profile.unlockedCosmetics
        }
        soundsOwned + palettesOwned + aurasOwned
    }
    val collectionProgress = (ownedCount.toFloat() / totalUnlockables.coerceAtLeast(1)).coerceIn(0f, 1f)

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val animatedCollection by animateFloatAsState(
        targetValue = if (revealed) collectionProgress else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "shopCollectionProgress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("rewards_shop_screen"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
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

        // 2. Frosted Glass Hero Gem Vault & Collection Progress Card
        item {
            StaggeredEntrance(index = 1) {
                MorphismSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    elevated = true,
                    shimmer = true,
                    frostedBlur = true,
                    blurRadius = 22.dp,
                    accentTint = palette.gemColor,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ACCUMULATED GEMS",
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
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Earn +2 Gems/min + 15 bonus every Pomodoro",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.streakColor.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
                                border = BorderStroke(1.dp, palette.streakColor.copy(alpha = 0.34f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Stars,
                                            contentDescription = "Play Points",
                                            tint = palette.streakColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${profile.focusXp}",
                                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = NunitoDisplayFamily),
                                            color = palette.streakColor
                                        )
                                    }
                                    Text(
                                        text = "${currentTier.displayName} Pts",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rewards Unlocked",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textSecondary
                            )
                            Text(
                                text = "$ownedCount / $totalUnlockables",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.primaryLip
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (palette.isDark) Color.White.copy(alpha = 0.10f)
                                    else Color.White.copy(alpha = 0.60f)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedCollection)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(palette.primary, palette.gemColor)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // 2B. Un-Bypassable 500-Hour Pro Edition Vault Card
        item {
            StaggeredEntrance(index = 2) {
                ProEditionVaultCard(
                    status = proUnlockStatus,
                    onOpenProStudio = {
                        if (proUnlockStatus.isUnlocked) {
                            showProStudioDialog = true
                        }
                    }
                )
            }
        }

        // 3. Fluid Sliding Glass Category Switcher
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

        // 4. Category Items with Smooth Metamorphosis Transition
        item {
            MetamorphosisContainer(targetState = selectedCategory, label = "shopCategory") { category ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (category) {
                        ShopCategory.POINTS -> {
                            // Next Tier Status Card
                            MorphismSurface(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp,
                                elevated = true,
                                shimmer = true,
                                accentTint = palette.streakColor,
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${currentTier.displayName} Rewards Tier",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = NunitoDisplayFamily,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = if (nextTier != null) {
                                                "${(nextTier.minPoints - profile.focusXp).coerceAtLeast(0)} pts to ${nextTier.displayName}"
                                            } else {
                                                "Max Tier Reached!"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.streakColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val tierFrac = if (nextTier != null) {
                                        ((profile.focusXp - currentTier.minPoints).toFloat() /
                                            (nextTier.minPoints - currentTier.minPoints).coerceAtLeast(1)).coerceIn(0f, 1f)
                                    } else 1f
                                    LinearProgressIndicator(
                                        progress = { tierFrac },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = palette.streakColor,
                                        trackColor = palette.glassBorderSubtle
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Earn +3 Play Points for every minute you study to unlock bonus Gem chests.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            PlayPointsTier.entries.filter { it.gemBonus > 0 }.forEachIndexed { idx, tier ->
                                val unlocked = profile.focusXp >= tier.minPoints
                                val claimed = tier.claimKey in profile.claimedMilestones
                                StaggeredEntrance(index = idx) {
                                    PlayPointsTierCard(
                                        tier = tier,
                                        currentPoints = profile.focusXp,
                                        unlocked = unlocked,
                                        claimed = claimed,
                                        onClaim = { onClaimPlayPointsTier(tier) }
                                    )
                                }
                            }
                        }

                        ShopCategory.SOUNDS -> {
                            Text(
                                text = "Session Complete Chimes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            CompletionSoundPreset.entries.forEachIndexed { idx, chime ->
                                val selected = completionSound == chime
                                StaggeredEntrance(index = idx) {
                                    CompletionSoundItemCard(
                                        preset = chime,
                                        selected = selected,
                                        onSelect = { onSelectCompletionSound(chime) }
                                    )
                                }
                            }

                            Text(
                                text = "Ambient Focus Soundscapes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = NunitoDisplayFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = palette.textPrimary,
                                modifier = Modifier.padding(top = 6.dp)
                            )
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

                        ShopCategory.PALETTES -> {
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

private fun soundVisualMeta(preset: AmbientSoundPreset): Pair<ImageVector, Color> = when (preset) {
    AmbientSoundPreset.RAIN -> Icons.Rounded.WaterDrop to Color(0xFF1CB0F6)
    AmbientSoundPreset.CAFE_NOISE -> Icons.Rounded.LocalCafe to Color(0xFFD97706)
    AmbientSoundPreset.WHITE_NOISE -> Icons.Rounded.Air to Color(0xFF64748B)
    AmbientSoundPreset.FOREST_WIND -> Icons.Rounded.Park to Color(0xFF28B463)
    AmbientSoundPreset.OCEAN_WAVES -> Icons.Rounded.Waves to Color(0xFF0EA5E9)
    AmbientSoundPreset.BROWN_NOISE -> Icons.Rounded.GraphicEq to Color(0xFFA16207)
    AmbientSoundPreset.ALPHA_WAVES -> Icons.Rounded.SelfImprovement to Color(0xFF8B5CF6)
    AmbientSoundPreset.FIREPLACE -> Icons.Rounded.LocalFireDepartment to Color(0xFFF97316)
    AmbientSoundPreset.OFF -> Icons.Rounded.GraphicEq to Color(0xFF64748B)
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
    val (soundIcon, soundTint) = remember(preset) { soundVisualMeta(preset) }

    val eqAnim = rememberInfiniteTransition(label = "shopEq_${preset.name}")
    val eqPulse by eqAnim.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqPulse"
    )

    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevated = playing,
        shimmer = playing,
        frostedBlur = playing,
        blurRadius = 16.dp,
        accentTint = if (playing) soundTint else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(soundTint.copy(alpha = if (palette.isDark) 0.22f else 0.14f))
                    .border(1.dp, soundTint.copy(alpha = 0.34f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (playing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(18.dp)
                    ) {
                        val h1 = (0.35f + 0.65f * eqPulse)
                        val h2 = (1f - 0.55f * eqPulse)
                        val h3 = (0.45f + 0.55f * eqPulse)
                        listOf(h1, h2, h3).forEach { frac ->
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height((18f * frac).dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(soundTint)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = soundIcon,
                        contentDescription = null,
                        tint = soundTint,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (owned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primarySoft
                        ) {
                            Text(
                                text = when {
                                    playing -> "PLAYING"
                                    preset.gemCost == 0 -> "FREE"
                                    else -> "OWNED"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = palette.primaryLip,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preset.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
                modifier = Modifier.width(104.dp),
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
        cornerRadius = 24.dp,
        elevated = equipped,
        shimmer = equipped,
        frostedBlur = equipped,
        blurRadius = 18.dp,
        accentTint = if (equipped) preview.primary else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Multi-color miniature glass palette preview card
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(preview.canvasTop, preview.canvasBottom)
                            )
                        )
                        .border(1.5.dp, preview.primary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .padding(7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(preview.primary)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(preview.gemColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(preview.streakColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(preview.secondary)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = themeId.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (themeId.isDark) Color(0xFF1E293B) else palette.chipUnselected
                        ) {
                            Text(
                                text = if (themeId.isDark) "DARK" else "LIGHT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = if (themeId.isDark) Color.White else palette.textSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (themeId.gemCost == 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.primarySoft
                            ) {
                                Text(
                                    text = "FREE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp
                                    ),
                                    color = palette.primaryLip,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = themeId.tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
                        text = if (owned) "APPLY" else "${themeId.gemCost}",
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
                        modifier = Modifier.width(100.dp),
                        testTag = "shop_theme_${themeId.name.lowercase()}"
                    )
                }
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
        cornerRadius = 24.dp,
        elevated = equipped,
        shimmer = equipped,
        frostedBlur = equipped,
        blurRadius = 16.dp,
        accentTint = if (equipped) auraColors.first() else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = badge.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    if (badge.gemCost == 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primarySoft
                        ) {
                            Text(
                                text = "FREE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = palette.primaryLip,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = badge.subtitle,
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
                    modifier = Modifier.width(100.dp),
                    testTag = "shop_badge_${badge.name.lowercase()}"
                )
            }
        }
    }
}

@Composable
private fun PlayPointsTierCard(
    tier: PlayPointsTier,
    currentPoints: Int,
    unlocked: Boolean,
    claimed: Boolean,
    onClaim: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    val tierTint = when (tier) {
        PlayPointsTier.BRONZE -> Color(0xFFB45309)
        PlayPointsTier.SILVER -> Color(0xFF64748B)
        PlayPointsTier.GOLD -> Color(0xFFF59E0B)
        PlayPointsTier.CRYSTAL -> Color(0xFF0EA5E9)
        PlayPointsTier.DIAMOND -> Color(0xFF8B5CF6)
        PlayPointsTier.PRISM_LEGEND -> Color(0xFFEC4899)
    }
    val progress = (currentPoints.toFloat() / tier.minPoints.coerceAtLeast(1)).coerceIn(0f, 1f)

    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevated = unlocked && !claimed,
        shimmer = unlocked && !claimed,
        accentTint = if (unlocked && !claimed) tierTint else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(tierTint.copy(alpha = if (palette.isDark) 0.24f else 0.14f))
                        .border(1.dp, tierTint.copy(alpha = 0.4f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stars,
                        contentDescription = null,
                        tint = tierTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${tier.displayName} Tier",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    Text(
                        text = tier.perkLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }

                when {
                    claimed -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.primarySoft
                        ) {
                            Text(
                                text = "CLAIMED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.primaryLip,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    unlocked -> {
                        MorphismActionButton(
                            text = "+${tier.gemBonus}",
                            icon = Icons.Rounded.CardGiftcard,
                            onClick = onClaim,
                            containerColor = palette.streakColor,
                            lipColor = palette.streakLip,
                            height = 42.dp,
                            cornerRadius = 14.dp,
                            modifier = Modifier.width(98.dp),
                            testTag = "claim_tier_${tier.name.lowercase()}"
                        )
                    }

                    else -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.chipUnselected
                        ) {
                            Text(
                                text = "${tier.minPoints} pts",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (!claimed) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = tierTint,
                        trackColor = palette.glassBorderSubtle
                    )
                    Text(
                        text = "${currentPoints.coerceAtMost(tier.minPoints)}/${tier.minPoints} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionSoundItemCard(
    preset: CompletionSoundPreset,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    MorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = selected,
        accentTint = if (selected) palette.primary else null,
        contentPadding = PaddingValues(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.primarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = palette.primaryLip,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(
                    text = preset.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }

            MorphismActionButton(
                text = if (selected) "ACTIVE" else "USE",
                icon = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow,
                onClick = onSelect,
                containerColor = if (selected) palette.primary else palette.gemColor,
                lipColor = if (selected) palette.primaryLip else palette.gemLip,
                height = 40.dp,
                cornerRadius = 13.dp,
                modifier = Modifier.width(102.dp),
                testTag = "shop_chime_${preset.name.lowercase()}"
            )
        }
    }
}
