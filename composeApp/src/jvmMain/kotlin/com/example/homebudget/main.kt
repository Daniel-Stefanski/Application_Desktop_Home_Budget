package com.example.homebudget

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.util.Locale

fun main(args: Array<String>) {
    val locale = Locale.forLanguageTag("pl-PL")
    // Wymuszenie polskiego locale
    Locale.setDefault(locale)
    System.setProperty("user.language", "pl")
    System.setProperty("user.country", "PL")
    System.setProperty("user.variant", "")
    application {
        // Start aplikacji zawsze na pełnym ekranie
        val windowState = rememberWindowState(
            placement = WindowPlacement.Maximized
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "homebudget",
            state = windowState
        ) {
            App(initialResetToken = args.findResetToken())
        }
    }
}

private fun Array<String>.findResetToken(): String? {
    val tokenArg = firstOrNull { it.startsWith("--reset-token=") }
        ?.substringAfter("=")
        ?.takeIf { it.isNotBlank() }
    if (tokenArg != null) return tokenArg

    val resetUrl = firstOrNull {
        it.startsWith("homebudget://reset-password") || it.contains("reset-password")
    } ?: return null

    return resetUrl.substringAfter("token=", missingDelimiterValue = "")
        .substringBefore("&")
        .takeIf { it.isNotBlank() }
}
