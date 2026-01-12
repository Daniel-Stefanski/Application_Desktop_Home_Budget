package com.example.homebudget.ui.statistics

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.homebudget.viewmodel.statistics.PersonStat

/**
 * PersonBarChart
 *
 * Wykres słupkowy wydatków według osób.
 * Wykorzystuje wspólny komponent BarChart.
 * Kolor słupków jest stały (fioletowy).
 */
@Composable
fun PersonBarChart(data: List<PersonStat>) {
    BarChart(
        data = data.map {
            BarChartItem(
                label = it.name,
                value = it.total,
                color = Color(0xFF835BFF)
            )
        }
    )
}
