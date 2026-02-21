package com.chastity.diary.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.ui.components.*
import com.chastity.diary.util.Constants
import com.chastity.diary.viewmodel.DailyEntryViewModel
import com.chastity.diary.viewmodel.EntryFormState
import com.chastity.diary.viewmodel.SettingsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// ─── Rotating question pool (R1–R32, 31 total; EAV-backed) ──────────────────
private enum class RotatingQuestion(
    val key: String,
    val title: String,
    val feedback: String,
    val isMaleOnly: Boolean = false
) {
    R1 ("R1",  "今日是否有分泌物洩漏？",                           "看來身體已經開始用最誠實的方式抗議了……清潔工作可別偷懶哦。"),
    R2 ("R2",  "今日是否有主動頂鎖/摩擦，尋求快感？",                        "嗯？今天好像特別不甘心被關著呢……數一數有幾次試圖爭取自由吧。", isMaleOnly = true),
    R3 ("R3",  "今日是否進行邊緣訓練？",                           "走到懸崖邊又縮回來……這種欲拒還迎的把戲，到底誰在折磨誰？"),
    R4 ("R4",  "今日是否與Keyholder互動？",                        "今天有沒有乖乖報告？還是偷偷想留一點小秘密？溝通可是契約的氧氣。"),
    R6 ("R6",  "今日是否帶著鎖進入公眾場合？",                   "在人群中帶著這個小秘密走動，緊張感是不是特別清晰？下次還敢嗎？"),
    R7 ("R7",  "今日是否曾短暫解除鎖？",                         "手是不是有點癢？誠實交代解開的理由，否則下次可能更難熬哦。"),
    R8 ("R8",  "今日是否有意展示或洩露鎖蹤跡？",                 "故意讓邊緣露出一點點？這種小壞壞的試探……真的只是不小心嗎？"),
    R9 ("R9",  "今日是否接觸成人內容？",                           "看了那些東西，卻只能乾瞪眼……意志力今天考了幾分？"),
    R10("R10", "今日是否解鎖或進行自慰？",                         "破戒的瞬間一定很爽……但現在後悔的感覺是不是更強烈？記下來，好好反省。"),
    R11("R11", "今日是否進行乳頭開發/玩弄？",                      "開始把快感往上轉移了？看來下半身已經學會求饒，胸口卻越來越誠實。"),
    R12("R12", "今日是否進行後庭開發/探索？",                      "後面也開始主動爭寵了？身體地圖正在被重新繪製……感覺如何？"),
    R13("R13", "今天你有沒有感受到鎖帶來的不適或調整需求？",     "哪裡卡卡的？哪裡磨紅了？身體的小抱怨可不能忽視。"),
    R14("R14", "今天佩戴鎖是否讓你感覺到內心的平靜或成就？",    "居然真的覺得安心……這算不算已經有點上癮的跡象了？"),
    R15("R15", "今天有沒有想起Keyholder，並感受到連結的溫暖？",   "腦袋裡閃過那個人的臉時，心跳有沒有加速？這種思念也算是甜蜜的折磨。"),
    R16("R16", "今天鎖是否已融入你的日常routine中，感覺自然？", "已經開始像內褲一樣理所當然了？恭喜，墮落進度又前進了一步。"),
    R17("R17", "今天有沒有將慾望轉向其他活動，如運動或創作？",    "把精力丟到別的地方……聰明的轉移戰術，但下半身真的被騙到了嗎？"),
    R18("R18", "今天在人群中，你有沒有特別注意到自己的隱密狀態？","每走一步都在提醒自己「裡面有東西」……這種隱秘的刺激，有沒有讓你偷偷嘴角上揚？"),
    R19("R19", "今天有沒有進行放鬆活動來緩解可能的壓力？",        "學會哄自己了？不過再怎麼放鬆，鎖還是鎖著，逃不掉的哦。"),
    R20("R20", "今天醒來後，有沒有回想起與鎖相關的夢境？",      "連睡覺都在被管教……你的潛意識看來已經徹底投降了。"),
    R21("R21", "今天其他感官（如觸覺或聽覺）是否變得更敏銳？",    "碰一下衣服都像被撩撥……下半身被封印後，其他地方好像變得特別饑渴呢。"),
    R22("R22", "今天有沒有與Keyholder分享你的感受或想法？",       "今天敢不敢把心裡那些念頭說出來？還是只敢在腦袋裡演戲？"),
    R23("R23", "今天在不同環境中，鎖帶來的感受如何？",          "坐著的時候、走路的時候、蹲下的時候……它無時無刻不在提醒你誰才是主人。"),
    R24("R24", "今天有沒有遇到讓你猶豫或掙扎的時刻？",           "差點就伸手了對吧？最後還是忍住了……這次算你贏，但下次呢？"),
    R25("R25", "今天佩戴是否帶來任何意外的正面體驗？",           "居然還能挖到一點甜頭？看來被關著也能找到快樂……真是個奇怪的小傢伙。"),
    R26("R26", "今天有沒有特別注意清潔或保濕等保養？",           "認真擦拭、抹乳液……對待牢籠比對待自己還細心，這算不算斯德哥爾摩？"),
    R27("R27", "今天有沒有透過寫作或藝術表達你的體驗？",         "把被鎖的感覺寫成詩、畫成圖……這種昇華的方式還挺優雅的病態。"),
    R28("R28", "今天時間感覺過得快還是慢，受鎖影響？",         "時間明明過得慢，卻又忍不住想再熬久一點……這矛盾的癮頭還真有趣。"),
    R29("R29", "今天有沒有在匿名社群分享或閱讀相關經驗？",       "偷偷看別人被鎖的慘況，是不是有一種「同是天涯淪落人」的暗爽？"),
    R30("R30", "今天有沒有在想萬一鎖取不下來該怎麼辦？",         "緊急預案想了幾套？安全是第一位的，恐慌可是最難看的樣子。"),
    R31("R31", "今天情緒是否有起伏，與鎖相關？",               "一會兒覺得好色，一會兒又覺得好乖……這種心情過山車，玩得還開心嗎？"),
    R32("R32", "今天有沒有想像未來繼續佩戴的畫面？",             "腦中已經出現一年後的自己……看來你不只接受了，還開始期待了呢。"),
}

