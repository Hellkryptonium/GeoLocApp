package com.mohdharish.geolocapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Insert
    suspend fun insertGeofence(geofence: GeofenceEntity)

    @Query("SELECT * FROM geofences")
    fun getAllGeofences(): Flow<List<GeofenceEntity>>

    @Query("DELETE FROM geofences")
    suspend fun deleteAllGeofences()

    @Query("DELETE FROM geofences WHERE id = :id")
    suspend fun deleteGeofence(id: Int)

    // Potentially add other queries like getGeofenceById, deleteGeofence, etc. later
}
