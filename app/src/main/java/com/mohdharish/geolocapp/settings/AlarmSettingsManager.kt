package com.mohdharish.geolocapp.settings

import android.content.Context
import android.content.SharedPreferences

class AlarmSettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("GeoLocApp_AlarmSettings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_ALARM_SOUND_ENABLED = "alarm_sound_enabled"
        const val KEY_ALARM_VIBRATION_ENABLED = "alarm_vibration_enabled"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled" // General notifications for the app
    }

    fun setAlarmSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ALARM_SOUND_ENABLED, enabled).apply()
    }

    fun isAlarmSoundEnabled(defaultValue: Boolean = true): Boolean {
        return sharedPreferences.getBoolean(KEY_ALARM_SOUND_ENABLED, defaultValue)
    }

    fun setAlarmVibrationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ALARM_VIBRATION_ENABLED, enabled).apply()
    }

    fun isAlarmVibrationEnabled(defaultValue: Boolean = true): Boolean {
        return sharedPreferences.getBoolean(KEY_ALARM_VIBRATION_ENABLED, defaultValue)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areNotificationsEnabled(defaultValue: Boolean = true): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, defaultValue)
    }
}
