# Logy 2.0

Logy is a private, offline-first Android app for remembering when things happened.

## Features

- Create and manage reusable categories
- Create and edit color-coded events using a category dropdown
- Add or edit timestamped logs with optional notes and durations
- Add logs directly from the home screen
- See time elapsed since the latest entry
- Search events, categories, and log notes from the home screen
- Search log notes inside an event
- System, light, and dark appearance modes
- Export all data as CSV and restore it later
- No account, analytics, ads, or internet permission

## Build

The project requires Java 17, Android SDK 35, and Gradle 8.13.

```powershell
./gradlew.bat assembleRelease
```

The generated APK is in `app/build/outputs/apk/release/`.

## CSV restore behavior

Imports are intended for backups created by Logy. Restoring a backup replaces all data currently stored in the app after an explicit confirmation.
