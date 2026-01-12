package com.example.homebudget.ui.common.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * BaseDialog
 *
 * Bazowy wrapper na AlertDialog.
 * Zapewnia wspólną strukturę dla wszystkich dialogów w aplikacji.
 *
 * NIE zawiera logiki biznesowej.
 */
@Composable
fun BaseDialog(
    title: String,
    content: @Composable () -> Unit,

    confirmText: String,
    onConfirm: () -> Unit,

    onDismiss: () -> Unit,
    dismissText: String? = null,

    // Opcjonalny lewy przycisk, np. wyczyść
    extraText: String? = null,
    onExtra: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                content()
            }
        },
        // Robimy całą stopkę w confirmButton, żeby mieć 3 przyciski
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Wyczyść (opcjonalnie)
                if (extraText != null && onExtra != null) {
                    TextButton(onClick = onExtra) {
                        Text(extraText)
                    }
                }
                Spacer(Modifier.weight(1f))
                // Anuluj (opcjonalnie)
                if (dismissText != null) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissText)
                    }
                }
                // Zastosuj/Ok
                Button(onClick = onConfirm) {
                    Text(confirmText)
                }
            }
        },
        // Nie używamy, bo stopkę robimy ręcznie
        dismissButton = {}
    )
}