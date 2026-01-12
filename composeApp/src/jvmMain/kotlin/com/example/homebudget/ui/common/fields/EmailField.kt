package com.example.homebudget.ui.common.fields

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@Composable
fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isError: Boolean = false
) {
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Email",
        modifier = modifier,
        focusRequester = focusRequester,
        maxLength = 120,
        isError = isError
    )
}