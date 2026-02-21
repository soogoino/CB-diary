package com.chastity.diary.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.chastity.diary.ui.components.*
import com.chastity.diary.util.Constants
import com.chastity.diary.viewmodel.DailyEntryViewModel
import com.chastity.diary.viewmodel.EntryFormState
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// ─── Rotating question pool ───────────────────────────────────────────────────
private enum class RotatingQuestion(val title: String, val subtitle: String) {
    LEAKAGE("今天是否有洩漏情況？", "體液/液體滲出"),
    ERECTION("今天是否有勃起？", "男性限定"),

    EDGING("今天是否進行邊緣訓練？", "包括任何刺激但未達高潮的行為"),
    KEYHOLDER("今天與Keyholder/伴侶有互動嗎？", "可多選互動類型"),
    CLEANING("今天是否清潔了貞操裝置？", "選擇清潔方式"),
    SOCIAL("今天的社交活動", "在公開場合佩戴的感受"),
    REMOVAL("今天是否短暫取下裝置？", "含原因與時長"),
}

private fun getRotatingQuestionsForDate(date: LocalDate): List<RotatingQuestion> {
    val pool = RotatingQuestion.entries
    val seed = date.dayOfYear
    val a = pool[seed % pool.size]
    val b = pool[(seed + 3) % pool.size].let { if (it == a) pool[(seed + 5) % pool.size] else it }
    return listOf(a, b)
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

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Camera
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraImageUri?.let { viewModel.updateEntry { e -> e.copy(photoPath = it.toString()) } }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) { val u = createCameraImageUri(context); cameraImageUri = u; cameraLauncher.launch(u) }
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
                MorningTabContent(
                    entry = entry,
                    onUpdate = { viewModel.updateEntry { _ -> it } },
                    onSave = { viewModel.saveMorningCheck() },
                    outerPadding = outerPadding
                )
            } else {
                // ── Evening Tab ────────────────────────────────────────────────
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
                    DayStatusCard(entry, selectedDate)
                    CoreQuestionsCard(
                        entry = entry,
                        onUpdate = { viewModel.updateEntry { _ -> it } },
                        onTakePhoto = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                    val score = coreCompletionScore(entry)
                    AnimatedVisibility(visible = score >= 2) {
                        RealtimeFeedbackCard(entry, score)
                    }
                    RotatingQuestionsCard(
                        questions = remember(selectedDate) { getRotatingQuestionsForDate(selectedDate) },
                        entry = entry,
                        onUpdate = { viewModel.updateEntry { _ -> it } }
                    )
                    ExtendedQuestionsCard(
                        entry = entry,
                        onUpdate = { viewModel.updateEntry { _ -> it } }
                    )
                    if (isExisting) {
                        val loaded = (entryState as EntryFormState.Loaded).entry
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("記錄信息", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("建立：${loaded.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("更新：${loaded.updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Button(
                        onClick = { viewModel.saveEntry() },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isExisting) "更新記錄" else "儲存今日記錄",
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
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
    onTakePhoto: () -> Unit
) {
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

            // C1: Mood
            QuestionSection(title = "今天心情如何？") {
                MoodSelector(
                    selectedMood = entry.mood,
                    moods = Constants.MOODS,
                    onMoodSelected = { onUpdate(entry.copy(mood = it)) }
                )
            }

            // C2: Device worn (BRANCHING ROOT)
            QuestionSection(title = "今天有佩戴裝置嗎？") {
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

            // C3: Desire level
            QuestionSection(title = "今日性慾強度", subtitle = "1 = 很低   10 = 很強烈") {
                SliderWithLabel(entry.desireLevel?.toFloat() ?: 5f,
                    { onUpdate(entry.copy(desireLevel = it.toInt())) },
                    valueRange = 1f..10f, steps = 8, label = "性慾指數")
            }

            // C4: Comfort (只在佩戴時)
            AnimatedVisibility(visible = entry.deviceCheckPassed) {
                QuestionSection(title = "佩戴舒適度", subtitle = "整天佩戴裝置的感受") {
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

            // C6: Emotions
            QuestionSection(title = "今天的情緒狀態", subtitle = "可多選") {
                MultiSelectChipGroup(
                    options = Constants.EMOTIONS,
                    selectedOptions = entry.emotions,
                    onSelectionChange = { onUpdate(entry.copy(emotions = it)) }
                )
            }

            // Photo check-in
            Divider()
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
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
                OutlinedButton(onClick = onTakePhoto) {
                    Icon(Icons.Default.Camera, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (entry.photoPath.isNullOrBlank()) "拍照" else "重拍")
                }
            }
            if (!entry.photoPath.isNullOrBlank()) {
                val bitmap = remember(entry.photoPath) {
                    runCatching {
                        val f = File(Uri.parse(entry.photoPath).path ?: entry.photoPath!!)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                    }.getOrNull()
                }
                bitmap?.let {
                    Image(it.asImageBitmap(), "打卡照片",
                        Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
                }
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
                it <= 3 -> add("🧠 專注度 $it/10，裝置可能影響日常表現，留意調整。")
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
    QuestionSection(title = q.title, subtitle = q.subtitle) {
        when (q) {
            RotatingQuestion.LEAKAGE -> {
                YesNoToggle(entry.hadLeakage, { onUpdate(entry.copy(hadLeakage = it)) }, "有洩漏")
                AnimatedVisibility(entry.hadLeakage) {
                    MultiSelectChipGroup(
                        options = Constants.LEAKAGE_AMOUNTS,
                        selectedOptions = entry.leakageAmount?.let { listOf(it) } ?: emptyList(),
                        onSelectionChange = { onUpdate(entry.copy(leakageAmount = it.firstOrNull())) }
                    )
                }
            }
            RotatingQuestion.ERECTION -> {
                YesNoToggle(entry.hadErection, { onUpdate(entry.copy(hadErection = it)) }, "有勃起")
            }
            RotatingQuestion.EDGING -> {
                YesNoToggle(entry.hadEdging, { onUpdate(entry.copy(hadEdging = it)) }, "有進行")
                AnimatedVisibility(entry.hadEdging) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DurationPicker(entry.edgingDuration, { onUpdate(entry.copy(edgingDuration = it)) }, label = "時長")
                        MultiSelectChipGroup(
                            options = Constants.EDGING_METHODS,
                            selectedOptions = entry.edgingMethods,
                            onSelectionChange = { onUpdate(entry.copy(edgingMethods = it)) }
                        )
                    }
                }
            }
            RotatingQuestion.KEYHOLDER -> {
                YesNoToggle(entry.keyholderInteraction,
                    { onUpdate(entry.copy(keyholderInteraction = it)) }, "有互動")
                AnimatedVisibility(entry.keyholderInteraction) {
                    MultiSelectChipGroup(
                        options = Constants.INTERACTION_TYPES,
                        selectedOptions = entry.interactionTypes,
                        onSelectionChange = { onUpdate(entry.copy(interactionTypes = it)) }
                    )
                }
            }
            RotatingQuestion.CLEANING -> {
                MultiSelectChipGroup(
                    options = Constants.CLEANING_TYPES,
                    selectedOptions = entry.cleaningType?.let { listOf(it) } ?: emptyList(),
                    onSelectionChange = { onUpdate(entry.copy(cleaningType = it.firstOrNull())) }
                )
            }
            RotatingQuestion.SOCIAL -> {
                MultiSelectChipGroup(
                    options = Constants.SOCIAL_ACTIVITIES,
                    selectedOptions = entry.socialActivities,
                    onSelectionChange = { onUpdate(entry.copy(socialActivities = it)) }
                )
                AnimatedVisibility(entry.socialActivities.isNotEmpty()) {
                    SliderWithLabel(entry.socialAnxiety?.toFloat() ?: 1f,
                        { onUpdate(entry.copy(socialAnxiety = it.toInt())) },
                        label = "被發現的焦慮感 (1=毫不擔心 10=極度焦慮)")
                }
            }
            RotatingQuestion.REMOVAL -> {
                YesNoToggle(entry.temporarilyRemoved,
                    { onUpdate(entry.copy(temporarilyRemoved = it)) }, "有取下")
                AnimatedVisibility(entry.temporarilyRemoved) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DurationPicker(entry.removalDuration,
                            { onUpdate(entry.copy(removalDuration = it)) }, label = "取下時長")
                        MultiSelectChipGroup(
                            options = Constants.REMOVAL_REASONS,
                            selectedOptions = entry.removalReasons,
                            onSelectionChange = { onUpdate(entry.copy(removalReasons = it)) }
                        )
                    }
                }
            }
        }
    }
}

// ─── ⑤ Extended Questions (expandable) ───────────────────────────────────────
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
                Text(if (expanded) "收起詳細記錄" else "我想記錄更多 →",
                    style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text("選填", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Divider()

                    // Porn
                    QuestionSection("今天是否接觸色情內容？") {
                        YesNoToggle(entry.viewedPorn, { onUpdate(entry.copy(viewedPorn = it)) }, "有接觸")
                        AnimatedVisibility(entry.viewedPorn) {
                            DurationPicker(entry.pornDuration, { onUpdate(entry.copy(pornDuration = it)) }, label = "觀看時長")
                        }
                    }

                    // Exercise
                    QuestionSection("是否運動？") {
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

                    // Unlock / Masturbation
                    QuestionSection("是否解鎖？是否自慰？") {
                        YesNoToggle(entry.unlocked, { onUpdate(entry.copy(unlocked = it)) }, "解鎖")
                        YesNoToggle(entry.masturbated, { onUpdate(entry.copy(masturbated = it)) }, "自慰")
                        AnimatedVisibility(entry.masturbated) {
                            DurationPicker(entry.masturbationDuration,
                                { onUpdate(entry.copy(masturbationDuration = it)) }, label = "持續時長")
                        }
                    }

                    // Exposed lock
                    QuestionSection("是否露出貞操鎖？") {
                        YesNoToggle(entry.exposedLock, { onUpdate(entry.copy(exposedLock = it)) }, "有露出")
                        AnimatedVisibility(entry.exposedLock) {
                            MultiSelectChipGroup(
                                options = Constants.EXPOSED_LOCATIONS,
                                selectedOptions = entry.exposedLocations,
                                onSelectionChange = { onUpdate(entry.copy(exposedLocations = it)) }
                            )
                        }
                    }

                    // Discomfort (only if wearing)
                    AnimatedVisibility(visible = entry.deviceCheckPassed) {
                        QuestionSection("是否有不適或疼痛？") {
                            YesNoToggle(entry.hasDiscomfort,
                                { onUpdate(entry.copy(hasDiscomfort = it)) }, "有不適")
                            AnimatedVisibility(entry.hasDiscomfort) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MultiSelectChipGroup(
                                        options = Constants.DISCOMFORT_AREAS,
                                        selectedOptions = entry.discomfortAreas,
                                        onSelectionChange = { onUpdate(entry.copy(discomfortAreas = it)) }
                                    )
                                    SliderWithLabel(entry.discomfortLevel?.toFloat() ?: 5f,
                                        { onUpdate(entry.copy(discomfortLevel = it.toInt())) },
                                        label = "疼痛程度")
                                }
                            }
                        }
                    }

                    // Self rating
                    QuestionSection("今天的自我評價") {
                        StarRating(entry.selfRating ?: 3,
                            { onUpdate(entry.copy(selfRating = it)) }, label = "自我評分")
                    }

                    // Notes
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

// ─── Morning Tab Content ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorningTabContent(
    entry: DailyEntry,
    onUpdate: (DailyEntry) -> Unit,
    onSave: () -> Unit,
    outerPadding: PaddingValues
) {
    var showBedtimePicker by remember { mutableStateOf(false) }
    var showWakeTimePicker by remember { mutableStateOf(false) }
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

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
        // ── Completion banner ──────────────────────────────────────────────────
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

        // ── 🛏 Sleep Card ──────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Nightlight, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("睡眠記錄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Divider()

                // Bedtime
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
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

                // Wake time
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
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

                // Auto-calculated sleep duration
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
                        Row(Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                            Text("睡眠時長：${h}小時${if (m > 0) " ${m}分" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                Divider()

                // Sleep quality
                QuestionSection(title = "睡眠品質", subtitle = "整體睡眠感受") {
                    StarRating(
                        rating = entry.sleepQuality ?: 3,
                        onRatingChange = { onUpdate(entry.copy(sleepQuality = it)) },
                        label = "睡眠品質"
                    )
                }

                // Woke due to device
                YesNoToggle(
                    value = entry.wokeUpDueToDevice,
                    onValueChange = { onUpdate(entry.copy(wokeUpDueToDevice = it)) },
                    label = "因佩戴裝置而醒來"
                )
            }
        }

        // ── 💪 Body Card ───────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("身體狀況", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    AssistChip(onClick = {}, label = { Text("男性參考", style = MaterialTheme.typography.labelSmall) })
                }
                Divider()

                QuestionSection(title = "晨勃") {
                    YesNoToggle(entry.morningErection, { onUpdate(entry.copy(morningErection = it)) }, "有晨勃")
                }

                QuestionSection(title = "昨晚夜間勃起次數", subtitle = "0 = 無，可能因勃起而醒來") {
                    SliderWithLabel(
                        value = entry.nightErections?.toFloat() ?: 0f,
                        onValueChange = { onUpdate(entry.copy(nightErections = it.toInt())) },
                        valueRange = 0f..10f, steps = 9, label = "次數",
                        valueFormatter = { "${it.toInt()} 次" }
                    )
                }

                YesNoToggle(
                    value = entry.wokeUpFromErection,
                    onValueChange = { onUpdate(entry.copy(wokeUpFromErection = it)) },
                    label = "因夜間勃起而醒來"
                )
            }
        }

        // ── 😊 Morning Mood Card ───────────────────────────────────────────────
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

                QuestionSection(title = "起床能量指數", subtitle = "1 = 極度疲憊   5 = 精力充沛") {
                    StarRating(
                        rating = entry.morningEnergy ?: 3,
                        onRatingChange = { onUpdate(entry.copy(morningEnergy = it)) },
                        label = "能量指數",
                        maxStars = 5
                    )
                }
            }
        }

        // ── Save button ────────────────────────────────────────────────────────
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (entry.morningCheckDone) "更新早晨記錄" else "完成早晨記錄",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    // ── Time pickers ───────────────────────────────────────────────────────────
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
