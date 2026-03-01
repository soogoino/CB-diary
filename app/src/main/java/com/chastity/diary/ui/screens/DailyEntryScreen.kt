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
import androidx.annotation.StringRes

// ─── Rotating question pool (R1–R33, 32 total; EAV-backed) ──────────────────
private enum class RotatingQuestion(
    val key: String,
    @StringRes val titleRes: Int,
    val isMaleOnly: Boolean = false
) {
    R1 ("R1",  R.string.rq_r1_title),
    R2 ("R2",  R.string.rq_r2_title, isMaleOnly = true),
    R3 ("R3",  R.string.rq_r3_title),
    R4 ("R4",  R.string.rq_r4_title),
    R6 ("R6",  R.string.rq_r6_title),
    R7 ("R7",  R.string.rq_r7_title),
    R8 ("R8",  R.string.rq_r8_title),
    R9 ("R9",  R.string.rq_r9_title),
    R10("R10", R.string.rq_r10_title),
    R11("R11", R.string.rq_r11_title),
    R12("R12", R.string.rq_r12_title),
    R13("R13", R.string.rq_r13_title),
    R14("R14", R.string.rq_r14_title),
    R15("R15", R.string.rq_r15_title),
    R16("R16", R.string.rq_r16_title),
    R17("R17", R.string.rq_r17_title),
    R18("R18", R.string.rq_r18_title),
    R19("R19", R.string.rq_r19_title),
    R20("R20", R.string.rq_r20_title),
    R21("R21", R.string.rq_r21_title),
    R22("R22", R.string.rq_r22_title),
    R23("R23", R.string.rq_r23_title),
    R24("R24", R.string.rq_r24_title),
    R25("R25", R.string.rq_r25_title),
    R26("R26", R.string.rq_r26_title),
    R27("R27", R.string.rq_r27_title),
    R28("R28", R.string.rq_r28_title),
    R29("R29", R.string.rq_r29_title),
    R30("R30", R.string.rq_r30_title),
    R31("R31", R.string.rq_r31_title),
    R32("R32", R.string.rq_r32_title),
    R33("R33", R.string.rq_r33_title),
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
    var showRecordInfoDialog by remember { mutableStateOf(false) }
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
            val narrativeText = generateDailyNarrative(context, narrativeEntry)
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
                            Text(date.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern))))
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
                                Icon(Icons.Default.Delete, stringResource(R.string.topbar_delete_cd), tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { showRecordInfoDialog = true }) {
                                Icon(Icons.Default.Info, stringResource(R.string.entry_record_info_title))
                            }
                        }
                    }
                    IconButton(onClick = {
                        // C-1: Warn user if there are unsaved form changes before switching dates
                        if (hasUnsavedChanges) showUnsavedChangesDialog = true
                        else showDatePicker = true
                    }) {
                        Icon(Icons.Default.CalendarToday, stringResource(R.string.topbar_calendar_cd))
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

        // Sync: user swipes pager → ViewModel (settledPage avoids mid-scroll noise)
        LaunchedEffect(pagerState.settledPage) {
            viewModel.selectTab(pagerState.settledPage)
        }
        // Sync: ViewModel tab changed → animate pager.
        // LaunchedEffect 在 key 改變時自動取消上一次動畫，確保同一時間只有一個
        // animateScrollToPage 在執行，從根本避免多協程競爭導致 pager 卡住。
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
                    // 只更新 ViewModel；LaunchedEffect(currentTab) 負責動畫
                    onClick = { viewModel.selectTab(0) },
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
                    onClick = { viewModel.selectTab(1) },
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
            message = stringResource(R.string.delete_dialog_message, selectedDate.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern)))),
            onConfirm = { viewModel.deleteEntry() },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // C-1: Unsaved changes guard — shown when user taps the calendar icon with a dirty form
    if (showUnsavedChangesDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedChangesDialog = false
                    showDatePicker = true
                }) { Text(stringResource(R.string.unsaved_changes_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
                    stringResource(R.string.entry_narrative_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    selectedDate.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Divider()
                // B5: Use captured text (same as what was saved to notes), not a fresh call
                Text(
                    text = lastNarrativeText.ifBlank { generateDailyNarrative(context, narrativeEntry) },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showNarrativeSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ok_thumbs_up))
                }
            }
        }
    }

    // 紀錄訊息 Dialog
    if (showRecordInfoDialog) {
        val loadedEntry = (entryState as? EntryFormState.Loaded)?.entry
        if (loadedEntry != null && loadedEntry.id != 0L) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showRecordInfoDialog = false },
                title = { Text(stringResource(R.string.entry_record_info_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add, null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                            Text(
                                stringResource(R.string.entry_created_prefix, loadedEntry.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit, null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                            Text(
                                stringResource(R.string.entry_updated_prefix, loadedEntry.updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRecordInfoDialog = false }) { Text(stringResource(R.string.entry_close)) }
                }
            )
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
                    if (isToday) stringResource(R.string.entry_today_title) else selectedDate.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_short_pattern))),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (score <= 2) stringResource(R.string.entry_no_record_prompt, total) else stringResource(R.string.entry_completion_status, score, total),
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                Text(stringResource(R.string.section_core_questions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.section_core_daily_required), style = MaterialTheme.typography.labelSmall) })
            }

            Divider()

            // C2: Device worn (BRANCHING ROOT) — now first
            QuestionSection(title = stringResource(R.string.q_device_worn)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val wearing = entry.deviceCheckPassed
                    if (wearing) {
                        // PERF-FIX: 已選中的按鈕加 guard，避免點擊相同值觸發無效 updateEntry → recompose
                        Button(onClick = { /* already selected — no-op */ },
                            modifier = Modifier.weight(1f)) { Text(stringResource(R.string.chip_worn_yes)) }
                        OutlinedButton(onClick = { onUpdate(entry.copy(deviceCheckPassed = false)) },
                            modifier = Modifier.weight(1f)) { Text(stringResource(R.string.chip_worn_no)) }
                    } else {
                        OutlinedButton(onClick = { onUpdate(entry.copy(deviceCheckPassed = true)) },
                            modifier = Modifier.weight(1f)) { Text(stringResource(R.string.chip_worn_yes)) }
                        Button(onClick = { /* already selected — no-op */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text(stringResource(R.string.chip_worn_no))
                        }
                    }
                }
            }

            // C1: Mood (merged, emoji+text, single-select)
            QuestionSection(title = stringResource(R.string.q_mood_title), subtitle = stringResource(R.string.q_mood_subtitle)) {
                MoodSelector(
                    selectedMood = entry.mood,
                    moods = stringArrayResource(R.array.moods_array).toList(),
                    moodKeys = Constants.MOODS,
                    onMoodSelected = { onUpdate(entry.copy(mood = it)) }
                )
            }

            // C3: Desire level
            QuestionSection(title = stringResource(R.string.q_desire_title), subtitle = stringResource(R.string.q_desire_subtitle)) {
                SliderWithLabel(entry.desireLevel?.toFloat() ?: 5f,
                    { onUpdate(entry.copy(desireLevel = it.toInt())) },
                    valueRange = 1f..10f, steps = 8, label = stringResource(R.string.q_desire_label))
            }

            // C4: Comfort (只在佩戴時)
            AnimatedVisibility(visible = entry.deviceCheckPassed) {
                QuestionSection(title = stringResource(R.string.q_comfort_title), subtitle = stringResource(R.string.q_comfort_subtitle)) {
                    SliderWithLabel(entry.comfortRating?.toFloat() ?: 5f,
                        { onUpdate(entry.copy(comfortRating = it.toInt())) },
                        valueRange = 1f..10f, steps = 8, label = stringResource(R.string.q_comfort_label))
                }
            }

            // C5: Focus
            QuestionSection(title = stringResource(R.string.q_focus_title), subtitle = stringResource(R.string.q_focus_subtitle)) {
                SliderWithLabel(entry.focusLevel?.toFloat() ?: 5f,
                    { onUpdate(entry.copy(focusLevel = it.toInt())) },
                    valueRange = 1f..10f, steps = 8, label = stringResource(R.string.q_focus_label))
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
                        Text(stringResource(R.string.q_photo_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            if (entry.photoPath.isNullOrBlank()) stringResource(R.string.q_photo_subtitle_optional) else stringResource(R.string.q_photo_taken),
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
                            Text(stringResource(R.string.action_take_photo))
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
                                    bitmap!!.asImageBitmap(), stringResource(R.string.q_photo_title),
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
                                                stringResource(R.string.q_photo_tap_to_view),
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
                                    Text(stringResource(R.string.action_retake_photo))
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
                                    Text(stringResource(R.string.action_delete_photo))
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
                                    stringResource(R.string.error_photo_missing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                OutlinedButton(onClick = { onUpdate(entry.copy(photoPath = null)) }) {
                                    Text(stringResource(R.string.action_clear_record))
                                }
                            }
                        }
                    }
                }
            }

            // E7: Exercise (moved from extended to core)
            Divider()
            QuestionSection(title = stringResource(R.string.q_exercise_title)) {
                YesNoToggle(entry.exercised, { onUpdate(entry.copy(exercised = it)) }, stringResource(R.string.q_exercise_label))
            }

            // E8: Cleaning (moved from extended to core) — single-select
            QuestionSection(title = stringResource(R.string.q_cleaning_title)) {
                val cleaningLabels = stringArrayResource(R.array.cleaning_types_array)
                val cleaningKeys = Constants.CLEANING_TYPES
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cleaningLabels.forEachIndexed { index, label ->
                        val key = cleaningKeys.getOrElse(index) { label }
                        FilterChip(
                            selected = entry.cleaningType == key,
                            onClick = {
                                onUpdate(entry.copy(cleaningType = if (entry.cleaningType == key) null else key))
                            },
                            label = { Text(label) },
                            modifier = Modifier.widthIn(min = 88.dp),
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
                Text(stringResource(R.string.section_rotating_questions), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.section_rotating_daily_label), style = MaterialTheme.typography.labelSmall,
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
        Text(stringResource(q.titleRes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = answered && answerIsYes,
                onClick = {
                    val newAnswers = entry.rotatingAnswers.toMutableMap().also { it[q.key] = "true" }
                    onUpdate(entry.copy(rotatingAnswers = newAnswers))
                },
                label = { Text(stringResource(R.string.chip_yes)) },
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
                label = { Text(stringResource(R.string.chip_no)) },
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
                Text(if (expanded) stringResource(R.string.notes_collapse) else stringResource(R.string.notes_expand),
                    style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.notes_optional), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Divider()

                    // X5: 備註（唯一保留項目；其餘問題已整合至核心題或輪換題）
                    QuestionSection(stringResource(R.string.section_notes), subtitle = stringResource(R.string.section_notes_subtitle)) {
                        OutlinedTextField(
                            value = entry.notes ?: "",
                            onValueChange = { onUpdate(entry.copy(notes = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.notes_placeholder)) },
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
                        Text(stringResource(R.string.section_sleep_record), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Divider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(R.string.q_bedtime_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                entry.bedtime?.format(timeFmt) ?: stringResource(R.string.not_set),
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (entry.bedtime != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(onClick = { showBedtimePicker = true }) {
                            Icon(Icons.Default.Bedtime, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_set))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(R.string.q_wake_time_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                entry.wakeTime?.format(timeFmt) ?: stringResource(R.string.not_set),
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (entry.wakeTime != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(onClick = { showWakeTimePicker = true }) {
                            Icon(Icons.Default.WbSunny, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_set))
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
                                Text(if (m > 0) stringResource(R.string.sleep_duration_hours_minutes, h, m) else stringResource(R.string.sleep_duration_hours, h),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                    Divider()
                    QuestionSection(title = stringResource(R.string.q_sleep_quality_title), subtitle = stringResource(R.string.q_sleep_quality_subtitle)) {
                        SliderWithLabel(
                            entry.sleepQuality?.toFloat() ?: 5f,
                            { onUpdate(entry.copy(sleepQuality = it.toInt())) },
                            valueRange = 1f..10f, steps = 8, label = stringResource(R.string.q_sleep_quality_label)
                        )
                    }
                    QuestionSection(title = stringResource(R.string.q_woke_from_device_title)) {
                        YesNoToggle(
                            value = entry.wokeUpDueToDevice,
                            onValueChange = { onUpdate(entry.copy(wokeUpDueToDevice = it)) },
                            label = stringResource(R.string.q_woke_from_device_label)
                        )
                    }
                    QuestionSection(title = stringResource(R.string.q_erotic_dream_title)) {
                        YesNoToggle(
                            value = entry.hadEroticDream,
                            onValueChange = { onUpdate(entry.copy(hadEroticDream = it)) },
                            label = stringResource(R.string.q_erotic_dream_label)
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
                            Text(stringResource(R.string.section_body_status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Divider()
                        QuestionSection(title = stringResource(R.string.q_morning_erection_title)) {
                            YesNoToggle(entry.morningErection, { onUpdate(entry.copy(morningErection = it)) }, stringResource(R.string.q_morning_erection_label))
                        }
                        QuestionSection(title = stringResource(R.string.q_night_erection_title), subtitle = stringResource(R.string.q_night_erection_subtitle)) {
                            val nightErectionLabels = stringArrayResource(R.array.night_erection_options_array)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                nightErectionLabels.forEachIndexed { index, label ->
                                    val value = Constants.NIGHT_ERECTION_SCORE_FOR_INDEX.getOrElse(index) { 0 }
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
                        QuestionSection(title = stringResource(R.string.q_woke_from_night_erection_title)) {
                            YesNoToggle(
                                value = entry.wokeUpFromErection,
                                onValueChange = { onUpdate(entry.copy(wokeUpFromErection = it)) },
                                label = stringResource(R.string.q_woke_from_night_erection_label)
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
                        Text(stringResource(R.string.section_morning_wakeup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Divider()
                    QuestionSection(title = stringResource(R.string.q_morning_mood_title)) {
                        MoodSelector(
                            selectedMood = entry.morningMood,
                            moods = stringArrayResource(R.array.moods_array).toList(),
                            moodKeys = Constants.MOODS,
                            onMoodSelected = { onUpdate(entry.copy(morningMood = it)) }
                        )
                    }
                    QuestionSection(title = stringResource(R.string.q_morning_energy_title), subtitle = stringResource(R.string.q_morning_energy_subtitle)) {
                        SliderWithLabel(
                            entry.morningEnergy?.toFloat() ?: 5f,
                            { onUpdate(entry.copy(morningEnergy = it.toInt())) },
                            valueRange = 1f..10f, steps = 8, label = stringResource(R.string.q_morning_energy_label)
                        )
                    }
                }
            }

            ExtendedQuestionsCard(entry = entry, onUpdate = onUpdate)

        } else {
            // ── 🌙 Evening cards ───────────────────────────────────────────────
            DayStatusCard(entry, selectedDate)
            CoreQuestionsCard(entry = entry, onUpdate = onUpdate, onTakePhoto = onTakePhoto, photoBlurEnabled = photoBlurEnabled)
            RotatingQuestionsCard(
                questions = remember(selectedDate, isMale) { getRotatingQuestionsForDate(selectedDate, isMale) },
                entry = entry,
                onUpdate = onUpdate
            )
            EveningMasturbationCard(entry = entry, onUpdate = onUpdate)
            ExtendedQuestionsCard(entry = entry, onUpdate = onUpdate)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Favorite, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.section_masturbation),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Divider()
            com.chastity.diary.ui.components.QuestionSection(
                title = stringResource(R.string.q_masturbated_title)
            ) {
                com.chastity.diary.ui.components.YesNoToggle(
                    value = entry.masturbated,
                    onValueChange = { v ->
                        onUpdate(entry.copy(
                            masturbated = v,
                            masturbationCount = if (!v) null else entry.masturbationCount
                        ))
                    },
                    label = stringResource(R.string.q_masturbated_label)
                )
                if (entry.masturbated) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.q_masturbation_count),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline()
                        )
                        androidx.compose.material3.IconButton(
                            onClick = {
                                val cur = entry.masturbationCount ?: 1
                                if (cur > 1) onUpdate(entry.copy(masturbationCount = cur - 1))
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease))
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
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase))
                        }
                    }
                }
            }
        }
    }
}

// ─── Daily Narrative ──────────────────────────────────────────────────────────
private fun generateDailyNarrative(context: Context, entry: DailyEntry): String {
    val parts = mutableListOf<String>()

    // Mood
    entry.mood?.let { parts.add(context.getString(R.string.narrative_mood, it)) }

    // Desire
    entry.desireLevel?.let {
        parts.add(when {
            it >= 8 -> context.getString(R.string.narrative_desire_high, it)
            it <= 3 -> context.getString(R.string.narrative_desire_low, it)
            else    -> context.getString(R.string.narrative_desire_normal, it)
        })
    }

    // Device worn
    if (entry.deviceCheckPassed) {
        entry.comfortRating?.let { r ->
            parts.add(when {
                r >= 8 -> context.getString(R.string.narrative_comfort_high, r)
                r <= 3 -> context.getString(R.string.narrative_comfort_low, r)
                else   -> context.getString(R.string.narrative_comfort_normal, r)
            })
        }
    } else {
        parts.add(context.getString(R.string.narrative_not_worn))
    }

    // Exercise
    if (entry.exercised) {
        val dur = entry.exerciseDuration
        parts.add(if (dur != null) context.getString(R.string.narrative_exercised_with_duration, dur)
                  else context.getString(R.string.narrative_exercised))
    }

    // Cleaning — uses localised "no cleaning" key
    val noCleaning = context.getString(R.string.narrative_cleaning_none)
    entry.cleaningType?.takeIf { it != noCleaning }?.let {
        parts.add(context.getString(R.string.narrative_cleaning, it))
    }

    // Keyholder
    if (entry.keyholderInteraction) parts.add(context.getString(R.string.narrative_keyholder))

    // Photo
    if (entry.photoPath != null) parts.add(context.getString(R.string.narrative_photo))

    // Unlocked
    if (entry.unlocked) parts.add(context.getString(R.string.narrative_unlocked))

    // Masturbation
    if (entry.masturbated) {
        val cnt = entry.masturbationCount ?: 1
        parts.add(context.getString(R.string.narrative_masturbated, cnt))
    }

    // Edging
    if (entry.hadEdging) parts.add(context.getString(R.string.narrative_edging))

    return if (parts.isEmpty()) context.getString(R.string.narrative_default)
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
