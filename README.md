# GeoLocApp

> ⚠️ **Notice:** This codebase is for demonstration and educational purposes only. All rights reserved. Copying, redistribution, or use of the code in production is not permitted without explicit permission from the author. Viewing for learning and reference is allowed. For inquiries, contact the repository owner.

A native Android geo-alarm app that lets users set geofences on a map, receive alarms when entering/exiting those areas, and optionally share geofences with friends.

## Core Features

- **Map View**
  - Live map using Google Maps SDK.
  - Drop a pin at any location.
- **Set Geofence**
  - Define a radius (meters) around the pin.
  - Save geofence parameters locally (Room/DataStore).
- **Background Geofencing Service**
  - Monitor device location in the background.
  - Detect entry/exit events for geofences.
- **Alarm Trigger**
  - Play alarm tone, vibrate, or push notification on geofence events.
  - Options: stop, snooze, repeat alarm.
- **Friend-to-Friend Geofence (Optional)**
  - Share location or fence ID with a friend.
  - Trigger alarms when entering each other's geofence.
- **Basic Settings**
  - Enable/disable background tracking.
  - Choose alarm tone.
  - Customize distance radius presets.

## Recommended Tech Stack

| Layer                | Tech / Library                                 | Purpose                                 |
|----------------------|------------------------------------------------|-----------------------------------------|
| IDE                  | Android Studio                                 | App development environment             |
| Language             | Kotlin (or Java)                               | Core app logic & UI                     |
| Map Display          | Google Maps SDK for Android                    | Map rendering and interaction           |
| Geofence Management  | Android Location Services + FusedLocationProviderClient | Background geofence monitoring  |
| Database (local)     | Room or Jetpack DataStore                      | Store geofence details, settings locally|
| Notification System  | Android Notification Manager                   | Show alarms, reminders                  |
| Permissions Handling | AndroidX Activity & Permission libraries       | Handle location/background permissions  |
| Friend Location Sharing | Firebase Realtime Database / Firestore (Optional) | Real-time friend-to-friend alerts |
| Background Services  | WorkManager or Foreground Service              | Reliable background tracking            |
| Audio Playback       | MediaPlayer or ExoPlayer                       | Play alarm sounds                       |

## Optional Enhancements
- Firebase Authentication (user login, secure sharing)
- Firebase Analytics (feature usage tracking)
- Jetpack Compose (modern UI)
- Dagger-Hilt/Koin (dependency injection)

## Project Structure (Suggested)

```
app/
  src/
    main/
      java/com/yourdomain/geolocapp/
        data/         // Room DB, DataStore, models
        geofence/     // Geofence logic, services
        map/          // Map activity/fragments
        notification/ // Alarm, notification logic
        friend/       // Friend-to-friend sharing (optional)
        settings/     // Settings, preferences
      res/
        layout/
        values/
        drawable/ 
      AndroidManifest.xml
```

## Implementation Steps

1. **Map View**: Show Google Map, allow pin drop, save location.
2. **Set Geofence**: Set radius, save geofence.
3. **Background Geofencing**: Monitor location, handle entry/exit.
4. **Alarm Trigger**: Play alarm, vibrate, notify, snooze/stop/repeat.
5. **Friend-to-Friend Geofence**: (Optional) Real-time sharing via Firebase.
6. **Settings**: Tracking toggle, alarm tone, radius presets.

## Initial Setup
- Language: Kotlin
- Core SDK: Google Maps SDK + Android Geofence APIs
- Local Storage: Room or DataStore
- Background Work: WorkManager / Foreground Service
- Notification: Android Notification Manager
- Optional Social Feature: Firebase Realtime Database

---

This README will be updated as the project progresses. Refer here for architecture, tech stack, and feature planning.
