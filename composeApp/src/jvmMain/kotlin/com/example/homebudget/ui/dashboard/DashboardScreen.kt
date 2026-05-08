package com.example.homebudget.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.cards.InfoCard
import com.example.homebudget.ui.common.dialogs.ConfirmDialog
import com.example.homebudget.ui.common.feedback.ErrorState
import com.example.homebudget.ui.common.feedback.LoadingState
import com.example.homebudget.utils.money.MoneyFormatter
import com.example.homebudget.viewmodel.dashboard.DashboardViewModel
import com.example.homebudget.viewmodel.dashboard.DashboardUiState
import java.util.Locale

/**
 * DashboardScreen
 *
 * Główny ekran aplikacji HomeBudget.
 * Wyświetla:
 * - podsumowanie budżetu miesięcznego
 * - wykres wydatków
 * - listę kategorii
 * - ostrzeżenia o przekroczeniu budżetu
 */
@Composable
fun DashboardScreen() {
    // ViewModel odpowiedzialny za logikę dashboardu
    val viewModel: DashboardViewModel = viewModel()
    // Aktualny stan UI (Flow -> State)
    val state by viewModel.uiState.collectAsState()

    // Przy pierwszym wejściu na ekran – pobieramy dane
    LaunchedEffect(Unit) {
        viewModel.reload()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {

            // GŁÓWNA CZĘŚĆ
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp)
            ) {
                // Ekran ładowania
                if (state.isLoading) {
                    LoadingState()
                // Ekran błędu (np. brak usera)
                } else if (state.error != null) {
                    ErrorState(message = state.error!!)
                // Główna zawartość dashboardu
                } else {
                    DashboardContent(
                        state = state,
                        onPrevMonth = { viewModel.prevMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onBudgetChange = { viewModel.onBudgetChanged(it) },
                        onToggleUsePreviousBudget = { viewModel.toggleUsePreviousBudget(it) },
                        onSaveBudget = { viewModel.saveBudget() }
                    )
                    // Dialog ostrzegający o przekroczeniu budżetu
                    // Wyświetlany maksymalnie raz dziennie
                    if (state.showBudgetExceededDialog) {
                        val exceededBy = state.totalSpent - state.budget

                        ConfirmDialog(
                            title = "Przekroczono budżet!",
                            message =
                                "Uwaga! Przekroczyłeś budżet w tym miesiącu.\n\n" +
                                        "Wydano: ${MoneyFormatter.format(state.totalSpent)}\n" +
                                        "Budżet: ${MoneyFormatter.format(state.budget)}\n\n" +
                                        "❗ Przekroczono o: ${MoneyFormatter.format(exceededBy)}",
                            confirmText = "Rozumiem",
                            onConfirm = { viewModel.dismissBudgetWarning() }
                        )
                    }
                }
            }
        }
    }
}
/**
 * Sidebar
 *
 * Lewy panel nawigacyjny aplikacji.
 * Umożliwia przechodzenie między głównymi sekcjami
 * oraz wylogowanie użytkownika.
 */
@Composable
fun Sidebar(
    currentSection: String,
    onLogout: () -> Unit,
    onDashboard: () -> Unit,
    onAddExpense: () -> Unit,
    onHistory: () -> Unit,
    onSavings: () -> Unit,
    onBills: () -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "HomeBudget",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))

                SidebarItem(
                    icon = Icons.Filled.Dashboard,
                    label = "Menu główne",
                    selected = currentSection == "dashboard",
                    onClick = onDashboard
                )

                Spacer(Modifier.height(8.dp))

                // Nawigacja do addExpense
                SidebarItem(
                    label = "➕ Dodaj wydatek",
                    selected = currentSection == "addExpense",
                    onClick = onAddExpense
                )

                Spacer(Modifier.height(8.dp))

                // Nawigacja do History
                SidebarItem(
                    label = "📜 Historia wydatków",
                    selected = currentSection == "history",
                    onClick = onHistory
                )

                Spacer(Modifier.height(8.dp))

                // Nawigacja do Savings
                SidebarItem(
                    label = "🏦 Cele oszczędnościowe",
                    selected = currentSection == "savings",
                    onClick = onSavings
                    )

                Spacer(Modifier.height(8.dp))

                // Nawigacja do Bills
                SidebarItem(
                    label = "📅 Planer rachunków",
                    selected = currentSection == "bills",
                    onClick = onBills)

                Spacer(Modifier.height(8.dp))

                // Nawigacja do Statistics
                SidebarItem(
                    label = "📊 Statystyki",
                    selected = currentSection == "statistics",
                    onClick = onStatistics)

                Spacer(Modifier.height(8.dp))

                // Nawigacja do Settings
                SidebarItem(
                    label = "⚙ Ustawienia",
                    selected = currentSection == "settings",
                    onClick = onSettings)
            }

            SidebarItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Wyloguj",
                onClick = onLogout
            )
        }
    }
}
/**
 * SidebarItem
 *
 * Pojedynczy element menu bocznego.
 * Obsługuje:
 * - stan zaznaczenia
 * - ikonę (opcjonalnie)
 * - kliknięcie
 */
