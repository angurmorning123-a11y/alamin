# Expense Tracker

A native Android expense manager built with Kotlin, Jetpack Compose, Room, DataStore, and MVVM.

## Project structure
- `app/src/main/java/com/example/expensetracker/data/` — Room entities, DAOs, database, preferences, repository
- `domain/` — budget status and month comparison calculations
- `viewmodel/` — StateFlow-backed ViewModel
- `ui/` — Compose screens and theme
- `app/src/main/res/values/` — English strings and app styles
- `app/src/main/res/values-bn/` — Bengali strings
- `app/src/test/` — unit tests

## Build in Android Studio
1. Install JDK 17 and Android SDK 34.
2. Open the project root in Android Studio.
3. Allow Gradle sync to finish. Gradle 8.7 is compatible with AGP 8.5.2.
4. Select the `app` run configuration and run on an Android 8.0+ device.

## GitHub Actions
The workflow in `.github/workflows/android.yml` installs JDK 17 and Gradle 8.7 and builds `assembleDebug` on pushes to `main`. The APK is uploaded as a workflow artifact.

## Phone-only build
Upload the project source to your GitHub repository. The included workflow builds the debug APK without requiring the Gradle wrapper in this project.
