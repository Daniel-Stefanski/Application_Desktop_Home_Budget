package com.example.homebudget.ui.common.fields

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isError: Boolean = false,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    maxLength: Int? = null
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            val v = maxLength?.let { new.take(it) } ?: new
            onValueChange(v)
        },
        label = { Text(label) },
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent {
                if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                    focusManager.moveFocus(FocusDirection.Next)
                    true
                } else false
            },
        singleLine = true,
        isError = isError,
        visualTransformation =
            if (showPassword) VisualTransformation.None
            else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector =
                        if (showPassword) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff,
                    contentDescription = null
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor =
                if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor =
                if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outline
        )
    )
}