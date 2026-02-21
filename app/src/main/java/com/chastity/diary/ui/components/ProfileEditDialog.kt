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

/**
 * Dialog for editing user profile information
 * - Height (cm): Optional, 100-250 range
 * - Weight (kg): Optional, 30-200 range
 * - Device Name: Optional, free text input
 */
@Composable
fun ProfileEditDialog(
    currentHeight: Int?,
    currentWeight: Float?,
    currentDeviceName: String?,
    onDismiss: () -> Unit,
    onSave: (height: Int?, weight: Float?, deviceName: String?) -> Unit
) {
    var height by remember { mutableStateOf(currentHeight?.toString() ?: "") }
    var weight by remember { mutableStateOf(currentWeight?.toString() ?: "") }
    var deviceName by remember { mutableStateOf(currentDeviceName ?: "") }
    
    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    
    // Validate inputs
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯個人資料") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Height input
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
                    supportingText = { 
                        Text(heightError ?: "選填,範圍 100-250 cm") 
                    }
                )
                
                // Weight input
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
                    supportingText = { 
                        Text(weightError ?: "選填,範圍 30-200 kg") 
                    }
                )
                
                Divider()
                
                // Device name input
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("貞操鎖名稱") },
                    placeholder = { Text("例如: Holy Trainer V4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { 
                        Text("選填,記錄你正在使用的貞操鎖") 
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val heightValue = height.toIntOrNull()?.takeIf { it in 100..250 }
                    val weightValue = weight.toFloatOrNull()?.takeIf { it in 30f..200f }
                    val deviceValue = deviceName.trim().ifBlank { null }
                    
                    onSave(heightValue, weightValue, deviceValue)
                },
                enabled = heightError == null && weightError == null
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
