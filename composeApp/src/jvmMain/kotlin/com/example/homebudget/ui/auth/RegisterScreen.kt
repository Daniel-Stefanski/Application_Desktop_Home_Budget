package com.example.homebudget.ui.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.fields.EmailField
import com.example.homebudget.ui.common.fields.ErrorText
import com.example.homebudget.ui.common.fields.FormTextField
import com.example.homebudget.ui.common.fields.PasswordField
import com.example.homebudget.ui.theme.ThemeMode
import com.example.homebudget.viewmodel.auth.RegistrationViewModel
import com.example.homebudget.viewmodel.theme.ThemeViewModel
import com.example.homebudget.ui.common.constants.PASSWORD_HINT

/**
 * RegisterScreen
 *
 * Ekran rejestracji nowego użytkownika aplikacji HomeBudget.
 *
 * Odpowiada za:
 * - prezentację formularza rejestracyjnego
 * - obsługę walidacji w czasie rzeczywistym
 * - nawigację klawiaturą (TAB / Enter)
 * - wyświetlenie dialogu regulaminu
 * - przejście do ekranu logowania po sukcesie
 */
@Suppress("DuplicatedCode")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegisterScreen(
    themeViewModel: ThemeViewModel,
    onBackToLogin: () -> Unit,
    onSuccessRegister: () -> Unit
) {
    // Rejestracja zawsze odbywa się jasny motywie
    MaterialTheme(colorScheme = lightColorScheme()) {
        // ViewModel odpowiedzialny za logikę rejestracji
        val viewModel: RegistrationViewModel = viewModel()
        // Aktualny stan UI (StateFlow -> State)
        val uiState by viewModel.uiState.collectAsState()

        // Lokalny stan UI (tylko dla widoku)
        var showPassword by remember { mutableStateOf(false) }
        var showConfirmPassword by remember { mutableStateOf(false) }
        var showTermsDialog by remember { mutableStateOf(false) }

        // Manager fokusu do obsługi nawigacji klawiaturą
        val focusManager = LocalFocusManager.current
        // FocusRequestery – kontrolują kolejność TAB
        val nameFocus = remember { FocusRequester() }
        val emailFocus = remember { FocusRequester() }
        val passwordFocus = remember { FocusRequester() }
        val confirmPasswordFocus = remember { FocusRequester() }
        val termsDialogFocus = remember { FocusRequester() }

        // Główny kontener – centruje formularz
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Kolumna z formularzem rejestracji
            Column(
                modifier = Modifier.width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nagłówek ekranu
                Text("Zarejestruj się", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))
                // Pole opcjonalnej nazwy użytkownika
                FormTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChanged,
                    label = "Imię/Nick (opcjonalnie)",
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = nameFocus,
                    maxLength = 20

                )
                Spacer(Modifier.height(8.dp))
                // Pole email z walidają
                EmailField(
                    value = uiState.username,
                    onValueChange = viewModel::onUsernameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = emailFocus,
                    isError = uiState.usernameError != null
                )
                ErrorText(uiState.usernameError)
                Spacer(Modifier.height(8.dp))
                // Pole hasła z wymaganiami i z pokaż/ukryj hasło
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PasswordField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "Hasło",
                            modifier = Modifier.weight(1f),
                            focusRequester = passwordFocus,
                            isError = uiState.passwordError != null,
                            showPassword = showPassword,
                            onToggleVisibility = { showPassword = !showPassword }
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
                                contentDescription = "Wymagania hasła.",
                                modifier = Modifier.padding(start = 8.dp).size(20.dp)
                            )
                        }
                    }
                    ErrorText(uiState.passwordError)
                }
                Spacer(Modifier.height(8.dp))
                // Pole powtórz hasło z funkcją pokaż/ukryj hasło
                PasswordField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChanged,
                    label = "Powtórz hasło",
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = confirmPasswordFocus,
                    isError = uiState.confirmPasswordError != null,
                    showPassword = showConfirmPassword,
                    onToggleVisibility = { showConfirmPassword = !showConfirmPassword }
                )
                ErrorText(uiState.confirmPasswordError)
                Spacer(Modifier.height(8.dp))
                // Checkbox z regulaminem który musimy zaznaczyć
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .focusRequester(termsDialogFocus)
                        .onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else false
                        }
                ) {
                    Checkbox(
                        checked = uiState.acceptedTerms,
                        onCheckedChange = viewModel::onTermsAccepted
                    )
                    Text("Akceptuję ")
                    Text(
                        text = "Regulamin",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            showTermsDialog = true
                        }
                    )
                    uiState.termsError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                // Utworznie konta
                Button(
                    onClick = {
                        viewModel.registerUser {
                            themeViewModel.setTheme(ThemeMode.LIGHT)
                            onSuccessRegister()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else false
                        }
                ) {
                    Text("Zarejestruj się")
                }
                // Powrót do ekranu logowania
                TextButton(onClick = onBackToLogin) {
                    Text("Masz już konto? Zaloguj się")
                }
            }
            if (showTermsDialog) {
                TermsDialog(
                    onAccept = {
                        viewModel.onTermsAccepted(true)
                        showTermsDialog = false
                    },
                    onDismiss = {
                        showTermsDialog = false
                    }
                )
            }
        }
    }
}
