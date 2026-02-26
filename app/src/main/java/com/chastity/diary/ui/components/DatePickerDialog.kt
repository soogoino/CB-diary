package com.chastity.diary.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Material 3 date picker dialog for selecting historical entry dates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: LocalDate = LocalDate.now(),
    onConfirm: (date: LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert LocalDate to milliseconds since epoch for DatePicker
    val initialMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onConfirm(selectedDate)
                }
                onDismiss()
            }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = MaterialTheme.colorScheme.onSurface,
                subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                currentYearContentColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                disabledSelectedDayContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                disabledSelectedDayContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary,
                dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}
