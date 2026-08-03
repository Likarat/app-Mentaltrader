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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.MonthSummary
import com.miguel.mentaltrader.core.data.MonthSummaryFormatting
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.ResultSign
import com.miguel.mentaltrader.core.data.resultSign
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.feature.registro.OperationFormViewModel
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * HU-015/HU-016: listado real de Historial agrupado por mes/día y paginado (Paging 3). Reemplaza
 * el listado mínimo de verificación de EP-001. Detalle completo (HU-018), editar/eliminar
 * (HU-020/021), resumen mensual (HU-017) y filtros + búsqueda (HU-022/023, ver
 * [HistorialFilterPanel]) ya están cableados aquí; el visor de imagen (HU-019) y la persistencia
 * del filtro entre sesiones (HU-024) llegan en los sub-slices siguientes de EP-003.
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
    val filterState by viewModel.filterState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // HU-022/HU-023: el panel de filtros vive SIEMPRE visible (no dentro del bloque de
        // "listado vacío" de abajo) -- si un filtro deja el listado sin resultados, "Limpiar
        // filtros" (HU-022 Escenario 4) debe seguir siendo alcanzable.
        HistorialFilterPanel(viewModel = viewModel, filterState = filterState)

        if (items.itemCount == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // HU-025 (sub-slice EP-003-f, siguiente): este mensaje todavía no distingue "sin
                // ninguna operación registrada nunca" de "hay operaciones pero el filtro activo no
                // arroja resultados" -- distinción deliberadamente diferida (ver design.md/tasks.md
                // 6.1, secuenciado después de EP-003-e para tener al menos un filtro real que
                // probar en ese escenario).
                Text("Todavía no registraste ninguna operación")
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
}

/**
 * HU-022/HU-023: panel de filtros + búsqueda de Historial. Colapsado por defecto (icono con punto
 * indicador si hay algún criterio activo); al expandirlo muestra: periodo predefinido/personalizado
 * (HU-022 Escenario 2), selectores por etiqueta -- Activo/Resultado/Error/Emoción antes/Emoción
 * después (HU-022 Escenario 1, combinados con AND entre sí y con los demás -- HU-022 Escenario 3),
 * campo de búsqueda de texto (HU-023) y "Limpiar filtros" (HU-022 Escenario 4).
 */
@Composable
private fun HistorialFilterPanel(viewModel: HistorialViewModel, filterState: HistorialFilterState) {
    var expanded by remember { mutableStateOf(false) }
    val assets by viewModel.assets.collectAsState(initial = emptyList())
    val emotions by viewModel.emotions.collectAsState(initial = emptyList())
    val errorsCatalog by viewModel.errorsCatalog.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filtros",
                    tint = if (!filterState.isEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (filterState.isEmpty) "Filtros" else "Filtros activos",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (!filterState.isEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Ocultar filtros" else "Mostrar filtros",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filterState.searchText.orEmpty(),
                    onValueChange = { viewModel.onSearchTextChanged(it) },
                    label = { Text("Buscar por palabra clave") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Periodo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PERIODO_CHIPS) { (periodo, label) ->
                        FilterChip(
                            selected = filterState.selectedPeriodo == periodo,
                            onClick = {
                                viewModel.onSelectPeriodo(if (filterState.selectedPeriodo == periodo) null else periodo)
                            },
                            label = { Text(label) }
                        )
                    }
                }
                if (filterState.selectedPeriodo == PeriodoFiltro.PERSONALIZADO) {
                    CustomDateRangeFields(filterState = filterState, onRangeChanged = viewModel::onCustomDateRange)
                }

                FilterCatalogDropdown("Activo", assets, filterState.assetId, viewModel::onFilterAsset)
                FilterResultDropdown(filterState.result, viewModel::onFilterResult)
                FilterCatalogDropdown("Error", errorsCatalog, filterState.errorId, viewModel::onFilterError)
                FilterCatalogDropdown("Emoción antes", emotions, filterState.emotionBeforeId, viewModel::onFilterEmotionBefore)
                FilterCatalogDropdown("Emoción después", emotions, filterState.emotionAfterId, viewModel::onFilterEmotionAfter)

                if (!filterState.isEmpty) {
                    TextButton(
                        onClick = { viewModel.onClearFilters() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Limpiar filtros", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

private val PERIODO_CHIPS = listOf(
    PeriodoFiltro.ULTIMA_SEMANA to "Última semana",
    PeriodoFiltro.ULTIMO_MES to "Último mes",
    PeriodoFiltro.ESTE_MES to "Este mes",
    PeriodoFiltro.ULTIMOS_3_MESES to "Últimos 3 meses",
    PeriodoFiltro.PERSONALIZADO to "Personalizado"
)

/** HU-022 Escenario 2: rango de fecha personalizado, mismo formato de texto (`dd/MM/yyyy`) que ya
 * usa el formulario de registro ([OperationFormViewModel.DATE_FORMATTER]) -- este proyecto no usa
 * un `DatePicker` nativo en ningún flujo existente (fecha/hora de la operación también se ingresan
 * como texto, ver `OperationFormScreen`), se mantiene la misma convención aquí. */
@Composable
private fun CustomDateRangeFields(filterState: HistorialFilterState, onRangeChanged: (Long?, Long?) -> Unit) {
    var desdeText by remember(filterState.dateFrom) {
        mutableStateOf(filterState.dateFrom?.let { millisToDateText(it) } ?: "")
    }
    var hastaText by remember(filterState.dateTo) {
        mutableStateOf(filterState.dateTo?.let { millisToDateText(it) } ?: "")
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = desdeText,
            onValueChange = {
                desdeText = it
                onRangeChanged(parseDateStartMillis(it), filterState.dateTo)
            },
            label = { Text("Desde (dd/mm/aaaa)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = hastaText,
            onValueChange = {
                hastaText = it
                onRangeChanged(filterState.dateFrom, parseDateEndMillis(it))
            },
            label = { Text("Hasta (dd/mm/aaaa)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

private val FILTER_DATE_FORMATTER: DateTimeFormatter = OperationFormViewModel.DATE_FORMATTER

private fun millisToDateText(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(FILTER_DATE_FORMATTER)

private fun parseDateStartMillis(text: String): Long? = try {
    LocalDate.parse(text, FILTER_DATE_FORMATTER).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
} catch (e: DateTimeParseException) {
    null
}

private fun parseDateEndMillis(text: String): Long? = try {
    LocalDate.parse(text, FILTER_DATE_FORMATTER).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
} catch (e: DateTimeParseException) {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterCatalogDropdown(
    label: String,
    items: List<CatalogItem>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = items.firstOrNull { it.id == selectedId }?.name ?: "Todos"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = { onSelected(null); expanded = false })
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item.name) }, onClick = { onSelected(item.id); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterResultDropdown(selected: ResultType?, onSelected: (ResultType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = selected?.name ?: "Todos"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Resultado") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = { onSelected(null); expanded = false })
            ResultType.entries.forEach { result ->
                DropdownMenuItem(text = { Text(result.name) }, onClick = { onSelected(result); expanded = false })
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
