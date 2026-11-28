package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.model.CompletionSoundPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "TactileSoundEngine"
private const val TACTILE_RATE = 44100

/**
 * Apple-level crystal-clean procedural animation, UI sound & haptic feedback engine.
 * Synthesizes crisp micro-clicks, liquid-glass tab glides, card bloom pops, gem chimes,
 * Play Points tier chimes, and 5 customizable session-completion sounds.
 * Never triggered on app open or app close per user preference.
 */
class TactileSoundEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var soundsEnabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

    @Volatile
    var hapticStrengthScale: Float = 1.0f

    @Volatile
    var completionSoundPreset: CompletionSoundPreset = CompletionSoundPreset.CRYSTAL_CHIME

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    /** Crisp 14ms Apple-style glass micro-tap click + light haptic tick. */
    fun playTap() {
        triggerHaptic(durationMs = 10L, amplitude = 55)
        if (!soundsEnabled) return
        playPcmAsync(tapPcm)
    }

    /** Airy liquid-crystal glide swoosh when switching tabs or sliding glass capsules. */
    fun playTabGlide() {
        triggerHaptic(durationMs = 12L, amplitude = 62)
        if (!soundsEnabled) return
        playPcmAsync(tabGlidePcm)
    }

    /** Soft luminous crystal pop for animated card reveals and AI insights arrival. */
    fun playCardPop() {
        triggerHaptic(durationMs = 14L, amplitude = 75)
        if (!soundsEnabled) return
        playPcmAsync(cardPopPcm)
    }

    /** Soft rounded droplet pop for switches, chips, and task checkboxes. */
    fun playToggle(on: Boolean = true) {
        triggerHaptic(durationMs = 14L, amplitude = if (on) 85 else 45)
        if (!soundsEnabled) return
        playPcmAsync(if (on) toggleOnPcm else toggleOffPcm)
    }

    /** Sparkling crystalline gem chime when earning or claiming gems. */
    fun playGemSparkle() {
        triggerHaptic(durationMs = 24L, amplitude = 135)
        if (!soundsEnabled) return
        playPcmAsync(gemSparklePcm)
    }

    /** Rising luminous chord for Play Points tier rewards & rank-ups. */
    fun playPointsReward() {
        triggerHaptic(durationMs = 34L, amplitude = 175)
        if (!soundsEnabled) return
        playPcmAsync(pointsRewardPcm)
    }

    /** Magical glass chord when unlocking a new soundscape or cosmetic theme. */
    fun playUnlock() {
        triggerHaptic(durationMs = 30L, amplitude = 160)
        if (!soundsEnabled) return
        playPcmAsync(unlockChordPcm)
    }

    /** Plays the user's selected custom completion sound upon finishing a focus sprint. */
    fun playSessionComplete(
        preset: CompletionSoundPreset = completionSoundPreset,
        forcePlay: Boolean = false
    ) {
        triggerHaptic(durationMs = 48L, amplitude = 195)
        if (!soundsEnabled && !forcePlay) return
        playPcmAsync(pcmForCompletion(preset), isAlert = true)
    }

    /** Immediate preview when choosing a custom completion sound in Settings or Shop. */
    fun previewCompletionSound(preset: CompletionSoundPreset) {
        triggerHaptic(durationMs = 20L, amplitude = 110)
        playPcmAsync(pcmForCompletion(preset), isAlert = true)
    }

    /** Crisp Galaxy-style double-pulse haptic + airy glide when opening a study app from the Study Launcher. */
    fun playLauncherLaunch() {
        triggerHaptic(durationMs = 16L, amplitude = 120)
        if (!soundsEnabled) return
        playPcmAsync(tabGlidePcm)
    }

    /** Immediate tactile vibration preview when adjusting haptic intensity in Atmosphere Studio. */
    fun previewHapticStrength(scale: Float) {
        hapticStrengthScale = scale
        hapticsEnabled = scale > 0.01f
        if (scale > 0.01f) {
            triggerHaptic(durationMs = 22L, amplitude = 135)
        }
    }

    private fun triggerHaptic(durationMs: Long, amplitude: Int) {
        if (!hapticsEnabled || hapticStrengthScale <= 0.01f) return
        val vib = vibrator ?: return
        val scaledDuration = (durationMs * hapticStrengthScale.coerceIn(0.4f, 1.6f)).toLong().coerceIn(6L, 85L)
        val scaledAmp = (amplitude * hapticStrengthScale).toInt().coerceIn(1, 255)
        runCatching {
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(scaledDuration, scaledAmp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(scaledDuration)
            }
        }
    }

    private fun playPcmAsync(samples: ShortArray, isAlert: Boolean = false) {
        scope.launch {
            var track: AudioTrack? = null
            try {
                val byteLength = samples.size * 2
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(
                                if (isAlert) AudioAttributes.USAGE_NOTIFICATION_EVENT
                                else AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
                            )
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(TACTILE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(byteLength.coerceAtLeast(2048))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                val durationMs = ((samples.size.toDouble() / TACTILE_RATE) * 1000.0).toLong() + 55L
                kotlinx.coroutines.delay(durationMs)
            } catch (e: Exception) {
                Log.w(TAG, "Tactile sound playback skipped: ${e.message}")
            } finally {
                runCatching { track?.stop() }
                runCatching { track?.release() }
            }
        }
    }

    companion object {
        private val tapPcm: ShortArray by lazy { synthesizeCrispTap() }
        private val tabGlidePcm: ShortArray by lazy { synthesizeLiquidGlide() }
        private val cardPopPcm: ShortArray by lazy { synthesizeDroplet(startHz = 680.0, endHz = 1280.0) }
        private val toggleOnPcm: ShortArray by lazy { synthesizeDroplet(startHz = 540.0, endHz = 960.0) }
        private val toggleOffPcm: ShortArray by lazy { synthesizeDroplet(startHz = 720.0, endHz = 440.0) }
        private val gemSparklePcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(1046.5, 1318.5, 1568.0, 2093.0), stepMs = 58, tailMs = 240, gain = 0.24)
        }
        private val pointsRewardPcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(659.25, 880.0, 1174.66, 1567.98, 2093.0), stepMs = 64, tailMs = 380, gain = 0.27)
        }
        private val unlockChordPcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(587.33, 880.0, 1174.66, 1479.98), stepMs = 70, tailMs = 340, gain = 0.26)
        }

        private val crystalChimePcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(523.25, 659.25, 783.99, 1046.5, 1318.5), stepMs = 82, tailMs = 450, gain = 0.30)
        }
        private val victoryFanfarePcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(523.25, 659.25, 783.99, 1046.5, 987.77, 1318.5, 1567.98), stepMs = 78, tailMs = 520, gain = 0.31)
        }
        private val zenGongPcm: ShortArray by lazy { synthesizeZenBell() }
        private val harpBloomPcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(440.0, 554.37, 659.25, 880.0, 1108.73, 1318.51), stepMs = 54, tailMs = 560, gain = 0.25)
        }
        private val softBellPcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(698.46, 880.0, 1046.5), stepMs = 110, tailMs = 380, gain = 0.28)
        }

        private fun pcmForCompletion(preset: CompletionSoundPreset): ShortArray = when (preset) {
            CompletionSoundPreset.CRYSTAL_CHIME -> crystalChimePcm
            CompletionSoundPreset.VICTORY_FANFARE -> victoryFanfarePcm
            CompletionSoundPreset.ZEN_GONG -> zenGongPcm
            CompletionSoundPreset.HARP_BLOOM -> harpBloomPcm
            CompletionSoundPreset.SOFT_BELL -> softBellPcm
        }

        private fun synthesizeCrispTap(): ShortArray {
            val count = (TACTILE_RATE * 0.018).toInt()
            return ShortArray(count) { i ->
                val t = i.toDouble() / TACTILE_RATE
                val env = exp(-t * 280.0) * (1.0 - exp(-t * 3200.0))
                val body = sin(2.0 * PI * 1480.0 * t) * 0.55
                val click = sin(2.0 * PI * 3850.0 * t) * 0.35
                val air = sin(2.0 * PI * 6400.0 * t) * 0.12
                ((body + click + air) * env * 0.34 * Short.MAX_VALUE)
                    .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        private fun synthesizeLiquidGlide(): ShortArray {
            val durSec = 0.068
            val count = (TACTILE_RATE * durSec).toInt()
            var phase = 0.0
            return ShortArray(count) { i ->
                val p = i.toDouble() / count
                val freq = 380.0 + 740.0 * sin(p * PI * 0.65)
                phase += (2.0 * PI * freq) / TACTILE_RATE
                val env = sin(p * PI) * exp(-p * 1.9)
                val shimmer = sin(phase) * 0.65 + sin(phase * 2.02) * 0.25 + sin(phase * 3.0) * 0.10
                (shimmer * env * 0.22 * Short.MAX_VALUE)
                    .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        private fun synthesizeDroplet(startHz: Double, endHz: Double): ShortArray {
            val durSec = 0.038
            val count = (TACTILE_RATE * durSec).toInt()
            var phase = 0.0
            return ShortArray(count) { i ->
                val p = i.toDouble() / count
                val freq = startHz + (endHz - startHz) * p * p
                phase += (2.0 * PI * freq) / TACTILE_RATE
                val env = sin(p * PI) * exp(-p * 2.2)
                val s = (sin(phase) * 0.78 + sin(phase * 2.0) * 0.22) * env * 0.28
                (s * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        private fun synthesizeZenBell(): ShortArray {
            val durSec = 1.45
            val count = (TACTILE_RATE * durSec).toInt()
            return ShortArray(count) { i ->
                val t = i.toDouble() / TACTILE_RATE
                val attack = 1.0 - exp(-t * 210.0)
                val fundamental = sin(2.0 * PI * 432.0 * t) * exp(-t * 2.1) * 0.58
                val overtone1 = sin(2.0 * PI * 866.0 * t) * exp(-t * 3.4) * 0.26
                val overtone2 = sin(2.0 * PI * 1296.0 * t) * exp(-t * 5.2) * 0.14
                val shimmer = sin(2.0 * PI * 2160.0 * t) * exp(-t * 8.0) * 0.07
                val sample = (fundamental + overtone1 + overtone2 + shimmer) * attack * 0.42
                (sample.coerceIn(-0.92, 0.92) * Short.MAX_VALUE).toInt().toShort()
            }
        }

        private fun synthesizeArpeggio(
            freqs: List<Double>,
            stepMs: Int,
            tailMs: Int,
            gain: Double
        ): ShortArray {
            val stepSamples = (TACTILE_RATE * stepMs) / 1000
            val tailSamples = (TACTILE_RATE * tailMs) / 1000
            val totalSamples = stepSamples * freqs.size + tailSamples
            val mix = DoubleArray(totalSamples)

            freqs.forEachIndexed { idx, hz ->
                val offset = idx * stepSamples
                val noteLen = totalSamples - offset
                for (i in 0 until noteLen) {
                    val t = i.toDouble() / TACTILE_RATE
                    val env = (1.0 - exp(-t * 160.0)) * exp(-t * 6.8)
                    val tone = sin(2.0 * PI * hz * t) * 0.72 +
                        sin(2.0 * PI * hz * 2.0 * t) * 0.20 +
                        sin(2.0 * PI * hz * 3.0 * t) * 0.08
                    mix[offset + i] += tone * env * gain
                }
            }

            return ShortArray(totalSamples) { i ->
                (mix[i].coerceIn(-0.92, 0.92) * Short.MAX_VALUE).toInt().toShort()
            }
        }
    }
}
