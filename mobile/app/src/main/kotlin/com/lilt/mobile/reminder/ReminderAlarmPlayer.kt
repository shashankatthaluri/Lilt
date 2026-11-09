package com.lilt.mobile.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.lilt.mobile.data.DiagnosticLogger

object ReminderAlarmPlayer {
    private const val TAG = "ReminderAlarmPlayer"
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    private val VIBRATION_PATTERN = longArrayOf(0, 600, 300, 600, 300, 600)

    @Synchronized
    fun play(context: Context, isSilent: Boolean = false) {
        if (isPlaying) return
        isPlaying = true
        DiagnosticLogger.log(TAG, "Starting alarm (isSilent=$isSilent)")

        if (!isSilent) {
            try {
                // 1. Resolve Alarm Sound Uri
                val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = true
                    }
                    play()
                }
            } catch (e: Exception) {
                DiagnosticLogger.logError(TAG, "Failed to start ringtone: ${e.message}", e)
            }
        }

        try {
            // 2. Start Rhythmic Vibration
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator = vib
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(VIBRATION_PATTERN, 0)
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError(TAG, "Failed to start vibration: ${e.message}", e)
        }

        // 3. 60-Second Battery Safety Timeout
        autoStopRunnable = Runnable {
            DiagnosticLogger.log(TAG, "Safety timeout reached (60s). Stopping alarm automatically.")
            stop()
        }
        handler.postDelayed(autoStopRunnable!!, 60_000)
    }

    @Synchronized
    fun silenceAudio() {
        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError(TAG, "Error silencing audio: ${e.message}")
        }
        ringtone = null
        DiagnosticLogger.log(TAG, "Alarm audio silenced (vibration & UI remain active)")
    }

    @Synchronized
    fun stop() {
        if (!isPlaying) return
        isPlaying = false
        DiagnosticLogger.log(TAG, "Stopping alarm audio and vibration")

        autoStopRunnable?.let {
            handler.removeCallbacks(it)
            autoStopRunnable = null
        }

        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError(TAG, "Error stopping ringtone: ${e.message}")
        }
        ringtone = null

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            DiagnosticLogger.logError(TAG, "Error stopping vibrator: ${e.message}")
        }
        vibrator = null
    }

    fun isAlarmActive(): Boolean = isPlaying
}