private fun getRotatingQuestionsForDate(date: LocalDate, isMale: Boolean): List<RotatingQuestion> {
    val pool = RotatingQuestion.entries.filter { !it.isMaleOnly || isMale }
    // Use a simple deterministic shuffle based on the date
    val seed = date.toEpochDay()
    val shuffled = pool.sortedBy { (seed * 2654435761L + it.key.hashCode()) and Long.MAX_VALUE }
    return shuffled.take(2)
}

private fun coreCompletionScore(entry: DailyEntry): Int {
    var s = 0
    if (entry.mood != null) s++
    if (entry.desireLevel != null) s++
    if (entry.comfortRating != null) s++
    if (entry.focusLevel != null) s++
    if (entry.selfRating != null) s++
    if (entry.emotions.isNotEmpty()) s++
    return s
}

// ─── Main Screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryScreen(
    viewModel: DailyEntryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    outerPadding: PaddingValues = PaddingValues()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val entryState by viewModel.entryState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val morningSaveSuccess by viewModel.morningSaveSuccess.collectAsState()
    val userSettings by settingsViewModel.userSettings.collectAsState()
    val isMale = userSettings.gender == Gender.MALE

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Camera – store the actual File so we can save its absolutePath (content:// URI path is not readable)
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraImageFile?.let { file ->
            if (file.exists()) viewModel.updateEntry { e -> e.copy(photoPath = file.absolutePath) }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dir = File(context.getExternalFilesDir("Pictures"), "").also { it.mkdirs() }
            val file = File(dir, "PHOTO_$ts.jpg")
            cameraImageFile = file
            val u = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraImageUri = u
            cameraLauncher.launch(u)
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) { snackbarHostState.showSnackbar("儲存成功！"); viewModel.clearSaveSuccess() }
    }
    LaunchedEffect(morningSaveSuccess) {
        if (morningSaveSuccess) { snackbarHostState.showSnackbar("☀️ 早晨記錄已儲存！"); viewModel.clearMorningSaveSuccess() }
    }
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) { snackbarHostState.showSnackbar("刪除成功！"); viewModel.clearDeleteSuccess() }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
                        if (entryState is EntryFormState.Loaded) {
                            Text("編輯模式", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    if (entryState is EntryFormState.Loaded) {
                        val loaded = (entryState as EntryFormState.Loaded).entry
                        if (loaded.id != 0L) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, "刪除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, "選擇日期")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val entry = when (val s = entryState) {
            is EntryFormState.Loaded -> s.entry
            is EntryFormState.Empty -> DailyEntry(date = selectedDate)
        }
        val isExisting = entryState is EntryFormState.Loaded &&
                (entryState as EntryFormState.Loaded).entry.id != 0L

        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Tab Row ────────────────────────────────────────────────────────
            TabRow(selectedTabIndex = currentTab) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("☀️ 早晨")
                            if (!entry.morningCheckDone) {
                                Badge()
                            }
                        }
                    }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("🌙 晚間") }
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (currentTab == 0) {
                // ── Morning Tab ────────────────────────────────────────────────
                DailyEntryTabContent(
                    entry = entry,
                    onUpdate = { viewModel.updateEntry { _ -> it } },
                    onSave = { viewModel.saveMorningCheck() },
                    outerPadding = outerPadding,
                    isMorning = true,
                    isMale = isMale,
                    photoBlurEnabled = userSettings.photoBlurEnabled
                )
            } else {
                // ── Evening Tab ────────────────────────────────────────────────
                DailyEntryTabContent(
                    entry = entry,
                    onUpdate = { viewModel.updateEntry { _ -> it } },
                    onSave = { viewModel.saveEntry() },
                    outerPadding = outerPadding,
                    isMorning = false,
                    isMale = isMale,
                    selectedDate = selectedDate,
                    isExisting = isExisting,
                    onTakePhoto = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    photoBlurEnabled = userSettings.photoBlurEnabled
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedDate,
            onConfirm = { viewModel.selectDate(it) },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            title = "確認刪除",
            message = "確定要刪除 ${selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))} 的記錄嗎？\n\n此操作無法復原。",
            onConfirm = { viewModel.deleteEntry() },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ─── ① Day Status Card ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayStatusCard(entry: DailyEntry, selectedDate: LocalDate) {
    val score = coreCompletionScore(entry)
    val total = 6
    val isToday = selectedDate == LocalDate.now()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    if (isToday) "今日記錄" else selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (score == 0) "尚未開始記錄" else "核心題目完成 $score / $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = score.toFloat() / total,
                    modifier = Modifier.size(52.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Text(
                    "${(score.toFloat() / total * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── ② Core Questions Card ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoreQuestionsCard(
    entry: DailyEntry,
    onUpdate: (DailyEntry) -> Unit,
    onTakePhoto: () -> Unit,
    photoBlurEnabled: Boolean = true
) {
    // rememberSaveable survives recomposition; LaunchedEffect resets only when photo actually changes
    var photoRevealed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(entry.photoPath) { photoRevealed = false }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("核心問題", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text("每日必填", style = MaterialTheme.typography.labelSmall) })
            }

            Divider()

            // C2: Device worn (BRANCHING ROOT) — now first
            QuestionSection(title = "今天有佩戴鎖嗎？") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val wearing = entry.deviceCheckPassed
                    if (wearing) {
                        Button(onClick = { onUpdate(entry.copy(deviceCheckPassed = true)) },
                            modifier = Modifier.weight(1f)) { Text("✓ 有佩戴") }
                        OutlinedButton(onClick = { onUpdate(entry.copy(deviceCheckPassed = false)) },
                            modifier = Modifier.weight(1f)) { Text("✗ 沒有") }
                    } else {
                        OutlinedButton(onClick = { onUpdate(entry.copy(deviceCheckPassed = true)) },
                            modifier = Modifier.weight(1f)) { Text("✓ 有佩戴") }
                        Button(onClick = { onUpdate(entry.copy(deviceCheckPassed = false)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("✗ 沒有")
                        }
                    }
                }
            }

            // C1: Mood (merged, emoji+text, single-select)
            QuestionSection(title = "今天的心情狀態", subtitle = "選一個最接近的情緒") {
                MoodSelector(
                    selectedMood = entry.mood,
                    moods = Constants.MOODS,
                    onMoodSelected = { onUpdate(entry.copy(mood = it)) }
                )
            }

            // C3: Desire level
            QuestionSection(title = "今日性慾強度", subtitle = "1 = 很低   10 = 很強烈") {
                SliderWithLabel(entry.desireLevel?.toFloat() ?: 5f,
                    { onUpdate(entry.copy(desireLevel = it.toInt())) },
                    valueRange = 1f..10f, steps = 8, label = "性慾指數")
            }

            // C4: Comfort (只在佩戴時)
            AnimatedVisibility(visible = entry.deviceCheckPassed) {
                QuestionSection(title = "佩戴舒適度", subtitle = "整天佩戴鎖的感受") {
                    StarRating(entry.comfortRating ?: 3,
                        { onUpdate(entry.copy(comfortRating = it)) }, label = "舒適度")
                }
            }

            // C5: Focus
            QuestionSection(title = "今日專注度", subtitle = "1 = 完全分心   10 = 高度專注") {
                SliderWithLabel(entry.focusLevel?.toFloat() ?: 5f,
                    { onUpdate(entry.copy(focusLevel = it.toInt())) },
                    valueRange = 1f..10f, steps = 8, label = "專注指數")
            }

            // (C6 merged into C1 above)

            // Photo check-in
            Divider()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("打卡照片", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            if (entry.photoPath.isNullOrBlank()) "可選 · 視覺紀念" else "✓ 已拍攝",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entry.photoPath.isNullOrBlank())
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (entry.photoPath.isNullOrBlank()) {
                        OutlinedButton(onClick = onTakePhoto) {
                            Icon(Icons.Default.Camera, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("拍照")
                        }
                    }
                }

                if (!entry.photoPath.isNullOrBlank()) {
                    val bitmap = remember(entry.photoPath) {
                        runCatching {
                            val f = File(entry.photoPath!!)
                            if (!f.exists()) return@runCatching null
                            val raw = BitmapFactory.decodeFile(f.absolutePath) ?: return@runCatching null
                            // Correct orientation using EXIF data (Android camera often saves rotated)
                            val exif = ExifInterface(f.absolutePath)
                            val degrees = when (exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                            )) {
                                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                                else -> 0f
                            }
                            if (degrees == 0f) raw
                            else Bitmap.createBitmap(
                                raw, 0, 0, raw.width, raw.height,
                                Matrix().apply { postRotate(degrees) }, true
                            )
                        }.getOrNull()
                    }
                    // Aspect ratio from bitmap; 默認 4:3 (portrait = < 1, landscape = > 1)
                    val photoAspectRatio = bitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: (4f / 3f)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Photo preview with blur overlay — respects portrait / landscape
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(photoAspectRatio)
                                .clickable { photoRevealed = !photoRevealed }
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap.asImageBitmap(), "打卡照片",
                                    Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (photoBlurEnabled && !photoRevealed) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Lock, null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "點擊查看照片",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // Action buttons below photo
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onTakePhoto,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Camera, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("重新拍攝")
                            }
                            OutlinedButton(
                                onClick = { onUpdate(entry.copy(photoPath = null)) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    )
                                )
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("刪除照片")
                            }
                        }
                    }
                }
            }

            // E7: Exercise (moved from extended to core)
            Divider()
            QuestionSection(title = "是否運動？") {
                YesNoToggle(entry.exercised, { onUpdate(entry.copy(exercised = it)) }, "有運動")
                AnimatedVisibility(entry.exercised) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MultiSelectChipGroup(
                            options = Constants.EXERCISE_TYPES,
                            selectedOptions = entry.exerciseTypes,
                            onSelectionChange = { onUpdate(entry.copy(exerciseTypes = it)) }
                        )
                        DurationPicker(entry.exerciseDuration,
                            { onUpdate(entry.copy(exerciseDuration = it)) }, label = "運動時長")
                    }
                }
            }

            // E8: Cleaning (moved from extended to core)
            QuestionSection(title = "今天是否清潔了貞操鎖？") {
                MultiSelectChipGroup(
                    options = Constants.CLEANING_TYPES,
                    selectedOptions = entry.cleaningType?.let { listOf(it) } ?: emptyList(),
                    onSelectionChange = { onUpdate(entry.copy(cleaningType = it.firstOrNull())) }
                )
            }
        }
    }
}

