# Plan: 貞操日記手機應用程式開發

基於 Vue.js + Capacitor 架構開發跨平台手機App,單用戶模式,本地優先儲存搭配可選雲端同步。核心原則是最小化文字輸入,使用按鈕/開關/滑桿等互動元件來完成所有問題記錄。包含自動鎖定保護隱私、每日提醒、連續打卡計數器,以及多樣化統計圖表儀表板。

**技術棧選擇:** Vue 3 + Capacitor + Ionic Framework + TypeScript,使用 Capacitor Storage API 本地儲存,整合 Capacitor Biometric/PIN 自動鎖定,Capacitor Camera 可選圖片上傳,Capacitor Local Notifications 每日提醒,ECharts 圖表視覺化。混合儲存模式使用 Firebase/Supabase 可選同步。

## Steps

### 1. 專案初始化設置
- 使用 Vue CLI / Vite 建立 Vue 3 + TypeScript 專案
- 整合 Ionic Framework Vue 版本 (`@ionic/vue`)
- 安裝 Capacitor 並初始化 iOS/Android 平台
- 配置 TypeScript、ESLint、Prettier
- 建立專案資料夾結構:
  - `src/components/` (可重用 UI 元件)
  - `src/views/` (頁面: 每日記錄、儀表板)
  - `src/stores/` (Pinia 狀態管理)
  - `src/services/` (儲存、同步、通知服務)
  - `src/types/` (TypeScript 型別定義)
  - `src/utils/` (工具函式)

### 2. 資料模型與型別定義
- 根據研究報告建立 `src/types/entry.ts` 定義 `DailyEntry`、`UserProfile` 介面
- 建立 `src/types/ui-options.ts` 定義所有預設選項 (情緒標籤、運動類型、地點等)
- 設計資料結構以支援男性專屬問題的條件顯示

### 3. 本地儲存服務實作
- 建立 `src/services/storage.service.ts`
- 使用 Capacitor Storage API (`@capacitor/storage`) 實作 CRUD 操作
- 實作資料持久化: 每日記錄、用戶設定、連續天數計數
- 設計高效的查詢介面 (依日期範圍、統計匯總)

### 4. 每日記錄表單介面 (核心功能)
- 建立 `src/views/DailyEntryView.vue`
- 實作問題1 (心情): Ionic `ion-segment` 或自訂情緒圖示按鈕組
- 實作問題2 (色情內容): `ion-toggle` + 條件顯示時間選擇器 (`ion-select` 快速選項)
- 實作問題3 (勃起): 依據用戶性別條件顯示,使用 `ion-radio-group`
- 實作問題4 (運動): `ion-toggle` + `ion-chip` 多選標籤 + 時間選擇器
- 實作問題5 (解鎖/自慰): 兩階段 `ion-toggle` + 時間選擇器
- 實作問題6 (露出鎖): `ion-toggle` + `ion-chip` 地點多選
- 實作問題7 (打卡照片): 使用 Capacitor Camera API 可選上傳,圖片壓縮後存 base64 或本地路徑
- 表單底部固定儲存按鈕,支援草稿自動儲存
- 日期選擇器允許補填歷史記錄

### 5. 儀表板與統計圖表
- 建立 `src/views/DashboardView.vue`
- **頂部卡片區**:
  - 總配戴天數、本週記錄完成率、連續打卡天數 (streak counter)
- **行事曆熱力圖**: 使用 ECharts calendar heatmap 顯示記錄完成度
- **情緒趨勢圖**: ECharts 折線圖顯示近30天心情變化
- **運動統計**: 圓餅圖 (運動類型分佈) + 長條圖 (每週運動時長)
- **解鎖/自慰頻率**: 統計卡片 + 趨勢圖
- **其他統計**: 色情內容接觸頻率、勃起頻率 (男性)、露出記錄等
- 時間範圍選擇器 (本週/本月/近3個月/全部)
- 安裝 ECharts (`npm install echarts`) 並建立 `src/components/charts/` 可重用圖表元件

### 6. 隱私保護 - 自動鎖定功能
- 建立 `src/services/security.service.ts`
- 整合 `@capacitor/app` 監聽 app 狀態變化 (`appStateChange`)
- 當 app 進入背景時設置鎖定標記、模糊畫面 (CSS blur filter)
- 返回前景時顯示解鎖畫面 `src/components/UnlockScreen.vue`
- 使用 `@capacitor/biometric` 支援生物辨識 (指紋/Face ID)
- 提供 PIN 碼備用方案,PIN 儲存使用加密 (crypto-js)
- 初次啟動時引導設定 PIN/生物辨識

### 7. 每日通知提醒
- 建立 `src/services/notification.service.ts`
- 使用 `@capacitor/local-notifications` 實作每日定時提醒
- 用戶設定頁面允許設定提醒時間 (預設晚上21:00)
- 通知內容: "今天還沒記錄喔!別忘了打卡 📝"
- 檢查權限並請求通知授權

