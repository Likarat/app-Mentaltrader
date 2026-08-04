package com.miguel.mentaltrader.feature.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miguel.mentaltrader.core.data.CatalogRankingItem
import com.miguel.mentaltrader.core.data.OperationMetricsSummary
import com.miguel.mentaltrader.feature.historial.HistorialEmptyState
import com.miguel.mentaltrader.feature.historial.HistorialEmptyStateContent
import com.miguel.mentaltrader.feature.registro.OperationFormViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * HU-026/HU-027 (sub-slice EP-004-a) + HU-028/HU-029/HU-030 (sub-slices EP-004-b/EP-004-d) +
 * HU-031 (sub-slice EP-004-c): pantalla de Inicio real -- selector de periodo fijo (predefinido +
 * personalizado, persistido entre sesiones), tarjetas de resumen numérico, gráfica de línea de R
 * acumulado, gráfica de barras de distribución de resultados, ranking de emociones/errores más
 * frecuentes y los dos estados vacíos de HU-031 (ver [InicioEmptyStateContent]). Reemplaza el
 * placeholder de EP-005.
 *
 * HU-028 Escenario 3: [PeriodoSelectorRow] vive FUERA del `LazyColumn` de contenido (como hermano
 * en el `Column` raíz) -- así permanece fijo y visible al hacer scroll, en vez de desplazarse junto
 * con las tarjetas/gráficas/ranking, y sigue alcanzable incluso cuando se muestra un estado vacío
 * (HU-031 Escenario 3: el usuario debe poder cambiar de periodo sin que el selector desaparezca).
 *
 * [onNewOperationClick] (HU-031 Escenario 1, reutilizado de HU-025/HU-008): acceso directo al
 * formulario de nueva operación, mismo destino real que el FAB y que `HistorialScreen`
 * (`MainActivity` lo cablea a la misma navegación, sin duplicarla).
 */
