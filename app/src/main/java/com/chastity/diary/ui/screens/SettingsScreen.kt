package com.chastity.diary.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
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
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.R
import androidx.compose.ui.res.stringResource
import com.chastity.diary.BuildConfig
import com.chastity.diary.domain.model.AppLanguage
import com.chastity.diary.domain.model.DarkMode
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.ui.components.PinSetupDialog
import com.chastity.diary.ui.components.ProfileEditDialog
import com.chastity.diary.ui.components.TimePickerDialog
import com.chastity.diary.util.BiometricHelper
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
                title = { Text(stringResource(R.string.settings_title)) }
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
                        Text(stringResource(R.string.settings_profile), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showProfileDialog = true }) { Text(stringResource(R.string.settings_edit)) }
                    }

                    if (!userSettings.nickname.isNullOrBlank())
                        Text(stringResource(R.string.settings_nickname_prefix, userSettings.nickname!!), style = MaterialTheme.typography.bodyMedium)

                    Text(
                        text = stringResource(R.string.settings_start_date_prefix, userSettings.startDate?.toString() ?: stringResource(R.string.settings_start_date_not_set)),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Gender chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.settings_gender), style = MaterialTheme.typography.bodyMedium)
                        val genderOptions = listOf(
                            Gender.MALE to stringResource(R.string.settings_gender_male),
                            Gender.FEMALE to stringResource(R.string.settings_gender_female),
                            Gender.OTHER to stringResource(R.string.settings_gender_other)
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
                        Text(stringResource(R.string.settings_height_format, userSettings.height.toString()), style = MaterialTheme.typography.bodyMedium)
                    if (userSettings.weight != null)
                        Text(stringResource(R.string.settings_weight_format, userSettings.weight.toString()), style = MaterialTheme.typography.bodyMedium)

                    if (!userSettings.currentDeviceName.isNullOrBlank()) {
                        val sizeLabel = if (!userSettings.currentDeviceSize.isNullOrBlank())
                            " (${userSettings.currentDeviceSize})"
                        else ""
                        Text(stringResource(R.string.settings_device_format, "${userSettings.currentDeviceName}$sizeLabel"),
                            style = MaterialTheme.typography.bodyMedium)
                    }

                    if (userSettings.height == null && userSettings.weight == null &&
                        userSettings.currentDeviceName.isNullOrBlank() &&
                        userSettings.nickname.isNullOrBlank() && userSettings.startDate == null)
                        Text(stringResource(R.string.settings_no_profile), style = MaterialTheme.typography.bodySmall,
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
                                contentDescription = stringResource(R.string.settings_reminder_icon_desc),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_morning_reminder),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (userSettings.morningReminderEnabled) stringResource(R.string.settings_reminder_enabled) else stringResource(R.string.settings_reminder_disabled),
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
                            Text(stringResource(R.string.settings_reminder_time_prefix, String.format("%02d:%02d", userSettings.morningReminderHour, userSettings.morningReminderMinute)))
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
                                contentDescription = stringResource(R.string.settings_goodnight_reminder_icon_desc),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_goodnight_reminder),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (userSettings.reminderEnabled) stringResource(R.string.settings_reminder_enabled) else stringResource(R.string.settings_reminder_disabled),
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
                                contentDescription = stringResource(R.string.settings_time_icon_desc),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_reminder_time_prefix, String.format("%02d:%02d", userSettings.reminderHour, userSettings.reminderMinute)))
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
                        text = stringResource(R.string.settings_security),
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
                                contentDescription = stringResource(R.string.settings_biometric_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_biometric_unlock),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (biometricHelper.isBiometricAvailable())
                                        stringResource(R.string.settings_biometric_desc)
                                    else
                                        stringResource(R.string.settings_biometric_unsupported),
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
                                contentDescription = stringResource(R.string.settings_pin_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_pin_lock),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (userSettings.pinEnabled) stringResource(R.string.settings_pin_set) else stringResource(R.string.settings_pin_not_set),
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
                            Text(stringResource(R.string.settings_pin_change))
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
                                text = stringResource(R.string.settings_lock_desc),
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
                                contentDescription = stringResource(R.string.settings_photo_blur_cd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_photo_blur),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_photo_blur_desc),
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
                        text = stringResource(R.string.settings_ui),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // T3: Dark mode selector — three-option FilterChip row
                    val darkModeOptions = listOf(
                        Triple(DarkMode.LIGHT,  Icons.Default.LightMode,  stringResource(R.string.settings_theme_light)),
                        Triple(DarkMode.DARK,   Icons.Default.DarkMode,   stringResource(R.string.settings_theme_dark)),
                        Triple(DarkMode.SYSTEM, Icons.Default.Brightness6, stringResource(R.string.settings_theme_system))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        darkModeOptions.forEach { (mode, icon, label) ->
                            FilterChip(
                                selected = userSettings.darkMode == mode,
                                onClick = { viewModel.updateDarkMode(mode) },
                                label = { Text(label) },
                                leadingIcon = {
                                    Icon(icon, contentDescription = label,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize))
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                )
                            )
                        }
                    }
                }
            }

            // Language settings
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium
                    )

                    val languageOptions = listOf(
                        Triple(AppLanguage.ENGLISH,              "en",    stringResource(R.string.settings_language_english)),
                        Triple(AppLanguage.TRADITIONAL_CHINESE,  "zh-TW", stringResource(R.string.settings_language_traditional_chinese))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languageOptions.forEach { (lang, tag, label) ->
                            FilterChip(
                                selected = userSettings.language == lang,
                                onClick = {
                                    viewModel.updateLanguage(lang)
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(tag)
                                    )
                                },
                                label = { Text(label) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = label,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
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
                        text = stringResource(R.string.settings_data_management),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // T2: Cloud sync is planned for a future release.
                    // Firebase integration requires a real google-services.json and
                    // privacy policy review. Showing an informational row until ready.
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WbSunny, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                stringResource(R.string.settings_cloud_coming_soon),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val ts = java.time.LocalDate.now().toString().replace("-", "")
                            exportCsvLauncher.launch("diary_export_$ts.csv")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_export_csv))
                    }

                    OutlinedButton(
                        onClick = {
                            importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_import_csv))
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
                        text = stringResource(R.string.settings_about),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_version_format, BuildConfig.VERSION_NAME),
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
                title = { Text(stringResource(R.string.biometric_unavailable_title)) },
                text = { Text(stringResource(R.string.biometric_unavailable_message)) },
                confirmButton = {
                    TextButton(onClick = { showBiometricWarning = false }) {
                        Text(stringResource(R.string.confirm_ok))
                    }
                }
            )
        }
        
    }
}
