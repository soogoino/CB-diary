package com.chastity.diary.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
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
import com.chastity.diary.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import kotlin.math.abs
import com.chastity.diary.ui.components.*
import com.chastity.diary.util.Constants
import com.chastity.diary.MainActivity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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

// ─── Rotating question pool (R1–R33, 32 total; EAV-backed) ──────────────────
private enum class RotatingQuestion(
    val key: String,
    val title: String,
    val feedback: String,
    val isMaleOnly: Boolean = false
) {
    R1 ("R1",  "今日是否有分泌物洩漏？",                           "不論有沒有分泌物，記得別害羞地忽略清潔，身體會感謝你的。"),
    R2 ("R2",  "今日是否有主動頂鎖/摩擦，尋求快感？",                        "不管有沒有試探那份衝動，誠實面對自己就已經很勇敢了。", isMaleOnly = true),
    R3 ("R3",  "今日是否進行邊緣訓練？",                           "無論你是逗留邊緣還是退縮，能覺察這種誘惑本身就值得一點羞澀的笑。"),
    R4 ("R4",  "今日是否與Keyholder互動？",                        "不論有沒有說出來，關係的細節常常比結果更值得回味。"),
    R6 ("R6",  "今日是否帶著鎖進入公眾場合？",                   "在人前帶著這個小秘密，不管敢不敢暴露，這種心跳都是你的獎勵。"),
    R7 ("R7",  "今日是否曾短暫解除鎖？",                         "不管有沒有偷偷鬆開一秒，記得對自己誠實，並負起責任照顧好自己。"),
    R8 ("R8",  "今日是否有意展示或洩露鎖蹤跡？",                 "故意或不小心露出一點點，都會讓你心裡暗自發笑——接受這份小調皮吧。"),
    R9 ("R9",  "今日是否接觸成人內容？",                           "有沒有看，那種慾望的拉扯本身就值得你輕輕自嘲一下。"),
    R10("R10", "今日是否解鎖或進行自慰？",                         "不論今天怎麼做，承認自己的感受比隱瞞更誠實，也更有療癒力。"),
    R11("R11", "今日是否進行乳頭開發/玩弄？",                      "無論有沒有逗弄，發現新的敏感點總讓人又尷尬又好奇。"),
    R12("R12", "今日是否進行後庭開發/探索？",                      "不管有沒有探索，這類私密體驗的存在本身就會讓人面紅心跳。"),
    R13("R13", "今天你有沒有感受到鎖帶來的不適或調整需求？",     "有沒有不舒服都要記下來，照顧好身體比硬撐更值得一點羞澀。"),
    R14("R14", "今天佩戴鎖是否讓你感覺到內心的平靜或成就？",    "不論感覺如何，察覺到微妙的安心或不適都是成長的一部分。"),
    R15("R15", "今天有沒有想起Keyholder，並感受到連結的溫暖？",   "想或不想，這些小念頭讓你忍不住臉紅，證明了情感的存在。"),
    R16("R16", "今天鎖是否已融入你的日常routine中，感覺自然？", "無論像不像日常，發現自己適應或反彈都是值得悄悄慶祝的事。"),
    R17("R17", "今天有沒有將慾望轉向其他活動，如運動或創作？",    "試圖轉移注意力成功與否，都是在跟自己較勁的一種小勝利。"),
    R18("R18", "今天在人群中，你有沒有特別注意到自己的隱密狀態？","在人群裡的那點心跳感，不管你有沒有留意，都是你的秘密奢侈品。"),
    R19("R19", "今天有沒有進行放鬆活動來緩解可能的壓力？",        "不論有沒有刻意放鬆，對自己好一點的念頭本身就值得鼓勵。"),
    R20("R20", "今天醒來後，有沒有回想起與鎖相關的夢境？",      "做或沒做夢都無妨，夢裡的那些畫面只是偷偷告訴你內心的小秘密。"),
    R21("R21", "今天其他感官（如觸覺或聽覺）是否變得更敏銳？",    "感覺變細膩或平常無感，都是身體在跟你說話，別害羞聽它說完。"),
    R22("R22", "今天有沒有與Keyholder分享你的感受或想法？",       "說或不說都會讓人臉紅，能意識到想分享就是進步。"),
    R23("R23", "今天在不同環境中，鎖帶來的感受如何？",          "站著、坐著、走路時的那些小提醒，不管有沒有注意到，都是真實的回響。"),
    R24("R24", "今天有沒有遇到讓你猶豫或掙扎的時刻？",           "猶豫過或沒有，能回想起那瞬間就證明你還有人性（還有點可愛的弱點）。"),
    R25("R25", "今天佩戴是否帶來任何意外的正面體驗？",           "發現一點小樂子或完全沒有，承認它們會讓你覺得又羞又甜。"),
    R26("R26", "今天有沒有特別注意清潔或保濕等保養？",           "不管做了沒，對細節的在意其實是在偷偷寵自己，別不好意思接受。"),
    R27("R27", "今天有沒有透過寫作或藝術表達你的體驗？",         "寫或畫出來會讓自己臉紅，但這種表達很療癒，值得一點羞澀的驕傲。"),
    R28("R28", "今天時間感覺過得快還是慢，受鎖影響？",         "時間感的拉扯不管你注意沒注意，都在提醒你這段經驗有趣又奇怪。"),
    R29("R29", "今天有沒有在匿名社群分享或閱讀相關經驗？",       "偷看或分享都會讓人心裡有點小偷笑，這種連結感其實挺暖的。"),
    R30("R30", "今天有沒有在想萬一鎖取不下來該怎麼辦？",         "想過或沒想，準備備案本身就是成熟且有責任感的一步，給自己一個點讚。"),
    R31("R31", "今天情緒是否有起伏，與鎖相關？",               "情緒忽上忽下不需要羞愧，能覺察就是在進步，帶點自嘲也沒關係。"),
    R32("R32", "今天有沒有想像未來繼續佩戴的畫面？",             "想像或不想像都好，能看到未來的自己代表你在慢慢接受這件事。"),
    R33("R33", "今日是否有剔除陰毛？",                            "不論有沒有修整，這種私密的小事值得溫柔對待和好好衛生照護。"),
}

