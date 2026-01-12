@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.homebudget.ui.addexpense

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.dialogs.ConfirmDialog
import com.example.homebudget.ui.common.dropdowns.FormDropdown
import com.example.homebudget.ui.common.feedback.ErrorState
import com.example.homebudget.ui.common.feedback.LoadingState
import com.example.homebudget.ui.common.fields.ErrorText
import com.example.homebudget.ui.common.fields.FormTextField
import com.example.homebudget.ui.common.fields.NumberField
import com.example.homebudget.ui.dialogs.CalendarDatePickerDialog
import com.example.homebudget.viewmodel.addexpense.AddExpenseUiState
import com.example.homebudget.viewmodel.addexpense.AddExpenseViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * AddExpenseScreen
 *
 * Główny ekran dodawania wydatku.
 *
 * Odpowiedzialności:
 * - inicjalizacja AddExpenseViewModel
 * - obserwacja stanu UI (StateFlow)
 * - wywołanie wczytania domyślnych ustawień użytkownika
 * - obsługa dialogu po zapisaniu wydatku
 * - przekazanie callbacków do warstwy UI (AddExpenseContent)
 *
 * NIE zawiera:
 * - walidacji
 * - logiki zapisu
 * - dostępu do bazy danych
 */
@Composable
fun AddExpenseScreen(
    onBackToDashboard: () -> Unit
) {
    val viewModel: AddExpenseViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    /**
     * LaunchedEffect(Unit)
     *
     * Wywoływany tylko raz przy pierwszym wejściu na ekran.
     * Służy do:
     * - pobrania domyślnych ustawień użytkownika (kategorie, osoby, płatności)
     * - ustawienia wartości domyślnych w formularzu
     */
    LaunchedEffect(Unit) {
        viewModel.resetForm()
    }
    /**
     * Dialog wyświetlany po poprawnym zapisaniu wydatku.
     *
     * Pozwala użytkownikowi:
     * - dodać kolejny wydatek (formularz zostaje wyczyszczony)
     * - wrócić do dashboardu
     *
     * Dialog jest kontrolowany wyłącznie przez stan ViewModelu.
     */
    if (state.showSavedDialog) {
        ConfirmDialog(
            title = "✔️ Wydatek zapisany",
            message = "Czy chcesz dodać kolejny wydatek?",
            confirmText = "TAK",
            dismissText = "NIE",
            onConfirm = {
                viewModel.onSavedDialogResult(addAnother = true)
            },
            onDismiss = {
                viewModel.onSavedDialogResult(addAnother = false)
                onBackToDashboard()
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Treść ekranu
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp)
            ) {
                if (state.isLoading) {
                    LoadingState()
                } else if (state.globalError != null) {
                    ErrorState(message = state.globalError!!)
                } else {
                    key(state.showSavedDialog) {
                        AddExpenseContent(
                            state = state,
                            onAmountChange = viewModel::onAmountChange,
                            onDescriptionChange = viewModel::onDescriptionChange,
                            onNoteChange = viewModel::onNoteChange,
                            onCategorySelected = viewModel::onCategorySelected,
                            onPaymentSelected = viewModel::onPaymentSelected,
                            onPersonSelected = viewModel::onPersonSelected,
                            onDateChange = viewModel::onDateChange,
                            onRecurringChange = viewModel::onRecurringChange,
                            onCycleSelected = viewModel::onCycleSelected,
                            onCancel = onBackToDashboard,
                            onSave = viewModel::saveExpense
                        )
                    }
                }
            }
        }
    }
}

/**
 * AddExpenseContent
 *
 * Czysta warstwa UI formularza dodawania wydatku.
 *
 * Odpowiada za:
 * - renderowanie pól formularza
 * - obsługę fokusu i nawigacji klawiaturą (TAB)
 * - wyświetlanie błędów walidacji
 *
 * NIE zawiera:
 * - logiki biznesowej
 * - walidacji
 * - operacji asynchronicznych
 */
