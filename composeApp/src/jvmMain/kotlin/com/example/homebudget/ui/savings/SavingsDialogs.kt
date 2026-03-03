package com.example.homebudget.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.homebudget.ui.common.dialogs.BaseDialog
import com.example.homebudget.ui.common.dialogs.ConfirmDialog
import com.example.homebudget.ui.common.dialogs.FormDialog
import com.example.homebudget.ui.common.dropdowns.FormDropdown
import com.example.homebudget.ui.common.fields.FormTextField
import com.example.homebudget.ui.common.fields.NumberField
import com.example.homebudget.ui.common.dialogs.CalendarDatePickerDialog
import com.example.homebudget.utils.money.MoneyFormatter
import com.example.homebudget.viewmodel.savings.SavingsNotification
import com.example.homebudget.viewmodel.savings.SavingsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Główny kontener dialogów dla ekranu oszczędności.
 * Renderuje dialog w zależności od aktualnego stanu UI (SavingsUiState).
 */
@Suppress("DuplicatedCode")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsDialogs(viewModel: SavingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    // ➕ DODAJ CEL
    // Dialog dodawania nowego celu oszczędnościowego
    if (state.showAddGoalDialog) {

        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        val selectedPeople = remember { mutableStateListOf<String>() }
        var endDate by remember { mutableStateOf<Long?>(null) }
        var datePickerOpen by remember { mutableStateOf(false) }
        // Obsługa klawisza TAB – desktopowy przepływ fokusu między polami
        val focusManager = LocalFocusManager.current
        val titleFocus = remember { FocusRequester() }
        val amountFocus = remember { FocusRequester() }
        val dateFocus = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            titleFocus.requestFocus()
        }

        FormDialog(
            title = "Dodaj cel",
            confirmText = "Zapisz",
            onConfirm = {
                val parsed = amount.replace(",", ".").toDoubleOrNull()
                if (title.isNotBlank() && parsed != null && parsed > 0) {
                    viewModel.addGoal(title.trim(), parsed, selectedPeople.toList(), endDate)
                }
            },
            onDismiss = viewModel::dismissDialogs
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                FormTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nazwa celu",
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = titleFocus,
                    maxLength = 30
                )

                NumberField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Kwota docelowa",
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = amountFocus
                )

                OutlinedButton(
                    onClick = { datePickerOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(dateFocus)
                        .onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else false
                        }
                ) {
                    Text(
                        if (endDate == null)
                            "\uD83D\uDCC5 Termin (opcjonalnie)"
                        else {
                            val date = Instant.ofEpochMilli(endDate!!)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            "\uD83D\uDCC5 Termin: $date"
                        }
                    )
                }
                if (datePickerOpen) {
                    CalendarDatePickerDialog(
                        initialDate = LocalDate.now(),
                        onConfirm = {
                            endDate = it
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            datePickerOpen = false
                        },
                        onDismiss = { datePickerOpen = false }
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text("Wspólny cel z (opcjonalnie):")

                state.availablePeople
                    .filter { it != "Tylko ja" }
                    .forEach { person ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedPeople.contains(person),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selectedPeople.size < 3) {
                                            selectedPeople.add(person)
                                        }
                                    } else {
                                        selectedPeople.remove(person)
                                    }
                                }
                            )
                            Text(person)
                        }
                    }
                Text(
                    text = "Jeśli nic nie wybierzesz → cel będzie tylko dla Ciebie." +
                            "Możesz dodać maksymalnie 3 osoby.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ✏️ EDYTUJ CEL
    state.showEditGoal?.let { goal ->
        var title by remember(goal.id) { mutableStateOf(goal.title) }
        var amount by remember(goal.id) { mutableStateOf(goal.targetAmount.toString()) }
        var endDate by remember(goal.id) { mutableStateOf(goal.endDate) }
        var datePickerOpen by remember { mutableStateOf(false) }

        val selectedPeople = remember(goal.id) {
            mutableStateListOf<String>().apply {
                val raw = goal.sharedWith
                if (!raw.isNullOrBlank()) {
                    raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { add(it) }
                }
            }
        }

        FormDialog(
            title = "Edytuj cel",
            confirmText = "Zapisz",
            onConfirm = {
                val parsed = amount.replace(",", ".").toDoubleOrNull()
                if (title.isNotBlank() && parsed != null && parsed > 0) {
                    viewModel.updateGoal(goal, title.trim(), parsed, selectedPeople.toList(), endDate)
                }
            },
            onDismiss = viewModel::dismissDialogs
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nazwa celu") },
                    modifier = Modifier.fillMaxWidth()
                )

                NumberField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Kwota docelowa",
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { datePickerOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (endDate == null)
                            "\uD83D\uDCC5 Termin (opcjonalnie)"
                        else {
                            val date = Instant.ofEpochMilli(endDate!!)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            "\uD83D\uDCC5 Termin: $date"
                        }
                    )
                }
                if (datePickerOpen) {
                    CalendarDatePickerDialog(
                        initialDate = LocalDate.now(),
                        onConfirm = {
                            endDate = it
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            datePickerOpen = false
                        },
                        onDismiss = { datePickerOpen = false }
                    )
                }

                if (endDate != null) {
                    TextButton(
                        onClick = { endDate = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("❌ Usuń termin")
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Wspólny cel z (opcjonalnie):")

                state.availablePeople
                    .filter { it != "Tylko ja" }
                    .forEach { person ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedPeople.contains(person),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selectedPeople.size < 3) {
                                            selectedPeople.add(person)
                                        }
                                    } else {
                                        selectedPeople.remove(person)
                                    }
                                }
                            )
                            Text(person)
                        }
                    }
                Text(
                    text = "Jeśli nic nie wybierzesz → cel będzie tylko dla Ciebie." +
                            "Możesz dodać maksymalnie 3 osoby.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ➕ DODAJ WPŁATĘ (z wyborem osoby)
    state.showAddContributionFor?.let { goal ->
        var amount by remember(goal.id) { mutableStateOf("") }

        val peopleForGoal = remember(goal.id) {
            buildList {
                add("Tylko ja")
                val raw = goal.sharedWith
                if (!raw.isNullOrBlank()) {
                    raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { add(it) }
                }
            }.distinct()
        }

        var selectedPersonIndex by remember(goal.id) { mutableStateOf(0) }

        FormDialog(
            title = "Dodaj wpłatę",
            confirmText = "Zapisz",
            onConfirm = {
                val parsed = amount.replace(",", ".").toDoubleOrNull()
                val person = peopleForGoal.getOrNull(selectedPersonIndex) ?: "Tylko ja"
                if (parsed != null && parsed > 0) {
                    viewModel.addContribution(goal, parsed, person)
                }
            },
            onDismiss = viewModel::dismissDialogs
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                NumberField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Kwota",
                    modifier = Modifier.fillMaxWidth()
                )

                // jeśli cel wspólny → pokaż wybór osoby
                if (peopleForGoal.size > 1) {
                    FormDropdown(
                        label = "Kto wpłaca",
                        items = peopleForGoal,
                        selectedIndex = selectedPersonIndex,
                        onSelectedIndexChange = { selectedPersonIndex = it }
                    )
                }
            }
        }
    }

    // Wypłać środki
    state.showWithdrawFor?.let { goal ->
        var amount by remember { mutableStateOf("") }
        val peopleForGoal = remember(goal.id) {
            buildList {
                add("Tylko ja")
                goal.sharedWith
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.forEach { add(it) }
            }.distinct()
        }
        var selectedPersonIndex by remember { mutableStateOf(0) }
        var error by remember { mutableStateOf<String?>(null) }
        FormDialog(
            title = "Wypłać środki",
            confirmText = "Zapisz",
            onConfirm = {
                val parsed = amount.replace(",", ".").toDoubleOrNull()
                val person = peopleForGoal.getOrNull(selectedPersonIndex) ?: "Tylko ja"

                error = when {
                    parsed == null || parsed <= 0 ->
                        "Podaj poprawną kwotę."
                    parsed > goal.savedAmount ->
                        "Nie możesz wypłacić więcej niż ${goal.savedAmount} zł."
                    else -> null
                }

                if (error == null) {
                    viewModel.withdraw(goal, parsed!!, person)
                }
            },
            onDismiss = viewModel::dismissDialogs
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NumberField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Kwota do wypłaty",
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (peopleForGoal.size > 1) {
                    FormDropdown(
                        label = "Kto wypłaca",
                        items = peopleForGoal,
                        selectedIndex = selectedPersonIndex,
                        onSelectedIndexChange = { selectedPersonIndex = it }
                    )
                }
            }
        }
    }

    // 📜 HISTORIA WPŁAT
    state.showHistoryFor?.let { goal ->
        BaseDialog(
            title = "Historia – ${goal.title}",
            content = {
                if (state.contributions.isEmpty()) {
                    Text("Brak wpłat")
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        // Podsumowanie
                        val grouped = state.contributions.groupBy { it.personName }
                        grouped.forEach { (person, list) ->
                            val sum = list.sumOf { it.amount }
                            Text(
                                text = "$person: ${if (sum >= 0) "+" else "-"}${MoneyFormatter.format(abs(sum))} zł",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider()

                        // Szczegóły
                        state.contributions.forEach {
                            val date = Instant.ofEpochMilli(it.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val sign = if (it.amount < 0) "-" else "+"
                            val color =
                                if (it.amount < 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary

                            Text(
                                text = "${it.personName}: $sign${MoneyFormatter.format(abs(it.amount))} zł • $date",
                                color = color,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmText = "OK",
            onConfirm = viewModel::dismissDialogs,
            onDismiss = viewModel::dismissDialogs
        )
    }
    // Dialog potwierdzający usunięcie celu
    state.showDeleteGoal?.let { goal ->
        ConfirmDialog(
            title = "Usuń cel",
            message = "Czy na pewno chcesz usunąć cel „${goal.title}”?\n\n" +
            "Zostaną również usunięte wszystkie wpłaty do tego celu",
            confirmText = "Usuń",
            dismissText = "Anuluj",
            onConfirm = {
                viewModel.deleteGoal(goal)
                viewModel.dismissDialogs()
            },
            onDismiss = viewModel::dismissDialogs
        )
    }

    // Dialog Gratulacji/Termin
    state.notification?.let { notification ->
        when (notification) {
            is SavingsNotification.GoalCompleted -> {
                ConfirmDialog(
                    title = "🎉 Gratulacje!",
                    message = "Cel oszczędnościowy „${notification.title}” został osiągnięty.\n\nŚwietna robota! 💪",
                    confirmText = "Super 🎉",
                    onConfirm = { viewModel.clearNotifications() }
                )

            }
            is SavingsNotification.DeadlineSoon -> {
                val days = notification.daysLeft
                val title = when {
                    days > 1 -> "⏰ Zbliża się termin"
                    days == 1L -> "⚠️ Termin jutro!"
                    days == 0L -> "❗ Termin dziś!"
                    else -> "⚠️ Termin minął"
                }
                val message = when {
                    days > 1 ->
                        "Do terminu realizacji celu „${notification.title}” pozostało $days dni."
                    days == 1L ->
                        "Jutro mija termin realizacji celu „${notification.title}”."
                    days == 0L ->
                        "Dziś mija termin realizacji celu „${notification.title}”."
                    else ->
                        "Termin realizacji celu „${notification.title}” już minął."
                }
                ConfirmDialog(
                    title = title,
                    message = message,
                    onConfirm = { viewModel.clearNotifications() }
                )
            }
        }
    }
}
