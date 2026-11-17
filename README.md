# Logy

Logy is a private, offline-first Android app for remembering when things happened.

## Features

- Create color-coded events and organize them into categories
- Add timestamped entries with optional notes
- See time elapsed since the latest entry
- Search events and categories
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