@Composable
fun InicioScreen(
    viewModel: InicioViewModel,
    modifier: Modifier = Modifier,
    onNewOperationClick: () -> Unit = {}
) {
    val summary by viewModel.metricsSummary.collectAsState(initial = OperationMetricsSummary.EMPTY)
    val cumulativeSeries by viewModel.cumulativeSeries.collectAsState(initial = emptyList())
    val emotionRanking by viewModel.emotionRanking.collectAsState(initial = emptyList())
    val errorRanking by viewModel.errorRanking.collectAsState(initial = emptyList())
    val filterState by viewModel.filterState.collectAsState()
    // HU-029: error de UI cuando el rango personalizado confirmado tiene fin anterior a inicio
    // (Escenario 3) -- el ViewModel ya rechazó el cambio, esto solo refleja el mensaje.
    val customRangeError by viewModel.customRangeError.collectAsState()
    // HU-031: totalOperationCount (GLOBAL, sin filtrar por periodo) + summary.operationCount
    // (calculado dentro del periodo seleccionado, ya en SQL vía metricsSummary) son las dos únicas
    // señales que necesita InicioEmptyState.resolve -- lógica PURA, testeada aparte en
    // InicioEmptyStateTest, sin duplicar ningún cálculo nuevo aquí.
    val totalOperationCount by viewModel.totalOperationCount.collectAsState(initial = 0)

    Column(modifier = modifier.fillMaxWidth()) {
        PeriodoSelectorRow(selected = filterState.selectedPeriodo, onSelect = viewModel::onSelectPeriodo)

        // HU-029 Escenario 1: al elegir "Personalizado" el sistema muestra los campos de fecha de
        // inicio/fin -- garantía ESTRUCTURAL (solo se compone cuando ese periodo está seleccionado).
        // Vive FUERA del LazyColumn (como PeriodoSelectorRow), así sigue alcanzable incluso con un
        // estado vacío de HU-031 debajo (mismo criterio que el selector de periodo).
        if (filterState.selectedPeriodo == PeriodoInicioFiltro.PERSONALIZADO) {
            InicioCustomDateRangeFields(
                filterState = filterState,
                error = customRangeError,
                onRangeChanged = viewModel::onCustomDateRange
            )
        }

        val emptyState = InicioEmptyState.resolve(
            totalOperationCount = totalOperationCount,
            operationCountInPeriod = summary.operationCount
        )
        if (emptyState != null) {
            // HU-031 Escenario 2/3: mientras hay estado vacío, no se renderiza el LazyColumn de
            // tarjetas/gráficas/ranking -- reaparece automáticamente en el siguiente render en
            // cuanto `emptyState` resuelve `null` (HU-031 Escenario 3: recuperación fluida al
            // cambiar a un periodo con datos, sin rastros del estado vacío anterior, sin ningún
            // estado de UI adicional que "limpiar" -- se deriva 100% de los Flow reales).
            InicioEmptyStateContent(emptyState, onNewOperationClick)
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValuesVertical16,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { MetricsSummaryCards(summary) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RAcumuladoTitleWithHelp()
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
}

/**
 * HU-031: contenido del estado vacío de Inicio -- DOS mensajes distintos (Escenario 1 vs Escenario
 * 2 de HU-031), nunca simultáneos:
 * - [InicioEmptyState.PRIMERA_VEZ]: REUTILIZA tal cual el mensaje+CTA ya definidos en HU-025
 *   (`HistorialEmptyStateContent(HistorialEmptyState.PRIMERA_VEZ, ...)`), sin redefinirlos --
 *   design.md decisión #6 del change `metricas-deteccion-patrones-inicio`, HU-031 nota técnica:
 *   "ambas pantallas deben invocar el mismo componente/mensaje compartido".
 * - [InicioEmptyState.SIN_DATOS_PERIODO]: mensaje PROPIO de esta historia (no existe en Historial),
 *   SIN acceso directo de creación -- las operaciones sí existen, solo no en este rango; para ver
 *   datos, el usuario cambia de periodo con [PeriodoSelectorRow] (siempre visible arriba de este
 *   bloque, HU-031 Escenario 3).
 */
@Composable
private fun InicioEmptyStateContent(emptyState: InicioEmptyState, onNewOperationClick: () -> Unit) {
    when (emptyState) {
        InicioEmptyState.PRIMERA_VEZ ->
            HistorialEmptyStateContent(HistorialEmptyState.PRIMERA_VEZ, onNewOperationClick)
        InicioEmptyState.SIN_DATOS_PERIODO -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No hay operaciones en este periodo",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Probá con otro periodo para ver tus métricas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/** HU-028 Escenario 1: chips de periodo predefinido (Día/Semana/Mes/Últimos 3 meses) -- tocar un
 * chip ya seleccionado lo deselecciona (vuelve a "todas las operaciones"), mismo criterio de
 * toggle que `HistorialFilterPanel`'s periodo chips (feature/historial). */
@Composable
private fun PeriodoSelectorRow(selected: PeriodoInicioFiltro?, onSelect: (PeriodoInicioFiltro?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(PERIODO_INICIO_CHIPS) { (periodo, label) ->
            FilterChip(
                selected = selected == periodo,
                onClick = { onSelect(if (selected == periodo) null else periodo) },
                label = { Text(label) }
            )
        }
    }
}

private val PERIODO_INICIO_CHIPS = listOf(
    PeriodoInicioFiltro.DIA to "Día",
    PeriodoInicioFiltro.SEMANA to "Semana",
    PeriodoInicioFiltro.MES to "Mes",
    PeriodoInicioFiltro.ULTIMOS_3_MESES to "Últimos 3 meses",
    PeriodoInicioFiltro.PERSONALIZADO to "Personalizado"
)

/**
 * HU-029: rango de fecha personalizado, mismo formato de texto (`dd/MM/yyyy`) que ya usa el
 * formulario de registro ([OperationFormViewModel.DATE_FORMATTER]) y el filtro de fecha de
 * Historial (`HistorialScreen.CustomDateRangeFields`, HU-022) -- este proyecto no usa un
 * `DatePicker` nativo en ningún flujo existente, se mantiene la misma convención aquí (INVEST de
 * HU-029: "el componente exacto de calendario es negociable"). Escenario 3: mientras
 * `onRangeChanged` no confirma un rango con fin anterior a inicio, [error] (ya calculado por
 * `InicioViewModel.onCustomDateRange`) muestra el mensaje de rango inválido.
 */
@Composable
private fun InicioCustomDateRangeFields(
    filterState: InicioFilterState,
    error: Boolean,
    onRangeChanged: (Long?, Long?) -> Unit
) {
    var desdeText by remember(filterState.dateFrom) {
        mutableStateOf(filterState.dateFrom?.let { millisToDateText(it) } ?: "")
    }
    var hastaText by remember(filterState.dateTo) {
        mutableStateOf(filterState.dateTo?.let { millisToDateText(it) } ?: "")
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = desdeText,
                onValueChange = {
                    desdeText = it
                    onRangeChanged(parseInicioDateStartMillis(it), filterState.dateTo)
                },
                label = { Text("Desde (dd/mm/aaaa)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = hastaText,
                onValueChange = {
                    hastaText = it
                    onRangeChanged(filterState.dateFrom, parseInicioDateEndMillis(it))
                },
                label = { Text("Hasta (dd/mm/aaaa)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (error) {
            // HU-029 Escenario 3: rango inválido (fin anterior a inicio) -- no se confirma el
            // cambio, mensaje visible hasta que el usuario corrija las fechas.
            Text(
                "Rango inválido: la fecha de fin no puede ser anterior a la fecha de inicio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val INICIO_DATE_FORMATTER: DateTimeFormatter = OperationFormViewModel.DATE_FORMATTER

private fun millisToDateText(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(INICIO_DATE_FORMATTER)

private fun parseInicioDateStartMillis(text: String): Long? = try {
    LocalDate.parse(text, INICIO_DATE_FORMATTER).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
} catch (e: DateTimeParseException) {
    null
}

private fun parseInicioDateEndMillis(text: String): Long? = try {
    LocalDate.parse(text, INICIO_DATE_FORMATTER).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
} catch (e: DateTimeParseException) {
    null
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

/**
 * Bug real reportado por el usuario (2026-08-04): la gráfica de R acumulado no explicaba qué es
 * "R" ni qué está midiendo. Se agrega un ícono de ayuda junto al título que abre un diálogo con
 * la explicación -- el mismo criterio de "múltiplo de riesgo" ya implícito en Riesgo (%) y
 * Resultado en R del formulario de registro (HU-001/HU-004), pero nunca explicado en la UI.
 */
@Composable
private fun RAcumuladoTitleWithHelp() {
    var showHelp by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("R acumulado", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { showHelp = true }) {
            Icon(
                Icons.Filled.Info,
                contentDescription = "Qué significa R",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("¿Qué es \"R\"?") },
            text = {
                Text(
                    "R mide el resultado de una operación en múltiplos de lo que arriesgaste, " +
                        "no en dinero ni en porcentaje. +2R significa que ganaste el doble de lo " +
                        "que arriesgaste; -1R que perdiste exactamente lo arriesgado. Así podés " +
                        "comparar operaciones de distinto tamaño en una misma escala.\n\n" +
                        "Esta gráfica suma el R de cada operación en orden cronológico: cuando la " +
                        "línea sube, tu resultado acumulado mejora; cuando cruza por debajo de la " +
                        "línea gris (0R), tu balance acumulado es negativo."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Entendido") }
            }
        )
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
