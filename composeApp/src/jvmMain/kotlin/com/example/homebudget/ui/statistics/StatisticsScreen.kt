package com.example.homebudget.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homebudget.ui.common.cards.InfoCard
import com.example.homebudget.ui.common.feedback.EmptyState
import com.example.homebudget.ui.common.feedback.LoadingState
import com.example.homebudget.utils.money.MoneyFormatter
import com.example.homebudget.viewmodel.statistics.StatisticsViewModel
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * StatisticsScreen
 *
 * Główny ekran statystyk aplikacji.
 * Odpowiada za:
 * - wyświetlanie podsumowania rocznego (wydatki / budżet / oszczędności)
 * - nawigację między latami
 * - prezentację rocznego wykresu miesięcznego
 * - wybór miesiąca i pokazanie jego szczegółów
 * - wyświetlanie wykresów kategorii i osób dla wybranego miesiąca
 *
 * Dane są pobierane i zarządzane przez StatisticsViewModel.
 */
@Composable
fun StatisticsScreen() {
    val viewModel: StatisticsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val currentYear = LocalDate.now().year
    val isNextEnabled = state.year < currentYear

    LaunchedEffect(Unit) {
        viewModel.loadYear()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            ) {

                if (state.isLoading) {
                    LoadingState()
                    return@Box
                }

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {

                    // 🔹 Nagłówek
                    Text(
                        text = "Statystyki",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    // 🔹 Zmiana roku
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = viewModel::prevYear) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Poprzedni rok")
                        }

                        Text(
                            text = state.year.toString(),
                            style = MaterialTheme.typography.titleLarge
                        )

                        IconButton(
                            onClick = viewModel::nextYear,
                            enabled = isNextEnabled
                        ) {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = "Następny rok",
                                tint = if (isNextEnabled)
                                    LocalContentColor.current
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 🔹 Podsumowanie roku
                    InfoCard(
                        title = "Podsumowanie roku",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                            Text(
                                "💰 Wydatki: ${MoneyFormatter.format(state.yearSpent)}"
                            )
                            Text(
                                "📊 Budżet: ${MoneyFormatter.format(state.yearBudget)}"
                            )
                            val diff = state.yearBudget - state.yearSpent
                            Text(
                                text = if (diff >= 0)
                                    "✅ Zaoszczędzono: ${MoneyFormatter.format(diff)}"
                                else
                                    "❌ Przekroczono: ${MoneyFormatter.format(-diff)}",
                                color = if (diff >= 0) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                            )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Roczne zestawienie",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    YearlyBarChart(
                        data = state.monthlyStats,
                        onMonthClick = { index ->
                            viewModel.selectMonth(index)
                        }
                    )
                    if (state.selectedMonthIndex == null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Kliknij miesiąc, aby zobaczyć szczegóły",
                                color = InfoTextColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row (
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendItem(Color(0xFF3498DB), "Wydatki")
                        LegendItem(Color(0xFF2ECC71), "Budżet")
                        LegendItem(Color(0xFFE74C3C), "Przekroczenie")
                    }

                    // 🔹 Szczegóły miesiąca
                    state.selectedMonthDetails?.let { details ->
                        val monthIndex = state.selectedMonthIndex!!
                        val monthName = monthNamePl(monthIndex)
                        val diff = details.budget - details.spent
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Szczegóły: $monthName ${state.year}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Wydatki: ${MoneyFormatter.format(details.spent)}")
                        Text("Budżet: ${MoneyFormatter.format(details.budget)}")

                        Text(
                            text =
                                if (diff >= 0){
                                    "Zaoszczędzono: ${MoneyFormatter.format(diff)}"
                                } else {
                                    "Przekroczono: ${MoneyFormatter.format(-diff)}"
                                },
                            color = if (diff >= 0){
                                Color(0xFF2ECC71)
                            } else {
                                Color(0xFFE74C3C)
                            }
                        )
                        Text("Transakcje: ${details.count}")
                        Text("Min: ${MoneyFormatter.format(details.min)}")
                        Text("Max: ${MoneyFormatter.format(details.max)}")

                        Spacer(Modifier.height(12.dp))

                        Button(onClick = { viewModel.loadMonthCharts() }) {
                            Text("📊 Pokaż wykresy miesiąca")
                        }
                    }

                    // Dokładne szczegóły
                    if (state.showMonthCharts) {
                        if (state.categoryStats.isEmpty() && state.personStats.isEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(text = "Brak danych dla tego miesiąca")
                            }
                        } else {
                            Text(
                                "Wydatki według kategorii",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            CategoryBarChart(data = state.categoryStats)

                            Spacer(Modifier.height(24.dp))

                            Text(
                                "Wydatki według osób",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            PersonBarChart(state.personStats)
                        }
                    }
                }
            }
        }
    }
}
private fun monthNamePl(monthIndex: Int): String =
    Month.of(monthIndex + 1)
        .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pl-PL"))
        .replaceFirstChar { it.uppercase() }
// Mały element legendy do wykresów (kolor + opis)
@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
/*
// (NIEUŻYWANE) – stara wersja listy miesięcy, pozostawiona do ewentualnego refaktoru
@Composable
private fun MonthRow(
    monthIndex: Int,
    spent: Double,
    budget: Double,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val monthName =
        Month.of(monthIndex + 1)
            .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pl"))
            .replaceFirstChar { it.uppercase() }

    val bg =
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, shape = MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(monthName, style = MaterialTheme.typography.titleSmall)
        Text("Wydatki: ${formatMoney(spent)}")
        Text("Budżet: ${formatMoney(budget)}")
    }
}*/
private val InfoTextColor = Color(0xFF9E9E9E)