# Build steps

This is a tiny native Android wrapper app: a full-screen WebView that loads your
Calendar Rescheduler web app. Login happens once inside the app (Google account),
then it behaves like the PWA.

## Build the APK (one-time setup)

1. Install Android Studio (free) from https://developer.android.com/studio
2. Open Android Studio -> "Open an existing project" -> select this folder
   (`android-app/CalendarRescheduler`)
3. Let it sync (it downloads Gradle + Android SDK on first run - it may take a while).
   If it asks about a missing Gradle wrapper, click OK to let Android Studio configure it.
4. Build menu -> "Build Bundle(s) / APK(s)" -> "Build APK(s)"
5. The APK appears at:
   `app/build/outputs/apk/debug/app-debug.apk`

## Install on your phone

1. Copy the APK to your phone (Google Drive / USB / sending to yourself works)
2. Tap it and allow "Install unknown apps" for the file manager
3. Open the app, log in with your Google account once (the one that owns the calendar)

## Update the web-app URL (if it ever changes)

Edit `MainActivity.kt` -> `APP_URL` constant, then rebuild.

## Notes

- Cookies/session are stored in the WebView, so you stay logged in.
- Back button goes back through the web app history, then exits.
- Uses your system WebView (kept up to date by Google Play), so it renders exactly
  like Chrome on the phone.