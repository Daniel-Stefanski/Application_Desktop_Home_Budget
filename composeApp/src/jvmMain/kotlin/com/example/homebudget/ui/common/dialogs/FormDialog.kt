package com.example.homebudget.ui.common.dialogs

import androidx.compose.runtime.Composable

/**
 * FormDialog
 *
 * Dialog przeznaczony do wyświetlania formularzy:
 * - filtry
 * - dodawanie / edycja danych
 * - ustawienia w dialogach
 *
 * Oparty o BaseDialog.
 * NIE zawiera logiki formularza ani walidacji.
 */
@Composable
fun FormDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,

    clearText: String? = null,
    onClear: (() -> Unit)? = null,

    dismissText: String = "Anuluj",
    onDismiss: () -> Unit,

    scrollable: Boolean = true,
    content: @Composable () -> Unit
) {
    BaseDialog(
        title = title,
        content = {
            if (scrollable) {
                ScrollableDialogContent { content() }
            } else {
                content()
            }
        },
        confirmText = confirmText,
        onConfirm = onConfirm,

        extraText = clearText,
        onExtra = onClear,

        onDismiss = onDismiss,
        dismissText = dismissText
    )
}