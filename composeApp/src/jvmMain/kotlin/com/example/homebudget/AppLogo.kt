package com.example.homebudget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import homebudget.composeapp.generated.resources.Res
import homebudget.composeapp.generated.resources.logo_budzetu

/**
 * AppLogo
 *
 * Uniwersalny komponent logo aplikacji HomeBudget.
 *
 * Wykorzystuje Compose Multiplatform Resources (Res.drawable),
 * dzięki czemu działa poprawnie na:
 * - Desktop (JVM)
 * - Android (w przyszłości)
 * - innych targetach KMP
 *
 * Komponent jest celowo prosty i bez logiki:
 * - odpowiada wyłącznie za wyświetlenie logo
 * - rozmiar i pozycjonowanie kontrolowane z zewnątrz
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier, // umożliwia modyfikację z zewnątrz (padding, align, itp.)
    size: Dp = 80.dp               // domyślny rozmiar logo
) {
    Image(
        painter = painterResource(Res.drawable.logo_budzetu), // logo z composeResources/drawable
        contentDescription = "Logo aplikacji",               // dostępność (accessibility)
        modifier = modifier.size(size)                        // rozmiar logo
    )
}