private fun getRotatingQuestionsForDate(date: LocalDate, isMale: Boolean): List<RotatingQuestion> {
    val pool = RotatingQuestion.entries.filter { !it.isMaleOnly || isMale }
    // Deterministic shuffle: seed interacts with each element's own hash so relative order
    // changes every day. Using java.util.Random(seed) gives a proper per-date permutation.
    val seed = date.toEpochDay()
    return pool.shuffled(java.util.Random(seed)).take(2)
}

private fun coreCompletionScore(entry: DailyEntry): Int {
    var s = 0
    s++                                                                      // 1. 今天有佩戴（Boolean，是/否 chips 永遠算已答）
    if (entry.mood != null) s++                                              // 2. 今天的心情
    if (entry.desireLevel != null) s++                                       // 3. 今天的性慾強度
    if (entry.deviceCheckPassed && entry.comfortRating != null) s++          // 4. 佩戴舒適度（未佩戴時不顯示故不計）
    if (entry.focusLevel != null) s++                                        // 5. 今天的專注度
    s++                                                                      // 6. 是否運動（Boolean，是/否 chips 永遠算已答）
    if (entry.cleaningType != null) s++                                      // 7. 清潔
    return s
}

/** 佩戴時共 7 題；未佩戴時舒適度不顯示，共 6 題 */
private fun coreCompletionTotal(entry: DailyEntry) = if (entry.deviceCheckPassed) 7 else 6

// P4: Pre-computed list to avoid rebuilding on every recomposition
private val CLEANING_TYPES_ROWS: List<List<String>> by lazy { Constants.CLEANING_TYPES.chunked(2) }

