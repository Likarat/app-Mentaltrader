package com.miguel.mentaltrader.feature.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla de Ajustes (nueva, fuera de las 3 pestañas fijas de HU-032): hoy solo alberga
 * "Borrar todo" — borra operaciones e imágenes, conserva los catálogos personalizados.
 */
@Composable
fun AjustesScreen(viewModel: AjustesViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Borrar todo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Elimina todas las operaciones registradas y sus imágenes de este dispositivo. " +
                        "Los catálogos de Activos, Emociones y Errores que personalizaste NO se borran. " +
                        "Esta acción no se puede deshacer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.deleted) {
                    Text(
                        "Listo: se borraron todas las operaciones e imágenes.",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = !state.isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isDeleting) "Borrando..." else "Borrar todo")
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Borrar todas las operaciones?") },
            text = {
                Text(
                    "Se van a eliminar todas las operaciones registradas y sus imágenes de forma " +
                        "permanente. Los catálogos personalizados se conservan. Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.deleteAllOperationsAndImages()
                }) { Text("Borrar todo", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
