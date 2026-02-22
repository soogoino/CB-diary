# CB-diary-A 開發任務清單

> 根據 `plan-cbDiarySpec.prompt.md` 切割，採**資料層優先 → 領域層 → UI 層**順序排列。  
> 每個任務獨立可測試，帶有前置依賴標記。

---

## Phase 0 — 專案基礎建設

- [ ] **P0-1** 確認 `build.gradle.kts` 依賴版本與 spec §16 對齊（Room 2.6.1、Vico 1.13.1、security-crypto、biometric…）
- [ ] **P0-2** 建立 package 結構：`data/`、`domain/`、`ui/`、`util/`、`worker/`
- [ ] **P0-3** 建立 `util/AppConstants.kt`，定義 `AppConstants`、`SecurityConstants`、`NotificationConstants`、`NarrativeConfig`、`MotionTokens`（見 spec §17.1）
- [ ] **P0-4** 建立 `util/MoodOption.kt`，定義 `MoodOption(emoji, label, score)` data class 與 `Constants.MOODS` 清單（含 16 種情緒及對應 score）
- [ ] **P0-5** 建立 `util/Constants.kt`，定義其他靜態選項清單（CLEANING_TYPES、EXERCISE_TYPES、EXPOSED_LOCATIONS、DISCOMFORT_AREAS 等，見 spec §8）

---

## Phase 1 — 資料層：Entity & Database

### 1A — Entity 定義

- [ ] **1A-1** 建立 `data/entity/DailyEntryEntity.kt`，包含 spec §5.1 全部欄位，`List<String>` 欄位加 `@TypeConverter`（Gson）
- [ ] **1A-2** 建立 `data/entity/DailyEntryAttributeEntity.kt`（EAV table），複合 PK (`entryId`, `attributeKey`)，外鍵 `CASCADE DELETE`
- [ ] **1A-3** 建立 `data/entity/RotatingQuestionEntity.kt`（spec §7 table schema），`applicableGenders` 欄位存 JSON array
- [ ] **1A-4** 建立 `data/pojo/DailyEntryWithAttributes.kt`（`@Transaction @Relation`，JOIN 主表與 EAV）
- [ ] **1A-5** 建立 `data/converter/` 資料夾，實作 `LocalDateConverter`、`LocalTimeConverter`、`LocalDateTimeConverter`、`StringListConverter`

### 1B — Database & Migration

- [ ] **1B-1** 建立 `data/database/AppDatabase.kt`（`DATABASE_VERSION = 5`），宣告三張 table
- [ ] **1B-2** 實作 `MIGRATION_3_4`（建立穩定結構，含所有 DailyEntry 欄位）
- [ ] **1B-3** 實作 `MIGRATION_4_5`（新增 `UNIQUE INDEX index_daily_entries_date`）
- [ ] **1B-4** 實作 `MIGRATION_5_6`：新增 `rotating_questions` table，插入全部 32 筆 seed 資料（R1–R33，含 `applicableGenders`、`enabled`、`sortOrder`）
- [ ] **1B-5** 設定 `fallbackToDestructiveMigrationFrom(1, 2, 3)`

### 1C — DAO

- [ ] **1C-1** 建立 `DailyEntryDao.kt`：`getByDateWithAttributes()`、`upsert()`、`delete()`、`getEntriesInRangeSync()`
- [ ] **1C-2** 建立 `DailyEntryDao.kt` 統計查詢：`getAverageDesireLevel()`、`getAverageComfortRating()`、`getMasturbationCount()`（null→1）、`getExerciseCount()`、`getPornViewCount()`
- [ ] **1C-3** 建立 `DailyEntryAttributeDao.kt`：`upsertAttributes()`、`deleteByEntryId()`
- [ ] **1C-4** 建立 `RotatingQuestionDao.kt`：`getEnabledQuestions()`、`getAll()`、`insert()`

---

## Phase 2 — 資料層：Repository

- [ ] **2-1** 建立 `data/repository/DailyEntryRepository.kt`：封裝 DAO，提供 Flow 與 suspend 方法，轉換 Entity ↔ Domain model
- [ ] **2-2** 建立 `data/repository/RotatingQuestionRepository.kt`：提供 `getTodayQuestions(date: LocalDate, gender: Gender)` — 確定性洗牌邏輯（seed = `toEpochDay()`），依 `applicableGenders` 篩選，取前 `AppConstants.ROTATING_QUESTIONS_PER_DAY` 題
- [ ] **2-3** 建立 `data/datastore/UserSettingsRepository.kt`：DataStore 讀寫，提供 `userSettingsFlow: Flow<UserSettings>`，含所有 §5.2 欄位

---

## Phase 3 — 安全機制