@Suppress("DuplicatedCode")
@Composable
private fun AddExpenseContent(
    state: AddExpenseUiState,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCategorySelected: (Int) -> Unit,
    onPaymentSelected: (Int) -> Unit,
    onPersonSelected: (Int) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onCycleSelected: (Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("pl"))
    }
    /**
     * FocusRequesters
     *
     * Kontrolują kolejność poruszania się klawiszem TAB
     * w dokładnie takiej samej kolejności jak w RegisterScreen.
     *
     * Zapewniają spójny UX desktopowy.
     */
    val focusManager = LocalFocusManager.current
    val descriptionFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }
    val noteFocus = remember { FocusRequester() }
    val categoryFocus = remember { FocusRequester() }
    val paymentFocus = remember { FocusRequester() }
    val personFocus = remember { FocusRequester() }
    val dateFocus = remember { FocusRequester() }
    val saveButtonFocus = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Dodaj wydatek",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        // --- Opis ---
        FormTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = "🏷️ Opis",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = descriptionFocus,
            isError = state.descriptionError != null,
            maxLength = 100,
        )
        ErrorText(state.descriptionError)
        Spacer(Modifier.height(12.dp))

        // --- Kwota ---
        NumberField(
            value = state.amount,
            onValueChange = onAmountChange,
            label = "💵 Kwota",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = amountFocus,
            isError = state.amountError != null
        )
        ErrorText(state.amountError)
        Spacer(Modifier.height(12.dp))

        // --- Notatka ---
        FormTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = "📝 Notatka (opcjonalnie)",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = noteFocus,
            singleLine = false,
            maxLength = 300
        )
        Spacer(Modifier.height(16.dp))

        // --- Kategoria ---
        FormDropdown(
            label = "📂 Kategoria",
            items = state.categories,
            selectedIndex = state.selectedCategoryIndex,
            onSelectedIndexChange = onCategorySelected,
            error = state.categoryError,
            focusRequester = categoryFocus
        )
        Spacer(Modifier.height(12.dp))

        // --- Metoda płatności ---
        FormDropdown(
            label = "💳 Metoda płatności",
            items = state.paymentMethods,
            selectedIndex = state.selectedPaymentIndex,
            onSelectedIndexChange = onPaymentSelected,
            error = state.paymentError,
            focusRequester = paymentFocus
        )
        Spacer(Modifier.height(12.dp))

        // --- Osoba ---
        FormDropdown(
            label = "👤 Osoba",
            items = state.people,
            selectedIndex = state.selectedPersonIndex,
            onSelectedIndexChange = onPersonSelected,
            error = null,
            focusRequester = personFocus
        )
        Spacer(Modifier.height(16.dp))

        // --- Data ---
        Text(text = "📅 Data", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))

        var datePickerExpanded by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { datePickerExpanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(dateFocus)
                .onPreviewKeyEvent {
                    if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                        focusManager.moveFocus(FocusDirection.Next)
                        true
                    } else {
                        false
                    }
                },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                text = state.date.format(dateFormatter),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        if (datePickerExpanded) {
            CalendarDatePickerDialog(
                initialDate = state.date,
                onConfirm = {
                    onDateChange(it)
                    datePickerExpanded = false
                },
                onDismiss = { datePickerExpanded = false }
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- Powtarzalny wydatek ---
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.isRecurring,
                onCheckedChange = onRecurringChange
            )
            Text("🔁 Dodaj do Planera rachunków")
        }

        if (state.isRecurring) {
            Spacer(Modifier.height(8.dp))
            FormDropdown(
                label = "Powtarzaj co:",
                items = state.cycles,
                selectedIndex = state.selectedCycleIndex,
                onSelectedIndexChange = onCycleSelected,
                error = null
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- Przyciski ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("❌ Anuluj")
            }

            Button(
                onClick = onSave,
                enabled = state.isValid,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(saveButtonFocus)
            ) {
                Text("💾 Zapisz")
            }
        }
    }
}