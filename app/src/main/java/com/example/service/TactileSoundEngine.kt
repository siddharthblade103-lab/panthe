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
 * Apple-level crystal-clean procedural UI sound & haptic feedback engine.
 * Synthesizes crisp micro-clicks, smooth droplet toggles, gem chimes, and victory chords.
 * Never triggered on app open or app close per user preference.
 */
class TactileSoundEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var soundsEnabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

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

    /** Magical glass chord when unlocking a new soundscape or cosmetic theme. */
    fun playUnlock() {
        triggerHaptic(durationMs = 30L, amplitude = 160)
        if (!soundsEnabled) return
        playPcmAsync(unlockChordPcm)
    }

    /** Warm Duolingo-inspired triumphant major arpeggio on completing a focus session. */
    fun playSessionComplete() {
        triggerHaptic(durationMs = 45L, amplitude = 190)
        if (!soundsEnabled) return
        playPcmAsync(sessionCompletePcm)
    }

    private fun triggerHaptic(durationMs: Long, amplitude: Int) {
        if (!hapticsEnabled) return
        val vib = vibrator ?: return
        runCatching {
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        }
    }

    private fun playPcmAsync(samples: ShortArray) {
        scope.launch {
            var track: AudioTrack? = null
            try {
                val byteLength = samples.size * 2
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                val durationMs = ((samples.size.toDouble() / TACTILE_RATE) * 1000.0).toLong() + 45L
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
        private val toggleOnPcm: ShortArray by lazy { synthesizeDroplet(startHz = 540.0, endHz = 960.0) }
        private val toggleOffPcm: ShortArray by lazy { synthesizeDroplet(startHz = 720.0, endHz = 440.0) }
        private val gemSparklePcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(1046.5, 1318.5, 1568.0, 2093.0), stepMs = 58, tailMs = 240, gain = 0.24)
        }
        private val unlockChordPcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(587.33, 880.0, 1174.66, 1479.98), stepMs = 70, tailMs = 340, gain = 0.26)
        }
        private val sessionCompletePcm: ShortArray by lazy {
            synthesizeArpeggio(listOf(523.25, 659.25, 783.99, 1046.5, 1318.5), stepMs = 82, tailMs = 420, gain = 0.28)
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
                    val env = (1.0 - exp(-t * 160.0)) * exp(-t * 7.2)
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
