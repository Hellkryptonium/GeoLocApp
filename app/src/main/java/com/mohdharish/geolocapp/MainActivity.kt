package com.mohdharish.geolocapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mohdharish.geolocapp.map.MapScreen
import com.mohdharish.geolocapp.map.MapViewModel
import com.mohdharish.geolocapp.ui.theme.GeoLocAppTheme
import com.mohdharish.geolocapp.ui.MainAppUI
import com.mohdharish.geolocapp.alarm.AlarmScreen
import com.mohdharish.geolocapp.alarm.AlarmItem
import com.mohdharish.geolocapp.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    private val mapViewModel: MapViewModel by viewModels()

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            mapViewModel.updatePermissionStates()
            // Optionally, handle specific denied permissions or show rationale
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoLocAppTheme {
                // Surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val pinLocation by mapViewModel.pinLocation.collectAsState()
                    val searchQuery by mapViewModel.searchQuery.collectAsState()
                    val autocompleteSuggestions by mapViewModel.autocompleteSuggestions.collectAsState()
                    val allGeofences by mapViewModel.allGeofences.collectAsState()
                    val cameraMoveRequest by mapViewModel.cameraMoveRequest.collectAsState()

                    // Collect permission states
                    val hasFineLocationPermission by mapViewModel.hasFineLocationPermission.collectAsState()
                    val hasBackgroundLocationPermission by mapViewModel.hasBackgroundLocationPermission.collectAsState()
                    val hasNotificationPermission by mapViewModel.hasNotificationPermission.collectAsState()
                    val isCheckingCurrentLocation by mapViewModel.isCheckingCurrentLocation.collectAsState()
                    val isInsideGeofence by mapViewModel.isInsideGeofence.collectAsState()
                    val checkCurrentLocationStatus by mapViewModel.checkCurrentLocationStatus.collectAsState()
                    
                    val alarms = allGeofences.map {
                        com.mohdharish.geolocapp.alarm.AlarmItem(
                            id = it.id.toString(), // Convert Int to String
                            label = "Geofence ${it.id}", // Use a default label since GeofenceEntity has no 'name'
                            latLng = com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude),
                            radius = it.radius
                        )
                    }
                    
                    MainAppUI(
                        mapScreenContent = {
                            MapScreen(
                                pinLocation = pinLocation,
                                onMapClick = { latLng -> mapViewModel.setPinLocation(latLng) },
                                searchQuery = searchQuery,
                                onSearchQueryChange = { query -> mapViewModel.setSearchQuery(query) },
                                onSaveGeofence = { lat, lon, rad -> mapViewModel.saveGeofence(lat, lon, rad) },
                                autocompleteSuggestions = autocompleteSuggestions,
                                onSuggestionClick = { placeId -> mapViewModel.getPlaceDetailsAndRequestCameraMove(placeId) },
                                onClearSearch = { mapViewModel.setSearchQuery("") },
                                savedGeofencesCount = allGeofences.size,
                                cameraMoveRequest = cameraMoveRequest,
                                onConsumeCameraMoveRequest = { mapViewModel.consumeCameraMoveRequest() },
                                onClearPin = { mapViewModel.setPinLocation(null) },
                                onClearAllGeofences = { mapViewModel.clearAllGeofences() },
                                hasFineLocationPermission = hasFineLocationPermission,
                                hasBackgroundLocationPermission = hasBackgroundLocationPermission,
                                hasNotificationPermission = hasNotificationPermission,
                                requestPermissions = ::requestNeededPermissions,
                                openAppSettings = ::openAppSettings,
                                onRequestCurrentLocation = { mapViewModel.requestCurrentLocationAndPin() },
                                isCheckingCurrentLocation = isCheckingCurrentLocation,
                                isInsideGeofence = isInsideGeofence,
                                checkCurrentLocationStatus = checkCurrentLocationStatus,
                                onRequestBackgroundLocationPermission = { activity -> mapViewModel.requestBackgroundLocationPermission(activity) },
                                pinnedAlarms = alarms
                            )
                        },
                        alarmScreenContent = {
                            AlarmScreen(
                                alarms = alarms,
                                onRemoveAlarm = { alarm -> mapViewModel.deleteGeofenceById(alarm.id.toInt()) },
                                onAddAlarm = { /* TODO: Navigate to map to add alarm */ },
                                onAlarmClick = { alarm -> mapViewModel.setCameraMoveRequest(alarm.latLng) }
                            )
                        },
                        settingsScreenContent = { 
                            SettingsScreen()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update permission states when the app resumes, in case they changed in settings
        mapViewModel.updatePermissionStates()
    }

    private fun requestNeededPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (permissionsToRequest.contains(Manifest.permission.ACCESS_FINE_LOCATION) ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.VIBRATE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    // Add a callback to request background location permission
    fun onRequestBackgroundLocationPermission(activity: Activity) {
        mapViewModel.requestBackgroundLocationPermission(activity)
    }
}
