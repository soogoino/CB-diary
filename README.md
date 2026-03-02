# CB Diary — Chastity Device Wearers' Private Journal

**English | [繁體中文](README.zh-TW.md)**

[![Website](https://img.shields.io/badge/Website-CB%20Diary-7c3aed?style=for-the-badge&logo=github-pages&logoColor=white)](https://soogoino.github.io/CB-diary/)
[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ffdd00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/soogoino)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/soogoino)
[![爱发电](https://afdian.moeci.com/YOUR_AFDIAN_USER_ID/YOUR_AFDIAN_TOKEN/badge.svg)](https://afdian.com/a/soogoino)

<!--START_SECTION:buy-me-a-coffee-->
<!--END_SECTION:buy-me-a-coffee-->

A native Android journal app built with Kotlin + Jetpack Compose, designed specifically for chastity device wearers. Fully offline, all data stored locally — your privacy is paramount.

## Features

### Core Logging
- **Morning & Evening Dual-Tab Entry** — Progressive 4-step form covering 23+ physiological, psychological, and social dimensions
- **Photo Check-In** — Capture with camera or pick from gallery; photos are blurred by default (tap to reveal)
- **Rotating Questions** — 5 sets of supplemental questions auto-rotate daily by date (with gender-variant versions), showing both question and answer
- **Sleep & Routine Tracking** — Bedtime / wake-up time pickers with automatic sleep duration calculation

### Statistics & Review
- **Stats Dashboard** — Vico line and bar charts for libido, comfort, focus, sleep trends
- **History Calendar** — Mood heatmap calendar for quick daily review
- **Streak Tracking** — Track consecutive check-in days with 6 milestone badges (7 / 14 / 30 / 60 / 90 / 180 days)

### Summary Card Sharing
- **Daily Summary Card** — One-tap generation of a 1080×1920 px shareable PNG
- **Multiple Themes** — Built-in Midnight, Crimson, Ocean, Forest, Minimal themes with gradients and geometric overlays
- **Share / Save** — Share directly to other apps or save to device
- **Embed Photo** — Optionally embed today's check-in photo in the card

### Security
- **Auto-Lock** — Triggers automatically when app moves to background (camera exempted)
- **Biometric Unlock** — Fingerprint / face recognition
- **PIN Code** — Numeric fallback unlock, stored with AES encryption
- **Screenshot Protection** — `FLAG_SECURE` prevents screenshots and recent-app previews

### Reminders & Notifications
- **Daily Reminder** — Customizable time for entry reminders (WorkManager)
- **Morning Wake-Up** — Fixed morning push notification

### Personalization
- **Dark Mode** — Light / Dark / Follow system
- **Multi-Language** — Traditional Chinese / Simplified Chinese / English, live-switchable in Settings
- **Profile** — Nickname, start date, height, weight (with BMI), device name
- **CSV Export / Import** — Full data backup and restore via SAF

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin 1.9.22 |
| UI Framework | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + Clean Architecture (Repository interface layer) |
| Database | Room 2.6.1 (EAV attribute table + Migrations) |
| Preferences | DataStore Preferences 1.0.0 |
| Charts | Vico 1.13.1 |
| Camera / Gallery | `ActivityResultContracts` (TakePicture / GetContent) + FileProvider |
| Notifications | WorkManager 2.9.0 |
| Security | BiometricPrompt 1.1.0 + EncryptedSharedPreferences |
| Image Processing | BitmapFactory + ExifInterface (EXIF rotation correction) |
| Serialization | Gson 2.10.1 |
| Compile SDK | 34 (minSdk 24 / Android 7.0+) |

---

## Project Structure

```
app/src/main/java/com/chastity/diary/
├── DiaryApplication.kt
├── MainActivity.kt
├── ui/
│   ├── screens/
│   │   ├── DailyEntryScreen.kt      # Morning/Evening tab form + photo check-in
│   │   ├── SummaryCardContent.kt    # Summary card Composable + BottomSheet
│   │   ├── DashboardScreen.kt       # Vico chart stats dashboard
│   │   ├── HistoryScreen.kt         # History + mood calendar heatmap
│   │   ├── OnboardingScreen.kt      # Multi-step onboarding (12 steps)
│   │   ├── LockScreen.kt            # Biometric / PIN lock screen
│   │   └── SettingsScreen.kt        # Settings screen
│   ├── components/                  # Reusable UI components
│   ├── theme/
│   │   ├── CardThemes.kt            # Card themes (gradient, geometric, external assets)
│   │   └── Theme.kt                 # Material 3 color / typography theme
│   └── navigation/                  # NavGraph + BottomNavigationBar
├── data/
│   ├── local/
│   │   ├── entity/                  # DailyEntry + EAV attribute entities
│   │   ├── dao/                     # DailyEntryDao (with @Transaction attribute queries)
│   │   └── database/                # AppDatabase + Migrations
│   ├── repository/
│   └── datastore/                   # PreferencesManager
├── domain/
│   ├── model/                       # DailyEntry, CardData, CardTheme, UserSettings
│   └── repository/                  # Clean architecture interfaces
├── viewmodel/
│   ├── DailyEntryViewModel.kt
│   ├── DashboardViewModel.kt
│   ├── SettingsViewModel.kt
│   └── CardViewModel.kt             # Summary card / themes / export
└── util/
    ├── BiometricHelper.kt
    ├── CardRenderer.kt              # Off-screen render → PNG (density=1 for consistency)
    ├── CsvHelper.kt
    ├── NotificationHelper.kt
    └── Constants.kt
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Gradle 8.2+

### Build & Run

```bash
git clone <repository-url>
cd CB-diary-A
./gradlew installDebug
```

---

## Development Progress

### Completed ✅

#### Data Layer
- [x] Room Database (EAV attribute table + Migrations)
- [x] DataStore Preferences
- [x] Repository layer (EntryRepository / SettingsRepository / StreakRepository)
- [x] `getEntryByDateWithAttributesFlow()` — `@Transaction` query for correct rotating answer loading

#### UI / Features
- [x] Onboarding flow (12 steps)
- [x] Full daily entry form (Morning + Evening dual-tab, progressive 4 steps)
- [x] Rotating questions (auto-rotate daily, display question title + answer, correct locale)
- [x] Today snapshot merged into single 6-column row (mood, energy, libido, comfort, focus, sleep)
- [x] Photo check-in (camera + gallery, EXIF rotation fix, blur mask with tap-to-reveal)
- [x] Stats dashboard (Vico line + bar charts)
- [x] History calendar (mood heatmap)
- [x] Streak tracking (6 milestone badges)
- [x] Biometric + PIN lock (auto-lock on background, camera exemption)
- [x] Dual notifications (WorkManager)
- [x] CSV export / import (SAF)
- [x] Live language switching (Traditional Chinese / English)

#### Summary Card
- [x] `CardData` — aggregates today's snapshot, 7-day averages, streak, rotating Q&A
- [x] `SummaryCardContent` — 1080×1920 full-res Composable (density=1 off-screen render)
- [x] Multiple themes (Midnight / Crimson / Ocean / Forest / Minimal etc.), all free
- [x] Photo toggle (optionally embed today's check-in photo)
- [x] Share / Save PNG

### Planned 📋

- [ ] Unit tests / UI tests
- [ ] Custom card layout styles
- [ ] Home screen Widget
- [ ] Lockbox integration
- [ ] Social features (binding, community)
---

## Privacy & Security

- All data stored in local Room Database — nothing is uploaded to any server
- Biometric (fingerprint / face) + PIN dual protection
- Automatic lock when app enters background

## Contributing

Issues and Pull Requests are welcome!

---

**Status**: Beta  
**Last Updated**: 2026-03-01