// ─── ③ Realtime Feedback Card ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RealtimeFeedbackCard(entry: DailyEntry, score: Int) {
    val msgs = buildList {
        entry.desireLevel?.let {
            when {
                it >= 8 -> add("🔥 性慾強度 $it/10，今天可能是高峰期，自律加油！")
                it <= 3 -> add("😌 性慾強度 $it/10，今天狀態非常平靜。")
                else -> add("⚖️ 性慾強度 $it/10，處於正常範圍。")
            }
        }
        if (entry.deviceCheckPassed) {
            entry.comfortRating?.let {
                when {
                    it <= 2 -> add("⚠️ 舒適度偏低（$it/5），請檢查佩戴狀態。")
                    it >= 4 -> add("✅ 舒適度良好（$it/5），繼續保持！")
                    else -> {}
                }
            }
        }
        entry.focusLevel?.let {
            when {
                it <= 3 -> add("🧠 專注度 $it/10，鎖可能影響日常表現，留意調整。")
                it >= 8 -> add("💡 高度專注！$it/10，習慣養成中。")
                else -> {}
            }
        }
        if (score >= 5) add("🌟 今天填寫非常完整，小成就 +1！")
    }
    if (msgs.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Insights, null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("即時回饋", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
            }
            msgs.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

// ─── ④ Rotating Questions Card ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotatingQuestionsCard(
    questions: List<RotatingQuestion>,
    entry: DailyEntry,
    onUpdate: (DailyEntry) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Casino, null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("今日輪換題", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.weight(1f))
                Text("每日更替", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            }
            questions.forEach { q ->
                Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
                RotatingQuestionItem(q, entry, onUpdate)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotatingQuestionItem(q: RotatingQuestion, entry: DailyEntry, onUpdate: (DailyEntry) -> Unit) {
    val rawValue = entry.rotatingAnswers[q.key]
    val answered = rawValue != null
    val answerIsYes = rawValue == "true"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(q.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = answered && answerIsYes,
                onClick = {
                    val newAnswers = entry.rotatingAnswers.toMutableMap().also { it[q.key] = "true" }
                    onUpdate(entry.copy(rotatingAnswers = newAnswers))
                },
                label = { Text("有") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
            FilterChip(
                selected = answered && !answerIsYes,
                onClick = {
                    val newAnswers = entry.rotatingAnswers.toMutableMap().also { it[q.key] = "false" }
                    onUpdate(entry.copy(rotatingAnswers = newAnswers))
                },
                label = { Text("沒有") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        AnimatedVisibility(visible = answered) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = q.feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

// ─── ⑤ Extended Questions (備註) ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtendedQuestionsCard(entry: DailyEntry, onUpdate: (DailyEntry) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (expanded) "收起備註" else "我想記錄更多 →",
                    style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text("選填", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Divider()

                    // X5: 備註（唯一保留項目；其餘問題已整合至核心題或輪換題）
                    QuestionSection("備註", subtitle = "任何想補充的想法") {
                        OutlinedTextField(
                            value = entry.notes ?: "",
                            onValueChange = { onUpdate(entry.copy(notes = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("輸入備註...") },
                            minLines = 3, maxLines = 6
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─── Unified Tab Content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyEntryTabContent(
    entry: DailyEntry,
    onUpdate: (DailyEntry) -> Unit,
    onSave: () -> Unit,
    outerPadding: PaddingValues,
    isMorning: Boolean,
    isMale: Boolean = true,
    selectedDate: LocalDate = LocalDate.now(),
    isExisting: Boolean = false,
    onTakePhoto: () -> Unit = {},
    photoBlurEnabled: Boolean = true,
) {
    var showBedtimePicker by remember { mutableStateOf(false) }
    var showWakeTimePicker by remember { mutableStateOf(false) }
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    // ── Shared scrollable wrapper ──────────────────────────────────────────────
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp, top = 12.dp,
                bottom = outerPadding.calculateBottomPadding() + 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isMorning) {
            // ── ☀️ Morning cards ───────────────────────────────────────────────

            // Completion banner
            if (entry.morningCheckDone) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("早晨記錄已完成 ☀️",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("可隨時更新早晨記錄",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // 🛏 Sleep Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Nightlight, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("睡眠記錄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Divider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("就寢時間", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                entry.bedtime?.format(timeFmt) ?: "未設定",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (entry.bedtime != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(onClick = { showBedtimePicker = true }) {
                            Icon(Icons.Default.Bedtime, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("設定")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("起床時間", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                entry.wakeTime?.format(timeFmt) ?: "未設定",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (entry.wakeTime != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(onClick = { showWakeTimePicker = true }) {
                            Icon(Icons.Default.WbSunny, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("設定")
                        }
                    }
                    if (entry.bedtime != null && entry.wakeTime != null) {
                        val dur = java.time.Duration.between(entry.bedtime, entry.wakeTime).let {
                            if (it.isNegative) it.plusDays(1) else it
                        }
                        val h = dur.toHours()
                        val m = dur.toMinutes() % 60
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                Text("睡眠時長：${h}小時${if (m > 0) " ${m}分" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                    Divider()
                    QuestionSection(title = "睡眠品質") {
                        StarRating(
                            rating = entry.sleepQuality ?: 3,
                            onRatingChange = { onUpdate(entry.copy(sleepQuality = it)) },
                            label = "昨晚有睡好嗎？"
                        )
                    }
                    YesNoToggle(
                        value = entry.wokeUpDueToDevice,
                        onValueChange = { onUpdate(entry.copy(wokeUpDueToDevice = it)) },
                        label = "因佩戴鎖而醒來"
                    )
                    YesNoToggle(
                        value = entry.hadEroticDream,
                        onValueChange = { onUpdate(entry.copy(hadEroticDream = it)) },
                        label = "昨晚有春夢？（在備註紀錄吧）"
                    )
                }
            }

            // 💪 Body Card (男性限定)
            if (isMale) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("身體狀況", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Divider()
                        QuestionSection(title = "晨勃") {
                            YesNoToggle(entry.morningErection, { onUpdate(entry.copy(morningErection = it)) }, "有晨勃")
                        }
                        QuestionSection(title = "昨晚夜間勃起", subtitle = "大概的感受即可") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Constants.NIGHT_ERECTION_OPTIONS.forEach { label ->
                                    val value = Constants.NIGHT_ERECTION_VALUES[label] ?: 0
                                    FilterChip(
                                        selected = entry.nightErections == value,
                                        onClick = { onUpdate(entry.copy(nightErections = value)) },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = MaterialTheme.colorScheme.outline,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    )
                                }
                            }
                        }
                        YesNoToggle(
                            value = entry.wokeUpFromErection,
                            onValueChange = { onUpdate(entry.copy(wokeUpFromErection = it)) },
                            label = "因夜間勃起而醒來"
                        )
                    }
                }
            }

            // 😊 Morning Mood Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEmotions, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("起床狀態", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Divider()
                    QuestionSection(title = "起床後心情") {
                        MoodSelector(
                            selectedMood = entry.morningMood,
                            moods = Constants.MOODS,
                            onMoodSelected = { onUpdate(entry.copy(morningMood = it)) }
                        )
                    }
                    // M10: Battery-style energy display
                    QuestionSection(title = "起床能量指數", subtitle = "1 = 極度疲憊   5 = 精力充沛") {
                        val batteryIcons = listOf("🪫", "🔋", "🔋", "🔋", "⚡")
                        val batteryLabels = listOf("1", "2", "3", "4", "5")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            batteryLabels.forEachIndexed { index, lbl ->
                                val level = index + 1
                                Box(modifier = Modifier.weight(1f)) {
                                    FilterChip(
                                        modifier = Modifier.fillMaxWidth(),
                                        selected = entry.morningEnergy == level,
                                        onClick = { onUpdate(entry.copy(morningEnergy = level)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = MaterialTheme.colorScheme.outline,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        label = {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(batteryIcons[index], maxLines = 1)
                                                Spacer(Modifier.height(2.dp))
                                                Text(lbl, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // ── 🌙 Evening cards ───────────────────────────────────────────────
            DayStatusCard(entry, selectedDate)
            CoreQuestionsCard(entry = entry, onUpdate = onUpdate, onTakePhoto = onTakePhoto, photoBlurEnabled = photoBlurEnabled)
            val score = coreCompletionScore(entry)
            AnimatedVisibility(visible = score >= 2) {
                RealtimeFeedbackCard(entry, score)
            }
            RotatingQuestionsCard(
                questions = remember(selectedDate, isMale) { getRotatingQuestionsForDate(selectedDate, isMale) },
                entry = entry,
                onUpdate = onUpdate
            )
            ExtendedQuestionsCard(entry = entry, onUpdate = onUpdate)
            if (isExisting) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("記錄信息", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("建立：${entry.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("更新：${entry.updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Shared save button ─────────────────────────────────────────────────
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(
                imageVector = if (isMorning) Icons.Default.CheckCircle else Icons.Default.Save,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    isMorning && entry.morningCheckDone -> "更新早晨記錄"
                    isMorning -> "完成早晨記錄"
                    isExisting -> "更新記錄"
                    else -> "儲存今日記錄"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    // ── Time pickers (morning only) ────────────────────────────────────────────
    if (showBedtimePicker) {
        TimePickerDialog(
            onDismiss = { showBedtimePicker = false },
            onConfirm = { h, m ->
                onUpdate(entry.copy(bedtime = LocalTime.of(h, m)))
                showBedtimePicker = false
            },
            initialHour = entry.bedtime?.hour ?: 22,
            initialMinute = entry.bedtime?.minute ?: 0
        )
    }
    if (showWakeTimePicker) {
        TimePickerDialog(
            onDismiss = { showWakeTimePicker = false },
            onConfirm = { h, m ->
                onUpdate(entry.copy(wakeTime = LocalTime.of(h, m)))
                showWakeTimePicker = false
            },
            initialHour = entry.wakeTime?.hour ?: 7,
            initialMinute = entry.wakeTime?.minute ?: 30
        )
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────
private fun createCameraImageUri(context: Context): Uri {
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val dir = File(context.getExternalFilesDir("Pictures"), "").also { it.mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider",
        File(dir, "PHOTO_$ts.jpg"))
}
