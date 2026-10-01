# CalendarRescheduler

A modern Android calendar scheduling app built with Jetpack Compose, featuring event management, recurring events, drag-to-reschedule, widget, backup/restore, and a fully upgraded toolchain.

## Overview

CalendarRescheduler helps you manage daily events, recurring schedules, and focus/alert modes from your phone. The app replaces the old WebView wrapper with a native Compose UI and adds powerful features while keeping your Google calendar integration.

## ✨ Features

- **Calendar view** – Day/Week/Agenda/Month modes with pull‑to‑refresh
- **Drag‑to‑reschedule** – Long‑press any event and drag it to a new time
- **Recurring events** – Create events with repeat rules (daily, weekly, custom count)
- **Series mode** – Edit/delete an entire series or just this occurrence
- **Focus mode** – Toggle focus sessions with customizable duration
- **Alerts/Reminders** – Configure notification alerts per event
- **Widget** – Home‑screen widget showing today’s events with Quick‑Add button
- **Backup & Restore** – Export your data as JSON, auto‑backup weekly, import from file
- **Export** – PDF (this week) and CSV (full history) export
- **Theme** – Custom color scheme and font rendering

## 🛠️ Tech Stack

| Layer | Version |
|---|---|
| **AGP** | 8.13.0 |
| **Gradle** | 8.13 |
| **Kotlin** | 2.2.21 |
| **SDK** | 36 (platform 36 installed during build) |
| **Compose** | 2025.04.01 (BOM) |
| **Room** | 2.8.4 |
| **KSP** | 2.2.20‑2.0.2 |

## 📦 Build & Install

1. **Prerequisites** – Android Studio Iguana (or newer), JDK 17
2. **Open project** – `android-app/CalendarRescheduler`
3. **Sync Gradle** – let it download dependencies (first run may be slow)
4. **Build debug APK** – `Build > Build APK(s)` or run `./gradlew :app:assembleDebug`
5. **APK location** – `app/build/outputs/apk/debug/app-debug.apk` (≈137 MB)
6. **Install** – copy the APK to your phone, enable "Install unknown apps" for your file manager, tap to install
7. **First launch** – sign in with the Google account that owns the calendar

## 🔧 Configuration

- `APP_URL` – web‑app base URL (edited in `MainActivity.kt` if your calendar URL changes)
- `API_TOKEN` – Google Calendar API token (stored in `AuthManager`)
- Auto‑backup frequency – toggle in **Settings → Backup**

## 📱 Screenshots *(add later)*

- Calendar tab with drag‑to‑reschedule
- Create event sheet with repeat rule chips
- Widget with Quick‑Add
- Backup/Restore screen

## 🛠️ Development Notes

- Full rewrite from old WebView‑only wrapper to native Compose UI
- Three compiler bugs were fixed during the upgrade ( `!` parsing, `setMode`/`setThemeMode` name clash, `!nm.canUseFullScreenIntent()` syntax )
- Room DB migrations added for `SeriesEntity`/`SeriesMember` (migration 2→3)
- `withContext(Dispatchers.IO)` used for all DB/network ops
- `LaunchedEffect` used for state‑driven UI updates (tab changes, drag gestures, backup triggers)

## 📄 License

MIT – feel free to fork and adapt.

---

**Built with** ❤️ using Android Jetpack, Kotlin, and Material 3