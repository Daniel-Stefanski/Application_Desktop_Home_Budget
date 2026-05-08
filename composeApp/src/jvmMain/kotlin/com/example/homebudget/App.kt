package com.example.homebudget

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.data.sync.SupabaseToastBus
import com.example.homebudget.data.sync.SupabaseToastEvent
import com.example.homebudget.data.sync.SupabaseToastType
import com.example.homebudget.ui.billsplanner.AddBillScreen
import com.example.homebudget.ui.dashboard.DashboardScreen
import com.example.homebudget.ui.auth.LoginScreen
import com.example.homebudget.ui.auth.RegisterScreen
import com.example.homebudget.ui.auth.ResetPasswordScreen
import com.example.homebudget.ui.auth.SetNewPasswordScreen
import com.example.homebudget.ui.addexpense.AddExpenseScreen
import com.example.homebudget.ui.billsplanner.BillsPlannerScreen
import com.example.homebudget.ui.history.HistoryScreen
import com.example.homebudget.ui.savings.SavingsScreen
import com.example.homebudget.ui.settings.SettingsScreen
import com.example.homebudget.ui.dashboard.Sidebar
import com.example.homebudget.ui.statistics.StatisticsScreen
import com.example.homebudget.ui.theme.ThemeMode
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.viewmodel.auth.LoginViewModel
import com.example.homebudget.viewmodel.theme.ThemeViewModel
import kotlinx.coroutines.delay

/**
 * App
 *
 * Główna funkcja UI aplikacji.
 * Odpowiada za:
 * - nawigację między ekranami
 * - inicjalizację motywu
 * - obsługę zapamiętanego logowania
 * - wyświetlanie Sidebar
 */
@Composable
fun App() {
    // Obsługa motywu aplikacji
    val themeViewModel: ThemeViewModel = viewModel()
    val theme by themeViewModel.theme.collectAsState()
    val darkTheme = when (theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val darkColors = darkColorScheme(
        background = Color(0xFF0F0F14),
        surface = Color(0xFF16161D),

        onSurface = Color(0xFFF5F5F5),          // 🔥 główny tekst – prawie biały
        onSurfaceVariant = Color(0xFFCCCCCC),  // 🔥 opisy, labelki – jaśniejsze niż było

        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF1A1A1A),

        outline = Color(0xFF5A5A5A)             // obramowania pól
    )

    // Główna nawigacja aplikacji
    var screen by remember { mutableStateOf("login") }
    var editingBillId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        val rememberedId = Prefs.getRememberedUser()
        screen = if (rememberedId != null) "dashboard" else "login"
    }

    // Ekrany z sidebarem
    val screensWithSidebar = setOf(
        "dashboard",
        "addExpense",
        "history",
        "savings",
        "bills",
        "statistics",
        "settings"
    )
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColorScheme()
    ) {
        val showSidebar = screen in screensWithSidebar
        Row(Modifier.fillMaxSize()) {
            if (showSidebar) {
                Sidebar(
                    currentSection = screen,
                    onDashboard = { screen = "dashboard" },
                    onAddExpense = { screen = "addExpense" },
                    onHistory = { screen = "history" },
                    onSavings = { screen = "savings" },
                    onBills = { screen = "bills" },
                    onStatistics = { screen = "statistics" },
                    onSettings = { screen = "settings" },
                    onLogout = {
                        Prefs.setRememberedUser(null)
                        screen = "login"
                    }
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                when (screen) {

                    "loading" -> {
                        Text("Ładowanie...")
                    }

                    "login" -> LoginScreen(
                        viewModel = viewModel<LoginViewModel>(),
                        onLoginSuccess = { screen = "dashboard" },
                        onGoToRegister = { screen = "register" },
                        onGoToResetPassword = { screen = "resetpassword" }
                    )

                    "register" -> RegisterScreen(
                        themeViewModel = themeViewModel,
                        onBackToLogin = { screen = "login" },
                        onSuccessRegister = { screen = "dashboard" }
                    )

                    "resetpassword" -> ResetPasswordScreen(
                        onBackToLogin = { screen = "login" },
                        onSuccessReset = { screen = "login" },
                        onGoToSetNewPassword = { screen = "setnewpassword" }
                    )

                    "setnewpassword" -> SetNewPasswordScreen(
                        initialToken = null,
                        onBackToLogin = { screen = "login" }
                    )

                    "dashboard" -> {
                        DashboardScreen()
                    }

                    "addExpense" -> {
                        AddExpenseScreen(
                            onBackToDashboard = { screen = "dashboard" }
                        )
                    }

                    "history" -> {
                        HistoryScreen()
                    }

                    "savings" -> {
                        SavingsScreen()
                    }

                    "bills" -> {
                        BillsPlannerScreen(
                            onAddBill = {
                                editingBillId = null
                                screen = "addBill"
                            },
                            onEditBill = { id ->
                                editingBillId = id
                                screen = "editBill"
                            }
                        )
                    }

                    "addBill" -> {
                        AddBillScreen(
                            expenseId = null,
                            onBack = { screen = "bills" }
                        )
                    }

                    "editBill" -> {
                        AddBillScreen(
                            expenseId = editingBillId,
                            onBack = { screen = "bills" }
                        )
                    }

                    "statistics" -> {
                        StatisticsScreen()
                    }

                    "settings" -> SettingsScreen(
                        themeViewModel = themeViewModel,
                        onLogout = {
                            Prefs.setRememberedUser(null)
                            screen = "login"
                        }
                    )
                }
                SupabaseToastHost(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun SupabaseToastHost(modifier: Modifier = Modifier) {
    var toast by remember { mutableStateOf<SupabaseToastEvent?>(null) }

    LaunchedEffect(Unit) {
        SupabaseToastBus.events.collect { event ->
            toast = event
            delay(3_000L)
            if (toast == event) {
                toast = null
            }
        }
    }

    toast?.let { event ->
        val background = when (event.type) {
            SupabaseToastType.SUCCESS -> Color(0xFF2E7D32)
            SupabaseToastType.WARNING -> Color(0xFF6D4C41)
        }

        Surface(
            modifier = modifier.widthIn(min = 280.dp, max = 520.dp),
            color = background,
            contentColor = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = event.message,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
