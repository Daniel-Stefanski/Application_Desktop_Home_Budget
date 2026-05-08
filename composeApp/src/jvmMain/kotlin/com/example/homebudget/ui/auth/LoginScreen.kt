package com.example.homebudget.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import com.example.homebudget.AppLogo
import com.example.homebudget.ui.common.fields.EmailField
import com.example.homebudget.ui.common.fields.ErrorText
import com.example.homebudget.ui.common.fields.PasswordField
import com.example.homebudget.viewmodel.auth.LoginViewModel

/**
 * LoginScreen
 *
 * Ekran logowania użytkownika do aplikacji.
 * Obsługuje:
 * - logowanie email + hasło
 * - zapamiętanie użytkownika (checkbox)
 * - nawigację klawiaturą (TAB / Enter)
 * - przejście do rejestracji i resetu hasła
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToResetPassword: () -> Unit
) {
    // Domyślnie Logowanie ma motyw Jasny
    MaterialTheme(colorScheme = lightColorScheme()) {
        LaunchedEffect(viewModel.loginSuccess) {
            if (viewModel.loginSuccess) {
                onLoginSuccess()
            }
        }

        // Fokus klawiatury do nawigacji TAB
        val focusManager = LocalFocusManager.current
        val emailFocus = remember { FocusRequester() }
        val passwordFocus = remember { FocusRequester() }
        val rememberMeFocus = remember { FocusRequester() }
        val isError = viewModel.errorMessage != null

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier.width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo aplikacji
                AppLogo(
                    modifier = Modifier.padding(bottom = 10.dp),
                    size = 250.dp
                )
                // Tytuł ekranu
                Text("Zaloguj się", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))

                // Pole email
                EmailField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = emailFocus,
                    isError = isError
                )
                Spacer(Modifier.height(12.dp))

                // Pole hasła z możliwością pokazania/ukrycia
                PasswordField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = "Hasło",
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = passwordFocus,
                    isError = isError,
                    showPassword = viewModel.showPassword,
                    onToggleVisibility = {
                        viewModel.showPassword = !viewModel.showPassword
                    }
                )
                Spacer(Modifier.height(12.dp))

                // Checkbox "Zapamiętaj mnie" - obsługiwany klawiaturą
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = viewModel.rememberMe,
                        onCheckedChange = { viewModel.rememberMe = it },
                        modifier = Modifier
                            .focusRequester(rememberMeFocus)
                            .onPreviewKeyEvent {
                                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                    focusManager.moveFocus(FocusDirection.Next)
                                    true
                                } else false
                            }
                    )
                    Text("Zapamiętaj mnie")
                }

                // Wyświetlany błąd logowania
                ErrorText(viewModel.errorMessage)
                Spacer(Modifier.height(8.dp))

                // Przycisk logowania - Enter działa automatycznie w Compose Desktop
                Button(
                    onClick = { viewModel.login() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else false
                        }
                ) {
                    Text("Zaloguj")
                }
                Spacer(Modifier.height(12.dp))

                // Przycisk przejścia do Rejestracji Konta
                TextButton(onClick = { onGoToRegister() }) {
                    Text("Nie masz konta? Zarejestruj się")
                }

                // Przycisk prześcia do Resetowania Hasła
                TextButton(onClick = { onGoToResetPassword() }) {
                    Text("Zapomniałeś hasła? Zresetuj je")
                }
            }
        }
    }
}
