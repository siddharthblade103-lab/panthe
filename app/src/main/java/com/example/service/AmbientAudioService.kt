package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AmbientSoundPreset

private const val TAG = "AmbientAudioService"

/**
 * Play-compliant Foreground Service (mediaPlayback) that keeps ambient study audio
 * (White Noise, Rain, Forest Sounds, Brown Noise, Alpha 10Hz) playing reliably in the
 * background during Pomodoro sessions with notification controls and Android Audio Focus.
 */
class AmbientAudioService : Service() {

    private val engine: AmbientSoundEngine
        get() = AmbientSoundEngine.shared

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain: Boolean = false
    private var noisyReceiverRegistered: Boolean = false
    private var currentSubtitle: String = "Playing ambient focus sound"

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                handlePause(reasonSubtitle = "Paused (audio output changed)")
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                engine.setDucked(true)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (engine.isPlaying.value) {
                    resumeOnFocusGain = true
                    engine.pause()
                    updateNotification()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                handlePause(reasonSubtitle = "Paused by another audio app")
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                engine.setDucked(false)
                if (resumeOnFocusGain && engine.selectedPreset.value != AmbientSoundPreset.OFF) {
                    resumeOnFocusGain = false
                    engine.resume()
                    updateNotification()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        ensureNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val presetName = intent.getStringExtra(EXTRA_PRESET)
                val preset = AmbientSoundPreset.fromWire(presetName)
                val volume = intent.getFloatExtra(EXTRA_VOLUME, -1f)
                val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)
                if (!subtitle.isNullOrBlank()) {
                    currentSubtitle = subtitle
                }
                if (volume in 0f..1f) {
                    engine.setVolume(volume)
                }
                handlePlay(preset)
            }

