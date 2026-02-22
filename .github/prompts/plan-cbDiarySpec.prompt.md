# 貞操日記（CB-diary）— 產品規格書

**版本：** 1.0 ｜ **撰寫日期：** 2026-02-22 ｜ **平台：** Android（minSdk 24 / targetSdk 34）

---

## 目錄

1. [產品定位](#1-產品定位)
2. [技術架構](#2-技術架構)
3. [啟動流程](#3-啟動流程)
4. [畫面規格](#4-畫面規格)
5. [資料模型](#5-資料模型)
6. [資料庫規格](#6-資料庫規格)
7. [輪換問題題庫](#7-輪換問題題庫r1r33)
8. [常數與可選值](#8-常數與可選值)
9. [安全機制](#9-安全機制)
10. [通知機制](#10-通知機制)
11. [導覽架構](#11-導覽架構)
12. [每日敘事自動生成](#12-每日敘事自動生成)
13. [資料匯出匯入](#13-資料匯出匯入)
14. [效能規格](#14-效能規格)
15. [UI 動畫規格](#15-ui-動畫規格)
16. [依賴項清單](#16-依賴項清單)
17. [可擴展性設計規範](#17-可擴展性設計規範)

---

## 1. 產品定位

**貞操日記**是一款專為使用貞操鎖者設計的隱私日記 App，協助用戶記錄每日佩戴狀態、身心感受與 Keyholder 互動，並透過長期統計圖表觀察自身變化趨勢。

**核心價值主張：**
- **隱私優先：** 本地加密儲存，支援生物辨識 / PIN 應用程式鎖
- **結構化記錄：** 早晨 + 晚間雙 Tab，降低每日填寫心理負擔
- **趣味化：** 每日輪換問題、調皮反饋文字、連續打卡成就系統
- **數據回顧：** 心情、性慾、舒適度長期折線圖與儀表板統計摘要

**目標用戶：** 使用貞操裝置的成年人（18+），不限性別。  
**包名：** `com.chastity.diary` ｜ **版本：** 1.0.0（versionCode = 1）

---

## 2. 技術架構

| 層次 | 技術選型 |
|------|----------|
| 語言 | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material 3（BOM 2023.10.01）|
| 架構模式 | MVVM + Repository Pattern（無 DI 框架）|
| 本地資料庫 | Room 2.6.1 |
| 使用者偏好 | DataStore Preferences 1.0.0 |
| 安全儲存 | EncryptedSharedPreferences（security-crypto 1.1.0-alpha06）|
| 生物辨識 | BiometricPrompt（biometric 1.1.0）|
| 背景工作 | WorkManager 2.9.0 |
| 圖表 | Vico 1.13.1 |
| 影像方向修正 | ExifInterface 1.3.7 |
| 序列化 | Gson 2.10.1 |

**包名：** `com.chastity.diary`  
**版本：** 1.0.0（versionCode = 1）

---

## 3. 啟動流程

```
App 啟動
  ├─ installSplashScreen()  ← 必須在 super.onCreate() 之前
  ├─ IO 執行緒並行執行（anti-main-thread-IO）
  │   ├─ async: MasterKey.build() + EncryptedSharedPreferences.create()  ~500–800ms
  │   ├─ async: DataStore.isOnboardingCompleted.first()
  │   └─ async: DataStore.userSettingsFlow.first()
  ├─ SplashScreen 持續顯示直到三者全部完成
  └─ 根據結果路由
      ├─ onboardingCompleted == false → OnboardingScreen
      ├─ lock_enabled == true         → LockScreen
      └─ 否則                         → MainScreen
```

`ProcessLifecycleOwner` 觀察者在啟動資料就緒後才安裝，負責 App 背景自動鎖定。

---

## 4. 畫面規格

### 4.1 OnboardingScreen — 首次引導

#### 新用戶（5 頁 HorizontalPager，禁止滑動）

| 頁 | 名稱 | 內容 |
|----|------|------|
| 0 | WelcomePage | 歡迎標語、App 圖示 |
| 1 | ProfilePage | 暱稱、性別（男/女/其他 Chip 單選）、開始日期 |
| 2 | DevicePage | 裝置名稱、身高（cm）、體重（kg）|
| 3 | SecurityPage | 生物辨識開關、PIN 設定（`PinSetupDialog`）|
| 4 | ReminderPage | 晚間提醒開關 + TimePicker（預設 21:00），點「完成」寫入 DataStore |

頂部：步驟計數（`x / 5`）+ 線性進度條。  
「跳過」：僅標記 `onboarding_completed = true`，不寫入任何資料。

#### 既有用戶（已有 `startDate`）

顯示單頁「全新升級」歡迎畫面，說明新功能後直接進入主程式。

---

### 4.2 LockScreen — 應用程式鎖定

**觸發：** `lock_enabled == true` 且 App 進入背景（`ON_STOP`）；拍照期間豁免（`isCameraLaunching`）。

| 解鎖方式 | 條件 | 行為 |
|----------|------|------|
| 生物辨識 | 設備支援 `BIOMETRIC_STRONG` | BiometricPrompt，成功後清除鎖定狀態 |
| PIN 碼 | 永遠顯示 | 數字鍵盤，最少 4 位，比對 EncryptedSharedPreferences |

失敗或錯誤時顯示錯誤訊息文字。

---

### 4.3 DailyEntryScreen — 每日記錄（核心畫面）

#### TopAppBar

- **標題：** `yyyy年MM月dd日`，切換日期時 `AnimatedContent`（新日期下滑入 200ms / 舊日期上淡出 150ms）
- **副標題：** 已有記錄時顯示「編輯模式」
- **右側：** 刪除圖示（已存在記錄才顯示） + 日曆選擇（有未儲存變更時先警告）

#### 雙 Tab（HorizontalPager）

- ☀️ 早晨 Tab：未完成早晨記錄時顯示 Badge
- 🌙 晚間 Tab
- IME 可見時 `userScrollEnabled = false`（防誤滑）
- 兩 Tab 各自獨立 ScrollState
- 切換 `isLoading` 時以 180ms `Crossfade` 過渡

---

#### ☀️ 早晨 Tab

**[A] 睡眠記錄卡**

| 欄位 | 控制元件 |
|------|----------|
| 就寢時間 | TimePickerDialog → `bedtime` |
| 起床時間 | TimePickerDialog → `wakeTime` |
| 睡眠時長 | 自動計算顯示 |
| 睡眠品質 | 1–5 星 → `sleepQuality` |
| 因佩戴鎖而醒來 | 是/否 → `wokeUpDueToDevice` |
| 昨晚有春夢 | 是/否 → `hadEroticDream` |

**[B] 身體狀況卡（男性限定）**

| 欄位 | 控制 |
|------|------|
| 晨勃 | 是/否 → `morningErection` |
| 夜間勃起 | 無/偶爾/頻繁 Chip → `nightErections`（0/5/10）|
| 因夜間勃起醒來 | 是/否 → `wokeUpFromErection` |

**[C] 起床狀態卡**

| 欄位 | 控制 |
|------|------|
| 起床心情 | 16 種情緒單選 Chip → `morningMood` |
| 起床能量 | 1–5 電池圖示 → `morningEnergy` |

**儲存：** 未記錄→「完成早晨記錄」；已記錄→「更新早晨記錄」。成功 Snackbar：「☀️ 早晨記錄已儲存！」

---

#### 🌙 晚間 Tab

**[①] DayStatusCard**
- 今日日期 + 核心題完成進度（`CircularProgressIndicator` + 百分比）
- 分母 = `coreQuestions.count { it.isApplicable(entry) }`，由問題集動態計算，新增/刪除題目無需手動維護魔術數字（目前有佩戴=7，無佩戴=6）

**[②] CoreQuestionsCard — 核心問題**

| ID | 題目 | 控制 | 欄位 |
|----|------|------|------|
| C2 | 今天有佩戴鎖嗎？ | 有佩戴/沒有 Chip | `deviceCheckPassed` |
| C1 | 今天的心情狀態 | 16種情緒單選 Chip | `mood` |
| C3 | 今日性慾強度 | Slider 0–10 | `desireLevel` |
| C4 | 佩戴舒適度（佩戴時才顯示）| Slider 0–10 | `comfortRating` |
| C5 | 今日專注度 | Slider 1–10 | `focusLevel` |
| E7 | 是否運動？ | 是/否 | `exercised` |
| E8 | 今天是否清潔了貞操鎖？ | 4種單選 Chip | `cleaningType` |

**打卡照片（可選）：**
- CAMERA 權限 → FileProvider → `TakePicture` → 外部 `Pictures/` 目錄
- 預設模糊（`photoBlurEnabled`），點擊才顯示清晰圖
- 讀取防 OOM：兩段式 BitmapFactory decode（先 `inJustDecodeBounds` 計算 `inSampleSize`）
- ExifInterface 修正旋轉方向

**[③] RotatingQuestionsCard — 每日輪換問題**
- 以 `LocalDate.toEpochDay()` 為種子，從題庫確定性洗牌取前 `AppConstants.ROTATING_QUESTIONS_PER_DAY` 題（預設 2）
- 依 `applicableGenders: Set<Gender>` 欄位篩選（取代舊的 `isMaleOnly: Boolean`，支援未來女性專屬題）
- 是/否 Chip 回答，回答後顯示調皮反饋文字（淡入）
- 題庫由 Room table `rotating_questions` 驅動（見§7），新增/停用題目不需重新編譯

**[④] ExtendedQuestionsCard — 備註**
- 預設折疊（`AnimatedVisibility`）
- 單一多行 TextField → `notes`

**[⑤] EveningMasturbationCard**

| 欄位 | 控制 |
|------|------|
| 是否自慰 | 是/否 → `masturbated` |
| 次數 | +/− 按鈕，最小 1 → `masturbationCount` |

**晚間儲存成功：** Snackbar「儲存成功！」+ 自動開啟今日摘要 `ModalBottomSheet`

---

### 4.4 DashboardScreen — 儀表板

**時間範圍篩選（FilterChip 四選一）：** 本週 / 本月 / 3個月 / 全部

**統計卡（4 張）：**

| 標題 | 計算 |
|------|------|
| 總佩戴天數 | 範圍內 `deviceCheckPassed == true` 的筆數 |
| 記錄完成率 | 記錄筆數 ÷ 範圍天數 × 100% |
| 連續打卡 🔥 | `currentStreak` |
| 最長連續 | `longestStreak` |

**連續打卡成就徽章：** `StreakBadgesSection` 根據 currentStreak / longestStreak 顯示。

**趨勢折線圖（Vico，最近 `AppConstants.DASHBOARD_TREND_DAYS` 天，預設 14）：**

| 圖表 | 資料 | Y 軸 |
|------|------|------|
| 心情趨勢 | `MoodOption.score`（隨情緒定義內聚，不另設散落映射）| 0–5 |
| 性慾強度 | `desireLevel` | 0–10 |
| 舒適度 | `comfortRating` | 0–10 |

> **改進：** 心情→分數映射原為 DashboardViewModel 中散落的 `when` 運算式（開心=5、平靜=4…），改為在 `MoodOption` data class 中內聚 `score: Float` 欄位，新增情緒選項時只需在一處定義。

**統計摘要（4 項）：** 平均性慾強度、平均舒適度、自慰次數（`masturbationCount` 為 null 時以 1 計）、運動次數

**效能：** 7 個 DB 查詢以 `async{}` 並行（`Dispatchers.IO`），~5–20ms。

---

### 4.5 HistoryScreen — 歷史紀錄

**心情日曆（MoodCalendarSection）：**
- 顯示當月，週日起始
- 格子：情緒 emoji 前2字 + 日期數字
- 有記錄：`primaryContainer`；無記錄：`surfaceVariant`；未來：alpha 0.3；今日：primary 色邊框
- 點擊 → 呼叫共享 `DailyEntryViewModel.selectDate()` + 導航至 DailyEntry

**最近記錄列表（RecentEntriesSection）：**
- 降序排列，最多 `AppConstants.HISTORY_RECENT_LIMIT` 筆（預設 30）
- 每筆：日期、心情、性慾評分、舒適度評分
- `remember(key){}` 快取所有衍生計算，最小化 recompose
- 未來可替換為 Paging 3 無限捲動，只需更換 Repository 層，UI 無需改動

---

### 4.6 SettingsScreen — 設定

**[A] 個人資料卡**  
顯示：暱稱、開始日期、性別（FilterChip）、身高、體重、裝置名稱、裝置尺寸  
「編輯」→ `ProfileEditDialog`

**[B] ☀️ 早安提醒卡**  
Switch（`morning_reminder_enabled`）+ TimePicker（預設 07:30）

**[C] 🌙 晚安提醒卡**  
Switch（`reminder_enabled`；Android 13+ 申請 `POST_NOTIFICATIONS`）+ TimePicker（預設 21:00）

**[D] 安全設定卡**

| 選項 | 說明 |
|------|------|
| 生物辨識解鎖 | Switch；設備不支援時停用 |
| PIN 碼鎖定 | Switch；開啟時彈出 `PinSetupDialog` |
| 修改 PIN 碼 | Button；僅 `pin_enabled == true` 時顯示 |
| 照片預設模糊 | Switch（預設 true）|

**[E] 界面設定卡**  
顯示主題：淺色 / 深色 / 跟隨系統（FilterChip 三選一）

**[F] 資料管理卡**

| 操作 | 說明 |
|------|------|
| 匯出 CSV | SAF CreateDocument，`CsvHelper` 序列化 |
| 匯入 CSV | SAF OpenDocument，`CsvHelper` 反序列化 upsert |
| 雲端同步 | 顯示「開發中」（Firebase 預留未啟用）|
| 產生測試資料 | 開發用，`TestDataGenerator.kt` |

---

## 5. 資料模型

### 5.1 DailyEntry 完整欄位

```
── 識別 ──────────────────────────────────────────────────────────
id                   Long              主鍵（Room autoGenerate）
date                 LocalDate         日期（UNIQUE INDEX）
createdAt            LocalDateTime     建立時間
updatedAt            LocalDateTime     最後更新時間

── 晚間：核心 ────────────────────────────────────────────────────
deviceCheckPassed    Boolean           今天有佩戴鎖（預設 true）
mood                 String?           情緒（16 選一）
desireLevel          Int?              性慾強度 0–10
comfortRating        Int?              舒適度 0–10
focusLevel           Int?              專注度 1–10
exercised            Boolean           是否運動
cleaningType         String?           清潔類型
photoPath            String?           打卡照片本地路徑

── 晚間：擴展 ────────────────────────────────────────────────────
masturbated          Boolean           是否自慰
masturbationCount    Int?              自慰次數
masturbationDuration Int?              自慰時長（分鐘）
hadEdging            Boolean           是否邊緣訓練
edgingDuration       Int?              邊緣時長（分鐘）
edgingMethods        List<String>      邊緣方式（多選）
viewedPorn           Boolean           是否觀看色情
pornDuration         Int?              觀看時長（分鐘）
hadErection          Boolean           是否勃起（男性）
erectionCount        Int?              勃起次數
unlocked             Boolean           是否解鎖
temporarilyRemoved   Boolean           是否暫時取下
removalDuration      Int?              取下時長（分鐘）
removalReasons       List<String>      取下原因（多選）
exposedLock          Boolean           是否在公眾場所露出鎖
exposedLocations     List<String>      暴露地點（多選）
hadLeakage           Boolean           是否有分泌物洩漏
leakageAmount        String?           洩漏量
hasDiscomfort        Boolean           是否有不適
discomfortAreas      List<String>      不適部位（多選）
discomfortLevel      Int?              不適程度 1–10
keyholderInteraction Boolean           是否與 Keyholder 互動
interactionTypes     List<String>      互動類型（多選）
exerciseTypes        List<String>      運動種類（多選）
exerciseDuration     Int?              運動時長（分鐘）
emotions             List<String>      情緒多選
completedTasks       List<String>      已完成任務
socialActivities     List<String>      社交活動（多選）
socialAnxiety        Int?              社交焦慮 1–10
selfRating           Int?              自我評分 1–5
notes                String?           備註

── 早晨 ──────────────────────────────────────────────────────────
morningCheckDone     Boolean           早晨記錄是否已提交
bedtime              LocalTime?        就寢時間
wakeTime             LocalTime?        起床時間
sleepQuality         Int?              睡眠品質 1–5
wokeUpDueToDevice    Boolean           因鎖而醒來
hadEroticDream       Boolean           昨晚有春夢
morningMood          String?           起床後心情（16 選一）
morningEnergy        Int?              起床能量 1–5
morningErection      Boolean           晨勃（男性）
nightErections       Int?              夜間勃起強度（0/5/10）
wokeUpFromErection   Boolean           因夜間勃起醒來

── EAV（獨立 Table）──────────────────────────────────────────────
rotatingAnswers      Map<String,String>  key = rotating_questions.key (e.g. "R1")，value="true"/"false"
```

### 5.2 UserSettings 欄位

```
gender                  MALE / FEMALE / OTHER（預設 MALE）
startDate               LocalDate?
reminderEnabled         Boolean（預設 true）
reminderHour / Minute   Int（預設 21:00）
biometricEnabled        Boolean（預設 false）
pinEnabled              Boolean（預設 false）
darkMode                LIGHT / DARK / SYSTEM（預設 SYSTEM）
cloudSyncEnabled        Boolean（預設 false，預留）
currentStreak           Int
longestStreak           Int
lastEntryDate           LocalDate?
height                  Int?（cm）
weight                  Float?（kg）
currentDeviceName       String?
currentDeviceSize       String?
nickname                String?
morningReminderEnabled  Boolean（預設 false）
morningReminderHour / Minute  Int（預設 07:30）
photoBlurEnabled        Boolean（預設 true）
```

---

## 6. 資料庫規格

**名稱：** `chastity_diary_db` ｜ **目前版本：** 5

| 版本 | 變更 | 策略 |
|------|------|------|
| 1–3 | 開發測試 | `fallbackToDestructiveMigrationFrom(1, 2, 3)` |
| 3→4 | 建立穩定結構 | 自定義 migration |
| 4→5 | 新增 `UNIQUE INDEX index_daily_entries_date` | `MIGRATION_4_5` |

**Table：**
- `daily_entries`：所有 DailyEntry 欄位；`List<String>` 以 JSON 序列化
- `daily_entry_attributes`：EAV，PK = `(entryId, attributeKey)`，外鍵 CASCADE 刪除

**POJO：** `DailyEntryWithAttributes`（`@Transaction @Relation`，單次 JOIN 查詢主表與 EAV 屬性）

**主要 DAO 方法：**
`getByDateWithAttributes()`、`getEntriesInRangeSync()`、`getAverageDesireLevel()`、`getAverageComfortRating()`、`getMasturbationCount()`（null 以 1 計）、`getExerciseCount()`、`getPornViewCount()`

---

## 7. 輪換問題題庫（R1–R33）

> **設計原則：** 題庫不再是編譯期 `enum class`，而是由 Room table `rotating_questions` 驅動。新增、停用、修改題目只需操作資料庫（或 seed migration），無需重新打包 App。

**`RotatingQuestionEntity` schema：**

| 欄位 | 類型 | 說明 |
|------|------|------|
| `id` | Int PK autoGenerate | 主鍵，EAV 外鍵引用此 id |
| `key` | String UNIQUE | 識別碼（R1, R2…），對外顯示/遷移用 |
| `title` | String | 題目文字 |
| `feedback` | String | 回答後調皮反饋文字 |
| `applicableGenders` | String (JSON array) | ["MALE","FEMALE","OTHER"]，空陣列=全適用 |
| `enabled` | Boolean | false 可停用不刪除（保留歷史答案完整性）|
| `sortOrder` | Int | 顯示排序 |

> **改進：** 舊設計 `isMaleOnly: Boolean` 只能表達男性專屬一種限制；`applicableGenders: Set<Gender>` 可擴展至任意性別組合，後續新增女性專屬題無需改動程式邏輯。

每日以 `LocalDate.toEpochDay()` 為種子確定性洗牌，取前 `AppConstants.ROTATING_QUESTIONS_PER_DAY`（預設 2）題。

**Seed 資料（32 筆，初始 migration 插入）：**

| Key | 題目 | 適用性別 |
|-----|------|:--------:|
| R1 | 今日是否有分泌物洩漏？ | ALL |
| R2 | 今日是否有主動頂鎖/摩擦，尋求快感？ | MALE |
| R3 | 今日是否進行邊緣訓練？ | ALL |
| R4 | 今日是否與 Keyholder 互動？ | ALL |
| R6 | 今日是否帶著鎖進入公眾場合？ | ALL |
| R7 | 今日是否曾短暫解除鎖？ | ALL |
| R8 | 今日是否有意展示或洩露鎖蹤跡？ | ALL |
| R9 | 今日是否接觸成人內容？ | ALL |
| R10 | 今日是否解鎖或進行自慰？ | ALL |
| R11 | 今日是否進行乳頭開發/玩弄？ | ALL |
| R12 | 今日是否進行後庭開發/探索？ | ALL |
| R13 | 今天你有沒有感受到鎖帶來的不適或調整需求？ | ALL |
| R14 | 今天佩戴鎖是否讓你感覺到內心的平靜或成就？ | ALL |
| R15 | 今天有沒有想起 Keyholder，並感受到連結的溫暖？ | ALL |
| R16 | 今天鎖是否已融入你的日常 routine 中，感覺自然？ | ALL |
| R17 | 今天有沒有將慾望轉向其他活動，如運動或創作？ | ALL |
| R18 | 今天在人群中，有沒有特別注意到自己的隱密狀態？ | ALL |
| R19 | 今天有沒有進行放鬆活動來緩解可能的壓力？ | ALL |
| R20 | 今天醒來後，有沒有回想起與鎖相關的夢境？ | ALL |
| R21 | 今天其他感官（如觸覺或聽覺）是否變得更敏銳？ | ALL |
| R22 | 今天有沒有與 Keyholder 分享你的感受或想法？ | ALL |
| R23 | 今天在不同環境中，鎖帶來的感受如何？ | ALL |
| R24 | 今天有沒有遇到讓你猶豫或掙扎的時刻？ | ALL |
| R25 | 今天佩戴是否帶來任何意外的正面體驗？ | ALL |
| R26 | 今天有沒有特別注意清潔或保濕等保養？ | ALL |
| R27 | 今天有沒有透過寫作或藝術表達你的體驗？ | ALL |
| R28 | 今天時間感覺過得快還是慢，受鎖影響？ | ALL |
| R29 | 今天有沒有在匿名社群分享或閱讀相關經驗？ | ALL |
| R30 | 今天有沒有在想萬一鎖取不下來該怎麼辦？ | ALL |
| R31 | 今天情緒是否有起伏，與鎖相關？ | ALL |
| R32 | 今天有沒有想像未來繼續佩戴的畫面？ | ALL |
| R33 | 今日是否有剔除陰毛？ | ALL |

*備注：無 R5（歷史跳號），現有 32 筆 seed 資料。後續版本透過 DB migration 直接插入新題目，無需更動應用程式碼。*

---

## 8. 可選值清單與可擴展性

> **設計原則：** 所有多選清單統一定義於 `Constants.kt`，UI 元件從常數讀取，不在 Composable 中寫死字串陣列。高頻變動的清單（MOODS、EXERCISE_TYPES）預留升級為 Room table 的路徑，以支援用戶自訂。

| 類型 | 初始選項 | 備注 |
|------|----------|------|
| MOODS（16種）| 😊開心、😌平靜、😐普通、😔沮喪、😰焦慮、😤挫折、🥵興奮、😴無聊、😳羞恥、😎自豪、🤩期待、😬緊張、🧘放鬆、😕困惑、💪充實、😶空虛 | 含 `score` 欄位（見下） |
| CLEANING_TYPES | 未清潔、簡單沖洗、深度清潔、完全取下清潔 | |
| EXERCISE_TYPES | 跑步、健身、游泳、瑜伽、騎車、球類運動、散步、重訓、有氧運動、其他 | 預留自訂升級路徑 |
| EXPOSED_LOCATIONS | 家中、健身房、游泳池、公共浴室、戶外、更衣室、醫院、朋友家、工作場所、其他公共場所 | |
| DISCOMFORT_AREAS | 陰莖、睪丸、會陰、大腿根部、恥骨、尿道、其他 | |
| LEAKAGE_AMOUNTS | 少量、中等、大量 | |
| EDGING_METHODS | 視覺刺激、觸摸、聲音、想像、閱讀、影片、其他 | |
| INTERACTION_TYPES | 訊息聊天、語音通話、視訊、實體見面、任務指派、獎勵、懲罰、檢查、其他 | |
| REMOVAL_REASONS | 清潔、醫療、工作需求、緊急狀況、Keyholder允許、不適、其他 | |
| NIGHT_ERECTION | 無→0、偶爾→5、頻繁→10 | |
| DURATION_OPTIONS（分）| 5、10、15、30、45、60、90、120、180、240 | |

**`MoodOption` data class（改進心情分數映射）：**

```kotlin
data class MoodOption(val emoji: String, val label: String, val score: Float)
// 圖表分數與情緒選項一起定義，不再散落於 DashboardViewModel
// 各 score：開心=5f、平靜=4f、普通=3f、沮喪=2f、焦慮=1.5f、挫折=1f；其餘=3f
```

---

## 9. 安全機制

### 生物辨識
- `BiometricManager.Authenticators.BIOMETRIC_STRONG`
- Prompt：標題「解鎖貞操日記」、副標「使用生物辨識解鎖」、取消「使用 PIN 碼」

### PIN 碼
- 格式：最少 `SecurityConstants.PIN_MIN_LENGTH` 位純數字（預設 4）
- 儲存：`EncryptedSharedPreferences`（`secure_prefs`）
  - MasterKey：`AES256_GCM`；Key：`AES256_SIV`；Value：`AES256_GCM`
  - 儲存鍵：`pin_code`（String）、`lock_enabled`（Boolean）

### 自動鎖定邏輯

```
ProcessLifecycleOwner:
  ON_STOP  → if (_lockEnabled.value && !isCameraLaunching) _isLocked = true
  ON_START → isCameraLaunching = false
```

`_lockEnabled` 為 in-memory `MutableStateFlow`，啟動時讀取一次後快取，避免每次背景事件執行 AES 解密。

---

## 10. 通知機制

| 項目 | 晚間提醒（DailyReminderWorker）| 早晨提醒（MorningReminderWorker）|
|------|-------------------------------|----------------------------------|
| 工作名稱 | `NotificationConstants.WORK_DAILY` | `NotificationConstants.WORK_MORNING` |
| 週期 | 1 天 | 1 天 |
| 策略 | `ExistingPeriodicWorkPolicy.UPDATE` | 同左 |
| 邏輯 | 當日已有記錄 → **靜默不發** | 直接發送 |
| Channel ID | `NotificationConstants.CHANNEL_DAILY` | `NotificationConstants.CHANNEL_MORNING` |
| Notification ID | `NotificationConstants.ID_DAILY`（1001）| `NotificationConstants.ID_MORNING`（1002）|

> **改進：** 通知相關字串與 ID 集中於 `NotificationConstants` object，避免 Worker 類與 SettingsScreen 各自寫死字串導致對不上。

Android 13+：兩者均需 `POST_NOTIFICATIONS` 執行期限授權，在 SettingsScreen 開啟提醒時申請。

---

## 11. 導覽架構

```
MainActivity
  └── MainScreen（Scaffold）
       ├── BottomNavigationBar（4 NavigationBarItem）
       │    ├── 每日記錄  Icons.Filled.Edit         route="daily_entry"（預設）
       │    ├── 儀表板    Icons.Filled.Dashboard     route="dashboard"
       │    ├── 歷史紀錄  Icons.Filled.CalendarMonth route="history"
       │    └── 設定      Icons.Filled.Settings      route="settings"
       └── NavGraph（Box 堆疊 4 個 KeepAliveScreen）
```

**Keep-Alive 策略：** 4 個畫面全時 Compose，切換瞬間完成（0ms），不銷毀重建。  
不可見的畫面：alpha 動畫至 0（`MotionTokens.DurationEmphasis` ms）+ `zIndex=0` + `pointerInput` 攔截所有觸控。

> **改進：** 底部導覽項目改為 `val bottomNavItems: List<BottomNavItem>` 資料驅動，新增畫面只需在清單中追加一筆，不需改動 NavGraph 骨架。

**系統返回鍵（`BackHandler`）：** 非 DailyEntry Tab 時 → 導回 DailyEntry，而非退出 App。

**ViewModel 共享：** `DailyEntryViewModel` 在 NavGraph 層建立，`HistoryScreen` 點日期後可直接切換記錄日期並導航。

---

## 12. 每日敘事自動生成

晚間儲存成功後自動產生，顯示於 `ModalBottomSheet`。

依序輸出（有資料才輸出該段）：心情評語 → 性慾高低評語（高≥`NarrativeConfig.HIGH_THRESHOLD`/中/低≤`NarrativeConfig.LOW_THRESHOLD`，預設 7/3）→ 舒適度建議 → 運動紀錄 → 清潔方式 → Keyholder 互動 → 打卡照片提示 → 解鎖記錄 → 自慰次數 → 邊緣訓練記錄

若所有欄位均空：顯示「今日記錄已儲存，繼續保持！」

---

## 13. 資料匯出匯入

**格式：** CSV（UTF-8）｜ **API：** Storage Access Framework

| 操作 | 流程 |
|------|------|
| 匯出 | `CreateDocument("text/csv")` → `CsvHelper.exportToCsv(entries)` → 寫入用戶選擇的 URI |
| 匯入 | `OpenDocument("text/csv")` → `CsvHelper.importFromCsv(uri)` → 按日期衝突 upsert（不清空現有資料）|

---

## 14. 效能規格

| 項目 | 目標 | 實作 |
|------|------|------|
| App 冷啟動 | < 1s | SplashScreen + EncryptedPrefs / DataStore 並行 IO |
| Dashboard 載入 | < 50ms | 7 DB 查詢 `async{}` 並行，Dispatchers.IO |
| 切換日期查詢 | 最快 | `@Index(date)` UNIQUE 索引 |
| getEntryByDate | 單次 JOIN | `@Transaction @Relation` DailyEntryWithAttributes |
| 照片讀取 | 不 OOM | 兩段式 BitmapFactory decode（inSampleSize）|
| HistoryScreen 重組 | 最小化 | `remember{}`、`remember(key){}` 快取衍生計算 |
| 底部 Tab 切換 | 0ms | Keep-Alive NavGraph |

---

## 15. UI 動畫規格

> **設計原則：** 所有動畫時長統一由 `MotionTokens` object 定義，各 Composable 引用常數，不寫死毫秒數。

```kotlin
object MotionTokens {
    const val DurationShort    = 150  // 淡出、消失
    const val DurationMedium   = 200  // 標準切換
    const val DurationEmphasis = 220  // 畫面 crossfade
    const val DurationContent  = 180  // 內容加載
}
```

| 位置 | 動畫類型 | Token |
|------|----------|-------|
| 底部 Tab 切換 | `animateFloatAsState` Crossfade，FastOutSlowInEasing | `DurationEmphasis` |
| TopAppBar 日期文字 | `AnimatedContent`（滑動+淡入/出）| 進 `DurationMedium`，出 `DurationShort` |
| isLoading ↔ 內容 | `Crossfade` | `DurationContent` |
| 備註卡展開/折疊 | `AnimatedVisibility`（expandVertically）| 預設彈簧 |
| 輪換題反饋文字 | `AnimatedVisibility`（fadeIn）| 預設 |

---

## 16. 依賴項清單

| 函式庫 | 版本 | 用途 |
|--------|------|------|
| `core-splashscreen` | 1.0.1 | SplashScreen API |
| `compose-bom` | 2023.10.01 | Compose 版本管理 |
| `material3` | BOM | UI 元件 |
| `navigation-compose` | 2.7.6 | 導覽（Keep-Alive 自訂）|
| `lifecycle-viewmodel-compose` | 2.7.0 | ViewModel |
| `lifecycle-runtime-compose` | 2.7.0 | collectAsStateWithLifecycle |
| `lifecycle-process` | 2.7.0 | ProcessLifecycleOwner（自動鎖定）|
| `room-runtime` + `room-ktx` | 2.6.1 | 本地資料庫 |
| `room-compiler`（kapt）| 2.6.1 | 程式碼生成 |
| `datastore-preferences` | 1.0.0 | 使用者設定 |
| `security-crypto` | 1.1.0-alpha06 | EncryptedSharedPreferences |
| `biometric` | 1.1.0 | BiometricPrompt |
| `work-runtime-ktx` | 2.9.0 | WorkManager（通知）|
| `exifinterface` | 1.3.7 | 照片旋轉修正 |
| `vico-compose-m3` + `vico-core` | 1.13.1 | 折線圖 |
| `gson` | 2.10.1 | List\<String\> 序列化 |
| `kotlinx-coroutines-android` | 1.7.3 | 協程 |
| Firebase | 預留 | 雲端同步（開發中，plugin 已 comment out）|

---

## 17. 可擴展性設計規範

本章彙整所有曾被硬編碼的數值，以及各自改進後應歸屬的位置，作為開發時的單一參考點。

### 17.1 Constants 集中化

```kotlin
// util/AppConstants.kt
object AppConstants {
    const val ROTATING_QUESTIONS_PER_DAY = 2   // 每日輪換題數
    const val DASHBOARD_TREND_DAYS       = 14  // 趨勢圖回溯天數
    const val HISTORY_RECENT_LIMIT       = 30  // 最近記錄列表上限
}

object SecurityConstants {
    const val PIN_MIN_LENGTH       = 4
    const val ENCRYPTED_PREFS_NAME = "secure_prefs"
    const val KEY_PIN_CODE         = "pin_code"
    const val KEY_LOCK_ENABLED     = "lock_enabled"
}

object NotificationConstants {
    const val WORK_DAILY    = "daily_reminder"
    const val WORK_MORNING  = "morning_reminder"
    const val CHANNEL_DAILY = "daily_reminder"
    const val CHANNEL_MORNING = "morning_reminder"
    const val ID_DAILY      = 1001
    const val ID_MORNING    = 1002
}

object NarrativeConfig {
    const val HIGH_THRESHOLD = 7
    const val LOW_THRESHOLD  = 3
}

object MotionTokens {
    const val DurationShort    = 150
    const val DurationMedium   = 200
    const val DurationEmphasis = 220
    const val DurationContent  = 180
}
```

### 17.2 原硬編碼問題對照表

| 問題 | 原實作 | 改進後 | 優先 |
|------|--------|--------|:----:|
| 輪換題庫 | `enum class RotatingQuestion`（編譯期固定）| Room table `rotating_questions` + seed migration | 高 |
| 性別篩選 | `isMaleOnly: Boolean`（只能男性專屬）| `applicableGenders: Set<Gender>`（支援任意組合）| 高 |
| 心情→圖表分數 | DashboardViewModel `when` 散落寫死 | `MoodOption.score` 欄位內聚 | 高 |
| DayStatusCard 分母 | 手動 `7`/`6`（魔術數字）| `coreQuestions.count { it.isApplicable(entry) }` | 中 |
| 每日抽題數 `2` | 字面量 | `AppConstants.ROTATING_QUESTIONS_PER_DAY` | 中 |
| 趨勢圖天數 `14` | 字面量 | `AppConstants.DASHBOARD_TREND_DAYS` | 中 |
| 歷史記錄上限 `30` | `take(30)` | `AppConstants.HISTORY_RECENT_LIMIT` | 中 |
| 敘事閾值 `7`/`3` | 字面量 | `NarrativeConfig.HIGH_THRESHOLD / LOW_THRESHOLD` | 中 |
| 底部導覽項目 | 4 個硬編碼分支 | `List<BottomNavItem>` 資料驅動 | 低 |
| PIN 最少位數 `4` | `pin.length >= 4` | `SecurityConstants.PIN_MIN_LENGTH` | 低 |
| 通知 Channel/ID | Worker 各自寫死 | `NotificationConstants.*` | 低 |
| 動畫時長 `220/200/180/150` | Composable 內寫死 ms | `MotionTokens.*` | 低 |

### 17.3 可選值升級路徑

以下清單目前為 `Constants.kt` 靜態陣列，未來可依需求升級為 Room table 支援用戶自訂：

| 清單 | 規劃 DB Table | 預估工時 |
|------|--------------|----------|
| MOODS | `mood_options`（含 `score`、`emoji`、`label`、`enabled`）| 3h |
| EXERCISE_TYPES | `exercise_types`（含 `label`、`enabled`）| 2h |
| 其餘清單 | 使用頻率低，維持靜態即可 | — |

---

*本規格書根據 CB-diary-A 專案原始碼（截至 2026-02-22）自動彙整，並已針對可擴展性進行改進。*
