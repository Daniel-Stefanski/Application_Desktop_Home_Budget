package com.example.homebudget.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.homebudget.utils.locale.AppLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun CalendarDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            CalendarHeader(
                month = currentMonth,
                onPrev = { currentMonth = currentMonth.minusMonths(1) },
                onNext = { currentMonth = currentMonth.plusMonths(1) }
            )
        },
        text = {
            CalendarMonth(
                month = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDate) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val monthName = month.month
        .getDisplayName(TextStyle.FULL, AppLocale.PL)
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Text("◀")
        }
        Text(
            text = "$monthName ${month.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Text("▶")
        }
    }
}

@Composable
private fun CalendarMonth(
    month: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    Column {
        // Dni tygodnia
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach {
                Text(
                    it.getDisplayName(TextStyle.SHORT, AppLocale.PL),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val firstDayOfMonth = month.atDay(1)
        val startOffset = (firstDayOfMonth.dayOfWeek.value + 6) % 7
        val daysInMonth = month.lengthOfMonth()

        val totalCells = startOffset + daysInMonth
        val rows = (totalCells / 7) + if (totalCells % 7 == 0) 0 else 1

        var day = 1

        repeat(rows) {
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val index = it * 7 + column
                    if (index < startOffset || day > daysInMonth) {
                        Box(Modifier.weight(1f).height(36.dp))
                    } else {
                        val date = month.atDay(day)
                        val isSelected = date == selectedDate

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .padding(2.dp)
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day.toString(),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                        day++
                    }
                }
            }
        }
    }
}