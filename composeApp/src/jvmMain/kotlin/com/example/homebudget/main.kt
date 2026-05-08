package com.example.homebudget

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.util.Locale

fun main() = application {
    // Wymuszenie polskiego locale
    Locale.setDefault(Locale.forLanguageTag("pl-PL"))
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