            ACTION_PAUSE -> {
                val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)
                handlePause(reasonSubtitle = subtitle ?: "Paused — Tap Play to resume")
            }

            ACTION_RESUME -> {
                val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)
                if (!subtitle.isNullOrBlank()) {
                    currentSubtitle = subtitle
                }
                val current = engine.selectedPreset.value
                if (current != AmbientSoundPreset.OFF) {
                    handlePlay(current)
                } else {
                    handleStop()
                }
            }

            ACTION_TOGGLE_PLAY_PAUSE -> {
                if (engine.isPlaying.value) {
                    handlePause("Paused — Tap Play to resume")
                } else {
                    val current = engine.selectedPreset.value
                    if (current != AmbientSoundPreset.OFF) {
                        currentSubtitle = "Focus ambient audio active"
                        handlePlay(current)
                    } else {
                        handleStop()
                    }
                }
            }

            ACTION_UPDATE_STATUS -> {
                val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)
                if (!subtitle.isNullOrBlank()) {
                    currentSubtitle = subtitle
                }
                if (engine.selectedPreset.value != AmbientSoundPreset.OFF) {
                    updateNotification()
                }
            }

            ACTION_STOP -> {
                handleStop()
            }

            else -> {
                if (engine.selectedPreset.value == AmbientSoundPreset.OFF) {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun handlePlay(preset: AmbientSoundPreset) {
        if (preset == AmbientSoundPreset.OFF) {
            handleStop()
            return
        }
        resumeOnFocusGain = false
        val focusGranted = requestAudioFocus()
        if (!focusGranted) {
            Log.w(TAG, "Audio focus denied; starting softly")
        }
        registerNoisyReceiver()
        engine.setDucked(false)
        engine.play(preset)
        promoteToForeground(buildNotification(preset, isPlaying = true))
    }

    private fun handlePause(reasonSubtitle: String) {
        currentSubtitle = reasonSubtitle
        unregisterNoisyReceiver()
        engine.pause()
        val preset = engine.selectedPreset.value
        if (preset == AmbientSoundPreset.OFF) {
            handleStop()
        } else {
            promoteToForeground(buildNotification(preset, isPlaying = false))
        }
    }

    private fun handleStop() {
        resumeOnFocusGain = false
        unregisterNoisyReceiver()
        abandonAudioFocus()
        engine.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promoteToForeground(notification: Notification) {
        try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed; continuing playback safely: ${e.message}")
        }
    }

    private fun updateNotification() {
        val preset = engine.selectedPreset.value
        if (preset == AmbientSoundPreset.OFF) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            nm.notify(NOTIFICATION_ID, buildNotification(preset, engine.isPlaying.value))
        }
    }

    private fun buildNotification(preset: AmbientSoundPreset, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, AmbientAudioService::class.java).apply {
            action = ACTION_TOGGLE_PLAY_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            101,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AmbientAudioService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleLabel = if (isPlaying) {
            getString(R.string.ambient_action_pause)
        } else {
            getString(R.string.ambient_action_play)
        }
        val toggleIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle("Try Again • ${preset.displayName}")
            .setContentText(currentSubtitle)
            .setSubText(if (isPlaying) "Background Focus Audio" else "Paused")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(toggleIcon, toggleLabel, togglePendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.ambient_action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.ambient_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.ambient_notification_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerNoisyReceiver() {
        if (noisyReceiverRegistered) return
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        runCatching {
            ContextCompat.registerReceiver(
                this,
                becomingNoisyReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            noisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyReceiverRegistered) return
        runCatching { unregisterReceiver(becomingNoisyReceiver) }
        noisyReceiverRegistered = false
    }

    override fun onDestroy() {
        unregisterNoisyReceiver()
        abandonAudioFocus()
        engine.stop()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "lumina_ambient_audio"
        const val NOTIFICATION_ID = 4102

        const val ACTION_PLAY = "com.example.ambient.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ambient.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.ambient.ACTION_RESUME"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.ambient.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_UPDATE_STATUS = "com.example.ambient.ACTION_UPDATE_STATUS"
        const val ACTION_STOP = "com.example.ambient.ACTION_STOP"

        const val EXTRA_PRESET = "extra_preset"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_SUBTITLE = "extra_subtitle"

        fun play(
            context: Context,
            preset: AmbientSoundPreset,
            volume: Float,
            subtitle: String
        ) {
            if (preset == AmbientSoundPreset.OFF) {
                stop(context)
                return
            }
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, AmbientAudioService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_PRESET, preset.name)
                putExtra(EXTRA_VOLUME, volume.coerceIn(0f, 1f))
                putExtra(EXTRA_SUBTITLE, subtitle)
            }
            val started = runCatching {
                ContextCompat.startForegroundService(appCtx, intent)
            }.isSuccess
            if (!started) {
                // Fallback to direct engine playback if OS throttles FGS launch from background
                AmbientSoundEngine.shared.setVolume(volume)
                AmbientSoundEngine.shared.play(preset)
            }
        }

        fun pause(context: Context, subtitle: String = "Paused — Tap Play to resume") {
            val appCtx = context.applicationContext
            if (AmbientSoundEngine.shared.selectedPreset.value == AmbientSoundPreset.OFF) return
            val intent = Intent(appCtx, AmbientAudioService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_SUBTITLE, subtitle)
            }
            val sent = runCatching { appCtx.startService(intent) }.isSuccess
            if (!sent) {
                AmbientSoundEngine.shared.pause()
            }
        }

        fun resume(context: Context, subtitle: String = "Playing ambient focus sound") {
            val appCtx = context.applicationContext
            val preset = AmbientSoundEngine.shared.selectedPreset.value
            if (preset == AmbientSoundPreset.OFF) return
            val intent = Intent(appCtx, AmbientAudioService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_SUBTITLE, subtitle)
            }
            val started = runCatching {
                ContextCompat.startForegroundService(appCtx, intent)
            }.isSuccess
            if (!started) {
                AmbientSoundEngine.shared.resume()
            }
        }

        fun updateStatus(context: Context, subtitle: String) {
            if (AmbientSoundEngine.shared.selectedPreset.value == AmbientSoundPreset.OFF) return
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, AmbientAudioService::class.java).apply {
                action = ACTION_UPDATE_STATUS
                putExtra(EXTRA_SUBTITLE, subtitle)
            }
            runCatching { appCtx.startService(intent) }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            AmbientSoundEngine.shared.stop()
            val intent = Intent(appCtx, AmbientAudioService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { appCtx.stopService(intent) }
        }
    }
}
