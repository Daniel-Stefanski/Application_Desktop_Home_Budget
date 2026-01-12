package com.example.homebudget.ui.common.dropdowns

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * BaseDropdown
 *
 * Bazowy wrapper na ExposedDropdownMenu.
 * Zapewnia:
 * - scroll przy dużej liczbie elementów
 * - wspólną strukturę menu
 *
 * NIE zawiera OutlinedTextField.
 */

@Composable
fun BaseDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<String>,
    onItemSelected: (index: Int) -> Unit,
    maxHeight: Int = 280,
    enabled: Boolean = true
) {
    if (!enabled) return

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.heightIn(max = maxHeight.dp)
    ) {
        items.forEachIndexed { index, text ->
            DropdownMenuItem(
                text = { Text(text) },
                onClick = {
                    onItemSelected(index)
                    onDismiss()
                }
            )
        }
    }
}