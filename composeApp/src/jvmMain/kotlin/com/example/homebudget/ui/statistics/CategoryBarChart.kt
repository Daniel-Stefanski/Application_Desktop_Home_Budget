package com.example.homebudget.ui.statistics

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.homebudget.viewmodel.statistics.CategoryStat

/**
 * CategoryBarChart
 *
 * Wykres słupkowy wydatków według kategorii.
 * Jest to cienka warstwa nad BarChart,
 * która mapuje dane CategoryStat na BarChartItem.
 */
@Composable
fun CategoryBarChart(data: List<CategoryStat>) {
    BarChart(
        data = data.map {
            BarChartItem(
                label = it.name,
                value = it.total,
                color = hexToColor(it.colorHex)
            )
        }
    )
}
// Konwersja koloru zapisanego w HEX (#RRGGBB lub #AARRGGBB) na Color Compose
private fun hexToColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = cleanHex.toLong(16)
        when (cleanHex.length) {
           6 -> Color(
               red = ((colorLong shr 16) and 0xFF) / 255f,
               green = ((colorLong shr 8) and 0xFF) / 255f,
               blue = ((colorLong and 0xFF) / 255f)
           )
            8 -> Color(
                alpha = ((colorLong shr 24) and 0xFF) / 255f,
                red = ((colorLong shr 16) and 0xFF) / 255f,
                green = ((colorLong shr 8) and 0xFF) / 255f,
                blue = ((colorLong and 0xFF) / 255f)
            )
            else -> Color.Gray
        }
    } catch (_: Exception) {
        Color.Gray
    }
}