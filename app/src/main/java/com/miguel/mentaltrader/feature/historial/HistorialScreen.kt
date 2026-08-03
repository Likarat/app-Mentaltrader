package com.miguel.mentaltrader.feature.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.miguel.mentaltrader.core.data.MonthSummary
import com.miguel.mentaltrader.core.data.MonthSummaryFormatting
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.ResultSign
import com.miguel.mentaltrader.core.data.resultSign
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.ResultType
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * HU-015/HU-016: listado real de Historial agrupado por mes/día y paginado (Paging 3). Reemplaza
 * el listado mínimo de verificación de EP-001 -- el detalle completo (HU-018), editar/eliminar
 * (HU-020/021), resumen mensual (HU-017), filtros (HU-022/023) y visor de imagen (HU-019) llegan
 * en los sub-slices siguientes de EP-003.
 */
@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel? = null,
    onOperationClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (viewModel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pantalla de Historial (placeholder)")
        }
        return
    }

    val items = viewModel.historial.collectAsLazyPagingItems()
    // HU-021: red de seguridad de UI para la reactividad optimista del "deshacer" -- la
    // instancia de PagingData ya se filtra en HistorialViewModel, pero esta comprobación evita
    // depender de que un refresh de Paging ya haya ocurrido para ocultar la fila de inmediato
    // (ver el riesgo documentado en design.md sobre el orden filter/insertSeparators).
    val pendingDeleteIds by viewModel.pendingDeleteIds.collectAsState()
    // HU-017: mismo criterio -- colapsar/expandir es estado de UI puro (design.md decisión #2),
    // se aplica al renderizar (no re-filtra el PagingData) para no depender de un refresh de
    // Paging para reaccionar de inmediato al toque del usuario.
    val collapsedMonths by viewModel.collapsedMonths.collectAsState()
    val monthSummaries by viewModel.monthSummaries.collectAsState(initial = emptyList())

    if (items.itemCount == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Todavía no registraste ninguna operación")
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey { item ->
                when (item) {
                    is HistorialListItem.OperationRow -> "op-${item.operation.id}"
                    is HistorialListItem.GroupHeader -> "header-${item.month}-${item.day}"
                }
            }
        ) { index ->
            when (val item = items[index]) {
                is HistorialListItem.GroupHeader -> {
                    if (HistorialGroupCollapse.isVisible(item, collapsedMonths)) {
                        val month = item.monthOf()
                        GroupHeaderRow(
                            header = item,
                            summary = monthSummaries.find { it.yearMonth == month },
                            collapsed = month in collapsedMonths,
                            onToggleMonth = { viewModel.onToggleMonth(month) }
                        )
                    }
                }
                is HistorialListItem.OperationRow -> {
                    if (item.operation.id !in pendingDeleteIds && HistorialGroupCollapse.isVisible(item, collapsedMonths)) {
                        OperationCard(item.operation, viewModel, onOperationClick)
                    }
                }
                null -> Unit
            }
        }
    }
}

/**
 * HU-015/HU-016: encabezado de mes o de día. HU-017: cuando el encabezado es el de un mes
 * ([header.month] no nulo) además muestra el resumen agregado ([summary]) y un ícono de
 * colapsar/expandir; tocar esa fila alterna [collapsed] para ese mes sin afectar los demás (el
 * cálculo de qué otras filas se ocultan lo decide [HistorialGroupCollapse] en el llamador). El
 * toggle deliberadamente no es visualmente protagonista (icono pequeño, mismo tamaño de texto que
 * el resto del encabezado) -- HU-017 nota técnica: "esta opción existe, pero no es protagonista
 * visualmente".
 */
@Composable
private fun GroupHeaderRow(
    header: HistorialListItem.GroupHeader,
    summary: MonthSummary?,
    collapsed: Boolean,
    onToggleMonth: () -> Unit
) {
    Column {
        header.month?.let { month ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { onToggleMonth() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        month.month.getDisplayName(JavaTextStyle.FULL, SPANISH_LOCALE).replaceFirstChar { it.uppercase() } +
                            " ${month.year}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // HU-017 Escenario 1/3/4: resumen agregado del mes (conteo, % ganadas, R
                    // acumulado), calculado en SQL (OperationDao.monthlySummaries), coloreado
                    // según el signo del R acumulado.
                    if (summary != null) {
                        Text(
                            MonthSummaryFormatting.summaryText(summary),
                            style = MaterialTheme.typography.labelMedium,
                            color = monthSummaryColor(summary.resultSign)
                        )
                    }
                }
                Icon(
                    imageVector = if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                    contentDescription = if (collapsed) "Expandir mes" else "Colapsar mes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!collapsed) {
            Text(
                header.day.format(DAY_HEADER_FORMATTER),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun monthSummaryColor(sign: ResultSign) = when (sign) {
    ResultSign.POSITIVE -> MaterialTheme.colorScheme.tertiary
    ResultSign.NEGATIVE -> MaterialTheme.colorScheme.error
    ResultSign.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun OperationCard(operation: Operation, viewModel: HistorialViewModel, onClick: (Long) -> Unit) {
    val context = LocalContext.current
    val images by viewModel.imagesOf(operation.id).collectAsState(initial = emptyList())
    var assetName by remember(operation.assetId) { mutableStateOf<String?>(null) }
    LaunchedEffect(operation.assetId) {
        assetName = viewModel.assetNameOf(operation.assetId)
    }
    val resultColor = when (operation.result) {
        ResultType.WIN -> MaterialTheme.colorScheme.tertiary
        ResultType.LOSS -> MaterialTheme.colorScheme.error
        ResultType.BREAK_EVEN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val hora = java.time.Instant.ofEpochMilli(operation.dateTime)
        .atZone(java.time.ZoneId.systemDefault())
        .format(HORA_FORMATTER)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(operation.id) }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (images.isNotEmpty()) {
                val thumbnailPath = ImageProcessor.thumbnailPathFor(images.first().filePath)
                AsyncImage(
                    model = File(context.filesDir, thumbnailPath),
                    contentDescription = "Miniatura de la operación",
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = "Sin imagen adjunta",
                    modifier = Modifier
                        .size(56.dp)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("${assetName ?: "…"} · $hora")
                Text(
                    "${operation.result.name} · ${operation.resultInR?.let { "%.1fR".format(it) } ?: "—"}",
                    color = resultColor
                )
            }
        }
    }
}

private val SPANISH_LOCALE: Locale = Locale.forLanguageTag("es")
private val DAY_HEADER_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d", SPANISH_LOCALE)
private val HORA_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
