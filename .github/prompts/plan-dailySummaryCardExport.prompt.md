# Plan: Daily Summary Card Export 功能

這個功能讓使用者在完成每日記錄後，生成一張固定版面的統計卡片圖片並分享。卡片的視覺主題（背景漸層、強調色、幾何紋路）可由 App 提供的預設組合中選擇，確保文字永遠清晰可讀。進階主題透過輸入贊助碼（本地 SHA-256 驗證）解鎖，所有程式碼開源，F-Droid 標記為 `DisabledFreeFeatures`。

---

## 步驟

### 1. 資料模型

- 新增 `domain/model/CardTheme.kt`：使用密封類 `BackgroundSource` 抽象背景來源，取代原本的 `backgroundType`/`accentColor`/`patternType` 三個欄位：

  ```kotlin
  sealed class BackgroundSource {
      data class Gradient(val colors: List<Color>, val angle: Float) : BackgroundSource()
      data class CanvasPattern(val type: PatternType, val accent: Color) : BackgroundSource()
      data class ExternalAsset(val pngUri: Uri, val specUri: Uri) : BackgroundSource()  // 外部模版
  }

  data class CardTheme(
      val id: String,
      val nameResId: Int,
      val isPremium: Boolean,
      val backgroundSource: BackgroundSource,
      val textColorScheme: TextColorScheme,  // DARK / LIGHT，決定文字用深色或淺色
  )
  ```

- 新增 `domain/model/CardTemplateSpec.kt`：對應 `card_template_spec.json` 的資料結構，描述 safe zones 與文字色系
- 新增 `domain/model/CardData.kt`：彙整卡片所需的所有資料（streak、7日平均、今日欄位、輪換題答案）
- 修改 `domain/model/UserSettings.kt`：加入 `selectedCardThemeId: String = "midnight"` 與 `sponsorUnlocked: Boolean = false`

### 2. 主題定義清單

新增 `ui/theme/CardThemes.kt`，在 Kotlin 中以純色/漸層/Canvas 幾何紋路定義所有主題（無外部素材）：

| ID | 名稱 | 類型 | 免費？ |
|---|---|---|---|
| `midnight` | 星夜 | 深藍漸層 | ✓ |
| `dawn` | 晨曦 | 暖橘漸層 | ✓ |
| `forest` | 森林 | 深綠漸層 | ✓ |
| `crimson` | 深紅 | 紅黑漸層＋幾何紋 | 贊助 |
| `ocean` | 深海 | 藍綠漸層＋波浪紋 | 贊助 |
| `minimal` | 極簡白 | 淺色＋線條幾何紋 | 贊助 |

### 3. DataStore & Repository

- 修改 `data/datastore/PreferencesManager.kt`：加入 `CARD_THEME_ID`、`SPONSOR_UNLOCKED` 兩個 DataStore key
- 修改 `data/repository/SettingsRepository.kt`：加入 `updateCardTheme(id)`、`setSponsorUnlocked(true)` 方法

### 4. 贊助碼驗證

新增 `util/SponsorManager.kt`：`object SponsorManager`，內含 SHA-256 hash 對照表，`isValidCode(code: String): Boolean`。無需網路，完全本地驗證。贊助者透過 GitHub Sponsors 或 Liberapay 贊助後，手動獲取兌換碼。

### 5. 卡片 Composable（固定版面）

新增 `ui/screens/SummaryCardContent.kt`，`@Composable fun SummaryCardContent(data: CardData, theme: CardTheme)`，固定 1080×1350px 版面。

**強制三層結構**（確保外部模版上的文字永遠可讀）：

```
Box {
    // Layer 0：背景（Gradient / CanvasPattern / ExternalAsset bitmap）
    // Layer 1：半透明黑/白遮罩（overlayOpacity 由 CardTemplateSpec 決定，預設 0.0）
    // Layer 2：文字與資料 Composable（永遠在最上層）
}
```

版面內容：

```
[背景漸層 + 幾何紋路]
┌──────────────────────────────┐
│  App icon    •    日期字串   │  ← 頂列
├──────────────────────────────┤
│     🔥  42 天連續紀錄        │  ← 主視覺
│     歷史最長：87 天          │
├──────────────────────────────┤
│  今日心情 chip  •  晨間能量  │  ← 今日數據
│  自我評分 ★★★★☆            │
├──────────────────────────────┤
│  7日平均  慾望  舒適  專注 睡眠│  ← 4格平均列
│          6.2   7.1   8.0  7.5│
├──────────────────────────────┤
│  [✓ 運動]  [○ 暴露裝置]     │  ← 打卡圖示
│  今日輪換題：「…」           │
├──────────────────────────────┤
│  CB Diary  •  chastity.diary │  ← 頁腳
└──────────────────────────────┘
```

