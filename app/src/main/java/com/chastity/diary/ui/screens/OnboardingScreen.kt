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
import com.chastity.diary.R
import com.chastity.diary.ui.components.DatePickerDialog
import com.chastity.diary.ui.components.PinSetupDialog
import com.chastity.diary.ui.components.TimePickerDialog
import com.chastity.diary.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource

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
            Text(stringResource(R.string.onboarding_new_upgrade), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.onboarding_upgrade_features),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.onboarding_get_started), style = MaterialTheme.typography.titleMedium)
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
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
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
                    4 -> ReminderPage(viewModel)
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
                    ) { Text(stringResource(R.string.onboarding_back)) }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (currentPage < totalPages - 1) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(currentPage + 1) } },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text(stringResource(R.string.onboarding_next)) }
                } else {
                    // Last page: Complete button unified in nav bar
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.onboarding_finish), style = MaterialTheme.typography.titleSmall)
                    }
                }
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
        Text(stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeatureItem(Icons.Default.BarChart, stringResource(R.string.welcome_feat_daily))
            FeatureItem(Icons.Default.EmojiEvents, stringResource(R.string.welcome_feat_streak))
            FeatureItem(Icons.Default.Shield, stringResource(R.string.welcome_feat_private))
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
        PageHeader(Icons.Default.Person, stringResource(R.string.profile_page_title), stringResource(R.string.profile_page_subtitle))

        // Nickname
        OutlinedTextField(
            value = nickname,
            onValueChange = { viewModel.nickname.value = it },
            label = { Text(stringResource(R.string.profile_nickname)) },
            placeholder = { Text(stringResource(R.string.profile_nickname_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            leadingIcon = { Icon(Icons.Default.Badge, null) }
        )

        // Gender
        Text(stringResource(R.string.profile_gender), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.values().forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { viewModel.gender.value = g },
                    label = {
                        Text(when (g) {
                            Gender.MALE -> stringResource(R.string.settings_gender_male)
                            Gender.FEMALE -> stringResource(R.string.settings_gender_female)
                            Gender.OTHER -> stringResource(R.string.settings_gender_other)
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
        Text(stringResource(R.string.profile_body_section),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        val height by viewModel.height.collectAsState()
        val weight by viewModel.weight.collectAsState()

        // Height slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.profile_height), style = MaterialTheme.typography.bodyMedium)
                Text(if (height != null) "$height cm" else stringResource(R.string.not_set),
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
                Text(stringResource(R.string.profile_weight), style = MaterialTheme.typography.bodyMedium)
                Text(if (weight != null) "${"%.1f".format(weight)} kg" else stringResource(R.string.not_set),
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
        PageHeader(Icons.Default.Lock, stringResource(R.string.device_page_title), stringResource(R.string.device_page_subtitle))

        OutlinedTextField(
            value = deviceName,
            onValueChange = { viewModel.deviceName.value = it },
            label = { Text(stringResource(R.string.device_lock_name)) },
            placeholder = { Text(stringResource(R.string.device_lock_name_hint)) },
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
                    Text(stringResource(R.string.device_start_date), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Text(
                        startDate?.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern)))
                            ?: stringResource(R.string.device_tap_to_set_date),
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
                        if (days >= 0) stringResource(R.string.device_wearing_days, days) else stringResource(R.string.device_starting_soon),
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
    var showPinSetupDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PageHeader(Icons.Default.Security, stringResource(R.string.security_page_title), stringResource(R.string.security_page_subtitle))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SecurityRow(
                    icon = Icons.Default.Fingerprint,
                    title = stringResource(R.string.security_biometric),
                    subtitle = stringResource(R.string.security_biometric_desc),
                    checked = biometric,
                    onCheckedChange = { viewModel.biometricEnabled.value = it }
                )
                Divider()
                SecurityRow(
                    icon = Icons.Default.Pin,
                    title = stringResource(R.string.security_pin),
                    subtitle = stringResource(R.string.security_pin_desc),
                    checked = pin,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinSetupDialog = true   // must set PIN before enabling
                        } else {
                            viewModel.pinEnabled.value = false
                            viewModel.pinCode.value = ""
                        }
                    }
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
                        stringResource(R.string.security_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = {
                // User cancelled — keep PIN disabled
                viewModel.pinEnabled.value = false
                viewModel.pinCode.value = ""
                showPinSetupDialog = false
            },
            onConfirm = { newPin ->
                viewModel.pinCode.value = newPin
                viewModel.pinEnabled.value = true
                showPinSetupDialog = false
            },
            isChangingPin = false
        )
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
private fun ReminderPage(viewModel: OnboardingViewModel) {
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
        PageHeader(Icons.Default.Notifications, stringResource(R.string.reminder_page_title), stringResource(R.string.reminder_page_subtitle))

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
                        Text(stringResource(R.string.reminder_enable), style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.reminder_enable_desc), style = MaterialTheme.typography.bodySmall,
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
                                Text(stringResource(R.string.reminder_time), style = MaterialTheme.typography.bodyMedium)
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
