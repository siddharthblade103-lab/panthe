package com.example.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.data.model.AmbientSoundPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private const val TAG = "AmbientSoundEngine"
private const val SAMPLE_RATE = 22050

/**
 * Procedural stereo ambient sound synthesizer for Try Again:
 * Rain, White Noise, Forest Breeze, Ocean Tide, Deep Brown Noise, Alpha 10Hz, and Cozy Fireplace.
 */
class AmbientSoundEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var playbackJob: Job? = null

    @Volatile
    private var targetVolume: Float = 0.65f

    @Volatile
    private var duckMultiplier: Float = 1.0f

    private val _selectedPreset = MutableStateFlow(AmbientSoundPreset.OFF)
    val selectedPreset: StateFlow<AmbientSoundPreset> = _selectedPreset.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _waveAmplitude = MutableStateFlow(0f)
    val waveAmplitude: StateFlow<Float> = _waveAmplitude.asStateFlow()

    fun setVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
    }

    fun setDucked(ducked: Boolean) {
        duckMultiplier = if (ducked) 0.28f else 1.0f
    }

    fun play(preset: AmbientSoundPreset) {
        if (preset == AmbientSoundPreset.OFF) {
            stop()
            return
        }
        synchronized(lock) {
            if (_selectedPreset.value == preset && _isPlaying.value && playbackJob?.isActive == true) {
                return
            }
            cancelPlaybackLocked()
            _selectedPreset.value = preset
            _isPlaying.value = true
            startPlaybackLocked(preset)
        }
    }

    fun pause() {
        synchronized(lock) {
            if (!_isPlaying.value) return
            cancelPlaybackLocked()
            _isPlaying.value = false
            _waveAmplitude.value = 0f
        }
    }

    fun resume() {
        synchronized(lock) {
            val preset = _selectedPreset.value
            if (preset == AmbientSoundPreset.OFF) return
            if (_isPlaying.value && playbackJob?.isActive == true) return
            cancelPlaybackLocked()
            _isPlaying.value = true
            startPlaybackLocked(preset)
        }
    }

    fun stop() {
        synchronized(lock) {
            cancelPlaybackLocked()
            _isPlaying.value = false
            _selectedPreset.value = AmbientSoundPreset.OFF
            _waveAmplitude.value = 0f
        }
    }

    private fun cancelPlaybackLocked() {
        playbackJob?.cancel()
        playbackJob = null
    }

    private fun startPlaybackLocked(preset: AmbientSoundPreset) {
        playbackJob = engineScope.launch {
            val minBufBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufBytes * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build AudioTrack", e)
                _isPlaying.value = false
                _waveAmplitude.value = 0f
                return@launch
            }

            try {
                track.play()
                val framesPerChunk = 1024
                val pcmBuffer = ShortArray(framesPerChunk * 2)

                var smoothL = 0.0
                var smoothR = 0.0
                var brownL = 0.0
                var brownR = 0.0
                var lfoPhase = 0.0
                var phaseL = 0.0
                var phaseR = 0.0
                var impulseTimer = 0
                var impulseAmp = 0.0
                var birdTimer = 0
                var birdLength = 1
                var birdBaseFreq = 2100.0
                var birdPhase = 0.0
                var birdPan = 0.5
                var currentGain = 0.0

                while (isActive) {
                    val desiredGain = (targetVolume * duckMultiplier).toDouble()
                    var energyAccum = 0.0

                    for (i in 0 until framesPerChunk) {
                        currentGain += (desiredGain - currentGain) * 0.0025

                        val (sampleL, sampleR) = when (preset) {
                            AmbientSoundPreset.WHITE_NOISE -> {
                                val rawL = Random.nextDouble(-1.0, 1.0)
                                val rawR = Random.nextDouble(-1.0, 1.0)
                                smoothL = smoothL * 0.34 + rawL * 0.66
                                smoothR = smoothR * 0.34 + rawR * 0.66
                                (smoothL * 0.52).coerceIn(-0.9, 0.9) to (smoothR * 0.52).coerceIn(-0.9, 0.9)
                            }

                            AmbientSoundPreset.RAIN -> {
                                val whiteL = Random.nextDouble(-1.0, 1.0)
                                val whiteR = Random.nextDouble(-1.0, 1.0)
                                brownL = (brownL + 0.08 * whiteL) / 1.08
                                brownR = (brownR + 0.08 * whiteR) / 1.08
                                if (impulseTimer <= 0 && Random.nextInt(1650) == 0) {
                                    impulseTimer = 130
                                    impulseAmp = Random.nextDouble(0.14, 0.34)
                                }
                                val drop = if (impulseTimer > 0) {
                                    impulseTimer--
                                    impulseAmp * (impulseTimer / 130.0) * sin(impulseTimer * 0.44)
                                } else 0.0
                                (brownL * 1.9 + whiteL * 0.14 + drop).coerceIn(-0.95, 0.95) to
                                    (brownR * 1.9 + whiteR * 0.14 - drop * 0.7).coerceIn(-0.95, 0.95)
                            }

                            AmbientSoundPreset.FOREST_WIND -> {
                                lfoPhase += (2.0 * PI * 0.13) / SAMPLE_RATE
                                val swell = 0.42 + 0.58 * (0.5 + 0.5 * sin(lfoPhase))
                                val whiteL = Random.nextDouble(-1.0, 1.0)
                                val whiteR = Random.nextDouble(-1.0, 1.0)
                                brownL = (brownL + 0.045 * whiteL) / 1.045
                                brownR = (brownR + 0.045 * whiteR) / 1.045
                                smoothL = smoothL * 0.72 + whiteL * 0.28
                                smoothR = smoothR * 0.72 + whiteR * 0.28
                                if (birdTimer <= 0 && Random.nextInt(SAMPLE_RATE * 5) == 0) {
                                    birdLength = Random.nextInt(3200, 5600)
                                    birdTimer = birdLength
                                    birdBaseFreq = Random.nextDouble(1750.0, 2650.0)
                                    birdPhase = 0.0
                                    birdPan = Random.nextDouble(0.2, 0.8)
                                }
                                var birdL = 0.0
                                var birdR = 0.0
                                if (birdTimer > 0) {
                                    val progress = 1.0 - (birdTimer.toDouble() / birdLength.toDouble())
                                    val env = sin(progress * PI) * sin(progress * PI)
                                    val freqSweep = birdBaseFreq + sin(progress * PI * 3.0) * 320.0
                                    birdPhase += (2.0 * PI * freqSweep) / SAMPLE_RATE
                                    val chirp = sin(birdPhase) * env * 0.11
                                    birdL = chirp * (1.0 - birdPan)
                                    birdR = chirp * birdPan
                                    birdTimer--
                                }
                                (brownL * 2.5 * swell + smoothL * 0.07 * swell + birdL).coerceIn(-0.92, 0.92) to
                                    (brownR * 2.5 * swell + smoothR * 0.07 * swell + birdR).coerceIn(-0.92, 0.92)
                            }

                            AmbientSoundPreset.OCEAN_WAVES -> {
                                lfoPhase += (2.0 * PI * 0.105) / SAMPLE_RATE
                                val wave = 0.25 + 0.75 * (0.5 + 0.5 * sin(lfoPhase)) * (0.5 + 0.5 * sin(lfoPhase))
                                val whiteL = Random.nextDouble(-1.0, 1.0)
                                val whiteR = Random.nextDouble(-1.0, 1.0)
                                brownL = (brownL + 0.05 * whiteL) / 1.05
                                brownR = (brownR + 0.05 * whiteR) / 1.05
                                smoothL = smoothL * 0.65 + whiteL * 0.35
                                smoothR = smoothR * 0.65 + whiteR * 0.35
                                (brownL * 2.8 * wave + smoothL * 0.18 * wave * wave).coerceIn(-0.92, 0.92) to
                                    (brownR * 2.8 * wave + smoothR * 0.18 * wave * wave).coerceIn(-0.92, 0.92)
                            }

                            AmbientSoundPreset.BROWN_NOISE -> {
                                val whiteL = Random.nextDouble(-1.0, 1.0)
                                val whiteR = Random.nextDouble(-1.0, 1.0)
                                brownL = (brownL + 0.03 * whiteL) / 1.03
                                brownR = (brownR + 0.03 * whiteR) / 1.03
                                (brownL * 3.2).coerceIn(-0.9, 0.9) to (brownR * 3.2).coerceIn(-0.9, 0.9)
                            }

                            AmbientSoundPreset.ALPHA_WAVES -> {
                                phaseL += (2.0 * PI * 210.0) / SAMPLE_RATE
                                phaseR += (2.0 * PI * 220.0) / SAMPLE_RATE
                                val white = Random.nextDouble(-1.0, 1.0)
                                brownL = (brownL + 0.02 * white) / 1.02
                                val toneL = sin(phaseL) * 0.34 + sin(phaseL * 0.5) * 0.14 + brownL * 0.5
                                val toneR = sin(phaseR) * 0.34 + sin(phaseR * 0.5) * 0.14 + brownL * 0.5
                                toneL.coerceIn(-0.9, 0.9) to toneR.coerceIn(-0.9, 0.9)
                            }

                            AmbientSoundPreset.FIREPLACE -> {
                                val whiteL = Random.nextDouble(-1.0, 1.0)
                                val whiteR = Random.nextDouble(-1.0, 1.0)
                                brownL = (brownL + 0.025 * whiteL) / 1.025
                                brownR = (brownR + 0.025 * whiteR) / 1.025
                                if (impulseTimer <= 0 && Random.nextInt(1900) == 0) {
                                    impulseTimer = Random.nextInt(25, 80)
                                    impulseAmp = Random.nextDouble(0.22, 0.55)
                                    birdPan = Random.nextDouble(0.15, 0.85)
                                }
                                val crackle = if (impulseTimer > 0) {
                                    impulseTimer--
                                    Random.nextDouble(-1.0, 1.0) * impulseAmp * (impulseTimer / 80.0)
                                } else 0.0
                                (brownL * 2.8 + crackle * (1.0 - birdPan)).coerceIn(-0.92, 0.92) to
                                    (brownR * 2.8 + crackle * birdPan).coerceIn(-0.92, 0.92)
                            }

                            AmbientSoundPreset.OFF -> 0.0 to 0.0
                        }

                        val scaledL = (sampleL * currentGain * Short.MAX_VALUE * 0.55).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        val scaledR = (sampleR * currentGain * Short.MAX_VALUE * 0.55).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                        pcmBuffer[i * 2] = scaledL
                        pcmBuffer[i * 2 + 1] = scaledR
                        energyAccum += abs(sampleL * currentGain)
                    }

                    _waveAmplitude.value = (energyAccum / framesPerChunk * 2.6).toFloat().coerceIn(0.08f, 1f)
                    val written = track.write(pcmBuffer, 0, pcmBuffer.size)
                    if (written < 0) break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ambient audio stream stopped: ${e.message}")
            } finally {
                runCatching { track.pause() }
                runCatching { track.flush() }
                runCatching { track.release() }
                _waveAmplitude.value = 0f
            }
        }
    }

    companion object {
        val shared: AmbientSoundEngine by lazy { AmbientSoundEngine() }
    }
}
