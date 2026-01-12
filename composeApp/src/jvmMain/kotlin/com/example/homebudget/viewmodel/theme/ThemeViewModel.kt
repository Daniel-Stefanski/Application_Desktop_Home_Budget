package com.example.homebudget.viewmodel.theme

import androidx.lifecycle.ViewModel
import com.example.homebudget.ui.theme.ThemeMode
import com.example.homebudget.utils.settings.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel : ViewModel() {

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(Prefs.getThemeMode()) }
            .getOrElse { ThemeMode.SYSTEM }
    )
    val theme: StateFlow<ThemeMode> = _theme

    fun setTheme(mode: ThemeMode) {
        Prefs.setThemeMode(mode.name)
        _theme.value = mode
    }
}