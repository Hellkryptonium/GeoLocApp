package com.mohdharish.geolocapp.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun AlarmScreen(
    alarms: List<AlarmItem>,
    onRemoveAlarm: (AlarmItem) -> Unit,
    onAddAlarm: () -> Unit,
    onAlarmClick: (AlarmItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(alarms, key = { it.id }) { alarm ->
                    var dismissed by remember { mutableStateOf(false) }
                    if (!dismissed) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .pointerInput(alarm.id) {
                                    detectHorizontalDragGestures { change, dragAmount -> 
                                        if (dragAmount > 100) { // Swipe right to delete
                                            dismissed = true
                                            onRemoveAlarm(alarm)
                                        }
                                    }
                                },
                            onClick = { onAlarmClick(alarm) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(alarm.label, style = MaterialTheme.typography.titleMedium)
                                    Text("Lat: ${alarm.latLng.latitude}, Lng: ${alarm.latLng.longitude}", style = MaterialTheme.typography.bodySmall)
                                    Text("Radius: ${alarm.radius}m", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onRemoveAlarm(alarm) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Alarm")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // FAB
        FloatingActionButton(
            onClick = onAddAlarm,
            modifier = Modifier
                .padding(16.dp)
                .align(androidx.compose.ui.Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
        }
    }
}

data class AlarmItem(
    val id: String,
    val label: String,
    val latLng: LatLng,
    val radius: Float
)
