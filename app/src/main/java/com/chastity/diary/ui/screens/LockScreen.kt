package com.chastity.diary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    onUnlockWithBiometric: () -> Unit,
    onUnlockWithPin: (String) -> Unit,
    biometricAvailable: Boolean,
    errorMessage: String? = null
) {
    var pinInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        showError = errorMessage != null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "鎖定",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "貞操日記已鎖定",
                    style = MaterialTheme.typography.headlineSmall
                )

                if (biometricAvailable) {
                    Button(
                        onClick = onUnlockWithBiometric,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("使用生物辨識解鎖")
                    }

                    Divider()

                    Text(
                        text = "或使用 PIN 碼",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    label = { Text("PIN 碼") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError,
                    supportingText = if (showError && errorMessage != null) {
                        { Text(errorMessage) }
                    } else null
                )

                Button(
                    onClick = {
                        onUnlockWithPin(pinInput)
                        pinInput = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pinInput.length >= 4
                ) {
                    Text("解鎖")
                }
            }
        }
    }
}
