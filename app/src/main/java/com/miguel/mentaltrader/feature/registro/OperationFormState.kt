package com.miguel.mentaltrader.feature.registro

import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType

data class OperationFormState(
    val dateText: String = "",
    val timeText: String = "",
    val assetId: Long? = null,
    val direction: Direction? = null,
    val qualityText: String = "",
    val emotionBeforeId: Long? = null,
    val emotionBeforeReason: String = "",
    val emotionAfterId: Long? = null,
    val emotionAfterReason: String = "",
    val errorId: Long? = null,
    val errorReason: String = "",
    val result: ResultType? = null,
    val riskPercentageText: String = "",
    val resultInRSign: String = SIGN_POSITIVE,
    val resultInRText: String = "",
    val plannedRatio: String = "",
    val entryDescription: String = "",
    val pendingImages: List<PendingImage> = emptyList(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val imageError: String? = null,
    val isSaved: Boolean = false,
    /** HU-013: campo de catálogo (uno de los FIELD_*) cuyo alta inline está abierta; null si
     * ninguna está en curso. Solo uno puede estar abierto a la vez. */
    val addingCatalogField: String? = null,
    val addCatalogText: String = "",
    val addCatalogError: String? = null
) {
    companion object {
        val INITIAL = OperationFormState()
        const val MAX_IMAGES = 2

        // Claves de fieldErrors, usadas también por la UI y los tests para identificar qué campo falló.
        const val FIELD_DATE = "date"
        const val FIELD_TIME = "time"
        const val FIELD_ASSET = "asset"
        const val FIELD_DIRECTION = "direction"
        const val FIELD_QUALITY = "quality"
        const val FIELD_EMOTION_BEFORE = "emotionBefore"
        const val FIELD_EMOTION_AFTER = "emotionAfter"
        const val FIELD_ERROR = "error"
        const val FIELD_RESULT = "result"
        const val FIELD_RESULT_IN_R = "resultInR"
        const val FIELD_RISK = "risk"
        const val FIELD_RATIO = "ratio"
        const val FIELD_DESCRIPTION = "description"

        // HU-004: el signo de "Resultado en R" se define solo por este dropdown, nunca como
        // texto libre en el campo numérico.
        const val SIGN_POSITIVE = "+"
        const val SIGN_NEGATIVE = "-"

        /** "Resultado en R" con signo aplicado y sufijo "R" fijo, para mostrar en toda la UI
         * (HU-004 Esc.4). Null si la magnitud está vacía o no es numérica. */
        fun formatResultInR(sign: String, magnitudeText: String): String? {
            // Teclado en configuración regional español: coma como separador decimal.
            val magnitude = magnitudeText.replace(',', '.').toFloatOrNull() ?: return null
            val signed = if (sign == SIGN_NEGATIVE) -magnitude else magnitude
            return "${if (signed >= 0) "+" else ""}${signed}R"
        }
    }
}

/** Imagen elegida (cámara/galería) pero todavía no procesada ni persistida (HU-006). */
data class PendingImage(val uri: android.net.Uri)
