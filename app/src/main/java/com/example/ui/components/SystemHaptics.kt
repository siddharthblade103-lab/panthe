package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Semantic tactile events mapped directly to native Android [HapticFeedbackConstants].
 */
enum class SystemHapticEvent {
    BUTTON_TAP,
    TICK,
    TOGGLE_ON,
    TOGGLE_OFF,
    SESSION_START,
    SESSION_PAUSE,
    SESSION_STOP,
    SESSION_COMPLETE,
    CONFIRM,
    REJECT,
    LONG_PRESS
}

/**
 * System-wide tactile feedback controller backed by Android [View.performHapticFeedback]
 * and [HapticFeedbackConstants] for crisp, low-latency OS-native button and session haptics.
 */
@Stable
open class SystemHapticController(
    private val view: View?,
    var enabled: Boolean = true
) {
    open fun perform(event: SystemHapticEvent) {
        if (!enabled) return
        val hostView = view ?: return
        val constant = resolveConstant(event)
        runCatching {
            hostView.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }
    }

    fun buttonTap() = perform(SystemHapticEvent.BUTTON_TAP)
    fun tick() = perform(SystemHapticEvent.TICK)
    fun toggle(on: Boolean) = perform(if (on) SystemHapticEvent.TOGGLE_ON else SystemHapticEvent.TOGGLE_OFF)
    fun sessionStart() = perform(SystemHapticEvent.SESSION_START)
    fun sessionPause() = perform(SystemHapticEvent.SESSION_PAUSE)
    fun sessionStop() = perform(SystemHapticEvent.SESSION_STOP)
    fun sessionComplete() = perform(SystemHapticEvent.SESSION_COMPLETE)
    fun confirm() = perform(SystemHapticEvent.CONFIRM)
    fun reject() = perform(SystemHapticEvent.REJECT)
    fun longPress() = perform(SystemHapticEvent.LONG_PRESS)

    companion object {
        val NoOp: SystemHapticController = object : SystemHapticController(null, false) {
            override fun perform(event: SystemHapticEvent) = Unit
        }

        fun resolveConstant(event: SystemHapticEvent): Int = when (event) {
            SystemHapticEvent.BUTTON_TAP -> HapticFeedbackConstants.VIRTUAL_KEY
            SystemHapticEvent.TICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.SEGMENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
            }
            SystemHapticEvent.TOGGLE_ON -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.TOGGLE_ON
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
            }
            SystemHapticEvent.TOGGLE_OFF -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.TOGGLE_OFF
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
            }
            SystemHapticEvent.SESSION_START,
            SystemHapticEvent.SESSION_COMPLETE,
            SystemHapticEvent.CONFIRM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            }
            SystemHapticEvent.SESSION_PAUSE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    HapticFeedbackConstants.VIRTUAL_KEY_RELEASE
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
            }
            SystemHapticEvent.SESSION_STOP,
            SystemHapticEvent.REJECT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            }
            SystemHapticEvent.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
        }
    }
}

val LocalSystemHaptics = compositionLocalOf<SystemHapticController> { SystemHapticController.NoOp }

@Composable
fun rememberSystemHapticController(enabled: Boolean): SystemHapticController {
    val view = LocalView.current
    return remember(view) { SystemHapticController(view = view, enabled = enabled) }.apply {
        this.enabled = enabled
    }
}
