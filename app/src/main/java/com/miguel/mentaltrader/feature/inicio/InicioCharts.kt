package com.miguel.mentaltrader.feature.inicio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** HU-037: test tags para `RAcumuladoDragGestureTest` (instrumentado) -- localizar el Canvas para
 * performTouchInput y el tooltip para assertIsDisplayed/assertTextContains, sin depender de texto
 * (que cambia con la fecha/R del punto resaltado). */
const val RACUMULADO_CANVAS_TEST_TAG = "racumulado_canvas"
const val RACUMULADO_TOOLTIP_TEST_TAG = "racumulado_tooltip"

/**
 * HU-026 Escenario 2: gráfica de línea del R acumulado, dibujada con Compose Canvas puro (sin
 * librería de gráficas nueva -- design.md decisión #1: `stack-allowlist.json` no admite hoy
 * ninguna librería de gráficas, y Canvas alcanza para una polilínea simple). Sin operaciones (o
 * con una sola), no hay línea que trazar todavía -- se muestra un mensaje neutro en su lugar
 * (HU-026 Escenario 4, cálculo seguro sin caídas).
 *
 * HU-037 (nuevo slice, post-construccion): tocar y arrastrar sobre el Canvas muestra un indicador
 * (línea vertical + tooltip) sobre el punto más cercano a la posición X del dedo -- hit-testing vía
 * [RAcumuladoHitTest.nearestIndex] (misma escala `stepX` que ya usa este Canvas para dibujar la
 * polilínea, design.md decisión #1). El índice resaltado es estado de interacción puramente de UI
 * (`remember`, no vive en `InicioViewModel`, design.md decisión #2) -- permanece fijo tras soltar el
 * dedo (Escenario 3) hasta un nuevo arrastre o un toque simple (sin arrastre) sobre el propio Canvas.
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
    var highlightedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val indicatorColor = MaterialTheme.colorScheme.onSurface

    // Bug real reportado por el usuario (2026-08-04): la gráfica no tenía ninguna referencia
    // numérica -- una polilínea sin ejes no comunica qué se está midiendo. Se agregan las
    // etiquetas de valor máximo/mínimo del eje Y (mismo formato "+2.0R"/"-1.5R" que las tarjetas
    // de resumen, InicioFormatting.signedR) superpuestas en las esquinas del Canvas.
    //
    // HU-037: BoxWithConstraints (no Box) -- la verificación visual real (gate fidelity) encontró
    // que el tooltip quedaba SIEMPRE centrado arriba, sin seguir la posición X del punto resaltado
    // (el AC pide el indicador "sobre el punto"). Se necesita el ancho real en px para calcular esa
    // posición, de ahí BoxWithConstraints + LocalDensity.
    BoxWithConstraints(modifier = modifier.height(160.dp).fillMaxWidth()) {
        val density = LocalDensity.current
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag(RACUMULADO_CANVAS_TEST_TAG)
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            highlightedIndex = RAcumuladoHitTest.nearestIndex(offset.x, size.width.toFloat(), points.size)
                        },
                        onDrag = { change, _ ->
                            highlightedIndex = RAcumuladoHitTest.nearestIndex(change.position.x, size.width.toFloat(), points.size)
                        }
                    )
                }
                .pointerInput(points) {
                    detectTapGestures(onTap = { highlightedIndex = null })
                }
        ) {
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

            // HU-037: línea vertical del indicador sobre el punto resaltado por el arrastre.
            highlightedIndex?.let { index ->
                val x = stepX * index
                drawLine(
                    color = indicatorColor.copy(alpha = 0.6f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
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
        // HU-037 Escenario 1/2/3: tooltip con fecha + R del punto resaltado -- overlay independiente
        // del Canvas (misma técnica que las etiquetas de eje Y de arriba), posicionado en X sobre el
        // punto resaltado (mismo stepX que usa el Canvas para dibujar), no centrado fijo. Ancho
        // estimado de 90.dp (la mitad del tooltip) para centrarlo sobre el punto y evitar que se
        // salga del Canvas por los bordes (coerceIn).
        highlightedIndex?.let { index ->
            val point = points[index]
            val stepXPx = canvasWidthPx / (points.size - 1)
            val pointXPx = stepXPx * index
            val tooltipHalfWidthPx = with(density) { 45.dp.toPx() }
            val tooltipXPx = (pointXPx - tooltipHalfWidthPx).coerceIn(0f, (canvasWidthPx - 2 * tooltipHalfWidthPx).coerceAtLeast(0f))
            val tooltipXDp = with(density) { tooltipXPx.toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = tooltipXDp)
                    .testTag(RACUMULADO_TOOLTIP_TEST_TAG)
                    .semantics(mergeDescendants = true) {}
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "${millisToDateText(point.dateTime)} · ${InicioFormatting.signedR(point.cumulativeResultInR)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * HU-026 Escenario 3: gráfica de barras con el % de operaciones Ganadas/Perdidas/Break Even
 * (mismos porcentajes que las tarjetas de resumen, [com.miguel.mentaltrader.core.data.OperationMetricsSummary]),
 * Canvas puro. Con cero operaciones, las 3 barras quedan en 0% -- sin errores ni caídas (HU-026
 * Escenario 4).
 *
 * HU-038 Escenario 1/2: [mode] alterna la ETIQUETA de cada barra (porcentaje <-> conteo absoluto),
 * simultáneo con [MetricsSummaryCards] (mismo [ResumenDisplayMode] compartido) -- la ALTURA de la
 * barra sigue siempre proporcional al porcentaje ([winPercent]/[lossPercent]/[breakEvenPercent]),
 * nunca al conteo absoluto (que no tiene un techo fijo de 100 con el que escalar la barra).
 */
@Composable
fun ResultDistributionBarChart(
    mode: ResumenDisplayMode,
    winPercent: Int,
    lossPercent: Int,
    breakEvenPercent: Int,
    winCount: Int,
    lossCount: Int,
    breakEvenCount: Int,
    modifier: Modifier = Modifier
) {
    val winColor = MaterialTheme.colorScheme.tertiary
    val lossColor = MaterialTheme.colorScheme.error
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.height(160.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ResultBar(
            label = "Ganadas",
            percent = winPercent,
            displayValue = if (mode == ResumenDisplayMode.PORCENTAJE) "$winPercent%" else winCount.toString(),
            color = winColor,
            modifier = Modifier.weight(1f)
        )
        ResultBar(
            label = "Perdidas",
            percent = lossPercent,
            displayValue = if (mode == ResumenDisplayMode.PORCENTAJE) "$lossPercent%" else lossCount.toString(),
            color = lossColor,
            modifier = Modifier.weight(1f)
        )
        ResultBar(
            label = "Break Even",
            percent = breakEvenPercent,
            displayValue = if (mode == ResumenDisplayMode.PORCENTAJE) "$breakEvenPercent%" else breakEvenCount.toString(),
            color = neutralColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ResultBar(label: String, percent: Int, displayValue: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Text(displayValue, style = MaterialTheme.typography.labelMedium)
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