- [ ] **3-1** 建立 `util/EncryptedPrefsHelper.kt`：`MasterKey(AES256_GCM)` + `EncryptedSharedPreferences`，提供 `readPin()`、`writePin()`、`readLockEnabled()`、`writeLockEnabled()`，Key 名稱引用 `SecurityConstants`
- [ ] **3-2** 建立 `util/BiometricHelper.kt`：封裝 `BiometricPrompt` 建立與回調，標題/取消文字寫死於此處
- [ ] **3-3** 建立 `ui/components/PinSetupDialog.kt`：數字鍵盤，最少 `SecurityConstants.PIN_MIN_LENGTH` 位，二次確認，儲存後回調

---

## Phase 4 — ViewModel 層

- [ ] **4-1** 建立 `ui/viewmodel/DailyEntryViewModel.kt`：
  - `selectDate(date)` — 觸發 DB 查詢
  - `saveEvening()` / `saveMorning()` — upsert（主表 + EAV rotatingAnswers）
  - `deleteEntry()`
  - `uiState: StateFlow<DailyEntryUiState>`（含 `isLoading`、entry 資料、今日輪換題）
  - 連續打卡計算（`currentStreak`、`longestStreak`）寫回 DataStore
- [ ] **4-2** 建立 `ui/viewmodel/DashboardViewModel.kt`：
  - `timeRange: StateFlow<TimeRange>`（本週/本月/3個月/全部）
  - 7 個 DB 統計查詢以 `async{}` 並行
  - 趨勢圖資料使用 `DASHBOARD_TREND_DAYS` 天，心情分數取 `MoodOption.score`
- [ ] **4-3** 建立 `ui/viewmodel/HistoryViewModel.kt`：
  - 當月記錄 Flow（日曆）
  - 最近 `HISTORY_RECENT_LIMIT` 筆記錄
- [ ] **4-4** 建立 `ui/viewmodel/SettingsViewModel.kt`：讀寫 UserSettings DataStore，觸發 WorkManager 通知排程

---

## Phase 5 — MainActivity & 啟動流程

- [ ] **5-1** 實作 `MainActivity.kt`：
  - `installSplashScreen()` 在 `super.onCreate()` 之前
  - `async{}` 並行：EncryptedPrefs init、DataStore `isOnboardingCompleted`、DataStore `userSettingsFlow`
  - `_startupData: MutableStateFlow<StartupData?>` — null 時 Splash 持續顯示
  - `_lockEnabled: MutableStateFlow<Boolean>` in-memory cache
- [ ] **5-2** 實作 `ProcessLifecycleOwner` 觀察者：`ON_STOP` → 設定 `_isLocked`（引用 `_lockEnabled.value`，不重讀 AES）；`ON_START` → 清除 `isCameraLaunching`
- [ ] **5-3** 實作啟動路由：`onboardingCompleted == false` → Onboarding；`lock_enabled == true` → Lock；否則 → Main

---

## Phase 6 — UI：導覽架構

- [ ] **6-1** 建立 `ui/navigation/BottomNavItem.kt` data class（route、label、icon）及 `bottomNavItems: List<BottomNavItem>` 清單（資料驅動，見 spec §17.2）
- [ ] **6-2** 建立 `ui/navigation/NavGraph.kt`（Keep-Alive Box 堆疊）：
  - 4 個 `KeepAliveScreen`，alpha crossfade 使用 `MotionTokens.DurationEmphasis`
  - `zIndex` + `pointerInput` 攔截不可見畫面的觸控
- [ ] **6-3** 建立 `ui/screens/MainScreen.kt`：Scaffold + BottomNavigationBar（遍歷 `bottomNavItems`）

---

## Phase 7 — UI：OnboardingScreen

- [ ] **7-1** 建立 `ui/screens/onboarding/OnboardingScreen.kt`（HorizontalPager，禁止滑動，5 頁）
- [ ] **7-2** 實作 `WelcomePage`（頁 0）
- [ ] **7-3** 實作 `ProfilePage`（頁 1）：暱稱、性別 Chip（MALE/FEMALE/OTHER）、開始日期
- [ ] **7-4** 實作 `DevicePage`（頁 2）：裝置名稱、身高、體重
- [ ] **7-5** 實作 `SecurityPage`（頁 3）：生物辨識開關、呼叫 `PinSetupDialog`
- [ ] **7-6** 實作 `ReminderPage`（頁 4）：晚間提醒開關 + TimePicker，完成後寫入 DataStore
- [ ] **7-7** 實作頂部步驟進度條（`x / 5` + LinearProgressIndicator）
- [ ] **7-8** 實作既有用戶「全新升級」單頁邏輯

---

## Phase 8 — UI：LockScreen

- [ ] **8-1** 建立 `ui/screens/LockScreen.kt`：PIN 數字鍵盤（最少 `PIN_MIN_LENGTH` 位）+ 比對 EncryptedPrefs
- [ ] **8-2** 整合 `BiometricHelper`：設備支援時自動觸發，失敗/取消 fallback 到 PIN

