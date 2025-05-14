package com.mohdharish.geolocapp.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.mohdharish.geolocapp.data.AppDatabase
import com.mohdharish.geolocapp.data.GeofenceEntity
import com.mohdharish.geolocapp.geofence.GeofenceHelper
import com.mohdharish.geolocapp.util.AlarmUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MapViewModel"
    }

    private val appContext = application.applicationContext

    // Permission States
    private val _hasFineLocationPermission = MutableStateFlow(checkFineLocationPermission(appContext))
    val hasFineLocationPermission: StateFlow<Boolean> = _hasFineLocationPermission.asStateFlow()

    private val _hasBackgroundLocationPermission = MutableStateFlow(checkBackgroundLocationPermission(appContext))
    val hasBackgroundLocationPermission: StateFlow<Boolean> = _hasBackgroundLocationPermission.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(checkNotificationPermission(appContext))
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    // Combined state for geofencing readiness
    val canRegisterGeofences: StateFlow<Boolean> =
        combine(hasFineLocationPermission, hasBackgroundLocationPermission) { fine, background ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fine && background
            } else {
                fine
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    private val _pinLocation = MutableStateFlow<LatLng?>(null)
    val pinLocation: StateFlow<LatLng?> = _pinLocation

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _autocompleteSuggestions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val autocompleteSuggestions: StateFlow<List<AutocompletePrediction>> = _autocompleteSuggestions

    private val _cameraMoveRequest = MutableStateFlow<LatLng?>(null)
    val cameraMoveRequest: StateFlow<LatLng?> = _cameraMoveRequest

    private val _isCheckingCurrentLocation = MutableStateFlow(false)
    val isCheckingCurrentLocation: StateFlow<Boolean> = _isCheckingCurrentLocation
    private val _isInsideGeofence = MutableStateFlow<Boolean?>(null)
    val isInsideGeofence: StateFlow<Boolean?> = _isInsideGeofence
    private val _checkCurrentLocationStatus = MutableStateFlow<String?>(null)
    val checkCurrentLocationStatus: StateFlow<String?> = _checkCurrentLocationStatus

    private val geofenceDao = AppDatabase.getDatabase(application).geofenceDao()
    private lateinit var placesClient: PlacesClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceHelper: GeofenceHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    val allGeofences: StateFlow<List<GeofenceEntity>> = geofenceDao.getAllGeofences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        if (!Places.isInitialized()) {
            Places.initialize(appContext, "AIzaSyBjpUgf5w7UMenU6nccd0pOvOFOEILFyac")
        }
        placesClient = Places.createClient(application)

        geofencingClient = LocationServices.getGeofencingClient(application)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        geofenceHelper = GeofenceHelper(appContext)

        // Initial permission checks
        updatePermissionStates()


        viewModelScope.launch {
            // React to changes in geofences from DB AND permission status
            allGeofences.combine(canRegisterGeofences) { geofenceEntities, canRegister ->
                Pair(geofenceEntities, canRegister)
            }.collect { (geofenceEntities, canRegister) ->
                if (canRegister) {
                    updateSystemGeofences(geofenceEntities)
                } else {
                    Log.e(TAG, "Required geofencing permissions not granted. Geofences will not be registered or updated.")
                    // Attempt to remove any existing geofences if permissions are revoked
                    if (checkFineLocationPermission(appContext)) { // Need at least fine to attempt removal
                         removeAllSystemGeofences()
                    }
                }
            }
        }
    }

    fun updatePermissionStates() {
        _hasFineLocationPermission.value = checkFineLocationPermission(appContext)
        _hasBackgroundLocationPermission.value = checkBackgroundLocationPermission(appContext)
        _hasNotificationPermission.value = checkNotificationPermission(appContext)
        Log.d(TAG, "Permissions updated: Fine=${_hasFineLocationPermission.value}, Bg=${_hasBackgroundLocationPermission.value}, Notif=${_hasNotificationPermission.value}")
    }

    private fun checkFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android Q
        }
    }

    private fun checkNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android Tiramisu
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentDeviceLocation(): LatLng? {
        if (!hasFineLocationPermission.value) {
            Log.w(TAG, "Cannot get current location without fine location permission.")
            return null
        }
        return try {
            // Use getCurrentLocation for a fresh location. Priority can be adjusted.
            val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (location != null) {
                LatLng(location.latitude, location.longitude)
            } else {
                Log.w(TAG, "FusedLocationProviderClient returned null location.")
                // Fallback to lastLocation if getCurrentLocation fails, though it might be stale.
                val lastLocation = fusedLocationClient.lastLocation.await()
                lastLocation?.let { LatLng(it.latitude, it.longitude) }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting current location.", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting current location.", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateSystemGeofences(entities: List<GeofenceEntity>) {
        // Permission check is now handled by the 'canRegisterGeofences' flow
        Log.d(TAG, "Updating system geofences with ${entities.size} entities.")

        if (entities.isEmpty()) {
            removeAllSystemGeofences()
            return
        }

        val gmsGeofences = entities.mapNotNull { entity ->
            if (entity.id != 0) {
                geofenceHelper.getGeofence(entity)
            } else {
                Log.w(TAG, "GeofenceEntity with ID 0 encountered, skipping.")
                null
            }
        }
        
        if (gmsGeofences.isEmpty()) {
            Log.i(TAG, "No valid geofences to register after filtering.")
            // If entities were present but all had invalid IDs, ensure any existing are cleared.
            // This case should ideally be rare if DB IDs are managed well.
            // Consider if removing all is correct if entities list was non-empty but gmsGeofences is.
            // For now, if no valid GMS geofences, we don't add. If entities was empty, removeAllSystemGeofences was already called.
            return
        }

        val geofencingRequest = geofenceHelper.getGeofencingRequest(gmsGeofences)
        geofencingClient.addGeofences(geofencingRequest, geofenceHelper.geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.i(TAG, "${gmsGeofences.size} geofences successfully registered/updated.")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "Failed to register/update geofences: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun removeAllSystemGeofences() {
        if (!checkFineLocationPermission(appContext)) {
             Log.e(TAG, "Cannot remove system geofences without fine location permission.")
             return
        }
        geofencingClient.removeGeofences(geofenceHelper.geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.i(TAG, "All geofences successfully removed from system.")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofences from system: ${e.message}")
            }
        }
    }

    fun setPinLocation(latLng: LatLng?) {
        _pinLocation.value = latLng
        if (latLng != null) {
            _searchQuery.value = "" // Clear search query only if setting a new pin
            _autocompleteSuggestions.value = emptyList() // Clear suggestions
            _cameraMoveRequest.value = null // Clear any pending camera move request as user manually pinned
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length > 2) {
            fetchAutocompletePredictions(query)
        } else {
            _autocompleteSuggestions.value = emptyList()
        }
    }

    private fun fetchAutocompletePredictions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response -> 
            _autocompleteSuggestions.value = response.autocompletePredictions
        }.addOnFailureListener { exception -> 
            Log.e("MapViewModel", "Place autocomplete prediction failed", exception)
            _autocompleteSuggestions.value = emptyList()
        }
    }

    fun getPlaceDetailsAndRequestCameraMove(placeId: String) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request).addOnSuccessListener { response -> 
            val place = response.place
            place.latLng?.let {
                _cameraMoveRequest.value = it // Request camera move
                // _pinLocation.value = it // Do not set pin here
            }
            _searchQuery.value = "" // Clear search query
            _autocompleteSuggestions.value = emptyList() // Clear suggestions
        }.addOnFailureListener { exception -> 
            Log.e("MapViewModel", "Fetch place failed", exception)
        }
    }

    fun consumeCameraMoveRequest() {
        _cameraMoveRequest.value = null
    }

    fun saveGeofence(latitude: Double, longitude: Double, radius: Float) {
        viewModelScope.launch {
            val geofence = GeofenceEntity(latitude = latitude, longitude = longitude, radius = radius)
            val geofenceId = geofenceDao.insertGeofence(geofence)
            Log.d(TAG, "Geofence saved to DB: ID $geofenceId, Lat: $latitude, Lon: $longitude, Radius: $radius")
            // System update will be triggered by the allGeofences.combine(canRegisterGeofences) collector

            // Now check if current location is within this new geofence
            if (hasFineLocationPermission.value) {
                val currentLatLng = getCurrentDeviceLocation()
                if (currentLatLng != null) {
                    val geofenceCenter = Location("geofenceCenter").apply {
                        this.latitude = latitude
                        this.longitude = longitude
                    }
                    val currentLocation = Location("currentLocation").apply {
                        this.latitude = currentLatLng.latitude
                        this.longitude = currentLatLng.longitude
                    }

                    val distance = currentLocation.distanceTo(geofenceCenter)
                    Log.d(TAG, "Distance to new geofence center: $distance meters. Radius: $radius meters.")

                    if (distance <= radius) {
                        Log.i(TAG, "Current location is inside the newly saved geofence. Triggering alarm.")
                        AlarmUtils.playAlarmSound(appContext)
                        AlarmUtils.vibrateDevice(appContext)
                    } else {
                        Log.i(TAG, "Current location is outside the newly saved geofence.")
                    }
                } else {
                    Log.w(TAG, "Could not get current device location to check against new geofence.")
                }
            } else {
                Log.w(TAG, "Fine location permission not granted. Cannot check if inside new geofence.")
            }
        }
    }

    fun clearAllGeofences() {
        viewModelScope.launch {
            geofenceDao.deleteAllGeofences()
            Log.d(TAG, "All geofences cleared from DB.")
            // System update will be triggered by the allGeofences.combine(canRegisterGeofences) collector
        }
    }

    fun requestCurrentLocationAndPin() {
        viewModelScope.launch {
            _isCheckingCurrentLocation.value = true
            _checkCurrentLocationStatus.value = null
            _isInsideGeofence.value = null
            try {
                val currentLatLng = getCurrentDeviceLocation()
                if (currentLatLng != null) {
                    setPinLocation(currentLatLng)
                    _checkCurrentLocationStatus.value = "Pinned your current location."
                } else {
                    _checkCurrentLocationStatus.value = "Could not get current location."
                }
            } catch (e: Exception) {
                _checkCurrentLocationStatus.value = "Error getting current location."
            } finally {
                _isCheckingCurrentLocation.value = false
            }
        }
    }

    fun checkIfInsideAnyGeofence() {
        viewModelScope.launch {
            _isCheckingCurrentLocation.value = true
            _checkCurrentLocationStatus.value = null
            _isInsideGeofence.value = null
            try {
                val currentLatLng = getCurrentDeviceLocation()
                if (currentLatLng == null) {
                    _checkCurrentLocationStatus.value = "Could not get current location."
                    _isCheckingCurrentLocation.value = false
                    return@launch
                }
                val geofences = allGeofences.value
                if (geofences.isEmpty()) {
                    _checkCurrentLocationStatus.value = "No geofences saved."
                    _isCheckingCurrentLocation.value = false
                    return@launch
                }
                var found = false
                for (geofence in geofences) {
                    val geofenceCenter = android.location.Location("geofenceCenter").apply {
                        latitude = geofence.latitude
                        longitude = geofence.longitude
                    }
                    val currentLocation = android.location.Location("currentLocation").apply {
                        latitude = currentLatLng.latitude
                        longitude = currentLatLng.longitude
                    }
                    val distance = currentLocation.distanceTo(geofenceCenter)
                    if (distance <= geofence.radius) {
                        found = true
                        _checkCurrentLocationStatus.value = "You are inside geofence at Lat ${String.format("%.4f", geofence.latitude)}, Lon ${String.format("%.4f", geofence.longitude)} (radius ${geofence.radius.toInt()}m)"
                        AlarmUtils.playAlarmSound(appContext)
                        AlarmUtils.vibrateDevice(appContext)
                        break
                    }
                }
                if (!found) {
                    _checkCurrentLocationStatus.value = "You are not inside any geofence."
                }
                _isInsideGeofence.value = found
            } catch (e: Exception) {
                _checkCurrentLocationStatus.value = "Error checking geofence: ${e.localizedMessage}"
            } finally {
                _isCheckingCurrentLocation.value = false
            }
        }
    }

    fun requestBackgroundLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), 1002)
            }
        }
    }

    fun deleteGeofenceById(id: Int) {
        viewModelScope.launch {
            geofenceDao.deleteGeofence(id)
        }
    }

    fun setCameraMoveRequest(latLng: LatLng) {
        _cameraMoveRequest.value = latLng
    }
}