### 8. 連續打卡天數追蹤
- 在 `src/services/streak.service.ts` 實作邏輯
- 每次儲存記錄時更新連續天數計算
- 儀表板頂部顯著展示 streak counter (卡片或徽章動畫)
- 達成里程碑時顯示鼓勵訊息 (7天、30天、100天等)

### 9. 混合儲存 - 雲端同步 (可選)
- 建立 `src/services/sync.service.ts`
- 選用 Firebase Firestore 或 Supabase 作為後端
- 實作選擇性手動同步 (設定頁面提供「同步到雲端」按鈕)
- 使用匿名登入或裝置ID作為識別 (無需帳號註冊)
- 衝突處理策略: 本地優先,時間戳較新者勝出
- 同步狀態指示器 (上次同步時間)

### 10. 設定頁面與用戶偏好
- 建立 `src/views/SettingsView.vue`
- 性別設定 (決定是否顯示勃起問題)
- 開始配戴日期設定
- 通知提醒時間設定
- PIN/生物辨識開關
- 暗色/亮色主題切換
- 雲端同步開關與手動同步按鈕
- 資料匯出功能 (CSV 格式,使用 papaparse)
- 關於頁面

### 11. 導航與路由設置
- 配置 Vue Router 與 Ionic 頁面轉場動畫
- 底部 Tab Bar 導航:
  - Tab 1: 每日記錄 (`DailyEntryView`)
  - Tab 2: 儀表板 (`DashboardView`)
  - Tab 3: 設定 (`SettingsView`)
- 圖示使用 Ionicons

### 12. UI/UX 細節優化
- 使用 Ionic 的深色模式支援
- 確保所有互動元件符合「無需鍵盤輸入」原則 (除可選備註欄位外)
- 加入 haptic feedback (使用 `@capacitor/haptics`)
- Loading 狀態、錯誤處理 Toast 提示
- 空狀態提示 (首次使用、無資料時)
- 響應式設計確保不同螢幕尺寸適配

### 13. 圖片處理優化
- 整合 browser-image-compression 壓縮圖片至合理大小 (< 500KB)
- 圖片存儲策略: base64 存入 Capacitor Storage 或使用 Filesystem API
- 縮圖顯示於儀表板,點擊放大查看
- 提供刪除圖片功能

### 14. 狀態管理與資料流
- 使用 Pinia 建立全域狀態管理
- Store 模組:
  - `useEntriesStore` (所有日記記錄)
  - `useUserStore` (用戶設定與偏好)
  - `useStatsStore` (統計資料計算快取)
- Vue 3 Composition API 編寫元件邏輯

### 15. Capacitor 原生打包配置
- 配置 `capacitor.config.ts`
- 設定 App 名稱、Bundle ID、版本號
- iOS: 配置 Info.plist 權限 (相機、通知、Face ID)
- Android: 配置 AndroidManifest.xml 權限
- App 圖示與啟動畫面設計
- 建構指令: `npm run build && npx cap sync`

### 16. 測試與驗證
- 使用 Capacitor Live Reload 在實機測試
- iOS 模擬器與 Android 模擬器測試
- 驗證本地儲存持久化
- 驗證通知功能 (需實機)
- 驗證生物辨識 (需實機)
- 測試日期邊界情況 (跨日、時區)
- 測試資料匯出正確性

## Verification

- 安裝到 iOS/Android 實機,完整填寫一週日記,確認所有問題都能無需打字完成 (除可選備註)
- 驗證 app 切換到背景再返回時自動鎖定並要求解鎖
- 確認每日通知在設定時間準時觸發
- 檢查儀表板各圖表正確顯示統計數據
- 測試連續打卡計數器正確累計
- 驗證資料匯出 CSV 可用 Excel 開啟
- 測試雲端同步功能 (若啟用) 可在清除本地資料後恢復

## Decisions

- **選擇 Capacitor 而非 React Native/Flutter**: 用戶選擇 Vue.js,Capacitor 允許使用標準 Vue web 技術打包成原生 app,學習曲線低、開發效率高
- **選擇 Ionic Framework**: 提供成熟的移動端 UI 元件庫,與 Capacitor 深度整合,省去大量 UI 開發時間
- **選擇 ECharts**: 功能強大、圖表類型豐富,支援行事曆熱力圖等複雜視覺化,滿足「統計圖表多樣化」需求
- **混合儲存採用手動同步**: 避免自動同步消耗流量與電量,給用戶完全控制權,符合隱私優先原則
- **PIN + 生物辨識雙重方案**: 確保所有裝置都能使用鎖定功能,生物辨識提供更好體驗,PIN 作為備用

## 每日記錄問題清單參考

根據 [possible item.md](possible%20item.md) 的問題:

1. **今天的心情如何？**
2. **今天是否主動接觸色情內容？看了多久？**
3. **今天是否勃起？**（男性限定）
4. **是否運動了？做了什麼運動？持續多久？**
5. **是否解鎖？是否自慰了？持續多久？**
6. **是否露出鎖？在哪裡？**
7. **打卡（上傳圖片）**
