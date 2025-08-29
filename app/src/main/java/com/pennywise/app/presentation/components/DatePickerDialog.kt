package com.pennywise.app.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.pennywise.app.R
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A reusable DatePickerDialog component that allows users to select a date
 * and returns the selected date as a LocalDate object.
 * 
 * @param showDialog Whether to show the date picker dialog
 * @param onDismiss Callback when the dialog is dismissed
 * @param onDateSelected Callback when a date is selected, returns LocalDate
 * @param initialDate The initial date to display (optional, defaults to current date)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate? = null
) {
    if (!showDialog) return
    
    // Convert LocalDate to Calendar for DatePickerState
    val calendar = remember {
        val cal = Calendar.getInstance()
        initialDate?.let { date ->
            cal.set(date.year, date.monthValue - 1, date.dayOfMonth)
        }
        cal
    }
    
    val datePickerState = remember {
        DatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis,
            initialDisplayedMonthMillis = calendar.timeInMillis,
            yearRange = IntRange(1900, 2100),
            locale = Locale.getDefault()
        )
    }
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Date(millis).toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedDate)
                    }
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
}

/**
 * Extension function to convert Date to LocalDate
 */
fun Date.toLocalDate(): LocalDate {
    return this.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

/**
 * Extension function to convert LocalDate to Date
 */
fun LocalDate.toDate(): Date {
    return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
}
