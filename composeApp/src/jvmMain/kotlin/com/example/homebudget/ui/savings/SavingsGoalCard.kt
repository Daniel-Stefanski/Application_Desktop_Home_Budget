package com.example.homebudget.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homebudget.data.entity.SavingsGoal
import com.example.homebudget.utils.money.MoneyFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SavingsGoalCard(
    goal: SavingsGoal,
    onAddAmount: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHistory: () -> Unit
) {
    val progress =
        if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount * 100).roundToInt() else 0
    val isCompleted = progress >= 100
    val endDate = goal.endDate?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    val daysLeft = endDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val titleWarning = !isCompleted && daysLeft != null && daysLeft in 0..3
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("pl"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cel: ${goal.title}${if (titleWarning) " ⚠️" else ""}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(4.dp))
            Text("Kwota docelowa: ${MoneyFormatter.format(goal.targetAmount)} zł")
            Text("Zaoszczędzono: ${MoneyFormatter.format(goal.savedAmount)} zł")

            Spacer(Modifier.height(8.dp))
            val progressColor =
                if (progress >= 100) Color(0xFF2E7D32) else Color(0xFF4CAF50)
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                color = progressColor
            )
            Text(
                text = if (isCompleted) {
                    "🎉 Gratulacje! Cel osiągnięty!"
                } else {
                    "Postęp: ${progress.coerceAtMost(100)}%"
                }
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (goal.sharedWith.isNullOrBlank()) {
                    "Z kim: Tylko ja"
                } else {
                    "Z kim: ${goal.sharedWith}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    endDate == null -> "Bez terminu"
                    daysLeft != null && daysLeft >= 0 ->
                        "Termin: ${dateFormatter.format(endDate)}\nPozostało dni: $daysLeft"
                    daysLeft != null ->
                        "Termin: ${dateFormatter.format(endDate)}\nPo terminie o: ${-daysLeft} dni"
                    else -> "Bez terminu"
                },
                style = MaterialTheme.typography.bodySmall
            )

            if (isCompleted) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "🎉 Gratulacje! Cel został osiągnięty 🎉",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddAmount, enabled = !isCompleted) { Text("💵 Dodaj") }
                OutlinedButton(onClick = onWithdraw, enabled = goal.savedAmount > 0) { Text("💵 Wypłać") }
                OutlinedButton(onClick = onEdit) { Text("✏️ Edytuj") }
                OutlinedButton(onClick = onHistory) { Text("📜 Historia") }
                OutlinedButton(onClick = onDelete) { Text("🗑️ Usuń") }
            }
        }
    }
}
