package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AmbientSoundPreset
import com.example.data.model.MascotBadgeId
import com.example.data.model.PomodoroPhase
import com.example.data.model.TimerUiState
import com.example.service.FocusStreakCalculator
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.TimerMonoFamily
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Kotlin Pomodoro state machine managing start, pause, resume, reset,
 * duration customization, and 4-sprint focus/break cycles.
 */
@Stable
class PomodoroTimerController(
    initialFocusMinutes: Int = PomodoroPhase.FOCUS.defaultMinutes,
    initialShortBreakMinutes: Int = PomodoroPhase.SHORT_BREAK.defaultMinutes,
    initialLongBreakMinutes: Int = PomodoroPhase.LONG_BREAK.defaultMinutes
) {
    var phase: PomodoroPhase by mutableStateOf(PomodoroPhase.FOCUS)
        private set

    var focusMinutes: Int by mutableIntStateOf(
        initialFocusMinutes.coerceIn(PomodoroPhase.FOCUS.minMinutes, PomodoroPhase.FOCUS.maxMinutes)
    )
        private set

    var shortBreakMinutes: Int by mutableIntStateOf(
        initialShortBreakMinutes.coerceIn(PomodoroPhase.SHORT_BREAK.minMinutes, PomodoroPhase.SHORT_BREAK.maxMinutes)
    )
        private set

    var longBreakMinutes: Int by mutableIntStateOf(
        initialLongBreakMinutes.coerceIn(PomodoroPhase.LONG_BREAK.minMinutes, PomodoroPhase.LONG_BREAK.maxMinutes)
    )
        private set

    var plannedMinutes: Int by mutableIntStateOf(focusMinutes)
        private set

    var remainingSeconds: Int by mutableIntStateOf(focusMinutes * 60)
        private set

    var elapsedSeconds: Int by mutableIntStateOf(0)
        private set

    var isRunning: Boolean by mutableStateOf(false)
        private set

    var isPaused: Boolean by mutableStateOf(false)
        private set

    var completedPomodorosInCycle: Int by mutableIntStateOf(0)
        private set

    var sessionStartedDateKey: String by mutableStateOf("")
        private set

    var pausedAtEpochMs: Long? by mutableStateOf(null)
        private set

    var totalPausedSeconds: Int by mutableIntStateOf(0)
        private set

    var pauseCount: Int by mutableIntStateOf(0)
        private set

    var restoredFromSavedSession: Boolean by mutableStateOf(false)
        private set

    val totalSeconds: Int get() = (plannedMinutes * 60).coerceAtLeast(60)
    val progress: Float get() = ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    fun start(
        nowEpochMs: Long = System.currentTimeMillis(),
        todayDateKey: String = FocusStreakCalculator.formatDateKey(nowEpochMs)
    ) {
        val resumingFromPause = isPaused && remainingSeconds > 0
        if (remainingSeconds <= 0) {
            remainingSeconds = plannedMinutes * 60
            elapsedSeconds = 0
        }
        if (resumingFromPause) {
            val pausedAt = pausedAtEpochMs
            if (pausedAt != null && nowEpochMs > pausedAt) {
                totalPausedSeconds += ((nowEpochMs - pausedAt) / 1000L).toInt().coerceAtLeast(0)
            }
            pausedAtEpochMs = null
            if (sessionStartedDateKey.isBlank()) {
                sessionStartedDateKey = todayDateKey
            }
        } else if (!isRunning) {
            if (elapsedSeconds == 0 || sessionStartedDateKey.isBlank()) {
                sessionStartedDateKey = todayDateKey
            }
            pausedAtEpochMs = null
            if (elapsedSeconds == 0) {
                totalPausedSeconds = 0
                pauseCount = 0
            }
        }
        isRunning = true
        isPaused = false
        restoredFromSavedSession = false
    }

    fun pause(nowEpochMs: Long = System.currentTimeMillis()) {
        if (!isRunning) return
        isRunning = false
        isPaused = true
        pausedAtEpochMs = nowEpochMs
        pauseCount += 1
    }

    fun reset() {
        isRunning = false
        isPaused = false
        remainingSeconds = plannedMinutes * 60
        elapsedSeconds = 0
        sessionStartedDateKey = ""
        pausedAtEpochMs = null
        totalPausedSeconds = 0
        pauseCount = 0
        restoredFromSavedSession = false
    }

    fun restoreFromReconciled(reconciled: FocusStreakCalculator.ReconciledTimerState) {
        phase = reconciled.phase
        focusMinutes = reconciled.focusMinutesSetting
        shortBreakMinutes = reconciled.shortBreakMinutesSetting
        longBreakMinutes = reconciled.longBreakMinutesSetting
        plannedMinutes = reconciled.plannedMinutes
        remainingSeconds = reconciled.remainingSeconds
        elapsedSeconds = reconciled.elapsedSeconds
        isRunning = reconciled.isRunning
        isPaused = reconciled.isPaused
        completedPomodorosInCycle = reconciled.completedPomodorosInCycle
        sessionStartedDateKey = reconciled.sessionStartedDateKey
        pausedAtEpochMs = reconciled.pausedAtEpochMs
        totalPausedSeconds = reconciled.totalPausedSeconds
        pauseCount = reconciled.pauseCount
        restoredFromSavedSession = reconciled.restoredFromSavedSession
    }

    fun setDurationMinutes(minutes: Int) {
        if (isRunning) return
        val clamped = minutes.coerceIn(phase.minMinutes, phase.maxMinutes)
        when (phase) {
            PomodoroPhase.FOCUS -> focusMinutes = clamped
            PomodoroPhase.SHORT_BREAK -> shortBreakMinutes = clamped
            PomodoroPhase.LONG_BREAK -> longBreakMinutes = clamped
        }
        plannedMinutes = clamped
        remainingSeconds = clamped * 60
        elapsedSeconds = 0
        isPaused = false
        sessionStartedDateKey = ""
        pausedAtEpochMs = null
        totalPausedSeconds = 0
        pauseCount = 0
        restoredFromSavedSession = false
    }

    fun adjustDurationMinutes(deltaMinutes: Int) {
        setDurationMinutes(plannedMinutes + deltaMinutes)
    }

    fun selectPhase(newPhase: PomodoroPhase) {
        if (isRunning) return
        phase = newPhase
        val minutes = when (newPhase) {
            PomodoroPhase.FOCUS -> focusMinutes
            PomodoroPhase.SHORT_BREAK -> shortBreakMinutes
            PomodoroPhase.LONG_BREAK -> longBreakMinutes
        }
        plannedMinutes = minutes
        remainingSeconds = minutes * 60
        elapsedSeconds = 0
        isPaused = false
        sessionStartedDateKey = ""
        pausedAtEpochMs = null
        totalPausedSeconds = 0
        pauseCount = 0
        restoredFromSavedSession = false
    }

    fun tickOneSecond(): Pair<PomodoroPhase, Int>? = advanceBySeconds(1)

    fun advanceBySeconds(seconds: Int): Pair<PomodoroPhase, Int>? {
        if (!isRunning || seconds <= 0) return null
        val applied = seconds.coerceAtMost(remainingSeconds.coerceAtLeast(0))
        val nextRemaining = (remainingSeconds - applied).coerceAtLeast(0)
        remainingSeconds = nextRemaining
        elapsedSeconds += applied
        return if (nextRemaining == 0) {
            completeCurrentPhase(autoFinished = true)
        } else {
            null
        }
    }

    fun completeCurrentPhase(autoFinished: Boolean): Pair<PomodoroPhase, Int> {
        val finishedPhase = phase
        val completedMinutes = if (autoFinished) {
            plannedMinutes
        } else {
            (elapsedSeconds / 60).coerceAtLeast(1).coerceAtMost(plannedMinutes)
        }
        isRunning = false
        isPaused = false

        val nextPhase = when (finishedPhase) {
            PomodoroPhase.FOCUS -> {
                val nextCount = completedPomodorosInCycle + 1
                if (nextCount >= 4) {
                    completedPomodorosInCycle = 0
                    PomodoroPhase.LONG_BREAK
                } else {
                    completedPomodorosInCycle = nextCount
                    PomodoroPhase.SHORT_BREAK
                }
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS
        }

        phase = nextPhase
        val nextMinutes = when (nextPhase) {
            PomodoroPhase.FOCUS -> focusMinutes
            PomodoroPhase.SHORT_BREAK -> shortBreakMinutes
            PomodoroPhase.LONG_BREAK -> longBreakMinutes
        }
        plannedMinutes = nextMinutes
        remainingSeconds = nextMinutes * 60
        elapsedSeconds = 0
        sessionStartedDateKey = ""
        pausedAtEpochMs = null
        totalPausedSeconds = 0
        pauseCount = 0
        restoredFromSavedSession = false
        return finishedPhase to completedMinutes
    }
}

/**
 * Apple Watch + Regain + Duolingo liquid-glass circular focus timer card:
 * - Fluid sliding glass phase switcher
 * - Precision perimeter chrono ticks
 * - Breathing focus ripple waves radiating smoothly while focusing
 * - Cosmetic ring aura glow + glossy tip orb
 * - Smooth animated duration presets & 3D tactile pressable controls
 */
@Composable
fun TryAgainFocusRingCard(
    timerState: TimerUiState,
    equippedBadge: MascotBadgeId,
    activeTaskTitle: String?,
    activeAmbient: AmbientSoundPreset = AmbientSoundPreset.OFF,
    isAmbientPlaying: Boolean = false,
    ambientWaveAmplitude: Float = 0f,
    gemMultiplierPercent: Int = 100,
    showAmbientDrawerTrigger: Boolean = true,
    onSelectPhase: (PomodoroPhase) -> Unit,
    onSetDuration: (Int) -> Unit,
    onAdjustDuration: (Int) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onCompleteOrSkip: () -> Unit,
    onOpenAmbientDrawer: () -> Unit = {},
    onQuickToggleAmbient: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current
    val systemHaptics = LocalSystemHaptics.current

    val animatedProgress by animateFloatAsState(
        targetValue = timerState.progress,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessLow),
        label = "timerArcProgress"
    )

    val dialScale by animateFloatAsState(
        targetValue = if (timerState.isRunning) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "dialActiveScale"
    )

    val infinite = rememberInfiniteTransition(label = "ringLive")
    val breathingRipple by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleExpand"
    )
    val gentlePulse by infinite.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentlePulse"
    )
    val haloOrbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloOrbit"
    )

    val ringColors = when {
        timerState.isPaused -> listOf(palette.streakColor, palette.primary)
        timerState.phase != PomodoroPhase.FOCUS -> listOf(palette.gemColor, Color(0xFF5EEAD4))
        equippedBadge == MascotBadgeId.SCHOLAR_OWL -> listOf(Color(0xFF1CB0F6), Color(0xFF6366F1))
        equippedBadge == MascotBadgeId.ROYAL_CROWN -> listOf(Color(0xFFFFB020), Color(0xFFFF7A00))
        equippedBadge == MascotBadgeId.COSMIC_AURA -> listOf(Color(0xFF58CC02), Color(0xFF1CB0F6), Color(0xFFEC4899))
        else -> listOf(palette.primary, Color(0xFF89E219))
    }

    val baseGems = if (timerState.phase == PomodoroPhase.FOCUS) {
        (timerState.plannedMinutes * 2) + 15
    } else 0
    val safeMult = gemMultiplierPercent.coerceIn(100, 300)
    val potentialGems = ((baseGems * safeMult) / 100)

    MorphismSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 32.dp,
        elevated = true,
        accentTint = ringColors.first(),
        shimmer = timerState.isRunning || timerState.isPaused,
        frostedBlur = true,
        blurRadius = 24.dp,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fluid Sliding Glass Phase Switcher (locked while running or paused to prevent accidental session wipe)
            SlidingGlassSegmentedBar(
                labels = PomodoroPhase.entries.map { it.displayName },
                selectedIndex = PomodoroPhase.entries.indexOf(timerState.phase),
                onSelect = { idx -> onSelectPhase(PomodoroPhase.entries[idx]) },
                enabled = !timerState.isRunning && !timerState.isPaused,
                tagPrefix = "phase"
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Hero Breathing Frosted-Glass Circular Timer Display
            Box(
                modifier = Modifier
                    .size(252.dp)
                    .graphicsLayer {
                        scaleX = dialScale
                        scaleY = dialScale
                    },
                contentAlignment = Alignment.Center
            ) {
                // Layer 1: Inner Frosted Glassmorphic Lens with Blurred Refraction Core
                Box(
                    modifier = Modifier
                        .size(182.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            ambientColor = ringColors.first().copy(alpha = 0.28f),
                            spotColor = palette.shadowAmbient
                        )
                        .clip(CircleShape)
                ) {
                    // Hardware-blurred luminous refraction plane inside the circular timer display
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(radius = 20.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    ) {
                        val coreAlpha = if (timerState.isRunning) gentlePulse * 0.58f else 0.24f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ringColors.first().copy(alpha = coreAlpha),
                                    ringColors.last().copy(alpha = coreAlpha * 0.45f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.34f, size.height * 0.26f),
                                radius = size.width * 0.72f
                            ),
                            radius = size.width * 0.72f,
                            center = Offset(size.width * 0.34f, size.height * 0.26f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (palette.isDark) 0.14f else 0.65f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.66f, size.height * 0.74f),
                                radius = size.width * 0.58f
                            ),
                            radius = size.width * 0.58f,
                            center = Offset(size.width * 0.66f, size.height * 0.74f)
                        )
                    }

                    // Semi-transparent frosted glass disc surface + specular sapphire-glass sheen
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = if (palette.isDark) {
                                        listOf(
                                            Color(0x66223A2D),
                                            Color(0x4715261D)
                                        )
                                    } else {
                                        listOf(
                                            Color.White.copy(alpha = 0.66f),
                                            Color.White.copy(alpha = 0.36f)
                                        )
                                    }
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (palette.isDark) 0.54f else 0.96f),
                                        palette.prismCyan.copy(alpha = if (palette.isDark) 0.36f else 0.42f),
                                        ringColors.first().copy(alpha = if (palette.isDark) 0.30f else 0.24f),
                                        palette.prismViolet.copy(alpha = if (palette.isDark) 0.26f else 0.30f),
                                        Color.White.copy(alpha = if (palette.isDark) 0.18f else 0.58f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Subtle curved upper-left crystal highlight reflection
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawArc(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (palette.isDark) 0.22f else 0.78f),
                                    Color.Transparent
                                ),
                                start = Offset(size.width * 0.15f, size.height * 0.08f),
                                end = Offset(size.width * 0.75f, size.height * 0.48f)
                            ),
                            startAngle = 205f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(7.dp.toPx(), 7.dp.toPx()),
                            size = Size(size.width - 14.dp.toPx(), size.height - 14.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Layer 2: Precision Chrono Ticks, Breathing Ripples & Progress Ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val pad = strokeWidth + 14.dp.toPx()
                    val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                    val topLeft = Offset(pad, pad)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = arcSize.width / 2f

                    // Expanding Mindfulness / Regain-style breathing ripple while actively focusing
                    if (timerState.isRunning) {
                        val rippleRadius = radius + (24.dp.toPx() * breathingRipple)
                        val rippleAlpha = (1f - breathingRipple) * 0.30f
                        drawCircle(
                            color = ringColors.first().copy(alpha = rippleAlpha),
                            radius = rippleRadius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Soft ambient halo glow behind ring
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ringColors.first().copy(
                                    alpha = if (timerState.isRunning) gentlePulse * 0.40f else 0.12f
                                ),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.width * 0.52f
                        ),
                        radius = size.width * 0.52f,
                        center = center
                    )

                    // Subtle Apple-Chrono perimeter tick marks (24 clean hairline ticks)
                    val tickOuter = radius + strokeWidth * 0.72f
                    val tickInner = tickOuter - 4.5.dp.toPx()
                    for (t in 0 until 24) {
                        val a = (t * 15.0 - 90.0) * PI / 180.0
                        val major = t % 6 == 0
                        val inner = if (major) tickInner - 2.dp.toPx() else tickInner
                        drawLine(
                            color = palette.glassBorderSubtle.copy(alpha = if (major) 0.92f else 0.48f),
                            start = Offset(
                                center.x + (inner * cos(a)).toFloat(),
                                center.y + (inner * sin(a)).toFloat()
                            ),
                            end = Offset(
                                center.x + (tickOuter * cos(a)).toFloat(),
                                center.y + (tickOuter * sin(a)).toFloat()
                            ),
                            strokeWidth = if (major) 2.dp.toPx() else 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Translucent recessed glass progress track
                    drawArc(
                        color = if (palette.isDark) {
                            Color.White.copy(alpha = 0.10f)
                        } else {
                            Color.White.copy(alpha = 0.58f)
                        },
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active sweep progress arc
                    val sweep = (animatedProgress * 360f).coerceIn(3f, 360f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = ringColors + ringColors.first(),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Glowing tip orb on arc leading edge
                    val angleRad = ((-90f + sweep) * PI / 180.0)
                    val tipCenter = Offset(
                        center.x + (radius * cos(angleRad)).toFloat(),
                        center.y + (radius * sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = ringColors.first().copy(alpha = 0.35f),
                        radius = 9.dp.toPx(),
                        center = tipCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.6.dp.toPx(),
                        center = tipCenter
                    )

                    // Orbiting shimmer dot when Cosmic / Metamorphosis aura is active
                    if (equippedBadge == MascotBadgeId.COSMIC_AURA || equippedBadge == MascotBadgeId.ROYAL_CROWN) {
                        val orbRad = (haloOrbitAngle * PI / 180.0)
                        val orbPos = Offset(
                            center.x + ((radius + strokeWidth * 0.85f) * cos(orbRad)).toFloat(),
                            center.y + ((radius + strokeWidth * 0.85f) * sin(orbRad)).toFloat()
                        )
                        drawCircle(
                            color = ringColors.last().copy(alpha = 0.78f),
                            radius = 3.2.dp.toPx(),
                            center = orbPos
                        )
                    }
                }

                // Layer 3: Crisp Floating Minimal Digital Timer Readout over Frosted Glass Lens
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (timerState.isPaused) {
                            "PAUSED"
                        } else {
                            timerState.phase.displayName.uppercase(Locale.US)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp,
                            fontSize = 10.sp
                        ),
                        color = if (timerState.isPaused) palette.streakColor else palette.textMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Minimal translucent frosted pill framing the timer digits
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (palette.isDark) {
                                    Color.White.copy(alpha = 0.08f)
                                } else {
                                    Color.White.copy(alpha = 0.46f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = if (palette.isDark) 0.18f else 0.78f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatClock(timerState.displaySeconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = TimerMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                letterSpacing = (-1.2).sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            color = palette.textPrimary,
                            modifier = Modifier.testTag("timer_display")
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    if (potentialGems > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.gemColor.copy(alpha = if (palette.isDark) 0.18f else 0.12f),
                            border = BorderStroke(
                                1.dp,
                                palette.gemColor.copy(alpha = if (palette.isDark) 0.36f else 0.28f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Diamond,
                                    contentDescription = null,
                                    tint = palette.gemColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (safeMult > 100) "+$potentialGems Gems (${safeMult / 100f}x)" else "+$potentialGems Gems",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.gemColor
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Breathe & recharge",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }

                    // 4-sprint cycle pills
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) { idx ->
                            val filled = idx < timerState.completedPomodorosInCycle
                            Box(
                                modifier = Modifier
                                    .width(if (filled) 14.dp else 7.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (filled) palette.primary else palette.glassBorderSubtle)
                            )
                        }
                    }
                }
            }

            // Liquid-Glass Paused Streak-Protection Pill
            AnimatedVisibility(
                visible = timerState.isPaused,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(180)) + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .testTag("timer_paused_streak_badge"),
                    shape = RoundedCornerShape(50),
                    color = palette.streakColor.copy(alpha = if (palette.isDark) 0.18f else 0.12f),
                    border = BorderStroke(
                        1.dp,
                        palette.streakColor.copy(alpha = if (palette.isDark) 0.45f else 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Streak protected while paused",
                            tint = palette.streakColor,
                            modifier = Modifier.size(15.dp)
                        )
                        val elapsedLabel = if (timerState.elapsedSeconds >= 60) {
                            "${formatClock(timerState.elapsedSeconds)} saved"
                        } else {
                            "Progress saved"
                        }
                        Text(
                            text = "${stringResource(R.string.focus_paused_badge)} · $elapsedLabel",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            ),
                            color = palette.streakLip
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !activeTaskTitle.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.padding(top = 10.dp),
                    shape = RoundedCornerShape(50),
                    color = palette.primarySoft,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.28f))
                ) {
                    Text(
                        text = "Focus Task: ${activeTaskTitle.orEmpty()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.primaryLip,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
            }

            // Smooth Collapsible Duration Stepper (hides gracefully during active sprint for minimal calm)
            AnimatedVisibility(
                visible = !timerState.isRunning && !timerState.isPaused,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(180)) + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    val presets = when (timerState.phase) {
                        PomodoroPhase.FOCUS -> listOf(15, 25, 45, 60)
                        PomodoroPhase.SHORT_BREAK -> listOf(5, 10, 15)
                        PomodoroPhase.LONG_BREAK -> listOf(15, 20, 30)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("duration_minus")
                                .clip(CircleShape)
                                .clickable {
                                    systemHaptics.tick()
                                    tactile?.playTap()
                                    onAdjustDuration(-5)
                                },
                            shape = CircleShape,
                            color = palette.chipUnselected,
                            border = BorderStroke(1.dp, palette.glassBorderSubtle)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease duration",
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        presets.forEach { mins ->
                            MorphismChip(
                                label = "${mins}m",
                                selected = timerState.plannedMinutes == mins,
                                onClick = { onSetDuration(mins) },
                                modifier = Modifier.padding(horizontal = 3.dp),
                                testTag = "preset_${mins}m"
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("duration_plus")
                                .clip(CircleShape)
                                .clickable {
                                    systemHaptics.tick()
                                    tactile?.playTap()
                                    onAdjustDuration(5)
                                },
                            shape = CircleShape,
                            color = palette.chipUnselected,
                            border = BorderStroke(1.dp, palette.glassBorderSubtle)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increase duration",
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Morphing Primary 3D Tactile Controls
            MetamorphosisContainer(
                targetState = timerState.isRunning || timerState.isPaused,
                label = "timerButtonsMorph"
            ) { isActiveSession ->
                if (!isActiveSession) {
                    MorphismActionButton(
                        text = if (timerState.phase == PomodoroPhase.FOCUS) {
                            stringResource(R.string.focus_start)
                        } else {
                            stringResource(R.string.focus_start_break)
                        },
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onStartOrResume,
                        containerColor = if (timerState.phase == PomodoroPhase.FOCUS) palette.primary else palette.gemColor,
                        lipColor = if (timerState.phase == PomodoroPhase.FOCUS) palette.primaryLip else palette.gemLip,
                        hapticEvent = SystemHapticEvent.SESSION_START,
                        testTag = "timer_start_button"
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MorphismActionButton(
                            text = if (timerState.isRunning) {
                                stringResource(R.string.focus_pause)
                            } else {
                                stringResource(R.string.focus_resume)
                            },
                            icon = if (timerState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            onClick = if (timerState.isRunning) onPause else onStartOrResume,
                            containerColor = if (timerState.isRunning) palette.streakColor else palette.primary,
                            lipColor = if (timerState.isRunning) palette.streakLip else palette.primaryLip,
                            hapticEvent = if (timerState.isRunning) SystemHapticEvent.SESSION_PAUSE else SystemHapticEvent.SESSION_START,
                            modifier = Modifier.weight(1.25f),
                            testTag = "timer_pause_resume_button"
                        )

                        MorphismActionButton(
                            text = if (timerState.phase == PomodoroPhase.FOCUS) {
                                stringResource(R.string.focus_complete)
                            } else {
                                stringResource(R.string.focus_skip_break)
                            },
                            icon = if (timerState.phase == PomodoroPhase.FOCUS) Icons.Rounded.CheckCircle else Icons.Rounded.SkipNext,
                            onClick = onCompleteOrSkip,
                            containerColor = palette.gemColor,
                            lipColor = palette.gemLip,
                            hapticEvent = SystemHapticEvent.SESSION_COMPLETE,
                            modifier = Modifier.weight(1f),
                            testTag = "timer_complete_button"
                        )

                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("timer_reset_button")
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    systemHaptics.sessionStop()
                                    tactile?.playTap()
                                    onReset()
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = palette.chipUnselected,
                            border = BorderStroke(1.dp, palette.glassBorderSubtle)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.focus_reset),
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showAmbientDrawerTrigger) {
                Spacer(modifier = Modifier.height(14.dp))

                // 1-Tap Ambient Soundscape Selection Drawer Trigger (always accessible during focus sessions)
                AmbientSoundDrawerTriggerBar(
                    activeAmbient = activeAmbient,
                    isPlaying = isAmbientPlaying,
                    waveAmplitude = ambientWaveAmplitude,
                    onOpenDrawer = onOpenAmbientDrawer,
                    onQuickPlayPause = onQuickToggleAmbient
                )
            }
        }
    }
}

fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
