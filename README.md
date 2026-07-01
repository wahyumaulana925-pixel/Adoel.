# Adoel.

Native Android app (Kotlin + Jetpack Compose) for tracking machine doffing schedules and estimates.

## Stack

- Kotlin 2.0.21
- Jetpack Compose (BOM 2024.12.01) + Material3
- DataStore (Preferences) + Gson for persistence
- AlarmManager + BroadcastReceiver for scheduled doff notifications

## Build

```
cd android
./gradlew assembleDebug
```

The debug APK is produced at `android/app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
android/app/src/main/java/com/jekael/adoel/
├── data/            # models, default machine DB, DataStore repository
├── notification/    # AlarmManager scheduling + BroadcastReceivers
├── viewmodel/       # DoffViewModel (business logic), UIViewModel (toast/confirm)
├── ui/
│   ├── components/  # RadarCard, HistoryDrawer, SettingsDrawer, sheets, dialogs
│   └── theme/       # color palette + MaterialTheme
├── MainActivity.kt
```
