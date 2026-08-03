package com.miguel.mentaltrader.feature.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miguel.mentaltrader.core.data.CatalogRankingItem
import com.miguel.mentaltrader.core.data.OperationMetricsSummary
import java.util.Locale

/**
 * HU-026/HU-027 (sub-slice EP-004-a): pantalla de Inicio real -- tarjetas de resumen numérico,
 * gráfica de línea de R acumulado, gráfica de barras de distribución de resultados y ranking de
 * emociones/errores más frecuentes. Reemplaza el placeholder de EP-005.
 *
 * EP-004-a asume "el periodo ya seleccionado" es el conjunto completo de operaciones (ver KDoc de
 * [InicioViewModel]) -- el selector de periodo (predefinido/personalizado, con persistencia)
 * llega con HU-028/HU-029/HU-030 en los sub-slices siguientes de EP-004, por eso no aparece
 * todavía en el layout (spec §"Pestaña de Inicio", punto 1, diferido). El estado vacío amigable
 * de "sin ninguna operación registrada" (HU-031) también llega en un sub-slice siguiente
 * (EP-004-c); por ahora, sin operaciones, las tarjetas/gráficas/ranking muestran sus valores
 * neutros (HU-026 Escenario 4 / HU-027 Escenario 4).
 */
@Composable
fun InicioScreen(viewModel: InicioViewModel, modifier: Modifier = Modifier) {
    val summary by viewModel.metricsSummary.collectAsState(initial = OperationMetricsSummary.EMPTY)
    val cumulativeSeries by viewModel.cumulativeSeries.collectAsState(initial = emptyList())
    val emotionRanking by viewModel.emotionRanking.collectAsState(initial = emptyList())
    val errorRanking by viewModel.errorRanking.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValuesVertical16,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MetricsSummaryCards(summary) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("R acumulado", style = MaterialTheme.typography.titleMedium)
                RAcumuladoLineChart(points = cumulativeSeries, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Distribución de resultados", style = MaterialTheme.typography.titleMedium)
                ResultDistributionBarChart(
                    winPercent = summary.winPercent,
                    lossPercent = summary.lossPercent,
                    breakEvenPercent = summary.breakEvenPercent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            RankingSection(title = "Emociones más frecuentes", items = emotionRanking)
        }
        item {
            RankingSection(title = "Errores más frecuentes", items = errorRanking)
        }
    }
}

private val PaddingValuesVertical16 = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp, horizontal = 16.dp)

@Composable
private fun MetricsSummaryCards(summary: OperationMetricsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(label = "Operaciones", value = summary.operationCount.toString(), modifier = Modifier.weight(1f))
            MetricCard(label = "Calidad prom.", value = InicioFormatting.oneDecimal(summary.avgQuality), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(label = "% Ganadas", value = "${summary.winPercent}%", modifier = Modifier.weight(1f))
            MetricCard(label = "% Perdidas", value = "${summary.lossPercent}%", modifier = Modifier.weight(1f))
            MetricCard(label = "% BE", value = "${summary.breakEvenPercent}%", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                label = "R total",
                value = InicioFormatting.signedR(summary.totalResultInR),
                modifier = Modifier.weight(1f),
                color = InicioFormatting.colorFor(summary.totalResultInR)
            )
            MetricCard(
                label = "R promedio",
                value = InicioFormatting.signedR(summary.avgResultInR),
                modifier = Modifier.weight(1f),
                color = InicioFormatting.colorFor(summary.avgResultInR)
            )
        }
    }
}

@Composable
private fun RowScope.MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RankingSection(title: String, items: List<CatalogRankingItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (items.isEmpty()) {
            // HU-027 Escenario 4: periodo sin operaciones -- ranking vacío, sin error.
            Text(
                "Sin datos todavía",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${index + 1}. ${item.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(item.frequency.toString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Formateo numérico determinístico (mismo criterio que `MonthSummaryFormatting`: `Locale.US`
 * explícito para no depender de la coma decimal del locale del dispositivo). */
object InicioFormatting {
    fun oneDecimal(value: Float): String = "%.1f".format(Locale.US, value)

    fun signedR(value: Float): String {
        val formatted = "%.1f".format(Locale.US, kotlin.math.abs(value))
        val sign = if (value < 0f) "-" else ""
        return "$sign${formatted}R"
    }

    @Composable
    fun colorFor(value: Float): androidx.compose.ui.graphics.Color = when {
        value > 0f -> MaterialTheme.colorScheme.tertiary
        value < 0f -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
