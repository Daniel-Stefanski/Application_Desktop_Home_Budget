package com.example.homebudget.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import kotlin.math.roundToInt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Karta pojedynczego celu oszczędnościowego.
 * Wyświetla postęp, uczestników, termin oraz akcje użytkownika.
 */
@Composable
fun SavingsGoalCard(
    goal: SavingsGoal,
    onAddAmount: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHistory: () -> Unit
) {
    // Procent realizacji celu (0-100)%
    val progress =
        if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount * 100).roundToInt() else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(goal.title, style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(4.dp))
            Text("Cel: ${MoneyFormatter.format(goal.targetAmount)} zł")
            Text("Zebrano: ${MoneyFormatter.format(goal.savedAmount)} zł")

            Spacer(Modifier.height(8.dp))
            val progessColor =
                if (progress >= 100) Color(0xFF2E7D32)
                else Color(0xFF4CAF50)
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                color = progessColor
            )
            Text("$progress%")
            val isCompleted = progress >= 100
            Spacer(Modifier.height(8.dp))
            // Informacja o osobach biorących udział w zbiórce
            goal.sharedWith?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "\uD83D\uDC65 Ja, $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } ?: run {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "\uD83D\uDC64 Tylko ja",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Termin tylko jeśli istnieje
            goal.endDate?.let { end ->
                val daysLeft = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    Instant.ofEpochMilli(end)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (daysLeft > 0)
                        "⏳ Termin: pozostało $daysLeft dni"
                    else
                        "⚠\uFE0F Termin minął",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isCompleted) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "\uD83C\uDF89 Gratulacje! Cel został osiągnięty \uD83C\uDF89",
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