暴露裝置 / 自慰資訊預設不顯示在卡片上（隱私保護），使用者可在生成前的設定頁開啟。

### 6. Bitmap 渲染與匯出

新增 `util/CardRenderer.kt`：

- 使用 `ComposeView` + `Activity.window.decorView.addView()` → `drawToBitmap()` → `removeView()` 流程，相容 Compose BOM 2023.10.01
- 儲存至 `context.cacheDir/cards/summary_YYYYMMDD.png`（使用 `Bitmap.compress(PNG, 95)`）
- 提供兩個動作：
  - **分享**：透過 `FileProvider` + `Intent.ACTION_SEND`（mime: `image/png`）
  - **儲存至相簿**：透過 `MediaStore.Images.Media.insertImage()`（需 `WRITE_EXTERNAL_STORAGE` 或僅 API 29+ 的 `MediaStore`）

### 7. AndroidManifest 與 FileProvider

- 修改 `app/src/main/AndroidManifest.xml`：加入 `<provider android:name="androidx.core.content.FileProvider" ...>` 與 `android:exported="false"`
- 新增 `app/src/main/res/xml/file_provider_paths.xml`：`<cache-path name="cards" path="cards/" />`

### 8. CardViewModel

新增 `viewmodel/CardViewModel.kt`：

- `cardData: StateFlow<CardData?>` — 從 Entry + DashboardStats 彙整
- `selectedTheme: StateFlow<CardTheme>`
- `availableThemes: List<CardTheme>` — 根據 `sponsorUnlocked` 過濾（包含使用者匯入的外部模版）
- `fun generateAndShare(activity)` / `fun generateAndSave(activity)`
- `fun selectTheme(themeId)` / `fun submitSponsorCode(code): Boolean`
- `fun importTemplate(zipUri: Uri): Result<CardTheme>` — 呼叫 `TemplateImporter`，成功後加入 `availableThemes`
- `fun deleteUserTemplate(themeId: String)` — 刪除使用者匯入的自定模版

### 9. UI — DailyEntry 底部按鈕

修改 `ui/screens/DailyEntryScreen.kt`：

- 儲存成功後，在頁面最底部顯示 `OutlinedButton(icon=Share){ "生成今日卡片" }` 按鈕
- 點擊後開啟 **卡片 BottomSheet**

### 10. UI — 卡片生成 BottomSheet

在 `ui/screens/SummaryCardContent.kt` 中一併實作 `CardBottomSheet`：

- 上方：1:1 可捲動卡片預覽（縮小版）
- 中段：橫向捲動的主題選擇列；鎖定的主題顯示 🔒 徽章，點擊則彈出「輸入贊助碼」Dialog；最末尾加入「**＋ 匯入模版**」格子，點擊呼叫系統文件選擇器（`application/zip`）
- 使用者匯入的模版顯示刪除鈕（長按或滑動顯示）
- 下方：`分享` 按鈕 + `儲存至相簿` 按鈕

### 11. Settings — 贊助碼入口

修改 `ui/screens/SettingsScreen.kt`：在現有設定區塊底部加入「支持開發者」區段，包含「輸入贊助碼」按鈕與 Liberapay 連結，狀態顯示「已解鎖進階主題 ✓」或「尚未解鎖」。

### 12. String Resources

在三個 locale 文件（`values/`、`values-b+zh+TW/`、`values-zh/`）補充所有新增 UI 字串（卡片標題、主題名稱、按鈕文字、贊助相關提示、模版匯入相關提示）。

### 13. BackgroundSource 抽象化 — `SummaryCardContent` 渲染邏輯

在 `SummaryCardContent.kt` 中，以 `when (theme.backgroundSource)` 分派三種背景渲染方式：

- `Gradient` → `Canvas.drawRect` + `Brush.linearGradient`
- `CanvasPattern` → 原有幾何紋路邏輯
- `ExternalAsset` → `BitmapFactory.decodeStream(pngUri)` 後以 `Image(bitmap, contentScale = ContentScale.Crop)` 填滿，再疊 `overlayOpacity` 半透明層

