package com.chastity.diary.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.domain.model.DarkMode
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.ui.components.PinSetupDialog
import com.chastity.diary.ui.components.ProfileEditDialog
import com.chastity.diary.ui.components.TimePickerDialog
import com.chastity.diary.utils.BiometricHelper
import com.chastity.diary.viewmodel.SettingsViewModel

/**
 * Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    outerPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val testDataMessage by viewModel.testDataMessage.collectAsState()
    val exportImportMessage by viewModel.exportImportMessage.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showBiometricWarning by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricHelper = remember { BiometricHelper(context) }

    // SAF launchers for CSV export/import
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsv(it) }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importCsv(it) }
    }
    
    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, enable reminder
            viewModel.updateReminderSettings(
                true,
                userSettings.reminderHour,
                userSettings.reminderMinute
            )
        } else {
            // Permission denied, show message
            snackbarHostState.currentSnackbarData?.dismiss()
            viewModel.updateReminderSettings(
                false,
                userSettings.reminderHour,
                userSettings.reminderMinute
            )
        }
    }
    
    // Check notification permission
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed for Android 12 and below
        }
    }
    
    // Show snackbar when test data message changes
    LaunchedEffect(testDataMessage) {
        testDataMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearTestDataMessage()
        }
    }

    LaunchedEffect(exportImportMessage) {
        exportImportMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearExportImportMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = outerPadding.calculateBottomPadding() + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 個人資料（包含偏好設定）──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("個人資料", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showProfileDialog = true }) { Text("編輯") }
                    }

                    if (!userSettings.nickname.isNullOrBlank())
                        Text("暱稱: ${userSettings.nickname}", style = MaterialTheme.typography.bodyMedium)

                    Text(
                        text = "開始日期: ${userSettings.startDate?.toString() ?: "未設定"}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // 生理性別（直接可點）
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("生理性別", style = MaterialTheme.typography.bodyMedium)
                        val genderOptions = listOf(
                            Gender.MALE to "男性 ♂",
                            Gender.FEMALE to "女性 ♀",
                            Gender.OTHER to "其他"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            genderOptions.forEach { (gender, label) ->
                                FilterChip(
                                    selected = userSettings.gender == gender,
                                    onClick = { viewModel.updateGender(gender) },
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

                    if (userSettings.height != null)
                        Text("身高: ${userSettings.height} cm", style = MaterialTheme.typography.bodyMedium)
                    if (userSettings.weight != null)
                        Text("體重: ${userSettings.weight} kg", style = MaterialTheme.typography.bodyMedium)
                    if (userSettings.bmi != null)
                        Text(
                            "BMI: ${String.format("%.1f", userSettings.bmi)} (${userSettings.bmiStatus ?: ""})",
                            style = MaterialTheme.typography.bodyMedium
                        )

                    if (!userSettings.currentDeviceName.isNullOrBlank()) {
                        val sizeLabel = if (!userSettings.currentDeviceSize.isNullOrBlank())
                            " (${userSettings.currentDeviceSize})"
                        else ""
                        Text("貞操鎖: ${userSettings.currentDeviceName}$sizeLabel",
                            style = MaterialTheme.typography.bodyMedium)
                    }

                    if (userSettings.height == null && userSettings.weight == null &&
                        userSettings.currentDeviceName.isNullOrBlank() &&
                        userSettings.nickname.isNullOrBlank() && userSettings.startDate == null)
                        Text("尚未設定個人資料", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            // ☀️ 早安提醒
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var showMorningTimePicker by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "早安提醒",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "☀️ 早安提醒",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (userSettings.morningReminderEnabled) "已啟用" else "已停用",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.morningReminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateMorningReminderSettings(
                                    enabled,
                                    userSettings.morningReminderHour,
                                    userSettings.morningReminderMinute
                                )
                            }
                        )
                    }

                    if (userSettings.morningReminderEnabled) {
                        Divider()
                        OutlinedButton(
                            onClick = { showMorningTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("提醒時間: ${String.format("%02d:%02d", userSettings.morningReminderHour, userSettings.morningReminderMinute)}")
                        }
                    }

                    if (showMorningTimePicker) {
                        TimePickerDialog(
                            onDismiss = { showMorningTimePicker = false },
                            onConfirm = { h, m ->
                                viewModel.updateMorningReminderSettings(true, h, m)
                                showMorningTimePicker = false
                            },
                            initialHour = userSettings.morningReminderHour,
                            initialMinute = userSettings.morningReminderMinute
                        )
                    }
                }
            }

            // 🌙 晚安提醒
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var showEveningTimePicker by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "晚安提醒",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "🌙 晚安提醒",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (userSettings.reminderEnabled) "已啟用" else "已停用",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (hasNotificationPermission()) {
                                        viewModel.updateReminderSettings(
                                            true,
                                            userSettings.reminderHour,
                                            userSettings.reminderMinute
                                        )
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.updateReminderSettings(
                                        false,
                                        userSettings.reminderHour,
                                        userSettings.reminderMinute
                                    )
                                }
                            }
                        )
                    }

                    if (userSettings.reminderEnabled) {
                        Divider()
                        OutlinedButton(
                            onClick = { showEveningTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "設定時間",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("提醒時間: ${String.format("%02d:%02d", userSettings.reminderHour, userSettings.reminderMinute)}")
                        }
                    }

                    if (showEveningTimePicker) {
                        TimePickerDialog(
                            onDismiss = { showEveningTimePicker = false },
                            onConfirm = { h, m ->
                                viewModel.updateReminderSettings(true, h, m)
                                showEveningTimePicker = false
                            },
                            initialHour = userSettings.reminderHour,
                            initialMinute = userSettings.reminderMinute
                        )
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "安全設定",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Divider()
                    
                    // Biometric authentication
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "生物辨識",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "生物辨識解鎖",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (biometricHelper.isBiometricAvailable()) 
                                        "使用指紋或臉部識別"
                                    else 
                                        "裝置不支援生物辨識",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.biometricEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !biometricHelper.isBiometricAvailable()) {
                                    showBiometricWarning = true
                                } else {
                                    viewModel.updateBiometricEnabled(enabled)
                                }
                            },
                            enabled = biometricHelper.isBiometricAvailable()
                        )
                    }
                    
                    Divider()
                    
                    // PIN code authentication
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "PIN 碼",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "PIN 碼鎖定",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (userSettings.pinEnabled) "已設定" else "未設定",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.pinEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // Show PIN setup dialog
                                    showPinSetupDialog = true
                                } else {
                                    // Disable PIN
                                    viewModel.updatePinEnabled(false)
                                }
                            }
                        )
                    }
                    
                    if (userSettings.pinEnabled) {
                        OutlinedButton(
                            onClick = { showPinSetupDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("修改 PIN 碼")
                        }
                    }
                    
                    if (userSettings.biometricEnabled || userSettings.pinEnabled) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "應用程式已啟用鎖定保護，每次進入時需要驗證",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Divider()

                    // Photo blur toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "照片模糊",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "照片預設模糊",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "瀏覽記錄時打卡照片自動模糊",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.photoBlurEnabled,
                            onCheckedChange = { viewModel.updatePhotoBlurEnabled(it) }
                        )
                    }
                }
            }
            
            // Interface settings
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "界面設定",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = "主題模式: ${userSettings.darkMode.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Data management
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "資料管理",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // TODO: 雲端同步功能待實作
                    /*
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("雲端同步")
                        Switch(
                            checked = userSettings.cloudSyncEnabled,
                            onCheckedChange = { viewModel.updateCloudSyncEnabled(it) }
                        )
                    }
                    
                    if (userSettings.lastSyncTime != null) {
                        Text(
                            text = "上次同步: ${userSettings.lastSyncTime}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    */
                    
                    Button(
                        onClick = {
                            val ts = java.time.LocalDate.now().toString().replace("-", "")
                            exportCsvLauncher.launch("diary_export_$ts.csv")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("匯出資料 (CSV)")
                    }

                    OutlinedButton(
                        onClick = {
                            importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("匯入資料 (CSV)")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.generateTestData() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("生成測試數據 (30天)")
                    }
                }
            }
            
            // About
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "關於",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "貞操日記 v1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Profile edit dialog
        if (showProfileDialog) {
            ProfileEditDialog(
                currentNickname = userSettings.nickname,
                currentStartDate = userSettings.startDate,
                currentHeight = userSettings.height,
                currentWeight = userSettings.weight,
                currentDeviceName = userSettings.currentDeviceName,
                currentDeviceSize = userSettings.currentDeviceSize,
                onDismiss = { showProfileDialog = false },
                onSave = { nickname, startDate, height, weight, deviceName, deviceSize ->
                    viewModel.updateNickname(nickname)
                    startDate?.let { viewModel.updateStartDate(it) }
                    viewModel.updateHeight(height)
                    viewModel.updateWeight(weight)
                    viewModel.updateCurrentDeviceName(deviceName)
                    viewModel.updateCurrentDeviceSize(deviceSize)
                    showProfileDialog = false
                }
            )
        }
        
        // PIN setup dialog
        if (showPinSetupDialog) {
            PinSetupDialog(
                onDismiss = { 
                    showPinSetupDialog = false
                    // If user cancels, also disable PIN switch
                    if (!userSettings.pinEnabled) {
                        viewModel.updatePinEnabled(false)
                    }
                },
                onConfirm = { newPin ->
                    viewModel.updatePinCode(newPin)
                    viewModel.updatePinEnabled(true)
                    showPinSetupDialog = false
                },
                isChangingPin = userSettings.pinEnabled
            )
        }
        
        // Biometric warning dialog
        if (showBiometricWarning) {
            AlertDialog(
                onDismissRequest = { showBiometricWarning = false },
                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                title = { Text("生物辨識不可用") },
                text = { Text("您的裝置不支援生物辨識功能，或未設定指紋/臉部識別。請先在系統設定中設定生物辨識。") },
                confirmButton = {
                    TextButton(onClick = { showBiometricWarning = false }) {
                        Text("確定")
                    }
                }
            )
        }
        
    }
}
