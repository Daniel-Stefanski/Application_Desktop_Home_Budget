package com.example.homebudget.ui.billsplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.homebudget.data.entity.Expense
import com.example.homebudget.utils.money.MoneyFormatter
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * BillsPlannerCard
 *
 * Pojedyncza karta rachunku w planerze.
 * Pokazuje:
 * - opis rachunku
 * - kwotę
 * - datę płatności
 * - status (opłacony / nieopłacony)
 * - ostrzeżenie o zbliżającym się terminie
 */
@Composable
fun BillsPlannerCard(
    expense: Expense,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPaid = expense.status.trim().lowercase().startsWith("op")
    val today = LocalDate.now()
    val billDate = Instant.ofEpochMilli(expense.date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val daysLeft = ChronoUnit.DAYS.between(today, billDate)

    val statusColor = if (isPaid) Color(0xFF4CAF50) else Color(0xFFF44336)
    val titleColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color(0xFFD0B6F0)
    } else {
        Color(0xFF7F61E0)
    }
    val dateColor = when {
        isPaid -> MaterialTheme.colorScheme.onSurface
        daysLeft < 0 -> MaterialTheme.colorScheme.error
        daysLeft == 0L -> Color(0xFFFF9800) // Pomarańczowy
        daysLeft in 1..2 -> Color(0xFFFFC107) // Żółty
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFFD0B6F0)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // pasek statusu
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f)
            ) {
                // Opis
                Text(
                    text = "Opis: ${expense.description ?: "-"}",
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // Kwota
                Text(
                    text = "Kwota: ${MoneyFormatter.format(expense.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                // Notatka
                if (!expense.note.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Notatka: ${expense.note}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Termin Płatności
                val date = SimpleDateFormat("dd.MM.yyyy").format(Date(expense.date))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Termin płatności: $date",
                        color = dateColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!isPaid) {
                        Spacer(Modifier.width(8.dp))
                        DeadlineBadge(daysLeft)
                    }
                }
                // Jaki cykl
                Text(
                    text = "Powtarza się: ${formatRepeatInterval(expense.repeatInterval)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Status: ${if (isPaid) "Opłacony" else "Nieopłacony"}",
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isPaid,
                            onCheckedChange = { onToggleStatus() }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPaid) "Opłacony" else "Nieopłacony")
                    }

                    Row {
                        TextButton(onClick = onEdit) { Text("✏️ Edytuj") }
                        TextButton(onClick = onDelete) { Text("🗑️ Usuń") }
                    }
                }
            }
        }
    }
}
private fun formatRepeatInterval(months: Int): String =
    when (months) {
        1 -> "co miesiąc"
        2 -> "co 2 miesiące"
        3 -> "co 3 miesiące"
        6 -> "co 6 miesięcy"
        12 -> "co 12 miesięcy"
        else -> "co $months miesięcy"
    }
// Mała etykieta informująca o terminie płatności
@Composable
private fun DeadlineBadge(daysLeft: Long) {
    val (text, color) = when {
        daysLeft == -1L -> "⚠️ wczoraj" to MaterialTheme.colorScheme.error
        daysLeft < -1L -> "⚠️ po terminie o ${-daysLeft} dni" to MaterialTheme.colorScheme.error
        daysLeft == 0L -> "⏰ dziś" to Color(0xFFFF9800)
        daysLeft == 1L -> "⏳ jutro" to Color(0xFFFFC107)
        daysLeft > 1L -> "⏳ za $daysLeft dni" to Color(0xFF4CAF50)
        else -> "" to MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold
    )
}
