package com.miguel.mentaltrader.feature.historial

import androidx.compose.foundation.background
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
import com.miguel.mentaltrader.core.data.Operation
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
    modifier: Modifier = Modifier
) {
    if (viewModel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pantalla de Historial (placeholder)")
        }
        return
    }

    val items = viewModel.historial.collectAsLazyPagingItems()

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
                is HistorialListItem.GroupHeader -> GroupHeaderRow(item)
                is HistorialListItem.OperationRow -> OperationCard(item.operation, viewModel)
                null -> Unit
            }
        }
    }
}

@Composable
private fun GroupHeaderRow(header: HistorialListItem.GroupHeader) {
    Column {
        header.month?.let { month ->
            Text(
                month.month.getDisplayName(JavaTextStyle.FULL, SPANISH_LOCALE).replaceFirstChar { it.uppercase() } +
                    " ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Text(
            header.day.format(DAY_HEADER_FORMATTER),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OperationCard(operation: Operation, viewModel: HistorialViewModel) {
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
        modifier = Modifier.fillMaxWidth()
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
