package com.chastity.diary.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chastity.diary.domain.model.FormStep

/**
 * Form navigation and progress components
 */

@Composable
fun FormProgressIndicator(
    currentStep: FormStep,
    completedSteps: Set<FormStep>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Overall progress bar
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Step indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepIndicator(
                step = FormStep.CORE,
                label = "核心",
                isCurrent = currentStep == FormStep.CORE,
                isCompleted = FormStep.CORE in completedSteps,
                modifier = Modifier.weight(1f)
            )
            
            Divider(
                modifier = Modifier
                    .width(20.dp)
                    .padding(horizontal = 4.dp),
                thickness = 2.dp
            )
            
            StepIndicator(
                step = FormStep.CONDITIONAL,
                label = "後續",
                isCurrent = currentStep == FormStep.CONDITIONAL,
                isCompleted = FormStep.CONDITIONAL in completedSteps,
                modifier = Modifier.weight(1f)
            )
            
            Divider(
                modifier = Modifier
                    .width(20.dp)
                    .padding(horizontal = 4.dp),
                thickness = 2.dp
            )
            
            StepIndicator(
                step = FormStep.ROTATING,
                label = "特別",
                isCurrent = currentStep == FormStep.ROTATING,
                isCompleted = FormStep.ROTATING in completedSteps,
                modifier = Modifier.weight(1f)
            )
            
            Divider(
                modifier = Modifier
                    .width(20.dp)
                    .padding(horizontal = 4.dp),
                thickness = 2.dp
            )
            
            StepIndicator(
                step = FormStep.REVIEW,
                label = "檢視",
                isCurrent = currentStep == FormStep.REVIEW,
                isCompleted = FormStep.REVIEW in completedSteps,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StepIndicator(
    step: FormStep,
    label: String,
    isCurrent: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circle indicator
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp)
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent || isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FormNavigationButtons(
    canGoBack: Boolean,
    canGoNext: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextButtonText: String = "下一步",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Back button
        OutlinedButton(
            onClick = onBack,
            enabled = canGoBack,
            modifier = Modifier.weight(1f)
        ) {
            Text("上一步")
        }
        
        // Next button
        Button(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.weight(1f)
        ) {
            Text(nextButtonText)
        }
    }
}
