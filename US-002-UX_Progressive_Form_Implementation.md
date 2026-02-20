# US-002-UX: 漸進式表單重構實作報告

## 📋 實作概述

**實作時間:** 2024  
**狀態:** ✅ 已完成  
**建置狀態:** BUILD SUCCESSFUL

將原本23個問題的單一長表單重構為4步驟漸進式揭露(Progressive Disclosure)設計，大幅改善用戶體驗。

---

## 🎯 設計目標

### 問題分類策略
1. **核心問題 (CORE)** - 10個必答問題，完成時間約3分鐘
2. **條件問題 (CONDITIONAL)** - 8個根據回答觸發的問題
3. **輪替問題 (ROTATING)** - 5個問題每天輪流顯示1個
4. **檢閱頁面 (REVIEW)** - 所有已填答內容的摘要與提交

### UX 改善成果
- **減少認知負擔**: 每步驟平均 2-3 個問題，而非一次顯示23個
- **智慧問題流**: 只在必要時詢問後續問題(例如:看了色情內容→詢問時長)
- **每日新鮮感**: 輪替問題每天不同，增加參與度
- **進度可視化**: 實時顯示完成百分比，鼓勵用戶完成表單

---

## 📁 新增檔案

### 1. FormFlow.kt (200 行)
**路徑:** `domain/model/FormFlow.kt`

**核心元件:**
```kotlin
enum class QuestionId { MOOD, DESIRE_LEVEL, ..., SOCIAL_ACTIVITIES }
enum class FormStep { CORE, CONDITIONAL, ROTATING, REVIEW }

data class FormFlowState(
    val currentStep: FormStep,
    val completedSteps: Set<FormStep>,
    val rotatingQuestionOfDay: QuestionId?
) {
    fun calculateProgress(entry: DailyEntry, gender: Gender): Float
    fun canProceedToNextStep(entry: DailyEntry): Boolean
}

fun generateRotatingQuestionOfDay(date: LocalDate): QuestionId
```

**特色實作:**
- **日期種子輪替演算法**: 使用 `dayOfYear % 5` 確保每天相同問題
- **進度計算邏輯**: 根據已回答問題數量計算0-100%進度
- **性別感知驗證**: 男性限定問題(勃起、夜間勃起)僅在相應性別時計入進度

---

### 2. CoreQuestions.kt (247 行)
**路徑:** `ui/components/CoreQuestions.kt`

**包含的10個問題:**
1. 心情 (MoodQuestion)
2. 性慾強度 (DesireLevelQuestion) - 1-10滑桿
3. 舒適度 (ComfortQuestion) - 1-5星星評分
4. 睡眠品質 (SleepQuestion) - 5級選項 + 是否因裝置醒來
5. 專注度 (FocusQuestion) - 1-10滑桿
6. 裝置檢查 (DeviceCheckQuestion) - 是/否切換
7. 自我評價 (SelfRatingQuestion) - 1-5星星評分
8. 照片 (PhotoQuestion) - 開啟相機按鈕
9. 情緒標籤 (EmotionsQuestion) - 多選 FilterChip
10. 備註 (NotesQuestion) - 多行文字輸入

**UI 模式:**
- 統一使用 `QuestionSection` 容器提供一致外觀
- 複用元件: `SliderWithLabel`, `StarRating`, `YesNoToggle`, `MultiSelectChipGroup`
- 即時狀態更新: 所有輸入直接呼叫 `viewModel.update*()` 方法

---

### 3. ConditionalQuestions.kt (418 行)
**路徑:** `ui/components/ConditionalQuestions.kt`

**8個條件問題與觸發邏輯:**

| 問題 | 觸發條件 | 後續欄位 |
|------|---------|---------|
| 1. PornQuestion | 總是顯示 | 觀看時長 (viewedPorn → pornDuration) |
| 2. ErectionQuestion | 性別=男性 | 勃起次數 (hadErection → erectionCount) |
| 3. UnlockQuestion | 總是顯示 | 自慰/時長 (unlocked → masturbated, duration) |
| 4. DiscomfortQuestion | 總是顯示 | 疼痛部位/程度 (hasDiscomfort → areas, level) |
| 5. LeakageQuestion | 總是顯示 | 洩漏程度 (hadLeakage → leakageAmount) |
| 6. EdgingQuestion | 總是顯示 | 時長/方法 (hadEdging → duration, methods) |
| 7. RemovalQuestion | 總是顯示 | 時長/原因 (temporarilyRemoved → duration, reasons) |
| 8. NightErectionQuestion | 性別=男性 | 夜勃次數/驚醒 (nightErections, wokeUpFromErection) |

