package com.example.homebudget.ui.common.fields

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorText(
    message: String?,
    modifier: Modifier = Modifier
) {
    Text(
        text = message ?: "",
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.heightIn(min = 16.dp),
        style = MaterialTheme.typography.bodySmall
    )
}