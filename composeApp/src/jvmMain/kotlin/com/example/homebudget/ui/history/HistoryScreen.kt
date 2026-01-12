@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.homebudget.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.data.entity.Expense
import com.example.homebudget.ui.common.dialogs.FormDialog
import com.example.homebudget.ui.common.dropdowns.FormDropdown
import com.example.homebudget.ui.common.dropdowns.SortDropdown
import com.example.homebudget.ui.common.feedback.EmptyState
import com.example.homebudget.ui.common.feedback.ErrorState
import com.example.homebudget.ui.common.feedback.LoadingState
import com.example.homebudget.ui.common.fields.FormTextField
import com.example.homebudget.ui.common.fields.NumberField
import com.example.homebudget.utils.money.MoneyFormatter
import com.example.homebudget.viewmodel.history.HistoryListItem
import com.example.homebudget.viewmodel.history.HistoryUiState
import com.example.homebudget.viewmodel.history.HistoryViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * HistoryScreen
 *
 * Ekran historii wydatków użytkownika.
 *
 * Odpowiada za:
 * - wyświetlanie listy wydatków pogrupowanych po dacie
 * - obsługę wyszukiwania, sortowania i filtrów
 * - nawigację klawiaturą (TAB)
 * - prezentację dialogu filtra zaawansowanego
 */
@Composable
fun HistoryScreen() {
    val viewModel: HistoryViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.reload()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp)
            ) {
                when {
                    state.isLoading -> {
                        LoadingState()
                    }

                    state.error != null -> {
                        ErrorState(message = state.error!!)
                    }

                    else -> {
                        HistoryContent(
                            state = state,
                            onSearchChange = viewModel::onSearchQueryChange,
                            onSortSelected = viewModel::onSortSelected,
                            onOpenAdvanced = { viewModel.setAdvancedDialogVisible(true) },
                            onApplyAdvanced = viewModel::applyAdvancedFilter,
                            onClearAdvanced = viewModel::clearAdvancedFilter,
                            onCloseAdvanced = { viewModel.setAdvancedDialogVisible(false) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Główna zawartość ekranu historii.
 *
 * Odpowiada za:
 * - wyszukiwanie wydatków
 * - sortowanie listy
 * - otwieranie filtra zaawansowanego
 * - wyświetlanie listy lub komunikatu o braku danych
 */
@Suppress("DuplicatedCode")
@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onSearchChange: (String) -> Unit,
    onSortSelected: (Int) -> Unit,
    onOpenAdvanced: () -> Unit,
    onApplyAdvanced: (
        startDate: String,
        endDate: String,
        minAmount: String,
        maxAmount: String,
        categoryIndex: Int,
        paymentIndex: Int,
        onlyRecurring: Boolean
    ) -> Unit,
    onClearAdvanced: () -> Unit,
    onCloseAdvanced: () -> Unit
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("pl"))
    }

    val focusManager = LocalFocusManager.current

    val searchFocus = remember { FocusRequester() }
    val sortFocus = remember { FocusRequester() }
    val advancedButtonFocus = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = "Historia wydatków",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // ===== WYSZUKIWANIE =====
        FormTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            label = "Szukaj wydatku po opisie lub notatce...",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = searchFocus,
            maxLength = 100,
        )

        Spacer(Modifier.height(16.dp))

        // ===== SORTOWANIE =====
        Text(
            text = "Sortowanie",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))
        SortDropdown(
            options = state.sortOptions,
            selectedIndex = state.selectedSortIndex,
            onSelectedIndexChange = onSortSelected,
            modifier = Modifier.focusRequester(sortFocus)
        )

        Spacer(Modifier.height(16.dp))

        // ===== FILTR ZAAWANSOWANY =====
        Button(
            onClick = onOpenAdvanced,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(advancedButtonFocus)
                .onPreviewKeyEvent {
                    if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                        focusManager.moveFocus(FocusDirection.Next)
                        true
                    } else false
                }
        ) {
            Text("Filtr zaawansowany")
        }

        Spacer(Modifier.height(16.dp))

        // ===== LISTA WYDATKÓW =====
        if (state.items.none { it is HistoryListItem.ExpenseItem }) {
            EmptyState(text = "📝 Brak wydatków w historii.\nDodaj pierwszy wydatek.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.items) { item ->
                    when (item) {
                        is HistoryListItem.Header -> {
                            DateHeaderCard(dateLabel = item.date)
                        }

                        is HistoryListItem.ExpenseItem -> {
                            ExpenseCard(expense = item.expense, dateFormatter = dateFormatter)
                        }
                    }
                }
            }
        }
    }

    if (state.showAdvancedDialog) {
        AdvancedFilterDialog(
            initialStartDate = state.startDateText,
            initialEndDate = state.endDateText,
            initialMinAmount = state.minAmountText,
            initialMaxAmount = state.maxAmountText,
            categories = state.categories,
            initialCategoryIndex = state.selectedCategoryIndex,
            paymentMethods = state.paymentMethods,
            initialPaymentIndex = state.selectedPaymentIndex,
            initialOnlyRecurring = state.onlyRecurring,
            onApply = { s, e, min, max, catIdx, payIdx, onlyRec ->
                onApplyAdvanced(s, e, min, max,catIdx, payIdx, onlyRec)
                onCloseAdvanced()
            },
            onClear = {
                onClearAdvanced()
                onCloseAdvanced()
            },
            onDismiss = onCloseAdvanced
        )
    }
}