**技術細節:**
- **性別過濾**: `if (userGender == Gender.MALE)` 條件渲染
- **Material3 實驗性 API**: 使用 `@OptIn(ExperimentalMaterial3Api::class)` 啟用 FilterChip
- **兩階段輸入**: 先問是/否，再根據答案顯示詳細欄位

---

### 4. RotatingQuestions.kt (259 行)
**路徑:** `ui/components/RotatingQuestions.kt`

**5個輪替問題:**
1. **ExerciseQuestion** - 運動類型多選 + 時長
2. **ExposedLockQuestion** - 露出地點多選
3. **KeyholderInteractionQuestion** - 互動類型多選
4. **CleaningQuestion** - 清潔類型單選 (FilterChip)
5. **SocialActivitiesQuestion** - 社交活動多選 + 焦慮程度

**輪替機制:**
```kotlin
when (formFlowState.rotatingQuestionOfDay) {
    QuestionId.EXERCISE -> ExerciseQuestion(...)
    QuestionId.EXPOSED_LOCK -> ExposedLockQuestion(...)
    QuestionId.KEYHOLDER_INTERACTION -> KeyholderInteractionQuestion(...)
    QuestionId.CLEANING -> CleaningQuestion(...)
    QuestionId.SOCIAL_ACTIVITIES -> SocialActivitiesQuestion(...)
    else -> Text("今日無輪替問題")
}
```

**每日固定問題計算:**  
日期 2024-01-15 → dayOfYear = 15 → 15 % 5 = 0 → Exercise  
日期 2024-01-16 → dayOfYear = 16 → 16 % 5 = 1 → ExposedLock

---

### 5. FormProgress.kt (143 行)
**路徑:** `ui/components/FormProgress.kt`

**兩個主要組件:**

#### FormProgressIndicator
```kotlin
@Composable
fun FormProgressIndicator(
    progress: Float,           // 0.0 - 1.0
    currentStep: FormStep,
    completedSteps: Set<FormStep>
)
```
- 頂部線性進度條
- 4個步驟圓點指示器
- 當前步驟粗體藍色，已完成步驟打勾，未完成灰色

#### FormNavigationButtons
```kotlin
@Composable
fun FormNavigationButtons(
    canGoBack: Boolean,
    canGoNext: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextButtonText: String = "下一步"
)
```
- 左側返回按鈕 (第一步隱藏)
- 右側下一步/檢閱/儲存按鈕 (動態文字)
- 自動停用邏輯 (缺少必填欄位時)

---

## 🔄 修改檔案

### DailyEntryViewModel.kt
**新增內容:**
```kotlin
// 表單流程狀態
val formFlowState = MutableStateFlow(
    FormFlowState(
        currentStep = FormStep.CORE,
        completedSteps = emptySet(),
        rotatingQuestionOfDay = generateRotatingQuestionOfDay(LocalDate.now())
    )
)

// 用戶設定 (用於性別過濾)
val userSettings: Flow<UserSettings> = settingsRepository.userSettings

// 導航方法
fun nextStep() { ... }
fun previousStep() { ... }
fun goToStep(step: FormStep) { ... }
```

**責任劃分:**
- ViewModel: 管理表單流程狀態、驗證邏輯、導航控制
- Screen: 純 UI 渲染、用戶輸入收集、事件分發

---

### DailyEntryScreen.kt (完全重寫)
**舊版 → 新版對比:**

| 項目 | 舊版 | 新版 |
|------|------|------|
| 結構 | 單一長表單 | 4步驟分頁 |
| 程式碼行數 | ~600行 | 333行 |
| 捲動行為 | 單一長 LazyColumn | 每步驟獨立 LazyColumn |
| 進度顯示 | 無 | 頂部進度條 + 步驟指示器 |
| 導航 | 無 | 上一步/下一步按鈕 |

**新版核心邏輯:**
```kotlin
when (formFlowState.currentStep) {
    FormStep.CORE -> {
        CoreQuestionsSection()
        FormNavigationButtons(
            canGoBack = false,
            canGoNext = entry.mood.isNotBlank(),
            onNext = { viewModel.nextStep() }
        )
    }
    FormStep.CONDITIONAL -> { ... }
    FormStep.ROTATING -> { ... }
    FormStep.REVIEW -> {
        ReviewSection()
        Button(onClick = { viewModel.saveEntry() }) {
            Text("儲存今日記錄")
        }
    }
}
```

---

## 🐛 修復的編譯錯誤

### 問題1: StateFlow initial 參數缺失
**錯誤訊息:**
```
No value passed for parameter 'initial'
```

**位置:** DailyEntryScreen.kt:34