// ─── Main Screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    // C-1: Detect unsaved form changes to warn before date switch
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    // P3: Stable lambda references — prevents DailyEntryTabContent from skipping recomposition
    val onUpdateEntry: (DailyEntry) -> Unit = remember(viewModel) { { e -> viewModel.updateEntry { _ -> e } } }
    val onSaveMorningCheck: () -> Unit = remember(viewModel) { { viewModel.saveMorningCheck() } }
    val onSaveEntry: () -> Unit = remember(viewModel) { { viewModel.saveEntry() } }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // C-1: Guard dialog when navigating away with unsaved changes
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showNarrativeSheet by rememberSaveable { mutableStateOf(false) }
    // B5: Capture generated narrative so BottomSheet displays the same text that was saved
    var lastNarrativeText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Camera – store the actual File so we can save its absolutePath (content:// URI path is not readable)
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        // 相機返回，清除旗標（雙重保險，ON_START 也會清）
        MainActivity.isCameraLaunching = false
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
            // 告知 MainActivity：即將進入相機，ON_STOP 不應觸發鎖定
            MainActivity.isCameraLaunching = true
            cameraLauncher.launch(u)
        }
    }
    // Stable camera lambda — new lambda instance is created every Scaffold recompose without
    // remember, causing the entire evening DailyEntryTabContent to re-layout unnecessarily.
    val onTakePhotoStable: () -> Unit = remember(cameraPermissionLauncher) {
        { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            // B3: Snapshot entry immediately before any async DB update can change entryState
            val narrativeEntry = when (val s = entryState) {
                is EntryFormState.Loaded -> s.entry
                is EntryFormState.Empty -> DailyEntry(date = selectedDate)
            }
            val narrativeText = generateDailyNarrative(narrativeEntry)
            lastNarrativeText = narrativeText
            // B3: Clear flag immediately
            viewModel.clearSaveSuccess()
            // C-3: Narrative is shown in the BottomSheet only — do NOT overwrite user's notes field
            showNarrativeSheet = true
            snackbarHostState.showSnackbar(context.getString(R.string.save_success))
        }
    }
    LaunchedEffect(morningSaveSuccess) {
        if (morningSaveSuccess) { snackbarHostState.showSnackbar(context.getString(R.string.morning_save_success)); viewModel.clearMorningSaveSuccess() }
    }
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) { snackbarHostState.showSnackbar(context.getString(R.string.delete_success)); viewModel.clearDeleteSuccess() }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Slide-and-fade the date text whenever the user picks a new day
                    AnimatedContent(
                        targetState = selectedDate,
                        transitionSpec = {
                            // New date slides up in, old slides down out
                            (slideInVertically { h -> h / 3 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically { h -> -h / 3 } + fadeOut(tween(150)))
                        },
                        label = "dateTitle"
                    ) { date ->
                        Column {
                            Text(date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
                            if (entryState is EntryFormState.Loaded) {
                                Text(stringResource(R.string.edit_mode), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
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
                    IconButton(onClick = {
                        // C-1: Warn user if there are unsaved form changes before switching dates
                        if (hasUnsavedChanges) showUnsavedChangesDialog = true
                        else showDatePicker = true
                    }) {
                        Icon(Icons.Default.CalendarToday, "選擇日期")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // H-2: Block HorizontalPager horizontal swipe while keyboard is open — prevents accidental
        // tab switch when the user moves their thumb on the autocomplete bar or types diagonally.
        val imeVisible = WindowInsets.isImeVisible
        val entry = when (val s = entryState) {
            is EntryFormState.Loaded -> s.entry
            is EntryFormState.Empty -> DailyEntry(date = selectedDate)
        }
        val isExisting = entryState is EntryFormState.Loaded &&
                (entryState as EntryFormState.Loaded).entry.id != 0L

        // Hoist scroll states so they survive recompositions of DailyEntryTabContent
        // (e.g. triggered by DB save). Without hoisting, the scroll position may reset
        // mid-interaction and causes an extra layout pass on each state update.
        val morningScrollState = rememberScrollState()
        val eveningScrollState = rememberScrollState()

        // HorizontalPager keeps both tabs composed simultaneously —
        // switching is instant (just slides viewport) with no destroy/recreate cost.
        val pagerState = rememberPagerState(initialPage = currentTab) { 2 }
        val coroutineScope = rememberCoroutineScope()

        // Sync: user swipes pager → ViewModel (settledPage avoids mid-scroll noise)
        LaunchedEffect(pagerState.settledPage) {
            viewModel.selectTab(pagerState.settledPage)
        }
        // Sync: ViewModel tab changed programmatically → animate pager
        LaunchedEffect(currentTab) {
            if (pagerState.settledPage != currentTab) {
                pagerState.animateScrollToPage(currentTab)
            }
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Tab Row ────────────────────────────────────────────────────────
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(stringResource(R.string.tab_morning))
                            if (!entry.morningCheckDone) {
                                Badge()
                            }
                        }
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.tab_evening)) }
                )
            }

            // PERF-FIX: 使用 Box overlay 取代 Crossfade 包裹 HorizontalPager。
            // 原本 Crossfade(isLoading) 每次 isLoading 切換（每次 save/load）都會
            // 將整個 HorizontalPager 從 Composition 移除後重建，導致：
            //   1. 兩個 Tab 的所有 Composable 重新 inflate（視覺卡頓）
            //   2. 重新測量/繪製所有 Card、Chip、Slider 造成多個 frame 掉幀
            // 改用 Box + AnimatedVisibility overlay：Pager 永遠留在 Composition，
            // 儲存時只在上方疊加半透明 loading 遮罩，切回後狀態完全保留。
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondBoundsPageCount = 1,  // keep the other page composed → instant tab switch
                    key = { it },               // stable page identity → skip recompose when offset-only changes
                    userScrollEnabled = !imeVisible // H-2: keyboard open → disable swipe to avoid accidental tab change
                ) { page ->
                    when (page) {
                        0 -> DailyEntryTabContent(
                            entry = entry,
                            onUpdate = onUpdateEntry,
                            onSave = onSaveMorningCheck,
                            outerPadding = outerPadding,
                            scrollState = morningScrollState,
                            isMorning = true,
                            isMale = isMale,
                            selectedDate = selectedDate,
                            photoBlurEnabled = userSettings.photoBlurEnabled
                        )
                        else -> DailyEntryTabContent(
                            entry = entry,
                            onUpdate = onUpdateEntry,
                            onSave = onSaveEntry,
                            outerPadding = outerPadding,
                            scrollState = eveningScrollState,
                            isMorning = false,
                            isMale = isMale,
                            selectedDate = selectedDate,
                            isExisting = isExisting,
                            onTakePhoto = onTakePhotoStable,
                            photoBlurEnabled = userSettings.photoBlurEnabled
                        )
                    }
                }

                // Loading overlay — 疊加在 Pager 上方，不破壞 Pager 的 Composition 樹。
                // 不用 AnimatedVisibility 是因為 BoxScope 與 ColumnScope 的 receiver 衝突；
                // 簡單 if 區塊即足夠，主要效益來自「Pager 不被 Crossfade 管理」。
                if (isLoading) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }   // closes Box
        }       // closes Column
}               // closes Scaffold { padding -> }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedDate,
            onConfirm = { viewModel.selectDate(it) },
            onDismiss = { showDatePicker = false }
        )
    }
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            title = stringResource(R.string.delete_dialog_title),
            message = "確定要刪除 ${selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))} 的記錄嗎？\n\n此操作無法復原。",
            onConfirm = { viewModel.deleteEntry() },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // C-1: Unsaved changes guard — shown when user taps the calendar icon with a dirty form
    if (showUnsavedChangesDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("有未儲存的變更") },
            text = { Text("切換日期將放棄目前尚未儲存的內容，確定要繼續？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedChangesDialog = false
                    showDatePicker = true
                }) { Text("直接切換") }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 💬 Daily Narrative Bottom Sheet
    if (showNarrativeSheet) {
        val narrativeEntry = when (val s = entryState) {
            is EntryFormState.Loaded -> s.entry
            is EntryFormState.Empty -> DailyEntry(date = selectedDate)
        }
        ModalBottomSheet(onDismissRequest = { showNarrativeSheet = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "📝 今日摘要",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Divider()
                // B5: Use captured text (same as what was saved to notes), not a fresh call
                Text(
                    text = lastNarrativeText.ifBlank { generateDailyNarrative(narrativeEntry) },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showNarrativeSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(" 好的 👍 ")
                }
            }
        }
    }
}