@Composable
private fun SidebarItem(
    icon: ImageVector? = null,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .wrapContentHeight(),
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(label)
        }
    }
}
/**
 * DashboardContent
 *
 * Główna zawartość dashboardu:
 * - nagłówek z powitaniem
 * - wybór miesiąca
 * - wykres kołowy wydatków
 * - podsumowanie budżetu
 * - edycja budżetu
 * - lista kategorii
 */
@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBudgetChange: (String) -> Unit,
    onToggleUsePreviousBudget: (Boolean) -> Unit,
    onSaveBudget: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Obsługa nawigacji klawiaturą (TAB)
        val focusManager = LocalFocusManager.current
        val budgetFocus = remember { FocusRequester() }

        // GÓRNY PASEK
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.userName.isNotBlank())
                        "Witaj, ${state.userName} w aplikacji HomeBudget 👋"
                    else
                        "Witaj w aplikacji HomeBudget 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Podsumowanie Twojego budżetu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onPrevMonth) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Poprzedni miesiąc"
                )
            }

            Text(
                text = state.monthLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = onNextMonth,
                enabled = !state.isCurrentMonth
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Następny miesiąc",
                    tint = if (state.isCurrentMonth)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else
                        LocalContentColor.current
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // GŁÓWNY BLOK: wykres + legendy
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // WYKRES
            // Kolor procentu zmienia się na czerwony po przekroczeniu budżetu
            val percentColor =
                when {
                    state.budget <= 0 -> MaterialTheme.colorScheme.onSurface
                    state.percentUsed > 100 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Dynamiczny rozmiar dla tekstu + procentu
                val chartSize = maxWidth
                val labelFontSize =
                    if (chartSize < 260.dp) 12.sp
                    else 14.sp
                val percentFontSize =
                    if (chartSize < 260.dp) 22.sp
                    else 30.sp
                PieChart(
                    data = state.categories.map { it.amount.toFloat() },
                    labels = state.categories.map { it.name },
                    colors = state.categoriesColors(),
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Łącznie wydano",
                        fontSize = labelFontSize,
                        style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = String.format(Locale.forLanguageTag("pl"),"%.1f%%", state.percentUsed),
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = percentFontSize,
                        fontWeight = FontWeight.Bold,
                        color = percentColor
                    )
                }
            }

            // PODSUMOWANIE + LISTA KATEGORII
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Spacer(Modifier.height(8.dp))

                InfoCard(
                    title = "Podsumowanie",
                    modifier = Modifier.fillMaxWidth(),

                ) {
                    Text(
                        "💰 Budżet: ${MoneyFormatter.format(state.budget)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "📊 Wydano: ${MoneyFormatter.format(state.totalSpent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.budget > 0 && state.totalSpent > state.budget) {
                        val exceededBy = state.totalSpent - state.budget
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "❗ Przekroczono o ${MoneyFormatter.format(exceededBy)}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.budgetInput,
                    onValueChange = { onBudgetChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(budgetFocus)
                        .onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else false
                        },
                    label = { Text("Ustaw budżet na ten miesiąc") },
                    placeholder = { Text("Np. 1200") },
                    enabled = state.canEditBudget
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.usePreviousBudget,
                        onCheckedChange = onToggleUsePreviousBudget,
                        enabled = state.canEditBudget
                    )
                    Text("Użyj budżetu z poprzedniego miesiąca")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onSaveBudget,
                    enabled = state.canEditBudget && state.budgetInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent {
                            if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                                focusManager.moveFocus(FocusDirection.Next)
                                true
                            } else false
                        }
                ) {
                    Text("Zapisz budżet")
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Kategorie",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                if (state.categories.isEmpty() || state.totalSpent == 0.0) {
                    Text("Brak wydatków w tym miesiącu.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.categories.forEachIndexed { index, category ->
                            val color = state.categoriesColors()[index % state.categoriesColors().size]

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${category.name} — ${MoneyFormatter.format(category.amount)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bardzo prosty wykres kołowy wzorowany na mobilnym – bez opisów na krawędziach,
 * legenda jest obok.
 */
@Composable
private fun PieChart(
    data: List<Float>,
    labels: List<String>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    holeFraction: Float = 0.25f // Wielkość wycięcia (20-28% jest idealne)
) {
    // Tło motywu
    val holeColor = MaterialTheme.colorScheme.surface
    // Desktop-safe
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.padding(32.dp)) {
        val total = data.sum().takeIf { it > 0f } ?: 1f
        var startAngle = -90f
        // Liczenie procentu
        val percentages = data.map { it / total * 100f }
        // Średnica i środek canvasu
        val radius = size.minDimension / 2f
        val center = this.center
        // Promień tekstu
        val textRadius = radius * (holeFraction + (1f - holeFraction) / 2f)
        // Dynamiczne skalowanie rozmiaru cionki dla nazwy i procentu
        val categoryFontSize = (radius * 0.065f).coerceIn(10f, 18f)
        val percentFontSize = (radius * 0.075f).coerceIn(12f, 22f)
        // Rysowanie segmentów
        data.forEachIndexed { index, value ->
            val sweep = value / total * 360f
            val percentage = percentages[index]
            // Segment
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true
            )
            // Zależne od rozmiaru wykresu
            val minPercentForLabel =
                if (radius < 180f) 10f
                else if (radius < 260f) 8f
                else 6f
            // Tekst Procentu
            if (percentage >= minPercentForLabel) {
                // Filtr, żeby nie zaśmiecać małych segmentów
                val angleRad = Math.toRadians((startAngle + sweep / 2).toDouble())
                val x = center.x + textRadius * kotlin.math.cos(angleRad).toFloat()
                val y = center.y + textRadius * kotlin.math.sin(angleRad).toFloat()
                // Nazwa kategorii
                val category = labels[index]
                // Procent kategorii
                val percentText = String.format(Locale.forLanguageTag("pl-PL"), "%.1f%%", percentage)
                // Pozycja tekstu kategorii
                val categoryLayout = textMeasurer.measure(
                    text = category,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = categoryFontSize.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
                // Pozycaj procentu kategorii
                val percentLayout = textMeasurer.measure(
                    text = percentText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = percentFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                // Połączenie tekstu i procent pisane jedno pod drugim
                val totalHeight = categoryLayout.size.height + percentLayout.size.height
                // Nazwa kategorii
                drawText(
                    textLayoutResult = categoryLayout,
                    topLeft = Offset(
                        x - categoryLayout.size.width / 2f,
                        y - totalHeight / 2f
                    )
                )
                // Procent kategorii (pod nazwą)
                drawText(
                    textLayoutResult = percentLayout,
                    topLeft = Offset(
                        x - percentLayout.size.width / 2f,
                        y - totalHeight / 2f + categoryLayout.size.height
                    )
                )
            }
            startAngle += sweep
        }
        // Zależne od rozmiaru wykresu
        val adaptiveHoleFraction =
            if (radius < 180f) 0.32f
            else holeFraction
        // Wycięcie środka (Donut)
        drawCircle(
            color = holeColor, // Tło motywu
            radius = radius * adaptiveHoleFraction,
            center = center
        )
    }
}
private fun DashboardUiState.categoriesColors(): List<Color> {
    if (categories.isEmpty()) return listOf(Color(0xFFBDBDBD))
    return categories.map { category ->
        val hex = categoryColors[category.name] ?: "#999999"
        colorFromHex(hex)
    }
}
private fun colorFromHex(hex: String): Color {
    val cleaned = hex.trim().removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: 0x999999
    return when (cleaned.length) {
        6 -> Color((0xFF000000 or value).toInt())
        8 -> Color(value.toInt())
        else -> Color(0xFF999999.toInt())
    }
}