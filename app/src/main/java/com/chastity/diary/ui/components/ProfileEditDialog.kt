package com.chastity.diary.ui.components

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.chastity.diary.R
import androidx.compose.ui.unit.dp
import java.time.LocalDate
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
    var startDate by remember { mutableStateOf(currentStartDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var height by remember { mutableStateOf(currentHeight?.toString() ?: "") }
    var weight by remember { mutableStateOf(currentWeight?.toString() ?: "") }
    var deviceName by remember { mutableStateOf(currentDeviceName ?: "") }
    var deviceSize by remember { mutableStateOf(currentDeviceSize ?: "") }

    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }

    // Pre-fetch composable string resources into locals so non-composable helpers can use them
    val errInvalidNumber = stringResource(R.string.error_invalid_number)
    val errHeightRange = stringResource(R.string.error_height_range)
    val errWeightRange = stringResource(R.string.error_weight_range)
    val errDateFormat = stringResource(R.string.profile_date_format_hint)

    fun validateHeight(input: String): String? {
        if (input.isBlank()) return null
        val value = input.toIntOrNull()
        return when {
            value == null -> errInvalidNumber
            value < 100 || value > 250 -> errHeightRange
            else -> null
        }
    }

    fun validateWeight(input: String): String? {
        if (input.isBlank()) return null
        val value = input.toFloatOrNull()
        return when {
            value == null -> errInvalidNumber
            value < 30f || value > 200f -> errWeightRange
            else -> null
        }
    }

    fun validateStartDate(input: String): String? {
        if (input.isBlank()) return null
        return try { LocalDate.parse(input); null } catch (e: Exception) { errDateFormat }
    }

    val isValid = heightError == null && weightError == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text(stringResource(R.string.profile_nickname)) },
                    placeholder = { Text(stringResource(R.string.profile_nickname_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.notes_optional)) }
                )

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_start_date_prefix, startDate?.toString() ?: stringResource(R.string.not_set)))
                }

                Divider()

                OutlinedTextField(
                    value = height,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            height = it
                            heightError = validateHeight(it)
                        }
                    },
                    label = { Text(stringResource(R.string.profile_height)) },
                    placeholder = { Text(stringResource(R.string.profile_example_height)) },
                    suffix = { Text("cm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = heightError != null,
                    supportingText = { Text(heightError ?: stringResource(R.string.optional_height_hint)) }
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        if (it.isEmpty() || it.toFloatOrNull() != null) {
                            weight = it
                            weightError = validateWeight(it)
                        }
                    },
                    label = { Text(stringResource(R.string.profile_weight)) },
                    placeholder = { Text(stringResource(R.string.profile_example_weight)) },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = weightError != null,
                    supportingText = { Text(weightError ?: stringResource(R.string.optional_weight_hint)) }
                )

                Divider()

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text(stringResource(R.string.device_lock_name)) },
                    placeholder = { Text(stringResource(R.string.device_lock_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.profile_optional)) }
                )

                OutlinedTextField(
                    value = deviceSize,
                    onValueChange = { deviceSize = it },
                    label = { Text(stringResource(R.string.profile_device_size_label)) },
                    placeholder = { Text(stringResource(R.string.profile_device_size_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.profile_optional_free_input)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedDate = startDate
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = startDate ?: LocalDate.now(),
            onConfirm = { date ->
                startDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

}