// ─── ① Day Status Card ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayStatusCard(entry: DailyEntry, selectedDate: LocalDate) {
    val score = coreCompletionScore(entry)
    val total = coreCompletionTotal(entry)
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
                    if (score <= 2) "尚未開始記錄（可填 $total 題）" else "核心題目完成 $score / $total",
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
    // B4: Use remember (not rememberSaveable) — photo reveal state must not persist across dates
    var photoRevealed by remember { mutableStateOf(false) }
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
                        // PERF-FIX: 已選中的按鈕加 guard，避免點擊相同值觸發無效 updateEntry → recompose
                        Button(onClick = { /* already selected — no-op */ },
                            modifier = Modifier.weight(1f)) { Text("✓ 有佩戴") }
                        OutlinedButton(onClick = { onUpdate(entry.copy(deviceCheckPassed = false)) },
                            modifier = Modifier.weight(1f)) { Text("✗ 沒有") }
                    } else {
                        OutlinedButton(onClick = { onUpdate(entry.copy(deviceCheckPassed = true)) },
                            modifier = Modifier.weight(1f)) { Text("✓ 有佩戴") }
                        Button(onClick = { /* already selected — no-op */ },
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
                    valueRange = 1f..10f, steps = 8, label = "性慾強度")
            }

            // C4: Comfort (只在佩戴時)
            AnimatedVisibility(visible = entry.deviceCheckPassed) {
                QuestionSection(title = "佩戴舒適度", subtitle = "1 = 非常不舒適  10 = 非常舒適") {
                    SliderWithLabel(entry.comfortRating?.toFloat() ?: 5f,
                        { onUpdate(entry.copy(comfortRating = it.toInt())) },
                        valueRange = 1f..10f, steps = 8, label = "舒適度")
                }
            }

            // C5: Focus
            QuestionSection(title = "今日專注度", subtitle = "1 = 完全分心   10 = 高度專注") {
                SliderWithLabel(entry.focusLevel?.toFloat() ?: 5f,
                    { onUpdate(entry.copy(focusLevel = it.toInt())) },
                    valueRange = 1f..10f, steps = 8, label = "專注度")
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
                    // P1: Load bitmap on IO thread to avoid main-thread jank
                    var bitmap by remember(entry.photoPath) { mutableStateOf<Bitmap?>(null) }
                    var bitmapLoaded by remember(entry.photoPath) { mutableStateOf(false) }
                    LaunchedEffect(entry.photoPath) {
                        bitmapLoaded = false
                        bitmap = withContext(Dispatchers.IO) {
                            runCatching {
                                val f = File(entry.photoPath)  // non-null: inside isNullOrBlank guard
                                if (!f.exists()) return@runCatching null
                                // H-1: Two-pass decode with inSampleSize prevents OOM on high-res camera images
                                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeFile(f.absolutePath, boundsOpts)
                                val sampleSize = calculateInSampleSize(boundsOpts, 1080, 1920)
                                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                val raw = BitmapFactory.decodeFile(f.absolutePath, decodeOpts) ?: return@runCatching null
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
                        bitmapLoaded = true
                    }

                    if (!bitmapLoaded) {
                        // Still loading — show a slim placeholder row
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else if (bitmap != null) {
                        // B2: Guard aspect ratio against divide-by-zero from corrupt images
                        val photoAspectRatio = bitmap!!.let { b ->
                            (b.width.toFloat() / b.height.toFloat()).takeIf { it > 0f && it.isFinite() }
                        } ?: (3f / 4f)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Photo preview with blur overlay — respects portrait / landscape
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(photoAspectRatio)
                                    .clickable { photoRevealed = !photoRevealed }
                            ) {
                                Image(
                                    bitmap!!.asImageBitmap(), "打卡照片",
                                    Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop
                                )
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
                    } else {
                        // B2: File was deleted externally — show error state and let user clear
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "⚠️ 照片檔案已遺失",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                OutlinedButton(onClick = { onUpdate(entry.copy(photoPath = null)) }) {
                                    Text("清除記錄")
                                }
                            }
                        }
                    }
                }
            }

            // E7: Exercise (moved from extended to core)
            Divider()
            QuestionSection(title = "是否運動？") {
                YesNoToggle(entry.exercised, { onUpdate(entry.copy(exercised = it)) }, "有運動")
            }

            // E8: Cleaning (moved from extended to core) — single-select
            QuestionSection(title = "今天是否清潔了貞操鎖？") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CLEANING_TYPES_ROWS.forEach { row -> // P4: use pre-computed constant
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = entry.cleaningType == option,
                                    onClick = {
                                        // Single-select: clicking selected item deselects, clicking another selects it
                                        onUpdate(entry.copy(cleaningType = if (entry.cleaningType == option) null else option))
                                    },
                                    label = { Text(option) },
                                    modifier = Modifier.weight(1f),
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
                            if (row.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ─── ③ Rotating Questions Card ────────────────────────────────────────────────
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
                val feedbacks = stringArrayResource(R.array.daily_rotating_feedback_generic)
                val unifiedFeedback = remember(q.key) { feedbacks[abs(q.key.hashCode()) % feedbacks.size] }
                Text(
                    text = unifiedFeedback,
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
    // Q1: Key on entry.date so switching dates auto-collapses the notes section
    var expanded by remember(entry.date) { mutableStateOf(false) }
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
    scrollState: ScrollState,
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
            .verticalScroll(scrollState)
            .imePadding()  // H-6: shift content above the soft keyboard
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
                            Text(stringResource(R.string.morning_done_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(stringResource(R.string.morning_done_subtitle),
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
                    QuestionSection(title = "睡眠品質", subtitle = "1 = 很差   10 = 極佳") {
                        SliderWithLabel(
                            entry.sleepQuality?.toFloat() ?: 5f,
                            { onUpdate(entry.copy(sleepQuality = it.toInt())) },
                            valueRange = 1f..10f, steps = 8, label = "睡眠品質"
                        )
                    }
                    QuestionSection(title = "因佩戴鎖而醒來？") {
                        YesNoToggle(
                            value = entry.wokeUpDueToDevice,
                            onValueChange = { onUpdate(entry.copy(wokeUpDueToDevice = it)) },
                            label = "因佩戴鎖而醒來"
                        )
                    }
                    QuestionSection(title = "昨晚有春夢？") {
                        YesNoToggle(
                            value = entry.hadEroticDream,
                            onValueChange = { onUpdate(entry.copy(hadEroticDream = it)) },
                            label = "昨晚有春夢"
                        )
                    }
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
                        QuestionSection(title = "因夜間勃起而醒來？") {
                            YesNoToggle(
                                value = entry.wokeUpFromErection,
                                onValueChange = { onUpdate(entry.copy(wokeUpFromErection = it)) },
                                label = "因夜間勃起而醒來"
                            )
                        }
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
                    QuestionSection(title = "起床能量指數", subtitle = "1 = 極度疲憊   10 = 精力充沛") {
                        SliderWithLabel(
                            entry.morningEnergy?.toFloat() ?: 5f,
                            { onUpdate(entry.copy(morningEnergy = it.toInt())) },
                            valueRange = 1f..10f, steps = 8, label = "起床能量"
                        )
                    }
                }
            }

        } else {
            // ── 🌙 Evening cards ───────────────────────────────────────────────
            DayStatusCard(entry, selectedDate)
            CoreQuestionsCard(entry = entry, onUpdate = onUpdate, onTakePhoto = onTakePhoto, photoBlurEnabled = photoBlurEnabled)
            RotatingQuestionsCard(
                questions = remember(selectedDate, isMale) { getRotatingQuestionsForDate(selectedDate, isMale) },
                entry = entry,
                onUpdate = onUpdate
            )
            ExtendedQuestionsCard(entry = entry, onUpdate = onUpdate)
            EveningMasturbationCard(entry = entry, onUpdate = onUpdate)
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
                    isMorning && entry.morningCheckDone -> stringResource(R.string.save_button_update_morning)
                    isMorning -> stringResource(R.string.save_button_complete_morning)
                    isExisting -> stringResource(R.string.save_button_update)
                    else -> stringResource(R.string.save_button_save_today)
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

// ─── Evening Masturbation Card ──────────────────────────────────────────────────────
@Composable
private fun EveningMasturbationCard(entry: DailyEntry, onUpdate: (DailyEntry) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "💧 自慰小記",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            com.chastity.diary.ui.components.QuestionSection(
                title = "今天是否有自慰？"
            ) {
                com.chastity.diary.ui.components.YesNoToggle(
                    value = entry.masturbated,
                    onValueChange = { v ->
                        onUpdate(entry.copy(
                            masturbated = v,
                            masturbationCount = if (!v) null else entry.masturbationCount
                        ))
                    },
                    label = "有自慰"
                )
                if (entry.masturbated) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "次數：",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline()
                        )
                        androidx.compose.material3.IconButton(
                            onClick = {
                                val cur = entry.masturbationCount ?: 1
                                if (cur > 1) onUpdate(entry.copy(masturbationCount = cur - 1))
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "減少")
                        }
                        Text(
                            text = "${entry.masturbationCount ?: 1}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alignByBaseline()
                        )
                        androidx.compose.material3.IconButton(
                            onClick = {
                                val cur = entry.masturbationCount ?: 1
                                onUpdate(entry.copy(masturbationCount = cur + 1))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "增加")
                        }
                    }
                }
            }
        }
    }
}

