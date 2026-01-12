package com.example.homebudget.ui.common.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ScrollableDialogContent
 *
 * Wrapper na zawartość dialogu z możliwością scrollowania.
 * Używany w dużych formularzach i listach.
 */
@Composable
fun ScrollableDialogContent(
    maxHeightDp: Int = 300,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(max = maxHeightDp.dp)
            .verticalScroll(rememberScrollState())
    ) {
        content()
    }
}