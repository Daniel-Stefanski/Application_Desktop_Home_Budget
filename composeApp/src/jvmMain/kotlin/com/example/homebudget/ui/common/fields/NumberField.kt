package com.example.homebudget.ui.common.fields

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.example.homebudget.utils.money.AmountParser

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    maxLength: Int? = null,
    isError: Boolean = false
) {
    FormTextField(
        value = value,
        onValueChange = { new ->
            // Pełna kontrola input przez AmountParser
            val sanitized = AmountParser.sanitize(new)
            onValueChange(sanitized)
        },
        label = label,
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused && AmountParser.isValid(value)) {
                onValueChange(AmountParser.format(value))
            }
        },
        focusRequester = focusRequester,
        maxLength = maxLength,
        isError = isError
    )
}
