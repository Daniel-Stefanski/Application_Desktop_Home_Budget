package com.example.homebudget.ui.common.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * ConfirmDialog
 *
 * Uniwersalny dialog potwierdzenia:
 * - usuwanie
 * - reset
 * - informacje
 * - ostrzeżenia
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    BaseDialog(
        title = title,
        content = { Text(message) },
        confirmText = confirmText,
        onConfirm = onConfirm,
        onDismiss = { onDismiss?.invoke() ?: onDismiss },
        dismissText = dismissText
    )
}