package com.example.homebudget.ui.billsplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.dialogs.ConfirmDialog
import com.example.homebudget.ui.common.feedback.EmptyState
import com.example.homebudget.ui.common.feedback.LoadingState
import com.example.homebudget.utils.money.MoneyFormatter
import com.example.homebudget.viewmodel.billsplanner.BillsNotification
import com.example.homebudget.viewmodel.billsplanner.BillsPlannerViewModel

/**
 * BillsPlannerScreen
 *
 * Główny ekran planera rachunków.
 * Odpowiada za:
 * - wyświetlanie listy rachunków cyklicznych
 * - podsumowanie liczby i kwoty rachunków
 * - obsługę dialogu usuwania
 * - wyświetlanie powiadomień o terminach płatności
 */
@Composable
fun BillsPlannerScreen(
    onAddBill: () -> Unit,
    onEditBill: (Int) -> Unit
) {
    val viewModel: BillsPlannerViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    state.billToDelete?.let { bill ->
        ConfirmDialog(
            title = "Usuń rachunek",
            message = "Czy na pewno chcesz usunąć „${bill.description}”?",
            confirmText = "Usuń",
            dismissText = "Anuluj",
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    state.notification?.let { notification ->
        when (notification) {
            is BillsNotification.DeadlineSoon -> {
                val days = notification.daysLeft
                val title = when (days) {
                    7L -> "⏰ Termin za tydzień"
                    2L -> "⏰ Termin za 2 dni"
                    1L -> "⚠️ Termin jutro!"
                    0L -> "❗ Termin dziś!"
                    -1L -> "⚠️ Termin minął wczoraj"
                    -2L -> "⚠️ Termin minął 2 dni temu"
                    -3L -> "⚠️ Termin minął 3 dni temu"
                    else -> "⚠️ Termin minął"
                }
                val message = when (days) {
                    7L -> "Do terminu płatności „${notification.title}” pozostało 7 dni."
                    2L -> "Do terminu płatności „${notification.title}” pozostały 2 dni."
                    1L -> "Jutro mija termin płatności „${notification.title}”."
                    0L -> "Dziś mija termin płatności „${notification.title}”."
                    -1L -> "Termin płatności „${notification.title}” minął wczoraj."
                    -2L -> "Termin płatności „${notification.title}” minął 2 dni temu."
                    -3L -> "Termin płatności „${notification.title}” minął 3 dni temu."
                    else -> "Termin płatności „${notification.title}” już minął."
                }

                ConfirmDialog(
                    title = title,
                    message = message,
                    onConfirm = { viewModel.clearNotifications() }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetPaidBillsIfNeeded()
        viewModel.loadBills()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            ) {
                Column {

                    Text(
                        "Planer rachunków",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Liczba rachunków",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = state.totalCount.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Łączna kwota",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = MoneyFormatter.format(state.totalAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = onAddBill) {
                        Text("➕ Dodaj rachunek")
                    }

                    Spacer(Modifier.height(16.dp))

                    if (state.isLoading) {
                        LoadingState()
                    } else if (state.bills.isEmpty()) {
                        EmptyState(text = "📝 Brak zaplanowanych rachunków.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.bills) { bill ->
                                BillsPlannerCard(
                                    expense = bill,
                                    onToggleStatus = { viewModel.toggleStatus(bill) },
                                    onEdit = { onEditBill(bill.id) },
                                    onDelete = { viewModel.requestDelete(bill) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
