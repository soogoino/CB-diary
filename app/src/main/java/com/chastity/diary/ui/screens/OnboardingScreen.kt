@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.chastity.diary.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.ui.components.DatePickerDialog
import com.chastity.diary.ui.components.TimePickerDialog
import com.chastity.diary.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── Entry Point ──────────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = viewModel(),
    onComplete: () -> Unit = {}
) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val isExistingUser by viewModel.isExistingUser.collectAsState()

    when {
        isOnboardingCompleted == null -> {
            // Loading — DataStore not yet read
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        isOnboardingCompleted == true -> {
            // Should not normally land here; safety net
            LaunchedEffect(Unit) { onComplete() }
        }
        isExistingUser -> {
            // Existing users (upgrading): show a brief "what's new" page
            ExistingUserWelcomePage(
                onContinue = { viewModel.skip(); onComplete() }
            )
        }
        else -> {
            // Fresh install → full 5-page onboarding
            NewUserOnboarding(
                viewModel = viewModel,
                onComplete = { viewModel.completeOnboarding(); onComplete() },
                onSkip = { viewModel.skip(); onComplete() }
            )
        }
    }
}

// ─── Existing User ─────────────────────────────────────────────────────────────
@Composable
private fun ExistingUserWelcomePage(onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text("全新升級 ✨", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "• 每日記錄 — 漸進式卡片設計\n" +
                "• 每日輪換題目（8 題池）\n" +
                "• 即時反饋與情緒追蹤\n" +
                "• CSV 匯出 / 匯入\n" +
                "• 個人資料與暱稱",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("進入應用程式", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─── New User Onboarding ───────────────────────────────────────────────────────
@Composable
private fun NewUserOnboarding(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 5 }
    val currentPage = pagerState.currentPage
    val totalPages = 5

    val progress by animateFloatAsState(
        targetValue = (currentPage + 1).toFloat() / totalPages,
        label = "progress"
    )

    Scaffold(
        topBar = {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onSkip) { Text("跳過") }
                }
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = false    // Prevent swipe; use buttons only
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> ProfilePage(viewModel)
                    2 -> DevicePage(viewModel)
                    3 -> SecurityPage(viewModel)
                    4 -> ReminderPage(viewModel, onComplete)
                }
            }

            // Navigation buttons
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("上一步") }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (currentPage < totalPages - 1) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(currentPage + 1) } },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("下一步") }
                }
                // Page 4 has its own "完成" button inside ReminderPage
            }
        }
    }
}

// ─── Page 0: Welcome ───────────────────────────────────────────────────────────
@Composable
private fun WelcomePage() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(32.dp))
        Text("歡迎使用\nChastity Diary",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            "記錄你的貞操佩戴旅程\n追蹤每日感受與成長",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeatureItem(Icons.Default.BarChart, "每日追蹤")
            FeatureItem(Icons.Default.EmojiEvents, "連續記錄")
            FeatureItem(Icons.Default.Shield, "安全私密")
        }
    }
}

@Composable
private fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Page 1: Profile ──────────────────────────────────────────────────────────
@Composable
private fun ProfilePage(viewModel: OnboardingViewModel) {
    val nickname by viewModel.nickname.collectAsState()
    val gender by viewModel.gender.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader(Icons.Default.Person, "個人身份", "讓我們認識你一點（可跳過此頁）")

        // Nickname
        OutlinedTextField(
            value = nickname,
            onValueChange = { viewModel.nickname.value = it },
            label = { Text("暱稱") },
            placeholder = { Text("例如：小鎖") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            leadingIcon = { Icon(Icons.Default.Badge, null) }
        )

        // Gender
        Text("性別", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.values().forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { viewModel.gender.value = g },
                    label = {
                        Text(when (g) {
                            Gender.MALE -> "男性 ♂"
                            Gender.FEMALE -> "女性 ♀"
                            Gender.OTHER -> "其他"
                        })
                    },
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
        }

        Divider()

        // Optional body stats
        Text("體型資料（選填）",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        val height by viewModel.height.collectAsState()
        val weight by viewModel.weight.collectAsState()

        // Height slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("身高", style = MaterialTheme.typography.bodyMedium)
                Text(if (height != null) "$height cm" else "未設定",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = (height ?: 170).toFloat(),
                onValueChange = { viewModel.height.value = it.toInt() },
                valueRange = 100f..250f,
                steps = 149
            )
        }

        // Weight slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("體重", style = MaterialTheme.typography.bodyMedium)
                Text(if (weight != null) "${"%.1f".format(weight)} kg" else "未設定",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = (weight ?: 65f),
                onValueChange = { viewModel.weight.value = it },
                valueRange = 30f..200f,
                steps = 169
            )
        }
    }
}

