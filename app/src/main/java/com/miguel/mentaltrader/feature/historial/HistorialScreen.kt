package com.miguel.mentaltrader.feature.historial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImageDao
import com.miguel.mentaltrader.core.image.ImageProcessor
import java.io.File

/**
 * Listado mínimo de verificación (no es la feature de Historial completa, ver EP-003):
 * solo permite confirmar visualmente que el guardado de una operación (y su miniatura, HU-007)
 * funciona de punta a punta.
 */
@Composable
fun HistorialScreen(
    operationDao: OperationDao? = null,
    operationImageDao: OperationImageDao? = null,
    modifier: Modifier = Modifier
) {
    if (operationDao == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pantalla de Historial (placeholder)")
        }
        return
    }

    val operations by operationDao.getAllOrderedByDateDesc().collectAsState(initial = emptyList())

    if (operations.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Todavía no registraste ninguna operación")
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(operations, key = { it.id }) { operation ->
            OperationRow(operation, operationImageDao)
            HorizontalDivider()
        }
    }
}

@Composable
private fun OperationRow(operation: Operation, operationImageDao: OperationImageDao?) {
    val context = LocalContext.current
    val images = operationImageDao?.getByOperationId(operation.id)?.collectAsState(initial = emptyList())?.value.orEmpty()

    Row(modifier = Modifier.padding(16.dp)) {
        if (images.isNotEmpty()) {
            val thumbnailPath = ImageProcessor.thumbnailPathFor(images.first().filePath)
            AsyncImage(
                model = File(context.filesDir, thumbnailPath),
                contentDescription = "Miniatura de la operación",
                modifier = Modifier.size(56.dp)
            )
        }
        Column(modifier = Modifier.padding(start = if (images.isNotEmpty()) 12.dp else 0.dp)) {
            Text("Operación #${operation.id} — ${operation.direction} — ${operation.result}")
            Text(operation.entryDescription)
        }
    }
}
