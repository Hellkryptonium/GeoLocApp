package com.mohdharish.geolocapp.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.app.Service
import android.media.Ringtone
import android.os.Handler
import android.os.Looper
import com.mohdharish.geolocapp.settings.AlarmSettingsManager // Added import

object AlarmUtils {
    private const val TAG = "AlarmUtils"
    private var ringtone: Ringtone? = null
    private var isRepeating = false
    private var repeatHandler: Handler? = null
    private var repeatRunnable: Runnable? = null

    fun playAlarmSound(context: Context, repeat: Boolean = false) {
        val settingsManager = AlarmSettingsManager(context)
        if (!settingsManager.isAlarmSoundEnabled()) {
            Log.d(TAG, "Alarm sound is disabled in settings.")
            return
        }
        try {
            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
            ringtone = RingtoneManager.getRingtone(context, alarmSound)
            ringtone?.play()
            Log.d(TAG, "Playing alarm sound.")
            if (repeat) {
                isRepeating = true
                repeatHandler = Handler(Looper.getMainLooper())
                repeatRunnable = Runnable {
                    if (isRepeating) {
                        ringtone?.play()
                        repeatHandler?.postDelayed(repeatRunnable!!, 5000)
                    }
                }
                repeatHandler?.postDelayed(repeatRunnable!!, 5000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }
    }

    fun stopAlarm() {
        isRepeating = false
        repeatHandler?.removeCallbacks(repeatRunnable!!)
        ringtone?.stop()
        ringtone = null
    }

    fun snoozeAlarm(snoozeMillis: Long = 60000) {
        stopAlarm()
        Handler(Looper.getMainLooper()).postDelayed({
            ringtone?.play()
        }, snoozeMillis)
    }

    fun vibrateDevice(context: Context) {
        val settingsManager = AlarmSettingsManager(context)
        if (!settingsManager.isAlarmVibrationEnabled()) {
            Log.d(TAG, "Alarm vibration is disabled in settings.")
            return
        }
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (vibrator?.hasVibrator() == true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibrate with a pattern: 0ms delay, 500ms vibrate, 500ms pause, 500ms vibrate
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500, 500), -1))
                } else {
                    // For older APIs (deprecated in API 26)
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 500, 500), -1)
                }
                Log.d(TAG, "Device vibrated.")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during vibration. Missing VIBRATE permission or restricted by OS.", e)
            }
            catch (e: Exception) {
                Log.e(TAG, "Error during vibration", e)
            }
        } else {
            Log.d(TAG, "Device does not have a vibrator or VIBRATE permission might be missing.")
        }
    }
}
