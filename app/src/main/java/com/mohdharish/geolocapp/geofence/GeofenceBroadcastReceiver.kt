package com.mohdharish.geolocapp.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.mohdharish.geolocapp.MainActivity
import com.mohdharish.geolocapp.R
import com.mohdharish.geolocapp.util.AlarmUtils
import com.mohdharish.geolocapp.settings.AlarmSettingsManager // Added import

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"
    private val CHANNEL_ID = "geofence_channel"
    private val NOTIFICATION_ID_PREFIX = "geofence_notification_"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

        val settingsManager = AlarmSettingsManager(context) // Initialize settings manager
        if (!settingsManager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications are disabled in settings. Skipping geofence event processing.")
            // Optionally, still handle alarm stop/snooze if they are independent of general notifications
            val action = intent.action
            if (action == "com.mohdharish.geolocapp.ACTION_STOP_ALARM") {
                AlarmUtils.stopAlarm()
            } else if (action == "com.mohdharish.geolocapp.ACTION_SNOOZE_ALARM") {
                AlarmUtils.snoozeAlarm()
            }
            return // Exit if notifications are globally off
        }

        val action = intent.action
        if (action == "com.mohdharish.geolocapp.ACTION_STOP_ALARM") {
            AlarmUtils.stopAlarm()
            return
        } else if (action == "com.mohdharish.geolocapp.ACTION_SNOOZE_ALARM") {
            AlarmUtils.snoozeAlarm()
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorMessage = geofencingEvent?.errorCode ?: "Unknown geofence error"
            Log.e(TAG, "Geofencing event error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                val geofenceId = geofence.requestId
                val notificationTitle: String
                val notificationText: String

                when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Log.i(TAG, "Entered geofence: $geofenceId")
                        notificationTitle = "Geofence Entered"
                        notificationText = "You have entered the geofenced area: $geofenceId"
                        sendNotification(context, notificationTitle, notificationText, geofenceId.hashCode(), true)
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Log.i(TAG, "Exited geofence: $geofenceId")
                        notificationTitle = "Geofence Exited"
                        notificationText = "You have exited the geofenced area: $geofenceId"
                        sendNotification(context, notificationTitle, notificationText, geofenceId.hashCode(), false)
                    }
                    else -> {
                        Log.w(TAG, "Unknown geofence transition: $geofenceTransition for ID: $geofenceId")
                    }
                }
            }
        } else {
            Log.e(TAG, "Invalid geofence transition type: $geofenceTransition")
        }
    }

    private fun sendNotification(context: Context, title: String, message: String, notificationId: Int, playSoundAndVibrate: Boolean) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = "com.mohdharish.geolocapp.ACTION_STOP_ALARM"
        }
        val snoozeIntent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = "com.mohdharish.geolocapp.ACTION_SNOOZE_ALARM"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(context, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val snoozePendingIntent = PendingIntent.getBroadcast(context, 2, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Snooze", snoozePendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
                Log.d(TAG, "Notification sent: ID $notificationId, Title: $title")
            }

            if (playSoundAndVibrate) {
                AlarmUtils.playAlarmSound(context)
                AlarmUtils.vibrateDevice(context)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while sending notification. Missing POST_NOTIFICATIONS permission?", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during notification/alarm: ", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Geofence Alerts"
            val descriptionText = "Notifications for geofence entry and exit events"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created/updated.")
        }
    }
}