// ─── Daily Narrative ──────────────────────────────────────────────────────────
private fun generateDailyNarrative(entry: DailyEntry): String {
    val parts = mutableListOf<String>()

    // 心情
    entry.mood?.let { parts.add("今天心情是 $it") }

    // 性慾
    entry.desireLevel?.let {
        parts.add(when {
            it >= 8 -> "性慾強度 $it/10，今天是高峰日——但你撐過來了 💪"
            it <= 3 -> "性慾強度 $it/10，今天異常平靜 😌"
            else    -> "性慾強度 $it/10，處於正常範圍"
        })
    }

    // 佩戴
    if (entry.deviceCheckPassed) {
        entry.comfortRating?.let { r ->
            parts.add("佩戴舒適度 $r/10" + when {
                r >= 8 -> "，狀況很好！"
                r <= 3 -> "，記得調整佩戴方式。"
                else   -> "。"
            })
        }
    } else {
        parts.add("今天沒有佩戴裝置。")
    }

    // 運動
    if (entry.exercised) {
        parts.add("有運動" + (entry.exerciseDuration?.let { "（${it} 分鐘）" } ?: "") + "，自律 +1 🏃")
    }

    // 清潔
    entry.cleaningType?.takeIf { it != "未清潔" }?.let { parts.add("清潔類型：$it 🧹") }

    // Keyholder
    if (entry.keyholderInteraction) parts.add("今天與 Keyholder 保持了連結 💬")

    // 打卡照
    if (entry.photoPath != null) parts.add("📷 今天有留下打卡照片")

    // 解鎖
    if (entry.unlocked) parts.add("今天解鎖了——誠實記錄是好事 🔓")

    // 自慰
    if (entry.masturbated) {
        val cnt = entry.masturbationCount ?: 1
        parts.add("今天有自慰 $cnt 次 💧")
    }

    // 邊緣
    if (entry.hadEdging) parts.add("邊緣訓練完成，耐力值 UP 😈")

    return if (parts.isEmpty()) "今日記錄已儲存，繼續保持！"
           else parts.joinToString("\n• ", prefix = "• ")
}

// H-1: Calculate inSampleSize to load camera photos at display resolution, preventing OOM.
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val origHeight = options.outHeight
    val origWidth  = options.outWidth
    var inSampleSize = 1
    if (origHeight > reqHeight || origWidth > reqWidth) {
        val halfHeight = origHeight / 2
        val halfWidth  = origWidth  / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
