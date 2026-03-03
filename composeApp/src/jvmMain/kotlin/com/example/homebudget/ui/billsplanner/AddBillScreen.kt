package com.example.homebudget.ui.billsplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.dialogs.CalendarDatePickerDialog
import com.example.homebudget.ui.common.dropdowns.FormDropdown
import com.example.homebudget.ui.common.feedback.LoadingState
import com.example.homebudget.ui.common.fields.ErrorText
import com.example.homebudget.ui.common.fields.FormTextField
import com.example.homebudget.ui.common.fields.NumberField
import com.example.homebudget.viewmodel.billsplanner.AddBillViewModel
import com.example.homebudget.viewmodel.billsplanner.AddBillUiState
import java.time.LocalDate
import kotlin.reflect.KClass

/**
 * AddBillScreen
 *
 * Ekran dodawania lub edycji rachunku cyklicznego.
 * Odpowiada za:
 * - inicjalizację AddBillViewModel (tryb dodawania / edycji)
 * - obsługę stanu ładowania
 * - wyświetlenie formularza rachunku
 * - zapis lub anulowanie zmian
 *
 * Logika walidacji i zapisu znajduje się w AddBillViewModel.
 */
@Composable
fun AddBillScreen(
    expenseId: Int?,
    onBack: () -> Unit
) {
    val viewModel: AddBillViewModel = viewModel(
        key = "AddBillViewModel_${expenseId ?: "new"}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return AddBillViewModel(expenseId) as T
            }
        }
    )

    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(expenseId) {
        viewModel.load()
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        if (state.saved) {
            viewModel.resetForm()
            onBack()
            return@Surface
        }
        if (state.isLoading) {
            LoadingState()
            return@Surface
        }

        AddBillContent(
            state = state,
            onDescriptionChange = { viewModel.updateDescription(it) },
            onAmountChange = { viewModel.updateAmount(it) },
            onNoteChange = { viewModel.updateNote(it) },
            onIntervalChange = { viewModel.updateInterval(it) },
            onDateChange = { viewModel.updateDate(it) },
            onMarkPaidChange = { viewModel.updateMarkPaid(it) },
            onSave = { viewModel.save() },
            onCancel = {
                viewModel.resetForm()
                onBack()
            }
        )
    }
}
// Główny formularz UI dodawania / edycji rachunku
@Suppress("DuplicatedCode")
@Composable
private fun AddBillContent(
    state: AddBillUiState,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onMarkPaidChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val scroll = rememberScrollState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("pl")) }
    // Obsługa klawisza TAB – desktopowy przepływ fokusu między polami
    val focusManager = LocalFocusManager.current
    val descriptionFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }
    val noteFocus = remember { FocusRequester() }
    val dateFocus = remember { FocusRequester() }
    val intervalFocus = remember { FocusRequester() }
    val saveButtonFocus = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(24.dp)
    ) {

        Text(
            text = if (state.isEditMode) "Edytuj rachunek" else "Dodaj rachunek",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        FormTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = "🏷️ Opis",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = descriptionFocus,
            isError = state.descriptionError != null
        )
        ErrorText(state.descriptionError)
        Spacer(Modifier.height(12.dp))
        NumberField(
            value = state.amount,
            onValueChange = onAmountChange,
            label = "💰 Kwota",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = amountFocus,
            isError = state.amountError != null
        )
        ErrorText(state.amountError)
        Spacer(Modifier.height(12.dp))
        FormTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = "📝 Notatka (opcjonalna)",
            modifier = Modifier.fillMaxWidth(),
            focusRequester = noteFocus,
            singleLine = false,
            maxLength = 300
        )

        Spacer(Modifier.height(16.dp))

        Text("📅 Data rachunku", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))

        var datePickerExpanded by remember { mutableStateOf(false) }

        OutlinedButton(
            onClick = { datePickerExpanded = true },
            modifier = Modifier.fillMaxWidth()
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

        FormDropdown(
            label = "🔁 Powtarzaj co",
            items = state.intervals,
            selectedIndex = state.repeatIntervalIndex,
            onSelectedIndexChange = onIntervalChange,
            error = null,
            focusRequester = intervalFocus
        )

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.markPaid,
                onCheckedChange = onMarkPaidChange,
                modifier = Modifier
                    .focusRequester(intervalFocus)
                    .onPreviewKeyEvent {
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        } else {
                            false
                        }
                    }
            )
            Spacer(Modifier.width(8.dp))
            Text("✅ Oznacz jako opłacony")
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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