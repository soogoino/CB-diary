# 貞操日記 Android 應用程式

**[English](README.md) | 繁體中文**

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ffdd00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/soogoino)
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/soogoino)
<!-- [![爱发电](https://afdian.moeci.com/YOUR_AFDIAN_USER_ID/YOUR_AFDIAN_TOKEN/badge.svg)](https://afdian.com/a/soogoino) -->

<!--START_SECTION:buy-me-a-coffee-->
<!--END_SECTION:buy-me-a-coffee-->

基於 Kotlin + Jetpack Compose 開發的 Android 原生日記應用程式，專為貞操裝置佩戴者設計。完全離線、資料儲存於本機，保護使用者隱私。

## 功能特色

### 核心記錄
- **早晨 / 晚間雙分頁** — 4 步驟漸進式表單，涵蓋 23+ 個生理、心理、社交維度
- **照片打卡** — 支援相機拍攝和從相簿選取，具模糊遮罩保護隱私（點擊才顯示）
- **輪替問題** — 每日依日期自動輪換 5 組補充問題（含性別差異化版本），顯示問題與答案
- **作息記錄** — 就寢 / 起床時間選擇器，自動計算睡眠時長

### 統計與回顧
- **統計儀表板** — Vico 圖表展示性慾、舒適度、專注度、睡眠等趨勢
- **歷史日曆** — 心情熱力圖，逐日快速回顧打卡狀態
- **連續打卡** — 追蹤連續天數，6 個里程碑徽章（7 / 14 / 30 / 60 / 90 / 180 天）

### 摘要卡片分享
- **每日摘要卡片** — 一鍵生成 1080×1920 像素的 PNG 分享卡
- **多主題** — 內建 Midnight、Crimson、Ocean、Forest、Minimal 等主題，支援漸層與幾何圖案
- **分享 / 儲存** — 直接分享到其他 App 或儲存至裝置
- **卡片照片** — 可選擇將當日打卡照片嵌入卡片

### 安全性
- **自動鎖定** — App 切換至背景時自動觸發（相機拍照期間豁免）
- **生物辨識** — 指紋 / 人臉辨識解鎖
- **PIN 碼** — 數字備用解鎖，AES 加密儲存
- **截圖保護** — FLAG_SECURE 防止敏感頁面被截圖

### 提醒與通知
- **每日提醒** — 可自訂時間的填寫提醒（WorkManager）
- **早晨喚醒** — 固定早晨通知推播

### 個人化
- **深色模式** — 支援淺色 / 深色 / 跟隨系統
- **多語系** — 繁體中文 / 簡體中文 / 英文，動態切換
- **個人資料** — 暱稱、開始日期、身高、體重（含 BMI）、裝置名稱
- **CSV 匯出 / 匯入** — SAF 存取，完整資料備份與還原

---

## 技術棧

| 分類 | 技術 |
|---|---|
| 語言 | Kotlin 1.9.22 |
| UI 框架 | Jetpack Compose + Material Design 3 |
| 架構 | MVVM + Clean Architecture（Repository 介面層） |
| 資料庫 | Room 2.6.1（EAV 屬性表 + Migrations） |
| 偏好設定 | DataStore Preferences 1.0.0 |
| 圖表 | Vico 1.13.1 |
| 相機 / 相簿 | ActivityResultContracts（TakePicture / GetContent）+ FileProvider |
| 通知 | WorkManager 2.9.0 |
| 安全 | BiometricPrompt 1.1.0 + EncryptedSharedPreferences |
| 圖片處理 | BitmapFactory + ExifInterface（EXIF 旋轉修正）|
| 序列化 | Gson 2.10.1 |
| 編譯 SDK | 34（minSdk 24 / Android 7.0+） |

---

## 專案結構

```
app/src/main/java/com/chastity/diary/
├── DiaryApplication.kt
├── MainActivity.kt
├── ui/
│   ├── screens/
│   │   ├── DailyEntryScreen.kt      # 早晨/晚間雙 Tab 記錄表單 + 照片打卡
│   │   ├── SummaryCardContent.kt    # 摘要卡片 Composable + BottomSheet
│   │   ├── DashboardScreen.kt       # Vico 圖表統計儀表板
│   │   ├── HistoryScreen.kt         # 歷史記錄 + 心情日曆熱力圖
│   │   ├── OnboardingScreen.kt      # 多步驟初始引導（12 個步驟）
│   │   ├── LockScreen.kt            # 生物辨識 / PIN 鎖定畫面
│   │   └── SettingsScreen.kt        # 設定頁面
│   ├── components/                  # 可重用 UI 元件
│   ├── theme/
│   │   ├── CardThemes.kt            # 卡片主題（漸層、幾何圖案、外部資產）
│   │   └── Theme.kt                 # Material 3 色彩 / 字型主題
│   └── navigation/                  # NavGraph + BottomNavigationBar
├── data/
│   ├── local/
│   │   ├── entity/                  # DailyEntry + EAV 屬性表 Entity
│   │   ├── dao/                     # DailyEntryDao（含 @Transaction 屬性查詢）
│   │   └── database/                # AppDatabase + Migrations
│   ├── repository/
│   └── datastore/                   # PreferencesManager
├── domain/
│   ├── model/                       # DailyEntry、CardData、CardTheme、UserSettings
│   └── repository/                  # 乾淨架構介面
├── viewmodel/
│   ├── DailyEntryViewModel.kt
│   ├── DashboardViewModel.kt
│   ├── SettingsViewModel.kt
│   └── CardViewModel.kt             # 摘要卡片 / 主題 / 匯出
└── util/
    ├── BiometricHelper.kt
    ├── CardRenderer.kt              # 離屏渲染 → PNG（density=1 保持一致）
    ├── CsvHelper.kt
    ├── NotificationHelper.kt
    └── Constants.kt
```

---

## 開始使用

### 環境需求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK 34
- Gradle 8.2+

### 建置與執行

```bash
git clone <repository-url>
cd CB-diary-A
./gradlew installDebug
```

---

## 開發進度

### 已完成 ✅

#### 資料層
- [x] Room Database（EAV 屬性表 + Migrations）
- [x] DataStore Preferences
- [x] Repository 層（EntryRepository / SettingsRepository / StreakRepository）
- [x] `getEntryByDateWithAttributesFlow()` — `@Transaction` 查詢確保輪替答案正確載入

#### UI / 功能
- [x] Onboarding 初始引導（12 步驟）
- [x] 完整每日記錄表單（早晨 + 晚間雙 Tab，漸進式 4 步驟）
- [x] 輪替問題（每日自動輪換，顯示問題標題 + 答案，locale 正確）
- [x] 今日快照合併為 6 欄單列（心情、能量、性慾、舒適、專注、睡眠）
- [x] 照片打卡（相機 + 相簿選取，EXIF 旋轉修正，模糊遮罩點擊顯示）
- [x] 統計儀表板（Vico 折線圖 + 長條圖）
- [x] 歷史記錄（心情日曆熱力圖）
- [x] 連續打卡追蹤（6 里程碑徽章）
- [x] 生物辨識 + PIN 鎖定（App 背景自動鎖定，相機拍照豁免）
- [x] 雙重通知（WorkManager）
- [x] CSV 匯出 / 匯入（SAF）
- [x] 中英語系動態切換（繁中 / 英文）

#### 摘要卡片
- [x] `CardData` — 彙整當日快照、7 日平均、連續天數、輪替問答
- [x] `SummaryCardContent` — 1080×1920 全解析度 Composable（density=1 離屏渲染）
- [x] 多主題（Midnight / Crimson / Ocean / Forest / Minimal 等），全部免費開放
- [x] 卡片照片開關（可選擇嵌入當日打卡照片）
- [x] 分享 / 儲存 PNG

### 待實作 📋

- [ ] 單元測試 / UI 測試
- [ ] 自訂卡片格風格
- [ ] 主螢幕 Widget

---

## 隱私與安全

- 所有資料儲存於本機 Room Database，不上傳任何伺服器
- 生物辨識（指紋 / 臉部）+ PIN 碼雙重保護
- App 進入背景自動觸發鎖定

## 貢獻

歡迎提交 Issue 和 Pull Request！

---

**開發狀態**: Beta  
**最後更新**: 2026-03-01
