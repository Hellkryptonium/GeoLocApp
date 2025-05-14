package com.mohdharish.geolocapp.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Notifications

@Composable
fun SettingsScreen(
    // alarmSettingsManager: AlarmSettingsManager // Consider passing for better testability
) {
    val context = LocalContext.current
    val alarmSettingsManager = remember { AlarmSettingsManager(context) }

    var alarmSoundEnabled by remember {
        mutableStateOf(alarmSettingsManager.isAlarmSoundEnabled())
    }
    var alarmVibrationEnabled by remember {
        mutableStateOf(alarmSettingsManager.isAlarmVibrationEnabled())
    }
    var notificationEnabled by remember {
        mutableStateOf(alarmSettingsManager.areNotificationsEnabled())
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Notification Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Alarm Sound",
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text("Alarm Sound")
                    }
                    Switch(
                        checked = alarmSoundEnabled,
                        onCheckedChange = { 
                            alarmSoundEnabled = it
                            alarmSettingsManager.setAlarmSoundEnabled(it)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Vibration",
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text("Vibration")
                    }
                    Switch(
                        checked = alarmVibrationEnabled,
                        onCheckedChange = {
                            alarmVibrationEnabled = it
                            alarmSettingsManager.setAlarmVibrationEnabled(it)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text("Notifications")
                    }
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = {
                            notificationEnabled = it
                            alarmSettingsManager.setNotificationsEnabled(it)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "About",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "GeoLocApp",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Developed by GeoLoc Team",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
