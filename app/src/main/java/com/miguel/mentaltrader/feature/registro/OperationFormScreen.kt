package com.miguel.mentaltrader.feature.registro

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.ui.theme.GradientButton
import java.io.File
import java.util.UUID

@Composable
fun OperationFormScreen(
    viewModel: OperationFormViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val emotions by viewModel.emotions.collectAsState()
    val errorsCatalog by viewModel.errors.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Bug real reportado por el usuario: un campo obligatorio sin completar (ej. Dirección)
        // podía bloquear "Guardar" sin ningún aviso visible si el usuario ya había scrolleado
        // más abajo — el resaltado por campo no alcanza si no está a la vista. Este banner fijo
        // (no se scrollea) queda siempre visible mientras haya errores pendientes.
        if (state.fieldErrors.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "No se pudo guardar: hay ${state.fieldErrors.size} campo(s) obligatorio(s) sin " +
                        "completar o con un valor inválido. Revisá los campos marcados en rojo.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // Bug real reportado por el usuario: con enableEdgeToEdge() + targetSdk 36,
                // windowSoftInputMode="adjustResize" ya no redimensiona la ventana solo — hay que
                // reservar el espacio del teclado a mano para poder scrollear los campos que tapa.
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        FormSectionCard(title = "Identificación") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.dateText,
                    onValueChange = viewModel::onDateChange,
                    label = { Text("Fecha (dd/MM/aaaa)") },
                    isError = state.fieldErrors.containsKey(OperationFormState.FIELD_DATE),
                    supportingText = { Text(state.fieldErrors[OperationFormState.FIELD_DATE] ?: "") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.timeText,
                    onValueChange = viewModel::onTimeChange,
                    label = { Text("Hora (HH:mm)") },
                    isError = state.fieldErrors.containsKey(OperationFormState.FIELD_TIME),
                    supportingText = { Text(state.fieldErrors[OperationFormState.FIELD_TIME] ?: "") },
                    modifier = Modifier.weight(1f)
                )
            }

            CatalogDropdown(
                label = "Activo",
                items = assets,
                selectedId = state.assetId,
                onSelected = viewModel::onAssetSelected,
                errorText = state.fieldErrors[OperationFormState.FIELD_ASSET],
                isAdding = state.addingCatalogField == OperationFormState.FIELD_ASSET,
                addText = state.addCatalogText,
                addError = state.addCatalogError,
                onAddRequested = { viewModel.onStartAddCatalogItem(OperationFormState.FIELD_ASSET) },
                onAddTextChange = viewModel::onAddCatalogTextChange,
                onConfirmAdd = viewModel::onConfirmAddCatalogItem,
                onCancelAdd = viewModel::onCancelAddCatalogItem
            )

            Text("Dirección")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Direction.entries.forEach { direction ->
                    FilterChip(
                        selected = state.direction == direction,
                        onClick = { viewModel.onDirectionSelected(direction) },
                        label = { Text(if (direction == Direction.BUY) "Compra" else "Venta") }
                    )
                }
            }
            if (state.fieldErrors.containsKey(OperationFormState.FIELD_DIRECTION)) {
                Text(
                    state.fieldErrors[OperationFormState.FIELD_DIRECTION] ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        FormSectionCard(title = "Contexto emocional y errores") {
            OutlinedTextField(
                value = state.qualityText,
                onValueChange = viewModel::onQualityChange,
                label = { Text("Calidad (0.0-10.0)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.fieldErrors.containsKey(OperationFormState.FIELD_QUALITY),
                supportingText = { Text(state.fieldErrors[OperationFormState.FIELD_QUALITY] ?: "") },
                modifier = Modifier.fillMaxWidth()
            )

            CatalogDropdown(
                label = "Emoción antes",
                items = emotions,
                selectedId = state.emotionBeforeId,
                onSelected = viewModel::onEmotionBeforeSelected,
                errorText = state.fieldErrors[OperationFormState.FIELD_EMOTION_BEFORE],
                isAdding = state.addingCatalogField == OperationFormState.FIELD_EMOTION_BEFORE,
                addText = state.addCatalogText,
                addError = state.addCatalogError,
                onAddRequested = { viewModel.onStartAddCatalogItem(OperationFormState.FIELD_EMOTION_BEFORE) },
                onAddTextChange = viewModel::onAddCatalogTextChange,
                onConfirmAdd = viewModel::onConfirmAddCatalogItem,
                onCancelAdd = viewModel::onCancelAddCatalogItem
            )
            OutlinedTextField(
                value = state.emotionBeforeReason,
                onValueChange = viewModel::onEmotionBeforeReasonChange,
                label = { Text("Motivo emoción antes (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            CatalogDropdown(
                label = "Emoción después",
                items = emotions,
                selectedId = state.emotionAfterId,
                onSelected = viewModel::onEmotionAfterSelected,
                errorText = state.fieldErrors[OperationFormState.FIELD_EMOTION_AFTER],
                isAdding = state.addingCatalogField == OperationFormState.FIELD_EMOTION_AFTER,
                addText = state.addCatalogText,
                addError = state.addCatalogError,
                onAddRequested = { viewModel.onStartAddCatalogItem(OperationFormState.FIELD_EMOTION_AFTER) },
                onAddTextChange = viewModel::onAddCatalogTextChange,
                onConfirmAdd = viewModel::onConfirmAddCatalogItem,
                onCancelAdd = viewModel::onCancelAddCatalogItem
            )
            OutlinedTextField(
                value = state.emotionAfterReason,
                onValueChange = viewModel::onEmotionAfterReasonChange,
                label = { Text("Motivo emoción después (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            CatalogDropdown(
                label = "Error",
                items = errorsCatalog,
                selectedId = state.errorId,
                onSelected = viewModel::onErrorSelected,
                errorText = state.fieldErrors[OperationFormState.FIELD_ERROR],
                isAdding = state.addingCatalogField == OperationFormState.FIELD_ERROR,
                addText = state.addCatalogText,
                addError = state.addCatalogError,
                onAddRequested = { viewModel.onStartAddCatalogItem(OperationFormState.FIELD_ERROR) },
                onAddTextChange = viewModel::onAddCatalogTextChange,
                onConfirmAdd = viewModel::onConfirmAddCatalogItem,
                onCancelAdd = viewModel::onCancelAddCatalogItem
            )
            OutlinedTextField(
                value = state.errorReason,
                onValueChange = viewModel::onErrorReasonChange,
                label = { Text("Motivo/descripción del error (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        FormSectionCard(title = "Resultado") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultType.entries.forEach { result ->
                    FilterChip(
                        selected = state.result == result,
                        onClick = { viewModel.onResultSelected(result) },
                        label = {
                            Text(
                                when (result) {
                                    ResultType.WIN -> "Ganada"
                                    ResultType.LOSS -> "Perdida"
                                    ResultType.BREAK_EVEN -> "Break Even"
                                }
                            )
                        }
                    )
                }
            }
            if (state.fieldErrors.containsKey(OperationFormState.FIELD_RESULT)) {
                Text(
                    state.fieldErrors[OperationFormState.FIELD_RESULT] ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = state.riskPercentageText,
                onValueChange = viewModel::onRiskPercentageChange,
                label = { Text("Riesgo (%) (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.fieldErrors.containsKey(OperationFormState.FIELD_RISK),
                supportingText = { Text(state.fieldErrors[OperationFormState.FIELD_RISK] ?: "") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Resultado en R (opcional)")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(OperationFormState.SIGN_POSITIVE, OperationFormState.SIGN_NEGATIVE).forEach { sign ->
                    FilterChip(
                        selected = state.resultInRSign == sign,
                        onClick = { viewModel.onResultInRSignChange(sign) },
                        label = { Text(sign) }
                    )
                }
                OutlinedTextField(
                    value = state.resultInRText,
                    onValueChange = viewModel::onResultInRChange,
                    label = { Text("Magnitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            OperationFormState.formatResultInR(state.resultInRSign, state.resultInRText)?.let { formatted ->
                Text(formatted, color = MaterialTheme.colorScheme.primary)
            }

            OutlinedTextField(
                value = state.plannedRatio,
                onValueChange = viewModel::onPlannedRatioChange,
                label = { Text("Ratio planeado (opcional, ej. 1:2)") },
                isError = state.fieldErrors.containsKey(OperationFormState.FIELD_RATIO),
                supportingText = { Text(state.fieldErrors[OperationFormState.FIELD_RATIO] ?: "") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        FormSectionCard(title = "Descripción e imágenes") {
            OutlinedTextField(
                value = state.entryDescription,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descripción entrada") },
                isError = state.fieldErrors.containsKey(OperationFormState.FIELD_DESCRIPTION),
                supportingText = { Text(state.fieldErrors[OperationFormState.FIELD_DESCRIPTION] ?: "") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            ImageAttachmentSection(
                pendingImages = state.pendingImages,
                imageError = state.imageError,
                onImageAdded = viewModel::onImageAdded,
                onImageRemoved = viewModel::onImageRemoved,
                onCameraPermissionDenied = viewModel::onCameraPermissionDenied
            )
        }

        GradientButton(text = "Guardar", onClick = viewModel::save, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun ImageAttachmentSection(
    pendingImages: List<PendingImage>,
    imageError: String?,
    onImageAdded: (Uri) -> Unit,
    onImageRemoved: (Uri) -> Unit,
    onCameraPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) onImageAdded(uri)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraOutputUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            onCameraPermissionDenied()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImageAdded(uri)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Imágenes (${pendingImages.size}/${OperationFormState.MAX_IMAGES})")

        if (pendingImages.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pendingImages.forEach { pending ->
                    Box {
                        AsyncImage(
                            model = pending.uri,
                            contentDescription = "Imagen adjunta sin procesar",
                            modifier = Modifier.size(96.dp)
                        )
                        IconButton(onClick = { onImageRemoved(pending.uri) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar imagen")
                        }
                    }
                }
            }
        }

        if (imageError != null) {
            Text(imageError, color = MaterialTheme.colorScheme.error)
        }

        if (pendingImages.size < OperationFormState.MAX_IMAGES) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        val uri = createCameraOutputUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Cámara") }
                OutlinedButton(onClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Galería") }
            }
        }
    }
}

private fun createCameraOutputUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "camera_tmp").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CatalogDropdown(
    label: String,
    items: List<CatalogItem>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
    errorText: String?,
    isAdding: Boolean,
    addText: String,
    addError: String?,
    onAddRequested: () -> Unit,
    onAddTextChange: (String) -> Unit,
    onConfirmAdd: () -> Unit,
    onCancelAdd: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = items.firstOrNull { it.id == selectedId }?.name ?: ""

    Column(modifier = Modifier.fillMaxWidth()) {
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
                isError = errorText != null,
                supportingText = { Text(errorText ?: "") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            onSelected(item.id)
                            expanded = false
                        }
                    )
                }
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("+ Agregar nueva") },
                    onClick = {
                        onAddRequested()
                        expanded = false
                    }
                )
            }
        }

        // HU-013: campo inline para crear un elemento de catálogo sin salir del formulario,
        // reutilizando CatalogRepository.addItem (misma validación de HU-010, no se reimplementa
        // aquí). El foco se recupera al mostrar un error de duplicado (Escenario 3: el campo
        // permanece abierto y con foco para corregir el nombre).
        if (isAdding) {
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            LaunchedEffect(addError) {
                if (addError != null) focusRequester.requestFocus()
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = addText,
                    onValueChange = onAddTextChange,
                    label = { Text("Nuevo valor para $label") },
                    isError = addError != null,
                    supportingText = { Text(addError ?: "") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
                androidx.compose.material3.Button(onClick = onConfirmAdd) { Text("Agregar") }
                androidx.compose.material3.TextButton(onClick = onCancelAdd) { Text("Cancelar") }
            }
        }
    }
}