**修復:**
```kotlin
// 修正前
val userSettings by viewModel.userSettings.collectAsState()

// 修正後
val userSettings by viewModel.userSettings.collectAsState(
    initial = UserSettings()
)
```

---

### 問題2: String? 空安全性
**錯誤訊息:**
```
Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver
```

**位置:** DailyEntryScreen.kt:255-256

**修復:**
```kotlin
// 修正前
if (entry.notes.isNotBlank()) {
    SummaryRow("備註", entry.notes)
}

// 修正後
if (!entry.notes.isNullOrBlank()) {
    SummaryRow("備註", entry.notes ?: "")
}
```

---

### 問題3: 實驗性 API 未標註
**錯誤訊息:**
```
This declaration is experimental and its usage should be marked with '@OptIn(...)' 
```

**相關元件:** FilterChip (Material3 實驗性 API)

**受影響檔案:**
- ConditionalQuestions.kt: 3處 (ConditionalQuestionsSection, PornQuestion, LeakageQuestion)
- RotatingQuestions.kt: 2處 (RotatingQuestionSection, CleaningQuestion)

**修復:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleaningQuestion(...) {
    FilterChip(selected = ..., onClick = ..., label = ...)
}
```

---

### 問題4: 衝突的函數重載
**錯誤訊息:**
```
Conflicting overloads: DailyEntryScreen
```

**原因:** 備份檔案 `DailyEntryScreen.kt.old` 與新版 `DailyEntryScreenNew.kt` 同時存在

**修復操作:**
```bash
rm app/src/main/java/.../DailyEntryScreen.kt.old
mv app/src/main/java/.../DailyEntryScreen.kt app/src/main/java/.../DailyEntryScreen.kt.backup
mv app/src/main/java/.../DailyEntryScreenNew.kt app/src/main/java/.../DailyEntryScreen.kt
```

---

## ✅ 建置驗證

**最終建置結果:**
```bash
$ ./gradlew assembleDebug

BUILD SUCCESSFUL in 10s
52 actionable tasks: 7 executed, 45 up-to-date
```

**生成的 APK:**
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📊 程式碼統計

| 類別 | 新增行數 |
|------|---------|
| FormFlow.kt | 200 |
| CoreQuestions.kt | 247 |
| ConditionalQuestions.kt | 418 |
| RotatingQuestions.kt | 259 |
| FormProgress.kt | 143 |
| DailyEntryScreen.kt (重寫) | 333 |
| **總計** | **1,600+** |

---

## 🧪 測試檢查清單

### 功能測試
- [ ] 核心問題頁面正常顯示10個問題
- [ ] 條件問題根據回答正確觸發/隱藏
- [ ] 輪替問題每日變化 (測試連續3天)
- [ ] 進度條正確反映完成百分比
- [ ] 上一步/下一步按鈕狀態正確
- [ ] 檢閱頁面顯示所有已填答內容
- [ ] 儲存功能正常運作

### 邊界測試
- [ ] 空白表單無法前進到下一步
- [ ] 快速點擊導航按鈕無異常
- [ ] 性別切換後條件問題正確更新
- [ ] 時區變化不影響輪替問題一致性

### UI/UX 測試
- [ ] 深色模式下所有顏色可辨識
- [ ] 手機橫屏模式佈局正常
- [ ] 小螢幕設備 (5吋) 可用性
- [ ] 大螢幕設備 (平板) 無異常留白

---

## 🔮 未來優化方向

### P1 後續工作
1. **動畫過渡**: 步驟切換時加入滑動動畫
2. **草稿儲存**: 自動儲存未完成的表單
3. **離開確認**: 有未儲存修改時顯示離開確認對話框

### P2 增強功能
4. **鍵盤優化**: 數字輸入欄位自動彈出數字鍵盤
5. **無障礙支援**: 增加 contentDescription 與 TalkBack 支援
6. **表單分析**: 追蹤各問題平均填寫時間

### P3 實驗性功能
7. **AI 建議**: 根據歷史記錄預填常見答案
8. **語音輸入**: 備註欄位支援語音轉文字
9. **快捷模式**: 僅顯示核心問題的快速完成模式

---

## 📚 參考資料

- [Material Design: Progressive Disclosure](https://m3.material.io/foundations/interaction/states/progressive-disclosure)
- [Jetpack Compose State Management](https://developer.android.com/jetpack/compose/state)
- [Android Stepper Pattern](https://material.io/archive/guidelines/components/steppers.html)

---

**實作者**: GitHub Copilot  
**審查狀態**: ✅ 編譯通過  
**文檔版本**: 1.0  
**最後更新**: 2024
