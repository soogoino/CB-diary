package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * Dialog for editing user profile information:
 * - Nickname (optional, free text)
 * - Start date (optional, yyyy-MM-dd)
 * - Height (cm): Optional, 100-250
 * - Weight (kg): Optional, 30-200
 * - Device Name: Optional, free text
 * - Device Size: Optional, free text (e.g. "S", "38mm")
 */
@Composable
fun ProfileEditDialog(
    currentNickname: String?,
    currentStartDate: LocalDate?,
    currentHeight: Int?,
    currentWeight: Float?,
    currentDeviceName: String?,
    currentDeviceSize: String?,
    onDismiss: () -> Unit,
    onSave: (
        nickname: String?,
        startDate: LocalDate?,
        height: Int?,
        weight: Float?,
        deviceName: String?,
        deviceSize: String?
    ) -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname ?: "") }
    var startDateText by remember { mutableStateOf(currentStartDate?.toString() ?: "") }
    var height by remember { mutableStateOf(currentHeight?.toString() ?: "") }
    var weight by remember { mutableStateOf(currentWeight?.toString() ?: "") }
    var deviceName by remember { mutableStateOf(currentDeviceName ?: "") }
    var deviceSize by remember { mutableStateOf(currentDeviceSize ?: "") }

    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var startDateError by remember { mutableStateOf<String?>(null) }

    fun validateHeight(input: String): String? {
        if (input.isBlank()) return null
        val value = input.toIntOrNull()
        return when {
            value == null -> "請輸入有效數字"
            value < 100 || value > 250 -> "身高範圍: 100-250 cm"
            else -> null
        }
    }

    fun validateWeight(input: String): String? {
        if (input.isBlank()) return null
        val value = input.toFloatOrNull()
        return when {
            value == null -> "請輸入有效數字"
            value < 30f || value > 200f -> "體重範圍: 30-200 kg"
            else -> null
        }
    }

    fun validateStartDate(input: String): String? {
        if (input.isBlank()) return null
        return try { LocalDate.parse(input); null } catch (e: Exception) { "格式: yyyy-MM-dd，例如 2024-01-15" }
    }

    val isValid = heightError == null && weightError == null && startDateError == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯個人資料") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── 暱稱 ──
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("暱稱") },
                    placeholder = { Text("例如: 小鎖鎖") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("選填") }
                )

                // ── 開始日期 ──
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = {
                        startDateText = it
                        startDateError = validateStartDate(it)
                    },
                    label = { Text("鎖定開始日期") },
                    placeholder = { Text("yyyy-MM-dd") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = startDateError != null,
                    supportingText = { Text(startDateError ?: "選填，格式 yyyy-MM-dd") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )

                Divider()

                // ── 身高 ──
                OutlinedTextField(
                    value = height,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            height = it
                            heightError = validateHeight(it)
                        }
                    },
                    label = { Text("身高") },
                    placeholder = { Text("例如: 175") },
                    suffix = { Text("cm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = heightError != null,
                    supportingText = { Text(heightError ?: "選填，範圍 100-250 cm") }
                )

                // ── 體重 ──
                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        if (it.isEmpty() || it.toFloatOrNull() != null) {
                            weight = it
                            weightError = validateWeight(it)
                        }
                    },
                    label = { Text("體重") },
                    placeholder = { Text("例如: 70.5") },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = weightError != null,
                    supportingText = { Text(weightError ?: "選填，範圍 30-200 kg") }
                )

                Divider()

                // ── 貞操鎖名稱 ──
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("貞操鎖名稱") },
                    placeholder = { Text("例如: Holy Trainer V4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("選填") }
                )

                // ── 貞操鎖尺寸 ──
                OutlinedTextField(
                    value = deviceSize,
                    onValueChange = { deviceSize = it },
                    label = { Text("貞操鎖尺寸") },
                    placeholder = { Text("例如: S / M / 38mm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("選填，自由輸入") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedDate = startDateText.trim().let {
                        if (it.isBlank()) null
                        else try { LocalDate.parse(it) } catch (e: Exception) { null }
                    }
                    val heightValue = height.toIntOrNull()?.takeIf { it in 100..250 }
                    val weightValue = weight.toFloatOrNull()?.takeIf { it in 30f..200f }
                    onSave(
                        nickname.trim().ifBlank { null },
                        parsedDate,
                        heightValue,
                        weightValue,
                        deviceName.trim().ifBlank { null },
                        deviceSize.trim().ifBlank { null }
                    )
                },
                enabled = isValid
            ) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