---

## Phase 9 — UI：DailyEntryScreen

- [ ] **9-1** 建立 `DailyEntryScreen.kt` 骨架：TopAppBar + HorizontalPager（早晨/晚間 Tab）
- [ ] **9-2** TopAppBar：
  - 日期文字用 `AnimatedContent`（進 `DurationMedium`，出 `DurationShort`）
  - 刪除圖示（有記錄才顯示）
  - 日曆選擇（有未儲存變更先警告 Dialog）
- [ ] **9-3** `isLoading` 狀態以 `Crossfade(DurationContent)` 包裹 Pager/Spinner
- [ ] **9-4** 實作早晨Tab — **[A] SleepCard**：就寢/起床 TimePickerDialog、睡眠時長自動計算、星評、勃起/春夢
- [ ] **9-5** 實作早晨Tab — **[B] BodyConditionCard**（性別==MALE 才顯示）：晨勃、夜間勃起 Chip、因勃起醒來
- [ ] **9-6** 實作早晨Tab — **[C] MorningMoodCard**：16 種情緒 Chip、能量 1–5 電池圖示
- [ ] **9-7** 實作早晨Tab 儲存按鈕（未記錄/已記錄 文字切換）
- [ ] **9-8** 實作晚間Tab — **[①] DayStatusCard**：CircularProgressIndicator，分母由 `coreQuestions.count { it.isApplicable(entry) }` 動態計算
- [ ] **9-9** 實作晚間Tab — **[②] CoreQuestionsCard**（C2→C1→C3→C4→C5→E7→E8，共 7 題）
- [ ] **9-10** 實作打卡照片區塊：CAMERA 權限 + FileProvider + `TakePicture`；兩段式 BitmapFactory decode；ExifInterface 旋轉修正；預設模糊（依 `photoBlurEnabled`）
- [ ] **9-11** 實作晚間Tab — **[③] RotatingQuestionsCard**：顯示今日 2 題，是/否 Chip，回答後 `AnimatedVisibility(fadeIn)` 反饋文字
- [ ] **9-12** 實作晚間Tab — **[④] NotesCard**（ExtendedQuestionsCard）：預設折疊 `AnimatedVisibility(expandVertically)`, 多行 TextField
- [ ] **9-13** 實作晚間Tab — **[⑤] MasturbationCard**：是/否切換，次數 +/− 按鈕
- [ ] **9-14** 晚間儲存成功後：Snackbar「儲存成功！」+ 自動開啟 `ModalBottomSheet`（每日敘事）

---

## Phase 10 — UI：DashboardScreen

- [ ] **10-1** 建立 `DashboardScreen.kt`：時間範圍 FilterChip（本週/本月/3個月/全部）
- [ ] **10-2** 實作 4 張統計卡（總佩戴天數、記錄完成率、連續打卡🔥、最長連續）
- [ ] **10-3** 實作 `StreakBadgesSection`：根據 `currentStreak`/`longestStreak` 條件顯示成就徽章
- [ ] **10-4** 實作 Vico 趨勢折線圖（心情 `MoodOption.score`、性慾強度、舒適度），回溯天數引用 `DASHBOARD_TREND_DAYS`
- [ ] **10-5** 實作統計摘要 4 項（平均性慾、平均舒適度、自慰次數、運動次數）

---

## Phase 11 — UI：HistoryScreen

- [ ] **11-1** 建立 `HistoryScreen.kt` 骨架
- [ ] **11-2** 實作 `MoodCalendarSection`：當月格子（週日起始），有/無記錄樣式、未來日期 alpha 0.3、今日色框，點擊呼叫 `ViewModel.selectDate()` + 導航
- [ ] **11-3** 實作 `RecentEntriesSection`：降序最多 `HISTORY_RECENT_LIMIT` 筆，`remember(key){}` 快取，卡片顯示日期/心情/評分

---

## Phase 12 — UI：SettingsScreen

- [ ] **12-1** 建立 `SettingsScreen.kt` 骨架（6 張卡片）
- [ ] **12-2** 實作 **[A] 個人資料卡** + `ProfileEditDialog`（暱稱、開始日期、性別、身高、體重、裝置名稱/尺寸）
- [ ] **12-3** 實作 **[B] 早安提醒卡** + **[C] 晚安提醒卡**：Switch + TimePicker，Android 13+ `POST_NOTIFICATIONS`
- [ ] **12-4** 實作 **[D] 安全設定卡**：生物辨識 Switch（不支援時 disable）、PIN Switch + `PinSetupDialog`、修改 PIN（`pinEnabled` 才顯示）、照片模糊 Switch
- [ ] **12-5** 實作 **[E] 界面設定卡**：主題 FilterChip（淺/深/系統）寫回 DataStore
- [ ] **12-6** 實作 **[F] 資料管理卡**：匯出 CSV / 匯入 CSV（見 Phase 14）、雲端同步「開發中」、產生測試資料

