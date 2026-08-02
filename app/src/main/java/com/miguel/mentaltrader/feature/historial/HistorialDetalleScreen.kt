package com.miguel.mentaltrader.feature.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.ResultType
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * HU-018: vista de detalle completo de una operación. Orden de secciones (mismo orden de lectura
 * que el formulario de registro, spec §4): imagen primero (si existe, omitida sin dejar espacio
 * vacío si no hay ninguna), "Datos generales", "Emociones y errores", "Resultado", y "Descripción
 * entrada" al final. Editar (HU-020) y eliminar (HU-021) llegan en el sub-slice EP-003-c.
 */
@Composable
fun HistorialDetalleScreen(
    viewModel: HistorialDetalleViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val operation = state.operation
    if (operation == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontró la operación")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HU-018 Escenario 3: sin imágenes, se omite la sección entera (sin espacio vacío).
        if (state.images.isNotEmpty()) {
            AsyncImage(
                model = File(context.filesDir, ImageProcessor.thumbnailPathFor(state.images.first().filePath)),
                contentDescription = "Imagen de la operación",
                modifier = Modifier
                    .fillMaxWidth()
                    .size(220.dp)
            )
        }

        DetalleSectionCard(title = "Datos generales") {
            CampoDetalle("Fecha", operation.dateTime.let { formatFecha(it) })
            CampoDetalle("Hora", operation.dateTime.let { formatHora(it) })
            CampoDetalle("Activo", state.assetName ?: "—")
            CampoDetalle("Dirección", if (operation.direction.name == "BUY") "Compra" else "Venta")
            CampoDetalle("Calidad", "%.1f".format(operation.quality))
        }

        DetalleSectionCard(title = "Emociones y errores") {
            CampoDetalle("Emoción antes", state.emotionBeforeName ?: "—")
            operation.emotionBeforeReason?.let { CampoDetalle("Motivo", it) }
            CampoDetalle("Emoción después", state.emotionAfterName ?: "—")
            operation.emotionAfterReason?.let { CampoDetalle("Motivo", it) }
            CampoDetalle("Error", state.errorName ?: "—")
            operation.errorReason?.let { CampoDetalle("Motivo", it) }
        }

        DetalleSectionCard(title = "Resultado") {
            val resultColor = when (operation.result) {
                ResultType.WIN -> MaterialTheme.colorScheme.tertiary
                ResultType.LOSS -> MaterialTheme.colorScheme.error
                ResultType.BREAK_EVEN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            CampoDetalle("Resultado", operation.result.name, resultColor)
            operation.riskPercentage?.let { CampoDetalle("Riesgo (%)", "%.1f".format(it)) }
            operation.resultInR?.let { CampoDetalle("Resultado en R", "%.1fR".format(it), resultColor) }
            operation.plannedRatio?.let { CampoDetalle("Ratio planeado", it) }
        }

        DetalleSectionCard(title = "Descripción entrada") {
            Text(operation.entryDescription)
        }
    }
}

@Composable
private fun DetalleSectionCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun CampoDetalle(etiqueta: String, valor: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$etiqueta:", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, color = color)
    }
}

private val FECHA_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val HORA_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatFecha(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(FECHA_FORMATTER)

private fun formatHora(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(HORA_FORMATTER)
