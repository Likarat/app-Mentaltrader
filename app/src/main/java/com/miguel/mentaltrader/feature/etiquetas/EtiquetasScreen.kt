package com.miguel.mentaltrader.feature.etiquetas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.model.CatalogType

private val TABS = listOf(
    CatalogType.ASSET to "Activos",
    CatalogType.EMOTION to "Emociones",
    CatalogType.ERROR to "Errores"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtiquetasScreen(viewModel: EtiquetasViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val emotions by viewModel.emotions.collectAsState()
    val errors by viewModel.errors.collectAsState()

    val items = when (state.selectedType) {
        CatalogType.ASSET -> assets
        CatalogType.EMOTION -> emotions
        CatalogType.ERROR -> errors
    }
    val selectedTabIndex = TABS.indexOfFirst { it.first == state.selectedType }

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
            TABS.forEach { (type, label) ->
                Tab(
                    selected = type == state.selectedType,
                    onClick = { viewModel.onTypeSelected(type) },
                    text = { Text(label) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.addFieldValue,
                onValueChange = viewModel::onAddFieldChange,
                label = { Text("Agregar nuevo elemento") },
                isError = state.addError != null,
                supportingText = { Text(state.addError ?: "") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = viewModel::onConfirmAdd) { Text("Agregar") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { viewModel.onStartEdit(item) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar ${item.name}")
                            }
                            IconButton(onClick = { viewModel.onRequestDelete(item) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${item.name}")
                            }
                        }
                    }
                )
            }
        }
    }

    state.editingItem?.let { editing ->
        EditCatalogItemDialog(
            item = editing,
            value = state.editFieldValue,
            error = state.editError,
            onValueChange = viewModel::onEditFieldChange,
            onConfirm = viewModel::onConfirmEdit,
            onDismiss = viewModel::onCancelEdit
        )
    }

    state.pendingDeleteItem?.let { pending ->
        val usageCount = state.pendingDeleteUsageCount
        AlertDialog(
            onDismissRequest = viewModel::onCancelDelete,
            title = { Text("Eliminar elemento") },
            text = {
                Text(
                    if (usageCount != null && usageCount > 0) {
                        "\"${pending.name}\" está en uso en $usageCount operación(es). Si lo " +
                            "eliminás, esas operaciones conservan su valor histórico sin cambios. " +
                            "¿Igual querés eliminarlo?"
                    } else {
                        "¿Estás seguro que deseas eliminar \"${pending.name}\"?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelDelete) { Text("Cancelar") }
            }
        )
    }

    state.blockedDeleteMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissBlockedDeleteMessage,
            title = { Text("No se puede eliminar") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::onDismissBlockedDeleteMessage) { Text("Entendido") }
            }
        )
    }
}

@Composable
private fun EditCatalogItemDialog(
    item: CatalogItem,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar elemento") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                isError = error != null,
                supportingText = { Text(error ?: "") }
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
