package com.miguel.mentaltrader.feature.inicio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * HU-026 Escenario 2: gráfica de línea del R acumulado, dibujada con Compose Canvas puro (sin
 * librería de gráficas nueva -- design.md decisión #1: `stack-allowlist.json` no admite hoy
 * ninguna librería de gráficas, y Canvas alcanza para una polilínea simple). Sin operaciones (o
 * con una sola), no hay línea que trazar todavía -- se muestra un mensaje neutro en su lugar
 * (HU-026 Escenario 4, cálculo seguro sin caídas).
 */
@Composable
fun RAcumuladoLineChart(
    points: List<ResultInRCumulativeSeries.Point>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Box(modifier = modifier.height(160.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Todavía no hay suficientes operaciones para trazar la línea de R acumulado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val lineColor = if (points.last().cumulativeResultInR >= 0f) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    val maxValue = points.maxOf { it.cumulativeResultInR }
    val minValue = points.minOf { it.cumulativeResultInR }

    // Bug real reportado por el usuario (2026-08-04): la gráfica no tenía ninguna referencia
    // numérica -- una polilínea sin ejes no comunica qué se está midiendo. Se agregan las
    // etiquetas de valor máximo/mínimo del eje Y (mismo formato "+2.0R"/"-1.5R" que las tarjetas
    // de resumen, InicioFormatting.signedR) superpuestas en las esquinas del Canvas.
    Box(modifier = modifier.height(160.dp).fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Evita una división por cero cuando todos los puntos son iguales (ej. serie plana en 0).
            val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
            val stepX = size.width / (points.size - 1)

            fun yFor(value: Float): Float = size.height - ((value - minValue) / range) * size.height

            val path = androidx.compose.ui.graphics.Path().apply {
                points.forEachIndexed { index, point ->
                    val x = stepX * index
                    val y = yFor(point.cumulativeResultInR)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(path = path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = StrokeCap.Round))

            // Línea de referencia en R=0, para poder ubicar visualmente dónde el acumulado cruza a
            // territorio negativo (siempre que 0 caiga dentro del rango visible).
            if (minValue <= 0f && maxValue >= 0f) {
                val zeroY = yFor(0f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.4f),
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = 2f
                )
            }
        }
        Text(
            InicioFormatting.signedR(maxValue),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            InicioFormatting.signedR(minValue),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        Text(
            InicioFormatting.signedR(points.last().cumulativeResultInR),
            style = MaterialTheme.typography.labelSmall,
            color = lineColor,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * HU-026 Escenario 3: gráfica de barras con el % de operaciones Ganadas/Perdidas/Break Even
 * (mismos porcentajes que las tarjetas de resumen, [com.miguel.mentaltrader.core.data.OperationMetricsSummary]),
 * Canvas puro. Con cero operaciones, las 3 barras quedan en 0% -- sin errores ni caídas (HU-026
 * Escenario 4).
 */
@Composable
fun ResultDistributionBarChart(
    winPercent: Int,
    lossPercent: Int,
    breakEvenPercent: Int,
    modifier: Modifier = Modifier
) {
    val winColor = MaterialTheme.colorScheme.tertiary
    val lossColor = MaterialTheme.colorScheme.error
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.height(160.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ResultBar(label = "Ganadas", percent = winPercent, color = winColor, modifier = Modifier.weight(1f))
        ResultBar(label = "Perdidas", percent = lossPercent, color = lossColor, modifier = Modifier.weight(1f))
        ResultBar(label = "Break Even", percent = breakEvenPercent, color = neutralColor, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResultBar(label: String, percent: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Text("$percent%", style = MaterialTheme.typography.labelMedium)
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val barHeight = size.height * (percent.coerceIn(0, 100) / 100f)
            drawRect(
                color = color,
                topLeft = Offset(x = 0f, y = size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(width = size.width, height = barHeight)
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
