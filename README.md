# 貞操日記 Android 應用程式

<!--START_SECTION:buy-me-a-coffee-->
<!--END_SECTION:buy-me-a-coffee-->

基於 Kotlin + Jetpack Compose 開發的 Android 原生日記應用程式,專為貞操裝置佩戴者設計。

## 功能特色

- ✅ **每日記錄表單** - 23 個問題（Q1–Q23）涵蓋生理、心理、社交等各方面
- 📊 **統計儀表板** - Vico 圖表展示統計數據
- 📅 **歷史記錄** - 含心情日曆熱力圖，逐日回顧記錄
- 🚀 **Onboarding 引導** - 多步驟初始設定流程（12 個步驟）
- 🔒 **自動鎖定** - 生物辨識 + PIN 雙重保護隱私
- 🔥 **連續打卡** - 追蹤連續記錄天數，達成里程碑獎勵
- 🔔 **雙重提醒** - 每日固定提醒 + 早晨喚醒通知（WorkManager）
- 📸 **照片打卡** - 可選圖片記錄（CameraX，選用）
- 📤 **CSV 匯出** - 一鍵匯出所有記錄資料
- 🌙 **深色模式** - 支援淺色/深色/跟隨系統

## 技術棧

- **語言**: Kotlin 1.9.22
- **UI 框架**: Jetpack Compose + Material Design 3
- **架構**: MVVM (ViewModel + StateFlow) + Clean Architecture（Repository 介面層）
- **資料庫**: Room 2.6.1（含 EAV 屬性表 + Migrations）
- **偏好設定**: DataStore Preferences 1.0.0
- **圖表**: Vico 1.13.1（Compose 原生圖表庫）
- **相機**: CameraX 1.3.1（選用）
- **通知**: WorkManager 2.9.0 + NotificationManager
- **安全**: BiometricPrompt 1.1.0 + EncryptedSharedPreferences
- **啟動畫面**: Splash Screen API 1.0.1
- **資料序列化**: Gson 2.10.1
- **編譯 SDK**: 34（minSdk 24 / Android 7.0+）

## 專案結構

```
app/src/main/java/com/chastity/diary/
├── DiaryApplication.kt
├── MainActivity.kt
├── ui/
│   ├── screens/
│   │   ├── DailyEntryScreen.kt   # 4 步驟漸進式記錄表單
│   │   ├── DashboardScreen.kt    # Vico 圖表統計儀表板
│   │   ├── HistoryScreen.kt      # 歷史記錄 + 心情日曆熱力圖
│   │   ├── OnboardingScreen.kt   # 多步驟初始引導（12 個 Composable）
│   │   ├── LockScreen.kt         # 生物辨識 / PIN 鎖定畫面
│   │   └── SettingsScreen.kt     # 設定頁面
│   ├── components/               # 可重用 UI 元件（12 個）
│   ├── theme/                    # Material 3 色彩、字型主題
│   └── navigation/               # NavGraph + BottomNavigationBar
├── data/
│   ├── local/
│   │   ├── entity/               # Room Entity（含 EAV 屬性表）
│   │   ├── dao/                  # Room DAO
│   │   └── database/             # AppDatabase + Migrations
│   ├── repository/               # Repository 實作
│   └── datastore/                # PreferencesManager
├── domain/
│   ├── model/                    # DailyEntry（23 欄位）、FormFlow、HeatmapModel
│   └── repository/               # 乾淨架構介面（IEntryRepository 等）
├── viewmodel/                    # 4 個 ViewModel
├── util/                         # BiometricHelper、CsvHelper、NotificationHelper 等
└── worker/
    ├── DailyReminderWorker.kt    # 有條件每日提醒
    └── MorningReminderWorker.kt  # 早晨固定喚醒通知
```

## 開始使用

### 1. 環境需求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK 34
- Gradle 8.2+

### 2. 下載專案

```bash
git clone <repository-url>
cd CB-diary-A
```

### 3. 建置專案

```bash
./gradlew build
```

### 4. 執行應用程式

- 使用 Android Studio 的 Run 按鈕
- 或使用命令列:

```bash
./gradlew installDebug
```

## 開發進度

### 已完成 ✅

- [x] 專案架構（Clean Architecture + MVVM）
- [x] Room Database 配置（含 EAV 屬性表 + Migrations）
- [x] DataStore Preferences
- [x] ViewModel 與 StateFlow（4 個 ViewModel）
- [x] Material 3 主題（淺色/深色/跟隨系統）
- [x] 底部導航（NavGraph Keep-alive 優化）
- [x] Onboarding 初始引導流程（12 步驟）
- [x] 完整每日記錄表單 UI（Q1–Q23，4 步驟漸進式）
- [x] 統計儀表板（Vico 圖表整合）
- [x] 歷史記錄頁（含心情日曆熱力圖）
- [x] 生物辨識 + PIN 鎖定畫面
- [x] 連續打卡追蹤與里程碑徽章
- [x] 雙重 WorkManager 通知（每日提醒 + 早晨喚醒）
- [x] CSV 資料匯出後端（`CsvHelper.kt`）
- [x] Repository 介面層（乾淨架構依賴倒置）

### 進行中 🚧

- [ ] CameraX 照片功能（依賴與 FileProvider 已設定，UI 整合待完成）
- [ ] CSV 匯出 UI 入口（後端已完成，Settings 頁觸發待接通）
- [ ] Splash Screen 整合（依賴已加，初始化待完成）
- [ ] 主題切換 UI（DataStore 已支援，SettingsScreen 待串接）

### 待實作 📋

- [ ] 單元測試
- [ ] UI 測試
- [ ] App 圖示（自訂 Launcher Icon）
- [ ] ProGuard 規則優化

## 資料模型

### DailyEntry (每日記錄)

詳見 [`DailyEntry.kt`](app/src/main/java/com/chastity/diary/domain/model/DailyEntry.kt)

## 隱私與安全

- 所有資料預設儲存於本機 (Room Database)
- 支援生物辨識 (指紋/Face ID) 鎖定
- PIN 碼備用方案,使用 EncryptedSharedPreferences 加密儲存
- App 切換到背景會自動鎖定

## 貢獻

歡迎提交 Issue 和 Pull Request!

## 許可證

[MIT License](LICENSE)

## 注意事項

此應用為個人日記工具,請妥善保管您的裝置與備份。開發者不對資料遺失負責。

---

**開發狀態**: Alpha (v1.0.0-alpha)  
**最後更新**: 2026-02-27
