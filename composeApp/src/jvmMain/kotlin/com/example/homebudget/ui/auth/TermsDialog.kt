package com.example.homebudget.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.homebudget.ui.common.dialogs.BaseDialog
import com.example.homebudget.ui.common.dialogs.ScrollableDialogContent

/**
 * TermsDialog
 *
 * Dialog prezentujący regulamin aplikacji HomeBudget.
 *
 * - Tekst jest scrollowany
 * - Maksymalna wysokość zapobiega zasłanianiu UI
 * - Brak logiki biznesowej (czysty komponent UI)
 */
@Composable
fun TermsDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseDialog(
        title = "Regulamin aplikacji",
        content = {
            ScrollableDialogContent(maxHeightDp = 300) {
                Text(
                    text = TERMS_TEXT,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmText = "Akceptuję",
        onConfirm = onAccept,
        onDismiss = onDismiss,
        dismissText = "Anuluj"
    )
}
// Treść regulaminu
private val TERMS_TEXT = """
Regulamin aplikacji HomeBudget

1. Aplikacja HomeBudget służy do zarządzania budżetem domowym oraz planowania wydatków.
2. Wszystkie dane użytkownika są przechowywane lokalnie na jego urządzeniu. Aplikacja może korzystać z usług chmurowych zgodnie z jej funkcjonalnością.
3. Użytkownik ponosi pełną odpowiedzialność za wprowadzane dane oraz ich poprawność.
4. Twórca aplikacji nie ponosi odpowiedzialności za ewentualne straty finansowe wynikające z korzystania z aplikacji.
5. Aplikacja ma charakter pomocniczy i nie zastępuje profesjonalnych usług księgowych ani doradczych.
6. Użytkownik jest odpowiedzialny za wykonywanie kopii zapasowych swoich danych. Usunięcie aplikacji może skutkować utratą danych lokalnych.
7. Aplikacja może być rozwijana i zmieniana w przyszłości, co może wiązać się ze zmianą funkcjonalności.
8. Korzystanie z aplikacji oznacza akceptację niniejszego regulaminu.

Klikając „Akceptuję” wyrażasz zgodę na powyższe warunki.
""".trimIndent()
