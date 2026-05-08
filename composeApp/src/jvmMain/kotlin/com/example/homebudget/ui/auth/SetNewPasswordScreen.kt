package com.example.homebudget.ui.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.constants.PASSWORD_HINT
import com.example.homebudget.ui.common.dialogs.ConfirmDialog
import com.example.homebudget.ui.common.fields.FormTextField
import com.example.homebudget.ui.common.fields.PasswordField
import com.example.homebudget.viewmodel.auth.SetNewPasswordViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetNewPasswordScreen(
    initialToken: String?,
    onBackToLogin: () -> Unit
) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        val viewModel: SetNewPasswordViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        var showNewPassword by remember { mutableStateOf(false) }
        var showConfirmPassword by remember { mutableStateOf(false) }
        var showSuccessDialog by remember { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current
        val tokenFocus = remember { FocusRequester() }
        val newPasswordFocus = remember { FocusRequester() }
        val confirmPasswordFocus = remember { FocusRequester() }

        LaunchedEffect(initialToken) {
            viewModel.setInitialToken(initialToken)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.width(350.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showSuccessDialog) {
                    ConfirmDialog(
                        title = "Sukces",
                        message = "Hasło zostało zmienione. Możesz zalogować się nowym hasłem.",
                        confirmText = "Przejdź do logowania",
                        onConfirm = {
                            showSuccessDialog = false
                            onBackToLogin()
                        }
                    )
                }

                Text("Ustaw nowe hasło", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))

                FormTextField(
                    value = uiState.token,
                    onValueChange = viewModel::onTokenChanged,
                    label = "Token resetu",
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = tokenFocus,
                    isError = uiState.errorMessage?.contains("token", ignoreCase = true) == true
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PasswordField(
                        value = uiState.newPassword,
                        onValueChange = viewModel::onNewPasswordChanged,
                        label = "Nowe hasło",
                        modifier = Modifier.weight(1f),
                        focusRequester = newPasswordFocus,
                        showPassword = showNewPassword,
                        onToggleVisibility = { showNewPassword = !showNewPassword },
                        isError = uiState.errorMessage != null
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
                            modifier = Modifier.padding(start = 8.dp).size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                PasswordField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChanged,
                    label = "Powtórz hasło",
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = confirmPasswordFocus,
                    showPassword = showConfirmPassword,
                    onToggleVisibility = { showConfirmPassword = !showConfirmPassword },
                    isError = uiState.errorMessage != null
                )

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.setNewPassword {
                            showSuccessDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else false
                        },
                    enabled = !uiState.isLoading
                ) {
                    Text("Ustaw nowe hasło")
                }

                if (uiState.isLoading) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }

                TextButton(onClick = onBackToLogin) {
                    Text("← Wróć do logowania")
                }
            }
        }
    }
}