// ─── Page 2: Device ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicePage(viewModel: OnboardingViewModel) {
    val deviceName by viewModel.deviceName.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader(Icons.Default.Lock, "貞操鎖", "記錄你的鎖資訊（可跳過此頁）")

        OutlinedTextField(
            value = deviceName,
            onValueChange = { viewModel.deviceName.value = it },
            label = { Text("鎖名稱") },
            placeholder = { Text("例如：CB-6000、Bon4 ...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.DeviceHub, null) }
        )

        // Start date picker
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.CalendarToday, null,
                    tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("鎖定開始日期", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Text(
                        startDate?.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
                            ?: "點選選擇日期",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (startDate != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (startDate != null) {
            val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.EmojiEvents, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        if (days >= 0) "已佩戴 $days 天 🎉" else "即將開始！",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = startDate ?: LocalDate.now(),
            onConfirm = { viewModel.startDate.value = it; showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
}

// ─── Page 3: Security ─────────────────────────────────────────────────────────
@Composable
private fun SecurityPage(viewModel: OnboardingViewModel) {
    val biometric by viewModel.biometricEnabled.collectAsState()
    val pin by viewModel.pinEnabled.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader(Icons.Default.Security, "安全與隱私", "保護你的記錄（可跳過此頁）")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SecurityRow(
                    icon = Icons.Default.Fingerprint,
                    title = "生物辨識鎖定",
                    subtitle = "使用指紋或面部辨識解鎖",
                    checked = biometric,
                    onCheckedChange = { viewModel.biometricEnabled.value = it }
                )
                Divider()
                SecurityRow(
                    icon = Icons.Default.Pin,
                    title = "PIN 碼鎖定",
                    subtitle = "使用數字密碼解鎖（可在設定中設置）",
                    checked = pin,
                    onCheckedChange = { viewModel.pinEnabled.value = it }
                )
            }
        }

        AnimatedVisibility(visible = biometric || pin) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp))
                    Text(
                        "詳細的安全設定可在設定頁面調整",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ─── Page 4: Reminder ─────────────────────────────────────────────────────────
@Composable
private fun ReminderPage(viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader(Icons.Default.Notifications, "每日提醒", "設定每日記錄提醒時間")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, null,
                        tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("啟用每日提醒", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Text("每天在選定時間提醒你記錄", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = reminderEnabled, onCheckedChange = { viewModel.reminderEnabled.value = it })
                }

                AnimatedVisibility(visible = reminderEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Divider()
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("提醒時間", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "%02d:%02d".format(reminderHour, reminderMinute),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Default.Edit, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    } // end Column inside AnimatedVisibility
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Complete button
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null)
            Spacer(Modifier.width(8.dp))
            Text("完成設定，開始記錄！", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                viewModel.reminderHour.value = h
                viewModel.reminderMinute.value = m
                showTimePicker = false
            },
            initialHour = reminderHour,
            initialMinute = reminderMinute
        )
    }
}

// ─── Shared ────────────────────────────────────────────────────────────────────
@Composable
private fun PageHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