---

## Phase 13 — 通知機制

- [ ] **13-1** 建立 `worker/DailyReminderWorker.kt`：查當日是否已有記錄，有則靜默，無則發通知；Channel ID / Notification ID / Work name 引用 `NotificationConstants`
- [ ] **13-2** 建立 `worker/MorningReminderWorker.kt`：直接發送早安通知
- [ ] **13-3** 在 SettingsViewModel 中實作排程/取消 WorkManager（`ExistingPeriodicWorkPolicy.UPDATE`）
- [ ] **13-4** 在 `AndroidManifest.xml` 宣告兩個 NotificationChannel（`CHANNEL_DAILY`、`CHANNEL_MORNING`）

---

## Phase 14 — 資料匯出匯入

- [ ] **14-1** 建立 `util/CsvHelper.kt`：`exportToCsv(entries)` — 序列化所有 DailyEntry 欄位為 CSV 行（UTF-8）
- [ ] **14-2** 實作 `importFromCsv(uri)` — 反序列化，按日期 upsert（不清空現有資料），欄位缺失 graceful 跳過
- [ ] **14-3** 在 SettingsScreen 連接 SAF `CreateDocument` / `OpenDocument` ActivityResult

---

## Phase 15 — 每日敘事生成

- [ ] **15-1** 建立 `domain/NarrativeGenerator.kt`：依序組裝文字段落（心情→性慾→舒適度→…），閾值引用 `NarrativeConfig.HIGH_THRESHOLD / LOW_THRESHOLD`
- [ ] **15-2** 建立 `ui/components/DailySummaryBottomSheet.kt`：`ModalBottomSheet`，顯示 `NarrativeGenerator` 輸出

---

## Phase 16 — 連續打卡邏輯

- [ ] **16-1** 建立 `domain/StreakCalculator.kt`：給定有記錄的日期集合，計算 `currentStreak` 與 `longestStreak`
- [ ] **16-2** 在 `DailyEntryViewModel.saveEvening()` 儲存成功後呼叫，結果寫回 `UserSettingsRepository`

---

## Phase 17 — UI 動畫完善

- [ ] **17-1** 確認所有 `animateFloatAsState`（Tab crossfade）使用 `MotionTokens.DurationEmphasis`
- [ ] **17-2** 確認 TopAppBar `AnimatedContent` 使用 `DurationMedium`（進）/ `DurationShort`（出）
- [ ] **17-3** 確認 `isLoading` Crossfade 使用 `MotionTokens.DurationContent`
- [ ] **17-4** 確認 `AnimatedVisibility` 展開/反饋文字符合 spec §15

---

## Phase 18 — 測試與驗收

- [ ] **18-1** 單元測試：`StreakCalculator`（邊界：連續/斷開/全空）
- [ ] **18-2** 單元測試：`RotatingQuestionRepository.getTodayQuestions()`（相同 date 相同結果、性別篩選正確）
- [ ] **18-3** 單元測試：`NarrativeGenerator`（各段落條件覆蓋、全空時 fallback）
- [ ] **18-4** 單元測試：`CsvHelper` 匯出再匯入，欄位值一致
- [ ] **18-5** Room instrumented test：Migration 4→5、5→6（seed 32 筆完整）
- [ ] **18-6** UI 手動驗收：冷啟動 < 1s（Splash 正確持續至 IO 完成）
- [ ] **18-7** UI 手動驗收：DayStatusCard 分母在有/無佩戴時分別為正確值
- [ ] **18-8** UI 手動驗收：輪換題每日固定、性別篩選生效
- [ ] **18-9** UI 手動驗收：生物辨識 + PIN 雙解鎖、背景 → 自動鎖定
- [ ] **18-10** UI 手動驗收：通知在已記錄當日靜默、未記錄時正確發送

---

## 依賴關係速查

```
P0 ──► 1A ──► 1B ──► 1C ──► 2 ──► 3
                                    │
                               4 ◄──┘
                               │
                    5 ─── 6 ──► 7,8,9,10,11,12
                                    │
                         13,14,15,16 ◄──┘
                                    │
                               17 ──► 18
```

| 階段 | 前置必須完成 |
|------|-------------|
| 1A | P0 |
| 1B | 1A |
| 1C | 1B |
| 2 | 1C |
| 3 | P0 |
| 4 | 2, 3 |
| 5 | 3, 4 |
| 6–12 | 4, 5 |
| 13 | 4, 6 |
| 14 | 2, 12 |
| 15, 16 | 4 |
| 17 | 6–12 |
| 18 | 全部 |
