package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chastity.diary.util.Constants
import com.chastity.diary.R

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

    // Pre-hoist: stringResource cannot be called inside non-@Composable lambdas (onValueChange, onClick)
    val errCurrentRequired = stringResource(R.string.pin_current_prompt)
    val errMinLength = stringResource(R.string.pin_min_length_error, Constants.PIN_MIN_LENGTH)
    val errMismatch = stringResource(R.string.pin_confirm_mismatch)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isChangingPin) stringResource(R.string.pin_title_change) else stringResource(R.string.pin_title_set))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isChangingPin) {
                    Text(
                        text = stringResource(R.string.pin_current_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { 
                            if (it.length <= Constants.PIN_MAX_LENGTH && it.all { char -> char.isDigit() }) {
                                currentPin = it
                                currentPinError = null
                            }
                        },
                        label = { Text(stringResource(R.string.pin_current_label)) },
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
                                    contentDescription = if (showCurrentPin) stringResource(R.string.pin_hide) else stringResource(R.string.pin_show)
                                )
                            }
                        },
                        isError = currentPinError != null,
                        supportingText = { currentPinError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = if (isChangingPin) stringResource(R.string.pin_new_prompt_change) else stringResource(R.string.pin_new_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { 
                        if (it.length <= Constants.PIN_MAX_LENGTH && it.all { char -> char.isDigit() }) {
                            newPin = it
                            newPinError = when {
                                it.isEmpty() -> null
                                it.length < Constants.PIN_MIN_LENGTH -> errMinLength
                                else -> null
                            }
                        }
                    },
                    label = { Text(if (isChangingPin) stringResource(R.string.pin_new_label) else stringResource(R.string.pin_label)) },
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
                                contentDescription = if (showNewPin) stringResource(R.string.pin_hide) else stringResource(R.string.pin_show)
                            )
                        }
                    },
                    isError = newPinError != null,
                    supportingText = { newPinError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { 
                        if (it.length <= Constants.PIN_MAX_LENGTH && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            confirmPinError = when {
                                it.isEmpty() -> null
                                it != newPin -> errMismatch
                                else -> null
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.pin_confirm_label)) },
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
                                contentDescription = if (showConfirmPin) stringResource(R.string.pin_hide) else stringResource(R.string.pin_show)
                            )
                        }
                    },
                    isError = confirmPinError != null,
                    supportingText = { confirmPinError?.let { Text(it) } },
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
                        currentPinError = errCurrentRequired
                        hasError = true
                    }
                    
                    if (newPin.length < Constants.PIN_MIN_LENGTH) {
                        newPinError = errMinLength
                        hasError = true
                    }
                    
                    if (newPin != confirmPin) {
                        confirmPinError = errMismatch
                        hasError = true
                    }
                    
                    if (!hasError) {
                        // If changing PIN, return current PIN for verification
                        // The parent will handle verification
                        onConfirm(newPin)
                    }
                },
                enabled = if (isChangingPin) {
                    currentPin.length >= Constants.PIN_MIN_LENGTH && newPin.length >= Constants.PIN_MIN_LENGTH && newPin == confirmPin
                } else {
                    newPin.length >= Constants.PIN_MIN_LENGTH && newPin == confirmPin
                }
                ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
