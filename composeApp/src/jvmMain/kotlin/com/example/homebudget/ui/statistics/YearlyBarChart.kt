package com.example.homebudget.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.homebudget.viewmodel.statistics.MonthlyStat
import kotlin.math.ceil
import kotlin.math.max

private val months = listOf(
    "Sty","Lut","Mar","Kwi","Maj","Cze",
    "Lip","Sie","Wrz","Paź","Lis","Gru"
)

private val colorSpent = Color(0xFF3498DB)
private val colorBudget = Color(0xFF2ECC71)
private val colorExceeded = Color(0xFFE74C3C)

/**
 * YearlyBarChart
 *
 * Roczny wykres miesięczny budżetu.
 * Pokazuje:
 * - wydatki (niebieski)
 * - niewykorzystany budżet (zielony)
 * - przekroczenie budżetu (czerwony)
 *
 * Obsługuje:
 * - kliknięcie w miesiąc
 * - dynamiczną skalę osi Y
 * - automatyczne marginesy na etykiety
 */
private data class ChartLayout(
    val leftPadding: Float,
    val chartWidth: Float,
    val barWidth: Float
)
@Suppress("DuplicatedCode")
@Composable
fun YearlyBarChart(
    data: List<MonthlyStat>,
    // Callback wywoływany po kliknięciu w miesiąc (0–11)
    onMonthClick: (Int) -> Unit
) {
    val maxValue = data.maxOfOrNull { max(it.spent, it.budget) } ?: 1.0
    val step = 500.0
    val roundedMax = ceil(maxValue / step) * step
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
            .height(340.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Te same wartości co w Canvas
                    val layout = calculateYearlyChartLayout(
                        size = size.toSize(),
                        textMeasurer = textMeasurer,
                        labelStyle = labelStyle,
                        maxValue = maxValue,
                        step = step
                    )
                    val xInChart = offset.x - layout.leftPadding
                    if (xInChart < 0f) return@detectTapGestures
                    val index = (xInChart / layout.barWidth).toInt()
                    if (index in 0..11) {
                        onMonthClick(index)
                    }
                }
            }
    ) {
        val bottomPadding = 24f
        val topPadding = 8f
        val steps = max(1, (roundedMax / step).toInt())

        // 🔹 wylicz szerokość najdłuższej etykiety osi Y → robimy margines z lewej
       val layout = calculateYearlyChartLayout(
           size = size,
           textMeasurer = textMeasurer,
           labelStyle = labelStyle,
           maxValue = maxValue,
           step = step
       )
        val leftPadding = layout.leftPadding
        val chartHeight = size.height - bottomPadding - topPadding
        val chartWidth = layout.chartWidth
        val barWidth = layout.barWidth

        // Oś Y + Siatka + etykiety (linia zaczyna się od cyfry)
        for (i in 0..steps) {
            val y = topPadding + chartHeight - (i / steps.toFloat()) * chartHeight
            val isZeroLine = i == 0
            drawLine(
                color = if (isZeroLine) axisColor else gridAxisColor,
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + chartWidth, y),
                strokeWidth = if (isZeroLine) 2.5f else 1f
            )
            val label = (i * step).toInt().toString()
            val measured = textMeasurer.measure(AnnotatedString(label), style = labelStyle)
            val textY = max(topPadding, y - measured.size.height / 2f)
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
        // Słupki + etykiety miesięcy
        data.forEachIndexed { index, month ->
            val x = leftPadding + index * barWidth + barWidth * 0.2f
            val width = barWidth * 0.6f

            val usedBudget = minOf(month.spent, month.budget)
            val unusedBudget = maxOf(0.0, month.budget - month.spent)
            val exceed = maxOf(0.0, month.spent - month.budget)

            val usedHeight = (usedBudget / roundedMax).toFloat() * chartHeight
            val unusedHeight = (unusedBudget / roundedMax).toFloat() * chartHeight
            val exceedHeight = (exceed / roundedMax).toFloat() * chartHeight

            var currentY = topPadding + chartHeight

            // 🟦 wydatki do budżetu
            drawRect(
                color = colorSpent,
                topLeft = Offset(x, currentY - usedHeight),
                size = Size(width, usedHeight)
            )
            currentY -= usedHeight

            // 🟩 niewykorzystany budżet
            if (unusedHeight > 0f) {
                drawRect(
                    color = colorBudget,
                    topLeft = Offset(x, currentY - unusedHeight),
                    size = Size(width, unusedHeight)
                )
                currentY -= unusedHeight
            }

            // 🟥 przekroczenie
            if (exceedHeight > 0f) {
                drawRect(
                    color = colorExceeded,
                    topLeft = Offset(x, currentY - exceedHeight),
                    size = Size(width, exceedHeight)
                )
            }

            // Miesiące
            val m = months[index]
            val measured = textMeasurer.measure(AnnotatedString(m), style = labelStyle)
            val textX = max(leftPadding, x + width / 2f - measured.size.width / 2f)
            val textY = topPadding + chartHeight + 4f
            if (textX.isFinite() && textY.isFinite() && textY >= 0f) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString(m),
                    topLeft = Offset(textX, textY),
                    style = labelStyle
                )
            }
        }
    }
}
private fun calculateYearlyChartLayout(
    size: Size,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    labelStyle: TextStyle,
    maxValue: Double,
    step: Double
): ChartLayout {
    val roundedMax = ceil(maxValue / step) * step
    val maxYLabel = roundedMax.toInt().toString()

    val maxYLabelWidth =
        textMeasurer.measure(
            AnnotatedString(maxYLabel),
            style = labelStyle
        ).size.width

    val leftPadding = maxYLabelWidth + 12f
    val rightPadding = 8f
    val chartWidth = size.width - leftPadding - rightPadding
    val barWidth = chartWidth / 12f

    return ChartLayout(
        leftPadding = leftPadding,
        chartWidth = chartWidth,
        barWidth = barWidth
    )
}