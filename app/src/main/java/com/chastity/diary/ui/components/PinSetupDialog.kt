package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Dialog for setting or changing PIN code
 */
@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isChangingPin: Boolean = false
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showCurrentPin by remember { mutableStateOf(false) }
    var showNewPin by remember { mutableStateOf(false) }
    var showConfirmPin by remember { mutableStateOf(false) }
    
    var currentPinError by remember { mutableStateOf<String?>(null) }
    var newPinError by remember { mutableStateOf<String?>(null) }
    var confirmPinError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isChangingPin) "修改 PIN 碼" else "設定 PIN 碼")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isChangingPin) {
                    Text(
                        text = "請輸入當前的 PIN 碼",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { 
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                currentPin = it
                                currentPinError = null
                            }
                        },
                        label = { Text("當前 PIN 碼") },
                        visualTransformation = if (showCurrentPin) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPin = !showCurrentPin }) {
                                Icon(
                                    imageVector = if (showCurrentPin) 
                                        Icons.Default.Visibility 
                                    else 
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (showCurrentPin) "隱藏" else "顯示"
                                )
                            }
                        },
                        isError = currentPinError != null,
                        supportingText = currentPinError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = if (isChangingPin) "請輸入新的 PIN 碼 (4-6 位數字)" else "請輸入 PIN 碼 (4-6 位數字)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            newPin = it
                            newPinError = when {
                                it.isEmpty() -> null
                                it.length < 4 -> "PIN 碼至少需要 4 位數字"
                                else -> null
                            }
                        }
                    },
                    label = { Text(if (isChangingPin) "新 PIN 碼" else "PIN 碼") },
                    visualTransformation = if (showNewPin) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { showNewPin = !showNewPin }) {
                            Icon(
                                imageVector = if (showNewPin) 
                                    Icons.Default.Visibility 
                                else 
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (showNewPin) "隱藏" else "顯示"
                            )
                        }
                    },
                    isError = newPinError != null,
                    supportingText = newPinError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            confirmPinError = when {
                                it.isEmpty() -> null
                                it != newPin -> "PIN 碼不一致"
                                else -> null
                            }
                        }
                    },
                    label = { Text("確認 PIN 碼") },
                    visualTransformation = if (showConfirmPin) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPin = !showConfirmPin }) {
                            Icon(
                                imageVector = if (showConfirmPin) 
                                    Icons.Default.Visibility 
                                else 
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (showConfirmPin) "隱藏" else "顯示"
                            )
                        }
                    },
                    isError = confirmPinError != null,
                    supportingText = confirmPinError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate all fields
                    var hasError = false
                    
                    if (isChangingPin && currentPin.isEmpty()) {
                        currentPinError = "請輸入當前 PIN 碼"
                        hasError = true
                    }
                    
                    if (newPin.length < 4) {
                        newPinError = "PIN 碼至少需要 4 位數字"
                        hasError = true
                    }
                    
                    if (newPin != confirmPin) {
                        confirmPinError = "PIN 碼不一致"
                        hasError = true
                    }
                    
                    if (!hasError) {
                        // If changing PIN, return current PIN for verification
                        // The parent will handle verification
                        onConfirm(newPin)
                    }
                },
                enabled = if (isChangingPin) {
                    currentPin.length >= 4 && newPin.length >= 4 && newPin == confirmPin
                } else {
                    newPin.length >= 4 && newPin == confirmPin
                }
            ) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
