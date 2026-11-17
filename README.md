# Logy 2.1

Logy is a private, local-first Android app for remembering when things happened.

## Features

- Create and manage reusable categories
- Create and edit color-coded events using a category dropdown
- Add or edit timestamped logs with optional notes and durations
- Add logs directly from the home screen
- Tap the add button, latest timestamp, or elapsed-time pill to add a log
- See time elapsed since the latest entry
- Filter by a date range to see log counts and total duration per event
- Search events, categories, and log notes from the home screen
- Search log notes inside an event
- Check, download, and install updates directly from GitHub Releases
- System, light, and dark appearance modes
- Export all data as CSV and restore it later
- No account, analytics, ads, or data uploads; internet access is used only for user-requested update checks and downloads

## Build

The project requires Java 17, Android SDK 35, and Gradle 8.13.

```powershell
./gradlew.bat assembleRelease
```

The generated APK is in `app/build/outputs/apk/release/`.

## CSV restore behavior

Imports are intended for backups created by Logy. Restoring a backup replaces all data currently stored in the app after an explicit confirmation.
