package com.miguel.mentaltrader.feature.registro

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.data.OperationImageDao
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class OperationFormViewModel(
    private val operationDao: OperationDao,
    private val catalogItemDao: CatalogItemDao,
    private val operationImageDao: OperationImageDao? = null,
    private val imageProcessor: ImageProcessor? = null,
    /** HU-005-AC5 (reloj del dispositivo desconfigurado): fuente del "ahora" usada para el
     * prellenado. Inyectable para poder probar el comportamiento con un reloj arbitrario
     * (pasado/futuro absurdo) sin depender de manipular el reloj real del sistema operativo,
     * que no es viable de forma confiable en este entorno. En producción siempre es
     * `LocalDateTime.now()` (valor real del dispositivo, sin corregir ni validar aquí — esa
     * responsabilidad es de HU-004 al guardar). */
    private val nowProvider: () -> LocalDateTime = LocalDateTime::now
) : ViewModel() {

    private val _state = MutableStateFlow(OperationFormState.INITIAL)
    val state: StateFlow<OperationFormState> = _state.asStateFlow()

    /** Snapshot contra el que se compara el estado actual para el dirty-check de HU-009. Se
     * actualiza una vez con los valores prellenados (HU-005) para que abrir el formulario y no
     * tocar nada no cuente como "sucio". */
    private var initialSnapshot: OperationFormState = OperationFormState.INITIAL

    /** true si el estado actual difiere del snapshot inicial del formulario (HU-009). */
    val isDirty: StateFlow<Boolean> = _state
        .map { it != initialSnapshot }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val assets: StateFlow<List<CatalogItem>> =
        catalogItemDao.getByType(CatalogType.ASSET)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val emotions: StateFlow<List<CatalogItem>> =
        catalogItemDao.getByType(CatalogType.EMOTION)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val errors: StateFlow<List<CatalogItem>> =
        catalogItemDao.getByType(CatalogType.ERROR)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val now = nowProvider()
            val assetCatalog = catalogItemDao.getByType(CatalogType.ASSET).first()
            // HU-005 Esc.2/3: solo se prellena el Activo si el catálogo no tiene más que la
            // semilla (ningún elemento agregado manualmente por el usuario).
            val prefillAssetId = if (assetCatalog.size == 1) assetCatalog.first().id else null
            val prefilled = OperationFormState.INITIAL.copy(
                dateText = now.format(DATE_FORMATTER),
                timeText = now.format(TIME_FORMATTER),
                assetId = prefillAssetId
            )
            // Bug real encontrado en la primera corrida instrumentada (2026-07-29): esta
            // corrutina corre en Dispatchers.Main y compite con llamadas síncronas a los
            // onXxx() (p.ej. desde un test o, en teoría, una interacción muy rápida del
            // usuario) — sin este guard, un `_state.value = prefilled` incondicional podía
            // pisar campos que ya se habían completado. Solo se prellena si el formulario
            // sigue intacto.
            //
            // Bug real #2 encontrado por FormExitGuardTest (HU-009-AC5, 2026-07-29): con
            // `viewModelScope` en `Dispatchers.Main.immediate`, escribir `_state.value` ANTES de
            // `initialSnapshot` dejaba una ventana donde el colector de `isDirty` (el `.map{}` de
            // más arriba, disparado sincrónicamente por el cambio de `_state` en el mismo hilo)
            // podía evaluarse contra el `initialSnapshot` TODAVÍA viejo (`INITIAL`), marcando
            // `isDirty=true` de forma permanente (nada vuelve a recalcularlo hasta la próxima
            // edición real) — el diálogo de "¿Salir sin guardar?" aparecía SIEMPRE al abrir el
            // formulario, incluso sin que el usuario tocara nada. Se actualiza `initialSnapshot`
            // PRIMERO para que, quien sea que reaccione al cambio de `_state`, ya vea el snapshot
            // correcto.
            if (_state.value == OperationFormState.INITIAL) {
                initialSnapshot = prefilled
                _state.value = prefilled
            }
        }
    }

    fun onDateChange(value: String) = update { it.copy(dateText = value) }
    fun onTimeChange(value: String) = update { it.copy(timeText = value) }
    fun onAssetSelected(id: Long) = update { it.copy(assetId = id) }
    fun onDirectionSelected(value: Direction) = update { it.copy(direction = value) }
    fun onQualityChange(value: String) = update { it.copy(qualityText = value) }
    fun onEmotionBeforeSelected(id: Long) = update { it.copy(emotionBeforeId = id) }
    fun onEmotionBeforeReasonChange(value: String) = update { it.copy(emotionBeforeReason = value) }
    fun onEmotionAfterSelected(id: Long) = update { it.copy(emotionAfterId = id) }
    fun onEmotionAfterReasonChange(value: String) = update { it.copy(emotionAfterReason = value) }
    fun onErrorSelected(id: Long) = update { it.copy(errorId = id) }
    fun onErrorReasonChange(value: String) = update { it.copy(errorReason = value) }
    fun onResultSelected(value: ResultType) = update { it.copy(result = value) }
    fun onRiskPercentageChange(value: String) = update { it.copy(riskPercentageText = value) }
    fun onResultInRSignChange(sign: String) = update { it.copy(resultInRSign = sign) }
    fun onResultInRChange(value: String) = update { it.copy(resultInRText = value) }
    fun onPlannedRatioChange(value: String) = update { it.copy(plannedRatio = value) }
    fun onDescriptionChange(value: String) = update { it.copy(entryDescription = value) }

    /** HU-006: adjunta una imagen sin procesar (miniatura se muestra tal cual hasta Guardar). */
    fun onImageAdded(uri: Uri) {
        val current = _state.value
        if (current.pendingImages.size >= OperationFormState.MAX_IMAGES) {
            _state.value = current.copy(imageError = "Ya adjuntaste el máximo de 2 imágenes")
            return
        }
        val processor = imageProcessor
        if (processor == null) {
            _state.value = current.copy(pendingImages = current.pendingImages + PendingImage(uri), imageError = null)
            return
        }
        viewModelScope.launch {
            if (processor.canDecode(uri)) {
                val latest = _state.value
                _state.value = latest.copy(
                    pendingImages = latest.pendingImages + PendingImage(uri),
                    imageError = null
                )
            } else {
                _state.value = _state.value.copy(
                    imageError = "No se pudo procesar la imagen (archivo dañado o formato no soportado)"
                )
            }
        }
    }

    fun onImageRemoved(uri: Uri) {
        val current = _state.value
        _state.value = current.copy(
            pendingImages = current.pendingImages.filterNot { it.uri == uri },
            imageError = null
        )
    }

    fun onCameraPermissionDenied() {
        _state.value = _state.value.copy(
            imageError = "Permiso de cámara rechazado. Podés continuar sin imagen o adjuntar desde la galería."
        )
    }

    private fun update(transform: (OperationFormState) -> OperationFormState) {
        _state.value = transform(_state.value).copy(fieldErrors = emptyMap(), isSaved = false)
    }

    fun save() {
        val current = _state.value
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _state.value = current.copy(fieldErrors = errors)
            return
        }
        val dateTimeMillis = parseDateTime(current.dateText, current.timeText)
        if (dateTimeMillis == null) {
            _state.value = current.copy(
                fieldErrors = mapOf(OperationFormState.FIELD_DATE to "Fecha/hora inválida")
            )
            return
        }
        if (dateTimeMillis > System.currentTimeMillis()) {
            _state.value = current.copy(
                fieldErrors = mapOf(OperationFormState.FIELD_DATE to "No se permiten fechas/horas futuras")
            )
            return
        }
        val riskPercentage = parseDecimal(current.riskPercentageText)
        val resultInRMagnitude = parseDecimal(current.resultInRText)
        val resultInR = resultInRMagnitude?.let {
            if (current.resultInRSign == OperationFormState.SIGN_NEGATIVE) -it else it
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val operationId = operationDao.insert(
                Operation(
                    dateTime = dateTimeMillis,
                    assetId = current.assetId!!,
                    direction = current.direction!!,
                    quality = parseDecimal(current.qualityText) ?: 0f,
                    emotionBeforeId = current.emotionBeforeId!!,
                    emotionBeforeReason = current.emotionBeforeReason.ifBlank { null },
                    emotionAfterId = current.emotionAfterId!!,
                    emotionAfterReason = current.emotionAfterReason.ifBlank { null },
                    errorId = current.errorId!!,
                    errorReason = current.errorReason.ifBlank { null },
                    result = current.result!!,
                    riskPercentage = riskPercentage,
                    resultInR = resultInR,
                    plannedRatio = current.plannedRatio.ifBlank { null },
                    entryDescription = current.entryDescription,
                    createdAt = now,
                    updatedAt = now
                )
            )
            persistImages(operationId, current.pendingImages, now)
            _state.value = current.copy(isSaved = true)
        }
    }

    private suspend fun persistImages(operationId: Long, pending: List<PendingImage>, now: Long) {
        val imageDao = operationImageDao
        val processor = imageProcessor
        if (imageDao == null || processor == null || pending.isEmpty()) return

        val processed = pending.mapIndexedNotNull { index, image ->
            processor.processAndSave(image.uri)?.let { result ->
                OperationImage(
                    operationId = operationId,
                    filePath = result.filePath,
                    position = index,
                    createdAt = now
                )
            }
        }
        if (processed.isNotEmpty()) imageDao.insertAll(processed)
    }

    private fun validate(s: OperationFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (s.dateText.isBlank()) errors[OperationFormState.FIELD_DATE] = "Campo obligatorio"
        if (s.timeText.isBlank()) errors[OperationFormState.FIELD_TIME] = "Campo obligatorio"
        if (s.assetId == null) errors[OperationFormState.FIELD_ASSET] = "Campo obligatorio"
        if (s.direction == null) errors[OperationFormState.FIELD_DIRECTION] = "Campo obligatorio"

        if (s.qualityText.isBlank()) {
            errors[OperationFormState.FIELD_QUALITY] = "Campo obligatorio"
        } else {
            val quality = parseDecimal(s.qualityText)
            if (quality == null || quality < 0f || quality > 10f) {
                errors[OperationFormState.FIELD_QUALITY] = "Debe estar entre 0.0 y 10.0"
            }
        }

        if (s.emotionBeforeId == null) errors[OperationFormState.FIELD_EMOTION_BEFORE] = "Campo obligatorio"
        if (s.emotionAfterId == null) errors[OperationFormState.FIELD_EMOTION_AFTER] = "Campo obligatorio"
        if (s.errorId == null) errors[OperationFormState.FIELD_ERROR] = "Campo obligatorio"
        if (s.result == null) errors[OperationFormState.FIELD_RESULT] = "Campo obligatorio"

        if (s.riskPercentageText.isNotBlank()) {
            val risk = parseDecimal(s.riskPercentageText)
            if (risk == null || risk < 0f || risk > 100f) {
                errors[OperationFormState.FIELD_RISK] = "Debe estar entre 0 y 100"
            }
        }

        // Bug real reportado por el usuario: el campo aceptaba cualquier texto (ej.
        // "hhahajajkfkg") sin ningun aviso. Formato esperado tipo "1:2" o "1.5:2".
        if (s.plannedRatio.isNotBlank() && !RATIO_PATTERN.matches(s.plannedRatio.trim())) {
            errors[OperationFormState.FIELD_RATIO] = "Formato inválido, ej. 1:2"
        }

        if (s.entryDescription.isBlank()) errors[OperationFormState.FIELD_DESCRIPTION] = "Campo obligatorio"
        return errors
    }

    /** Bug real reportado por el usuario: el teclado numérico en configuración regional
     * español muestra coma como separador decimal, pero `String.toFloatOrNull()` solo entiende
     * punto — un valor como "7,5" se leía como inválido de forma silenciosa (el campo se veía
     * "lleno" pero seguía bloqueando el guardado). Se normaliza coma -> punto antes de parsear. */
    private fun parseDecimal(text: String): Float? = text.replace(',', '.').toFloatOrNull()

    private fun parseDateTime(dateText: String, timeText: String): Long? {
        return try {
            val date = LocalDate.parse(dateText, DATE_FORMATTER)
            val time = LocalTime.parse(timeText, TIME_FORMATTER)
            LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            null
        }
    }

    companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        /** "Ratio planeado": dos números (con decimales opcionales, coma o punto) separados por
         * ":" — ej. "1:2", "1,5:2". */
        private val RATIO_PATTERN = Regex("""^\d+([.,]\d+)?:\d+([.,]\d+)?$""")
    }

    class Factory(
        private val operationDao: OperationDao,
        private val catalogItemDao: CatalogItemDao,
        private val operationImageDao: OperationImageDao,
        private val imageProcessor: ImageProcessor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OperationFormViewModel(operationDao, catalogItemDao, operationImageDao, imageProcessor) as T
        }
    }
}
