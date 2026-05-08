package com.example.homebudget.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.theme.ThemeMode
import com.example.homebudget.ui.common.dialogs.ConfirmDialog
import com.example.homebudget.ui.common.dropdowns.FormDropdown
import com.example.homebudget.ui.common.fields.EmailField
import com.example.homebudget.ui.common.fields.FormTextField
import com.example.homebudget.ui.common.fields.PasswordField
import com.example.homebudget.viewmodel.settings.SettingsViewModel
import com.example.homebudget.viewmodel.theme.ThemeViewModel
import com.example.homebudget.ui.common.cards.ExpandableCard
import com.example.homebudget.ui.common.feedback.LoadingState
import com.example.homebudget.ui.common.constants.PASSWORD_HINT
import com.example.homebudget.utils.settings.CategoryColorPalette

/**
 * SettingsScreen
 *
 * Główny ekran ustawień aplikacji.
 * Odpowiada wyłącznie za warstwę UI (Compose).
 *
 * Zakres odpowiedzialności:
 * - prezentacja ustawień konta użytkownika
 * - zarządzanie kategoriami i osobami
 * - zmiana motywu aplikacji
 * - reset danych użytkownika
 * - obsługa dialogów potwierdzających
 *
 * Logika biznesowa i operacje na bazie danych
 * są delegowane do SettingsViewModel.
 */
