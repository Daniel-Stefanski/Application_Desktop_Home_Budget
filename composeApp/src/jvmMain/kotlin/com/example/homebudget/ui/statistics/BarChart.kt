package com.example.homebudget.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.max

/**
 * BarChartItem
 *
 * Pojedynczy element wykresu:
 * - label  -> etykieta na osi X
 * - value  -> wysokość słupka
 * - color  -> kolor słupka
 */
data class BarChartItem(
    val label: String,
    val value: Double,
    val color: Color
)
/**
 * BarChart
 *
 * Uniwersalny komponent wykresu słupkowego.
 * Używany m.in. do:
 * - wykresów kategorii
 * - wykresów osób
 *
 * Obsługuje:
 * - dynamiczną skalę osi Y
 * - automatyczne marginesy na etykiety
 * - wyśrodkowane etykiety osi Y
 * - etykiety osi X pod słupkami
 */
@Suppress("DuplicatedCode")
@Composable
fun BarChart(
    data: List<BarChartItem>,
    height: Int = 260,
    step: Double? = null
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { it.value }
    val usedStep = step ?: when {
        maxValue <= 100 -> 10.0
        maxValue <= 500 -> 50.0
        maxValue <= 1000 -> 100.0
        else -> maxValue / 5
    }
    val roundedMax = ceil(maxValue / usedStep) * usedStep

    val textMeasurer = rememberTextMeasurer()

    val labelColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = remember(labelColor) {
        TextStyle(
            fontSize = 12.sp,
            color = labelColor,
            fontFamily = FontFamily.SansSerif
        )
    }
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val gridAxisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
    ) {

        val bottomPadding = 48f
        val topPadding = 8f
        val rightPadding = 8f
        val steps = max(1, (roundedMax / usedStep).toInt())

        // 🔹 wylicz szerokość najdłuższej etykiety osi Y → robimy margines z lewej
        val maxYLabel = (steps * usedStep).toInt().toString()
        val maxYLabelWidth = textMeasurer.measure(AnnotatedString(maxYLabel), style = labelStyle).size.width
        val leftPadding = maxYLabelWidth + 12f // 12px odstępu od tekstu do wykresu

        val chartHeight = size.height - bottomPadding - topPadding
        val chartWidth =  size.width - leftPadding - rightPadding
        val barWidth = chartWidth / data.size

        // 🔹 siatka + Y labels (linia zaczyna się od cyfry)
        for (i in 0..steps) {
            val y = topPadding + chartHeight - (i / steps.toFloat()) * chartHeight
            val isZeroLine = i == 0
            drawLine(
                color = if (isZeroLine) axisColor else gridAxisColor,
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + chartWidth, y),
                strokeWidth = if (isZeroLine) 2.5f else 1f
            )

            val label = (i * usedStep).toInt().toString()
            val measured = textMeasurer.measure(AnnotatedString(label), style = labelStyle)
            // tekst wyśrodkowany względem linii (i nie “ucina się” na górze)
            val textY = (y - measured.size.height / 2f)
                .coerceIn(0f, size.height - measured.size.height.toFloat())
            drawText(
                textMeasurer = textMeasurer,
                text = AnnotatedString(label),
                topLeft = Offset(4f, textY),
                style = labelStyle
            )
        }
        // Oś Y (linia pionowa)
        drawLine(
            color = gridAxisColor.copy(alpha = 0.6f),
            start = Offset(leftPadding, topPadding),
            end = Offset(leftPadding, topPadding + chartHeight),
            strokeWidth = 2.5f
        )
        // 🔹 słupki + etykiety X
        data.forEachIndexed { index, item ->
            val barHeight = (item.value / roundedMax).toFloat() * chartHeight
            val x = leftPadding + index * barWidth + barWidth * 0.2f
            val width = barWidth * 0.6f
            val y = topPadding + (chartHeight - barHeight)

            drawRect(
                color = item.color,
                topLeft = Offset(x, y),
                size = Size(width, barHeight)
            )

            val labelSize = textMeasurer.measure(AnnotatedString(item.label), style = labelStyle)

            drawText(
                textMeasurer = textMeasurer,
                text = AnnotatedString(item.label),
                topLeft = Offset(
                    x + width / 2 - labelSize.size.width / 2,
                    topPadding + chartHeight + 6f
                ),
                style = labelStyle
            )
        }
    }
}
