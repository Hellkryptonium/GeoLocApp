package com.mohdharish.geolocapp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import com.mohdharish.geolocapp.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppUI(
    mapScreenContent: @Composable () -> Unit,
    alarmScreenContent: @Composable () -> Unit,
    settingsScreenContent: @Composable () -> Unit, // Added settingsScreenContent
    startDestination: String = "map"
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Track current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "GeoLocApp", 
                    style = MaterialTheme.typography.titleLarge, 
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Map") },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    selected = currentRoute == "map",
                    onClick = { 
                        navController.navigate("map") {
                            // Pop up to the start destination of the graph to avoid
                            // building up a large stack of destinations
                            popUpTo("map") { saveState = true }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Alarms") },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alarms") },
                    selected = currentRoute == "alarms",
                    onClick = { 
                        navController.navigate("alarms") {
                            popUpTo("map") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem( // Added Settings navigation item
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    selected = currentRoute == "settings",
                    onClick = { 
                        navController.navigate("settings") {
                            popUpTo("map") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(when (currentRoute) {
                            "map" -> "Map"
                            "alarms" -> "Alarms"
                            "settings" -> "Settings" // Added Settings title
                            else -> "GeoLocApp"
                        }) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController, 
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("map") { 
                    mapScreenContent() 
                }
                composable("alarms") { 
                    alarmScreenContent() 
                }
                composable("settings") { // Added settings composable
                    settingsScreenContent()
                }
            }
        }
    }
}