@Suppress("DuplicatedCode")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    onLogout: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    // Dialog potwierdzenia usunięcia
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var deleteTargetType by remember { mutableStateOf<DeleteTarget?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp)
            ) {
                if (state.isLoading) {
                    LoadingState()
                    return@Box
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    item {
                        Text(
                            text = "⚙ Ustawienia",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    // Ustawienia konta
                    item {
                        ExpandableCard(
                            title = "\uD83D\uDC64 Ustawienia konta",
                            expanded = expandedSection == "account",
                            onToggle = { expandedSection = if (expandedSection == "account") null else "account" }
                        ) {
                            var name by remember(state.userName) { mutableStateOf(state.userName) }
                            var email by remember(state.userEmail) { mutableStateOf(state.userEmail) }
                            var oldPass by remember { mutableStateOf("") }
                            var newPass by remember { mutableStateOf("") }
                            var error by remember { mutableStateOf<String?>(null) }

                            var showOldPassword by remember { mutableStateOf(false) }
                            var showNewPassword by remember { mutableStateOf(false) }

                            // Obsługa nawigacji klawiaturą (TAB)
                            val focusManager = LocalFocusManager.current
                            val nameFocus = remember { FocusRequester() }
                            val emailFocus = remember { FocusRequester() }
                            val oldPassFocus = remember { FocusRequester() }
                            val newPassFocus = remember { FocusRequester() }
                            val saveButtonFocus = remember { FocusRequester() }

                            FormTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = "Imię / Nick",
                                modifier = Modifier.fillMaxWidth(),
                                focusRequester = nameFocus,
                                maxLength = 20
                            )
                            EmailField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(emailFocus)
                                    .onPreviewKeyEvent {
                                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                            focusManager.moveFocus(FocusDirection.Next)
                                            true
                                        } else false
                                    },
                                isError = error?.contains("Email", ignoreCase = true) == true
                            )


                            PasswordField(
                                value = oldPass,
                                onValueChange = { oldPass = it },
                                label = "Stare hasło",
                                modifier = Modifier.fillMaxWidth(),
                                focusRequester = oldPassFocus,
                                showPassword = showOldPassword,
                                onToggleVisibility = { showOldPassword = !showOldPassword },
                                isError = error?.contains("Hasło", true) == true
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PasswordField(
                                    value = newPass,
                                    onValueChange = { newPass = it },
                                    label = "Nowe hasło",
                                    modifier = Modifier.weight(1f),
                                    focusRequester = newPassFocus,
                                    showPassword = showNewPassword,
                                    onToggleVisibility = { showNewPassword = !showNewPassword },
                                    isError = error?.contains("Hasło", ignoreCase = true) == true
                                )
                                TooltipArea(
                                    tooltip = {
                                        Surface(
                                            tonalElevation = 4.dp,
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Text(
                                                text = PASSWORD_HINT,
                                                modifier = Modifier.padding(8.dp),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Wymagania hasła",
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(20.dp)
                                    )
                                }
                            }

                            error?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.saveAccountChanges(
                                        name, email, oldPass, newPass,
                                        onError = { error = it },
                                        onSuccess = {
                                            error = null
                                            oldPass = ""
                                            newPass = ""
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(saveButtonFocus)
                            ) {
                                Text("Zapisz zmiany")
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Usuń konto")
                            }
                        }
                    }

                    // Ustawienia kategorii
                    item {
                        ExpandableCard(
                            title = "\uD83D\uDCC2 Kategorie",
                            expanded = expandedSection == "categories",
                            onToggle = { expandedSection = if (expandedSection == "categories") null else "categories" }
                        ) {
                            state.categories.forEach { category ->
                                val colorHex = state.categoryColors[category] ?: "#CCCCCC"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 36.dp, height = 20.dp)
                                            .padding(end = 12.dp)
                                            .background(
                                                colorFromHex(colorHex),
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .clickable {
                                                selectedCategory = category
                                                showColorPicker = true
                                            }
                                    )

                                    Text(category, Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            deleteTargetType = DeleteTarget.Category(category)
                                            showConfirmDeleteDialog = true
                                        }
                                    ) {
                                        Text("Usuń")
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            AddItemRow(
                                placeholder = "Nowa kategoria",
                                onAdd = viewModel::addCategory
                            )
                        }
                    }

                    // Ustawienia osób
                    item {
                        ExpandableCard(
                            title = "👥 Osoby",
                            expanded = expandedSection == "persons",
                            onToggle = { expandedSection = if (expandedSection == "persons") null else "persons" }
                        ) {
                            state.people.forEach {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(it, Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            deleteTargetType = DeleteTarget.Person(it)
                                            showConfirmDeleteDialog = true
                                        }
                                    ) {
                                        Text("Usuń")
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            AddItemRow(
                                placeholder = "Nowa osoba",
                                onAdd = viewModel::addPerson
                            )
                        }
                    }

                    // Ustawienie stylu motywu
                    item {
                        ExpandableCard(
                            title = "🎨 Motyw aplikacji",
                            expanded = expandedSection == "theme",
                            onToggle = { expandedSection = if (expandedSection == "theme") null else "theme"}
                        ) {

                            val currentTheme by themeViewModel.theme.collectAsState()

                            /**
                             * Pojedynczy wiersz wyboru motywu aplikacji.
                             *
                             * - Wyświetla RadioButton oraz etykietę
                             * - Obsługuje kliknięcie w cały wiersz
                             * - Nie przechowuje stanu – stan pochodzi z ViewModelu
                             */
                            ThemeRow(
                                label = "☀️ Jasny",
                                selected = currentTheme == ThemeMode.LIGHT
                            ) {
                                themeViewModel.setTheme(ThemeMode.LIGHT)
                            }

                            ThemeRow(
                                label = "🌙 Ciemny",
                                selected = currentTheme == ThemeMode.DARK
                            ) {
                                themeViewModel.setTheme(ThemeMode.DARK)
                            }

                            ThemeRow(
                                label = "⚙️ Systemowy",
                                selected = currentTheme == ThemeMode.SYSTEM
                            ) {
                                themeViewModel.setTheme(ThemeMode.SYSTEM)
                            }
                        }
                    }

                    // Ustawienia aplikacji
                    item {
                        ExpandableCard(
                            title = "🧹 Preferencje użytkownika",
                            expanded = expandedSection == "data_application",
                            onToggle = { expandedSection = if (expandedSection == "data_application") null else "data_application" }
                        ) {
                            val categoryItems = listOf("Brak") + state.categories
                            // Domyślna kategoria
                            FormDropdown(
                                label = "Domyślna kategoria",
                                items = categoryItems,
                                selectedIndex = categoryItems.indexOf(state.defaultCategory ?: "Brak"),
                                onSelectedIndexChange = {
                                    val value = categoryItems[it]
                                    viewModel.updateDefaultCategory(
                                        if (value == "Brak") null else value
                                    )
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                            val paymentItems = listOf("Brak", "Gotówka", "Karta", "Blik", "Przelew")
                            // Domyślna płątność
                            FormDropdown(
                                label = "Domyślna metoda płatności",
                                items = paymentItems,
                                selectedIndex = paymentItems.indexOf(state.defaultPaymentMethod ?: "Brak"),
                                onSelectedIndexChange = {
                                    val value = paymentItems[it]
                                    viewModel.updateDefaultPaymentMethod(
                                        if (value == "Brak") null else value
                                    )
                                }
                            )
                            Spacer(Modifier.height(16.dp))

                            // Reset danych
                            Button(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Resetuj dane (zachowaj konto)")
                            }
                        }
                    }
                }
                // Dialog od usunięcia konta
                if (showDeleteDialog) {
                    ConfirmDialog(
                        title = "Usuń konto",
                        message = "Czy na pewno chcesz trwale usunąć konto i wszystkie dane?",
                        confirmText = "Usuń",
                        dismissText = "Anuluj",
                        onConfirm = {
                            viewModel.deleteAccount {
                                showDeleteDialog = false
                                onLogout()
                            }
                        },
                        onDismiss = {
                            showDeleteDialog = false
                        }
                    )
                }

                // Dialog od resetu danych
                if (showResetDialog) {
                    ConfirmDialog(
                        title = "Reset danych",
                        message = "Czy na pewno chcesz zresetować wszystkie dane aplikacji?\nKonto użytkownika zostanie zachowane.",
                        confirmText = "Resetuj",
                        dismissText = "Anuluj",
                        onConfirm = {
                            viewModel.resetUserData { showResetDialog = false }
                        },
                        onDismiss = {
                            showResetDialog = false
                        }
                    )
                }

                // Dialog od koloru
                if (showColorPicker && selectedCategory != null) {
                    val currentColor = state.categoryColors[selectedCategory!!]
                    ColorPickerDialog(
                        selectedColorHex = currentColor,
                        onDismiss = { showColorPicker = false },
                        onColorSelected = { hex ->
                            viewModel.updateCategoryColor(selectedCategory!!, hex)
                            showColorPicker = false
                        }
                    )
                }

                // Dialog do usunięcia kategorii/osoby
                if (showConfirmDeleteDialog && deleteTargetType != null) {
                    val target = deleteTargetType!!
                    val (title, message, onConfirmAction) = when (target) {
                        is DeleteTarget.Category -> Triple(
                            "Usuń kategorię",
                            "Czy na pewno chcesz usunąć kategorię „${target.name}”?\n\n" +
                                    "Istniejące wydatki pozostaną przypisane do tej kategorii.",
                            { viewModel.removeCategory(target.name) }
                        )
                        is DeleteTarget.Person -> Triple(
                            "Usuń osobę",
                            "Czy na pewno chcesz usunąć osobę „${target.name}”?\n\n" +
                                    "Istniejące wydatki pozostaną przypisane do tej osoby.",
                            { viewModel.removePerson(target.name) }
                        )
                    }

                    ConfirmDialog(
                        title = title,
                        message = message,
                        confirmText = "Usuń",
                        dismissText = "Anuluj",
                        onConfirm = {
                            onConfirmAction()
                            showConfirmDeleteDialog = false
                            deleteTargetType = null
                        },
                        onDismiss = {
                            showConfirmDeleteDialog = false
                            deleteTargetType = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
/**
 * Uniwersalny komponent do dodawania nowej pozycji tekstowej.
 *
 * Używany do:
 * - dodawania kategorii
 * - dodawania osób
 *
 * Odpowiada za:
 * - walidację długości tekstu
 * - przycinanie spacji
 * - reset pola po dodaniu
 */
@Composable
private fun AddItemRow(
    placeholder: String,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Row {
        OutlinedTextField(
            value = text,
            onValueChange = {
                if (it.length <= 20) {
                    text = it
                }
            },
            placeholder = { Text(placeholder) },
            supportingText = {
                Text("${text.length}/20")
            },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            if (text.isNotBlank()) {
                onAdd(text.trim())
                text = ""
            }
        }) {
            Text("Dodaj")
        }
    }
}

private fun colorFromHex(hex: String): Color {
    val cleaned = hex.trim().removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: 0xCCCCCC
    return when (cleaned.length) {
        6 -> Color((0xFF000000 or value).toInt()) // RRGGBB
        8 -> Color(value.toInt())                // AARRGGBB
        else -> Color(0xFFCCCCCC.toInt())
    }
}
/**
 * Dialog wyboru koloru kategorii.
 *
 * - Prezentuje predefiniowaną paletę kolorów
 * - Po kliknięciu zwraca hex koloru do ViewModelu
 * - Nie zapisuje danych – tylko emituje wybór
 */
@Composable
fun ColorPickerDialog(
    selectedColorHex: String?,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {

    val colors = CategoryColorPalette.COLORS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz kolor") },
        text = {
            Column {
                colors.chunked(8).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        row.forEach { hex ->
                            val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        colorFromHex(hex),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected)
                                            Color.Black
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable {
                                        if (!isSelected) {
                                        onColorSelected(hex)
                                            }
                                    }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
/**
 * Typ obiektu przeznaczonego do usunięcia.
 *
 * Wykorzystywany w dialogu potwierdzenia,
 * aby rozróżnić:
 * - usuwanie kategorii
 * - usuwanie osoby
 */
sealed class DeleteTarget {
    data class Category(val name: String) : DeleteTarget()
    data class Person( val name: String) : DeleteTarget()
}
