package com.chastity.diary.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chastity.diary.R
import com.chastity.diary.util.Constants

@Composable
fun LockScreen(
    onUnlockWithBiometric: () -> Unit,
    onUnlockWithPin: (String) -> Unit,
    biometricAvailable: Boolean,
    errorMessage: String? = null
) {
    var pinInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(errorMessage) {
        showError = errorMessage != null
        if (errorMessage != null) {
            // U-1: shake animation on PIN failure
            for (i in 0 until 4) {
                shakeOffset.animateTo(if (i % 2 == 0) 16f else -16f, tween(60))
            }
            shakeOffset.animateTo(0f, tween(60))
        }
    }

    // F-3: Use safeDrawing insets so the card clears the gesture nav bar on notchless devices
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeOffset.value }
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.lock_icon_desc),
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.lock_screen_title),
                    style = MaterialTheme.typography.headlineSmall
                )

                if (biometricAvailable) {
                    Button(
                        onClick = onUnlockWithBiometric,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.lock_biometric_button))
                    }

                    Divider()

                    Text(
                        text = stringResource(R.string.lock_or_pin),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    label = { Text(stringResource(R.string.lock_pin_label)) },
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
                    enabled = pinInput.length >= Constants.PIN_MIN_LENGTH
                ) {
                    Text(stringResource(R.string.lock_unlock_button))
                }
            }
        }
    }
}
