package com.example.homebudget.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.dialogs.ConfirmDialog
import com.example.homebudget.ui.common.fields.FormTextField
import com.example.homebudget.viewmodel.auth.ResetPasswordViewModel

@Suppress("DuplicatedCode")
@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit,
    onSuccessReset: () -> Unit,
    onGoToSetNewPassword: () -> Unit
) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        val viewModel: ResetPasswordViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()

        var showSuccessDialog by remember { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current
        val emailFocus = remember { FocusRequester() }

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
                        title = "Sprawdź pocztę",
                        message = "Jeśli konto istnieje, wysłaliśmy email z linkiem do autoryzacji resetu hasła.",
                        confirmText = "OK",
                        onConfirm = {
                            showSuccessDialog = false
                        }
                    )
                }

                Text("Resetuj hasło", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))

                FormTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Podaj swój email",
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = emailFocus,
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
                        viewModel.resetPassword {
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
                    Text("Wyślij link resetu")
                }

                if (uiState.isLoading) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }

                TextButton(onClick = onBackToLogin) {
                    Text("← Wróć do logowania")
                }

                TextButton(onClick = onGoToSetNewPassword) {
                    Text("Mam token resetu")
                }
            }
        }
    }
}
