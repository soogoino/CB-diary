# 貞操日記 Android 應用程式

基於 Kotlin + Jetpack Compose 開發的 Android 原生日記應用程式,專為貞操裝置佩戴者設計。

## 功能特色

- ✅ **每日記錄表單** - 20+ 個問題涵蓋生理、心理、社交等各方面
- 📊 **統計儀表板** - 多樣化圖表展示統計數據
- 🔒 **自動鎖定** - 生物辨識 + PIN 雙重保護隱私 🔥 **連續打卡** - 追蹤連續記錄天數,達成里程碑獎勵
- 🔔 **每日提醒** - 定時通知提醒記錄
- 📸 **照片打卡** - 可選圖片記錄
- ☁️ **雲端同步** - 可選 Firebase 雲端備份
- 🌙 **深色模式** - 支援淺色/深色/跟隨系統

## 技術棧

- **語言**: Kotlin 1.9.22
- **UI 框架**: Jetpack Compose + Material Design 3
- **架構**: MVVM (ViewModel + StateFlow)
- **資料庫**: Room Database
- **偏好設定**: DataStore Preferences
- **圖表**: Vico (Compose 原生圖表庫)
- **相機**: CameraX API
- **通知**: WorkManager + NotificationManager
- **安全**: BiometricPrompt + EncryptedSharedPreferences
- **雲端 (可選)**: Firebase Authentication + Firestore

## 專案結構

```
app/src/main/java/com/chastity/diary/
├── ui/
│   ├── screens/          # 畫面 (DailyEntry, Dashboard, Settings)
│   ├── components/       # 可重用 UI 元件
│   ├── theme/            # Material 3 主題
│   └── navigation/       # 導航配置
├── data/
│   ├── local/
│   │   ├── entity/       # Room Entity
│   │   ├── dao/          # Room DAO
│   │   └── database/     # Database 實例
│   ├── repository/       # Repository 實作
│   └── datastore/        # DataStore Preferences
├── domain/
│   └── model/            # Domain Models
├── viewmodel/            # ViewModels
├── util/                 # 工具函式與常數
└── worker/               # WorkManager Workers
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

### 3. Firebase 配置 (可選)

如果需要雲端同步功能:

1. 前往 [Firebase Console](https://console.firebase.google.com/)
2. 建立新專案
3. 新增 Android 應用程式,Package name 為 `com.chastity.diary`
4. 下載 `google-services.json` 並替換 `app/google-services.json`
5. 啟用 Authentication (Anonymous) 和 Firestore

如不需要雲端功能,可在 `app/build.gradle.kts` 中註解掉 Firebase 相關依賴:

```kotlin
// implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
// implementation("com.google.firebase:firebase-auth-ktx")
// implementation("com.google.firebase:firebase-firestore-ktx")
```

並在專案根目錄 `build.gradle.kts` 註解:

```kotlin
// id("com.google.gms.google-services") version "4.4.0" apply false
```

並在 `app/build.gradle.kts` 中註解:

```kotlin
// id("com.google.gms.google-services")
```

### 4. 建置專案

```bash
./gradlew build
```

### 5. 執行應用程式

- 使用 Android Studio 的 Run 按鈕
- 或使用命令列:

```bash
./gradlew installDebug
```

## 開發進度

### 已完成 ✅

- [x] 專案架構建立
- [x] Room Database 配置
- [x] DataStore Preferences
- [x] ViewModel 與 StateFlow
- [x] Material 3 主題
- [x] 底部導航
- [x] 三個主要畫面骨架 (每日記錄、儀表板、設定)
- [x] 基本統計功能

### 進行中 🚧

- [ ] 完整的每日記錄表單 UI (20+ 問題)
- [ ] 圖表視覺化 (使用 Vico)
- [ ] 生物辨識鎖定功能
- [ ] WorkManager 每日通知
- [ ] CameraX 照片功能
- [ ] 資料匯出 CSV
- [ ] Firebase 同步

### 待實作 📋

- [ ] 單元測試
- [ ] UI 測試
- [ ] App 圖示與啟動畫面
- [ ] ProGuard 規則優化
- [ ] 性能優化

## 資料模型

### DailyEntry (每日記錄)

包含 23 個維度的問題:

1. 心情 (mood)
2. 色情內容 (viewedPorn, pornDuration)
3. 勃起 (hadErection) - 男性限定
4. 運動 (exercised, exerciseTypes, exerciseDuration)
5. 解鎖/自慰 (unlocked, masturbated, masturbationDuration)
6. 露出 (exposedLock, exposedLocations)
7. 照片 (photoPath)
8-23. 擴充問題 (性慾強度、舒適度、不適、清潔、洩漏、邊緣訓練、Keyholder 互動、睡眠、取下、夜間勃起、專注度、任務、情緒、裝置檢查、社交、自我評價)

詳見 [`DailyEntry.kt`](app/src/main/java/com/chastity/diary/domain/model/DailyEntry.kt)

## 隱私與安全

- 所有資料預設儲存於本機 (Room Database)
- 支援生物辨識 (指紋/Face ID) 鎖定
- PIN 碼備用方案,使用 EncryptedSharedPreferences 加密儲存
- App 切換到背景會自動鎖定
- FLAG_SECURE 防止截圖
- 雲端同步為**可選功能**,需手動啟用

## 貢獻

歡迎提交 Issue 和 Pull Request!

## 許可證

[MIT License](LICENSE)

## 注意事項

此應用為個人日記工具,請妥善保管您的裝置與備份。開發者不對資料遺失負責。

---

**開發狀態**: 初始版本 (v1.0.0-alpha)  
**最後更新**: 2026-02-20