/**
 * Karta daty.
 *
 * Odpowiada za:
 * - wyświetlenie daty czyli dzień, miesiąc, rok
 * - to jest zwykły tytuł nic nie robi
 */
@Composable
private fun DateHeaderCard(dateLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Karta pojedynczego wydatku.
 *
 * Odpowiada za:
 * - wyświetlenie podstawowych danych wydatku
 * - rozwijanie szczegółów po kliknięciu
 * - obsługę fokusu i nawigacji klawiaturą
 */
@Composable
private fun ExpenseCard(
    expense: Expense,
    dateFormatter: DateTimeFormatter
) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) } // na start zwiniete

    val dateText = remember(expense.date) {
        val date = Instant.ofEpochMilli(expense.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        dateFormatter.format(date)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .focusable()
            .onPreviewKeyEvent {
                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                    focusManager.moveFocus(FocusDirection.Next)
                    true
                } else false
            },
        shape = RoundedCornerShape(16.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Nagłówek – kategoria + kwota
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kategoria: ${expense.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Kwota: ${MoneyFormatter.format(expense.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))

            if (expanded) {
                // Opis - główny kontekst
                Text(
                    "Opis: ${expense.description ?: "-"}",
                    fontWeight = FontWeight.Medium
                )
                // Notatka tylko jeśli istnieje
                if (!expense.note.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Notatka: ${expense.note}"
                    )
                }
                // Metadane
                // Metoda płatności
                Text("Metoda płatności: ${expense.paymentMethod}")
                // Osoba
                Text("Osoba: ${expense.person ?: "Ja"}")
                // Data
                Text("Data: $dateText")

                // Info o powtarzalności
                if (expense.isRecurring) {
                    Text(
                        text = "Powtarzalny wydatek co ${expense.repeatInterval} mies.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "Kliknij, aby rozwinąć szczegóły",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog filtra zaawansowanego.
 *
 * Odpowiada za:
 * - ustawianie zakresu dat i kwot
 * - filtrowanie po kategorii i metodzie płatności
 * - ograniczenie wyników do wydatków powtarzalnych
 */
@Composable
private fun AdvancedFilterDialog(
    initialStartDate: String,
    initialEndDate: String,
    initialMinAmount: String,
    initialMaxAmount: String,
    categories: List<String>,
    initialCategoryIndex: Int,
    paymentMethods: List<String>,
    initialPaymentIndex: Int,
    initialOnlyRecurring: Boolean,
    onApply: (String, String, String, String, Int, Int, Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember(initialStartDate) { mutableStateOf(initialStartDate) }
    var endDate by remember(initialEndDate) { mutableStateOf(initialEndDate) }
    var minAmount by remember(initialMinAmount) { mutableStateOf(initialMinAmount) }
    var maxAmount by remember(initialMaxAmount) { mutableStateOf(initialMaxAmount) }
    var categoryIndex by remember(initialCategoryIndex) { mutableStateOf(initialCategoryIndex) }
    var paymentIndex by remember(initialPaymentIndex) { mutableStateOf(initialPaymentIndex) }
    var onlyRecurring by remember(initialOnlyRecurring) { mutableStateOf(initialOnlyRecurring) }

    FormDialog(
        title = "Filtr zaawansowany",
        confirmText = "Zastosuj",
        onConfirm = {
            onApply(
                startDate, endDate,
                minAmount, maxAmount,
                categoryIndex, paymentIndex,
                onlyRecurring
            )
            onDismiss() // zamknij po zastosowaniu
        },
        clearText = "Wyczyść",
        onClear = {
            onClear() // Wyczyść filtrowanie ViewModel.clearAdvancedFilter()
            onDismiss() // Zamknij dialog
        },
        dismissText = "Anuluj",
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Zakres dat (dd.MM.rrrr):",
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Od") }
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Do") }
                )
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Zakres kwoty:",
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = minAmount,
                    onValueChange = { minAmount = it },
                    label = "Min",
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = maxAmount,
                    onValueChange = { maxAmount = it },
                    label = "Max",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Kategoria:",
                fontWeight = FontWeight.SemiBold
            )
            FormDropdown(
                label = "Kategoria",
                items = categories,
                selectedIndex = categoryIndex,
                onSelectedIndexChange = { categoryIndex = it }
            )


            Spacer(Modifier.height(8.dp))

            Text(
                text = "Metoda płatności:",
                fontWeight = FontWeight.SemiBold
            )
            FormDropdown(
                label = "Metoda płatności",
                items = paymentMethods,
                selectedIndex = paymentIndex,
                onSelectedIndexChange = { paymentIndex = it }
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = onlyRecurring,
                    onCheckedChange = { onlyRecurring = it }
                )
                Text("Tylko powtarzalne wydatki")
            }
        }
    }
}