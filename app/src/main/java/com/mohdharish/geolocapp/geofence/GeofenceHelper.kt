package com.mohdharish.geolocapp.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.mohdharish.geolocapp.data.GeofenceEntity

class GeofenceHelper(private val context: Context) {

    private val TAG = "GeofenceHelper"

    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        // Use FLAG_IMMUTABLE for Android S+ (API 31+) if the intent is not meant to be modified.
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun getGeofence(geofenceEntity: GeofenceEntity): Geofence {
        return Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(geofenceEntity.id.toString()) // Assuming GeofenceEntity has an 'id' field
            .setCircularRegion(
                geofenceEntity.latitude,
                geofenceEntity.longitude,
                geofenceEntity.radius
            )
            // Set the expiration duration of the geofence. This geofence only exists for
            // the limited amount of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Or a specific duration
            // Set the transition types of interest. Alerts are only generated for these
            // transition.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            // Create the geofence.
            .build()
    }

    fun getGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()
    }

    fun getGeofencingRequestForSingle(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()
    }
}
