# 快速開始指南

## 前置需求

1. **安裝 Android Studio**
   - 下載: https://developer.android.com/studio
   - 版本: Hedgehog (2023.1.1) 或更新

2. **安裝 JDK 17**
   - Android Studio 會自動下載,或手動安裝

3. **設置 Android SDK**
   - 在 Android Studio 中安裝 SDK Platform 34
   - 安裝 Android SDK Build-Tools

## 步驟 1: 開啟專案

```bash
# 使用 Android Studio 開啟專案
File → Open → 選擇 CB-diary-A 資料夾
```

## 步驟 2: Gradle 同步

專案開啟後,Android Studio 會自動執行 Gradle Sync。

如果沒有自動執行:
- 點擊右上角的 "Sync Project with Gradle Files" 圖示
- 或執行: Tools → Android → Sync Project with Gradle Files

## 步驟 3: 設定模擬器或實機

### 使用模擬器:
```
Tools → Device Manager → Create Device
選擇: Pixel 6 或任何 API 34 的裝置
```

### 使用實機:
1. 在手機上啟用「開發者選項」與「USB 偵錯」
2. 用 USB 連接電腦
3. 允許 USB 偵錯授權

## 步驟 4: Firebase 配置 (可選)

**如果不需要雲端同步,可以跳過此步驟。**

### 選項 A: 停用 Firebase (推薦初學者)

1. 編輯 `app/build.gradle.kts`,註解掉:
```kotlin
// id("com.google.gms.google-services")

// Firebase 相關依賴
// implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
// implementation("com.google.firebase:firebase-auth-ktx")
// implementation("com.google.firebase:firebase-firestore-ktx")
```

2. 編輯根目錄 `build.gradle.kts`,註解掉:
```kotlin
// id("com.google.gms.google-services") version "4.4.0" apply false
```

3. 重新 Gradle Sync

### 選項 B: 設定真實 Firebase (進階)

1. 前往 https://console.firebase.google.com/
2. 建立新專案
3. 新增 Android 應用程式
   - Package name: `com.chastity.diary`
4. 下載 `google-services.json`
5. 替換 `app/google-services.json`
6. 在 Firebase Console 啟用:
   - Authentication → Sign-in method → Anonymous
   - Firestore Database → Create database

## 步驟 5: 執行應用程式

### 方法 1: 使用 Android Studio
- 點擊綠色的 ▶️ Run 按鈕
- 或按 Shift + F10

### 方法 2: 使用命令列
```bash
# 建置專案
./gradlew assembleDebug

# 安裝到已連接的裝置
./gradlew installDebug

# 或建置並安裝
./gradlew build && adb install app/build/outputs/apk/debug/app-debug.apk
```

## 常見問題排解

### 問題 1: Gradle Sync 失敗

**解決方法:**
```bash
# 清理專案
./gradlew clean

# 重新建置
./gradlew build --refresh-dependencies
```

### 問題 2: "SDK location not found"

**解決方法:**
建立 `local.properties` 檔案:
```
sdk.dir=/path/to/Android/Sdk
```

在 Linux/Mac 通常是:
```
sdk.dir=/Users/[你的用戶名]/Library/Android/sdk
```

在 Windows 通常是:
```
sdk.dir=C\:\\Users\\[你的用戶名]\\AppData\\Local\\Android\\Sdk
```

### 問題 3: "Unresolved reference" 錯誤

**解決方法:**
1. File → Invalidate Caches → Invalidate and Restart
2. 重新 Gradle Sync

### 問題 4: Firebase 相關錯誤

如果不需要雲端功能,按照上面「停用 Firebase」的步驟操作。

### 問題 5: Compose 預覽無法顯示

**解決方法:**
1. 確保已安裝最新的 Android Studio
2. Build → Refresh all Gradle projects
3. 重新整理預覽: Tools → Compose → Refresh Preview

## 驗證安裝

執行應用程式後,你應該看到:

1. **底部導航列** - 三個 Tab (每日記錄、儀表板、設定)
2. **每日記錄頁面** - 顯示當前日期與表單骨架
3. **儀表板頁面** - 顯示統計卡片
4. **設定頁面** - 顯示各項設定選項

## 下一步

恭喜!專案已經成功執行。

接下來可以:
1. 閱讀 [README.md](README.md) 了解專案架構
2. 閱讀 [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) 查看實作細節
3. 開始實作完整的表單 UI
4. 整合圖表視覺化
5. 實作生物辨識鎖定

## 需要協助?

- 查看專案文檔: [README.md](README.md)
- 查看 Kotlin 官方文檔: https://kotlinlang.org/docs/home.html
- 查看 Jetpack Compose 教學: https://developer.android.com/jetpack/compose/tutorial
- 查看 Material Design 3: https://m3.material.io/

---

祝開發順利! 🚀