文字節點的 safe zones 偏移由 `CardTemplateSpec.safeZones` 提供；內建主題使用預設值（邊距 60px）。

### 14. 外部模版匯入工具 — `TemplateImporter.kt`

新增 `util/TemplateImporter.kt`：

- 接受 `.zip` 的 `Uri`，解壓後期望結構：
  ```
  template.png              # 1080×1350 背景圖
  card_template_spec.json   # 規格檔
  ```
- 驗證：圖片尺寸必須為 1080×1350；JSON 需含 `format_version`、`canvas`、`safe_zones` 欄位
- 驗證通過後複製至 `context.filesDir/templates/<uuid>/`，回傳 `CardTheme(backgroundSource = ExternalAsset(...))`
- 失敗回傳含錯誤原因的 `Result.failure`，UI 顯示 Snackbar
- 新增 `app/src/main/res/xml/file_provider_paths.xml` 的 `<files-path name="templates" path="templates/" />` 條目（與 cards 快取共用同一份 paths 檔）

### 15. 空白模版規格檔（供設計師參考）

新增 `assets/blank_card_template_spec.json`，隨 App 發佈，使用者可從 BottomSheet「下載設計參考」匯出至 Downloads，設計師在 Figma / Canva 中依此規格製作：

```json
{
  "format_version": 1,
  "canvas": { "width": 1080, "height": 1350 },
  "text_color_scheme": "light",
  "overlay_opacity": 0.0,
  "safe_zones": {
    "header": { "top": 60,  "left": 60, "right": 60, "height": 100 },
    "streak":  { "top": 220, "left": 60, "right": 60, "height": 200 },
    "stats":   { "top": 460, "left": 60, "right": 60, "height": 600 },
    "footer":  { "bottom": 60, "left": 60, "right": 60, "height": 80 }
  }
}
```

文字會自動排在 safe zones 內；設計師只需確保這些矩形區域在自己的設計中留白或半透明。

---

## Verification

```bash
./gradlew :app:assembleDebug   # 無編譯錯誤
```

手動測試流程：
1. 完成今日記錄 → 儲存 → 確認底部出現「生成今日卡片」按鈕
2. 點擊 → BottomSheet 開啟 → 預覽正確顯示所有資料
3. 切換免費主題 → 預覽即時更新背景/強調色
4. 點擊鎖定主題 → 彈出輸入碼 Dialog → 輸入正確碼 → 解鎖
5. 點擊「分享」→ 系統分享表單出現 PNG 圖片
6. 點擊「儲存至相簿」→ 圖片出現於手機相簿
7. 點擊「＋ 匯入模版」→ 選擇符合規格的 `.zip` → 新主題格子出現在選擇列 → 預覽套用正確背景圖
8. 匯入不符規格的 zip（錯誤尺寸 / 缺少 JSON）→ Snackbar 顯示錯誤原因，不崩潰
9. 長按使用者匯入的模版 → 刪除 → 格子消失，切回預設主題

---

## 決策記錄

- **Bitmap 渲染**：選 `ComposeView.drawToBitmap()` 而非純 Canvas 繪製，沿用 Compose 設計能力，維護成本低
- **主題定義**：全部以 Kotlin 程式碼（漸層色值 + Canvas 幾何紋路）定義，無外部素材，符合 F-Droid 開源要求
- **贊助驗證**：本地 SHA-256 hash 比對，無伺服器依賴，避免 `NonFreeNet` 標記；接受「技術用戶可自行編譯解鎖版」的風險
- **隱私設計**：暴露裝置 / 自慰相關欄位預設不出現在卡片上，需手動開啟
- **BackgroundSource 密封類**：將背景來源抽象化為 `Gradient` / `CanvasPattern` / `ExternalAsset` 三種，文字渲染層永遠疊在最上方，確保外部模版不會蓋住文字，且未來新增背景類型不需改動文字佈局邏輯
- **外部模版格式**：PNG（1080×1350）+ `card_template_spec.json` 打包為 `.zip`，App 驗證後存至 `filesDir/templates/`；設計師可在任意工具（Figma / Canva 等）製作，無需接觸 Kotlin 程式碼
- **Safe zones 規格**：以 JSON 明確定義 header / streak / stats / footer 四個文字安全區的邊界與高度，讓設計師有明確留白依據；內建主題使用預設值（四邊各 60px）
- **無新依賴**：不需加入任何新第三方函式庫（全用 AndroidX + 系統 API）
