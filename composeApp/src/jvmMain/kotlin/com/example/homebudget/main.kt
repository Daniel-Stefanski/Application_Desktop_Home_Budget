package com.example.homebudget

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.util.Locale

fun main() {
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
            App()
        }
    }
}