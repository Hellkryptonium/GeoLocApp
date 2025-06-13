package com.mohdharish.geolocapp.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Circle // Import Circle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MyLocation // Ensure this import is present
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import com.mohdharish.geolocapp.alarm.AlarmScreen
import com.mohdharish.geolocapp.alarm.AlarmItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    pinLocation: LatLng?,
    onMapClick: (LatLng) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSaveGeofence: (latitude: Double, longitude: Double, radius: Float) -> Unit,
    autocompleteSuggestions: List<AutocompletePrediction>,
    onSuggestionClick: (String) -> Unit,
    onClearSearch: () -> Unit,
    savedGeofencesCount: Int,
    cameraMoveRequest: LatLng?, // New
    onConsumeCameraMoveRequest: () -> Unit, // New
    onClearPin: () -> Unit, // New
    onClearAllGeofences: () -> Unit, // New
    hasFineLocationPermission: Boolean,
    hasBackgroundLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    requestPermissions: () -> Unit,
    openAppSettings: () -> Unit,
    onRequestCurrentLocation: () -> Unit, // NEW: lambda to request current location
    isCheckingCurrentLocation: Boolean, // NEW: show progress indicator
    isInsideGeofence: Boolean?, // NEW: null = not checked, true/false = result
    checkCurrentLocationStatus: String?, // NEW: status message
    onRequestBackgroundLocationPermission: (android.app.Activity) -> Unit, // NEW
    pinnedAlarms: List<AlarmItem>, // NEW: list of pinned alarms
    onClearStatus: (() -> Unit)? = null // Optional lambda to clear status
) {
    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()
    var geofenceRadius by rememberSaveable { mutableStateOf(200f) }

    var showPinNotification by remember { mutableStateOf(false) }
    var pinNotificationMessage by remember { mutableStateOf("") }

    var showGeofenceSavedNotification by remember { mutableStateOf(false) }
    var geofenceSavedNotificationMessage by remember { mutableStateOf("") }

    var showCurrentLocationDialog by remember { mutableStateOf(false) }
    var currentLocationToPin by remember { mutableStateOf<LatLng?>(null) }

    var showPinDialog by remember { mutableStateOf(false) }
    var pendingLocationToPin by remember { mutableStateOf<LatLng?>(null) }

    // var showAlarmScreen by remember { mutableStateOf(false) } // Removed: No longer needed
    // val alarms = pinnedAlarms // Use a parameter or state for alarms list // This line is fine, used by MapScreen for other purposes if any

    val focusManager = LocalFocusManager.current

    // Effect for when a pin is manually set or cleared
    LaunchedEffect(pinLocation) {
        if (pinLocation != null) {
            showGeofenceSavedNotification = false
            pinNotificationMessage = "Location pinned: ${String.format("%.4f", pinLocation.latitude)}, ${String.format("%.4f", pinLocation.longitude)}"
            showPinNotification = true

            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(
                    CameraPosition(pinLocation, 15f, 0f, 0f)
                ),
                durationMs = 1000
            )
            delay(3000)
            showPinNotification = false
        } else {
            // Pin cleared, hide notification
            showPinNotification = false
        }
    }

    // Effect for camera movement requested by search
    LaunchedEffect(cameraMoveRequest) {
        if (cameraMoveRequest != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(
                    CameraPosition(cameraMoveRequest, 15f, 0f, 0f) // Zoom to searched location
                ),
                durationMs = 1000
            )
            onConsumeCameraMoveRequest() // Consume the request after initiating animation
        }
    }
    // Scaffold removed, MainAppUI provides it.
    Box(modifier = Modifier.fillMaxSize()) { // This was previously inside Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
            // .padding(paddingValues) removed
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) { // Added padding for this column
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search location") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search Icon")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                onClearSearch()
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (autocompleteSuggestions.isNotEmpty()) {
                                // Request camera move to the first suggestion, do not pin
                                onSuggestionClick(autocompleteSuggestions.first().placeId)
                            }
                            focusManager.clearFocus()
                        }
                    )
                )
                // Spacer(modifier = Modifier.height(8.dp)) // Add space before permission prompts if suggestions are not showing

                // --- Permission Prompts --- 
                if (!hasFineLocationPermission) {
                    PermissionPromptCard(
                        icon = Icons.Default.LocationOn,
                        title = "Location Permission Needed",
                        description = "This app needs access to your precise location to set and monitor geofences.",
                        buttonText = "Grant Location Permission",
                        onGrantClick = requestPermissions,
                        onSettingsClick = openAppSettings,
                        showSettingsButton = true // Show settings as fine location is critical
                    )
                } else if (!hasBackgroundLocationPermission) { // Only show if fine location IS granted
                    PermissionPromptCard(
                        icon = Icons.Default.LocationOn,
                        title = "Background Location Access",
                        description = "For geofences to work even when the app is closed, please allow background location access ('Allow all the time').",
                        buttonText = "Grant Background Access",
                        onGrantClick = requestPermissions, // This will re-trigger request for all needed, including background
                        onSettingsClick = openAppSettings,
                        showSettingsButton = true // Background is critical for geofencing
                    )
                }

                if (!hasNotificationPermission) {
                    PermissionPromptCard(
                        icon = Icons.Default.Notifications,
                        title = "Notification Permission Needed",
                        description = "To alert you for geofence events, please grant notification permission.",
                        buttonText = "Grant Notification Permission",
                        onGrantClick = requestPermissions,
                        onSettingsClick = openAppSettings,
                        showSettingsButton = true // Notifications are key for alerts
                    )
                }
                // --- End Permission Prompts ---

                if (autocompleteSuggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                    ) {
                        items(autocompleteSuggestions) { suggestion ->
                            Text(
                                text = suggestion.getFullText(null).toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Request camera move to the clicked suggestion, do not pin
                                        onSuggestionClick(suggestion.placeId)
                                        focusManager.clearFocus()
                                    }
                                    .padding(16.dp)
                            )
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
                // Moved geofence count and clear button to be within the same padded Column
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved Geofences: $savedGeofencesCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Removed Manage Alarms Button
                }
            } // End of Column with horizontal padding

            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        onMapClick(latLng) // This will set the pinLocation
                    }
                ) {
                    pinLocation?.let {
                        Marker(state = com.google.maps.android.compose.rememberMarkerState(position = it), title = "Pinned Location")
                        Circle(
                            center = it,
                            radius = geofenceRadius.toDouble(), // Circle radius is Double
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 5f,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            if (pinLocation != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Geofence radius: ${geofenceRadius.toInt()} meters")
                    Slider(
                        value = geofenceRadius,
                        onValueChange = { geofenceRadius = it },
                        valueRange = 50f..1000f,
                        steps = 9,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                pinLocation?.let { loc ->
                                    onSaveGeofence(loc.latitude, loc.longitude, geofenceRadius)
                                    showPinNotification = false
                                    geofenceSavedNotificationMessage = "Geofence saved: Lat ${String.format("%.2f", loc.latitude)}, Lon ${String.format("%.2f", loc.longitude)}, R ${geofenceRadius.toInt()}m"
                                    showGeofenceSavedNotification = true
                                    coroutineScope.launch {
                                        delay(3000)
                                        showGeofenceSavedNotification = false
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Save Geofence")
                        }
                        Button(
                            onClick = onClearPin, // Call the unpin lambda
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear Pin")
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = {
                    // Instead of pinning directly, fetch location and show dialog
                    onRequestCurrentLocation()
                    // Assume pinLocation is updated by ViewModel after location fetch
                    pendingLocationToPin = pinLocation
                    showPinDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "My Location", tint = MaterialTheme.colorScheme.onPrimary)
            }
            // Removed second FAB for Manage Alarms if it existed here
        }

        // Dialog to confirm pinning current location
        if (showPinDialog && pendingLocationToPin != null) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Pin Current Location?") },
                text = { Text("Do you want to pin your current location at: ${String.format("%.4f", pendingLocationToPin!!.latitude)}, ${String.format("%.4f", pendingLocationToPin!!.longitude)}?") },
                confirmButton = {
                    Button(onClick = {
                        onMapClick(pendingLocationToPin!!)
                        showPinDialog = false
                    }) { Text("Pin it") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showPinDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Show progress indicator if checking
        if (isCheckingCurrentLocation) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Show result/status
        checkCurrentLocationStatus?.let { status ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // paddingValues.calculateTopPadding() removed, using fixed padding
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isInsideGeofence == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(16.dp),
                    color = if (isInsideGeofence == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            // Auto-dismiss logic for pin notification
            if (status == "Pinned your current location.") {
                LaunchedEffect(status) {
                    kotlinx.coroutines.delay(2000)
                    // Clear the status after 2 seconds
                    onClearStatus?.invoke()
                }
            }
        }

        // Pin Location Notification Panel
        AnimatedVisibility(
            visible = showPinNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                // paddingValues.calculateTopPadding() removed, using fixed padding
                .padding(top = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = pinNotificationMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        // Geofence Saved Notification Panel
        AnimatedVisibility(
            visible = showGeofenceSavedNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                // paddingValues.calculateTopPadding() removed, using fixed padding
                .padding(top = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) // Different color
            ) {
                Text(
                    text = geofenceSavedNotificationMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        // Add a button to request background location permission
        if (!hasBackgroundLocationPermission) {
            val activity = LocalContext.current as? android.app.Activity
            Button(
                onClick = { activity?.let { onRequestBackgroundLocationPermission(it) } },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Enable 'All the time' Location Access")
            }
        }
    }
}

@Composable
fun PermissionPromptCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onGrantClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsButton: Boolean = false // To decide if settings button should be shown
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Reduced vertical padding
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // Reduced padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onGrantClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(buttonText, style = MaterialTheme.typography.labelSmall)
                }
                if (showSettingsButton) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(onClick = onSettingsClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Open Settings", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settings", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
