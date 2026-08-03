package com.miguel.mentaltrader.core.data

import java.time.YearMonth
import java.util.Locale

/**
 * HU-017: resumen agregado de un mes para el encabezado de grupo de Historial. El cálculo de
 * [operationCount]/[winRatePercent]/[totalResultInR] se resuelve a nivel de SQL en
 * [OperationDao.monthlySummaries] (design.md decisión #2, "no en memoria") -- este data class
 * solo transporta lo que Room ya devuelve calculado, sin recalcular nada.
 */
data class MonthSummary(
    val year: Int,
    val month: Int,
    val operationCount: Int,
    val winRatePercent: Int,
    val totalResultInR: Float
) {
    val yearMonth: YearMonth get() = YearMonth.of(year, month)
}

/** HU-017 Escenario 4: signo del R acumulado del mes, para decidir el color de la línea de
 * resumen (verde/rojo/gris, misma codificación de color que el resto de la app -- ver
 * `OperationCard`/`HistorialDetalleScreen`). Lógica pura, sin Compose ni Room, testeada aparte de
 * la propia agregación SQL. */
enum class ResultSign { POSITIVE, NEGATIVE, NEUTRAL }

val MonthSummary.resultSign: ResultSign
    get() = when {
        totalResultInR > 0f -> ResultSign.POSITIVE
        totalResultInR < 0f -> ResultSign.NEGATIVE
        else -> ResultSign.NEUTRAL
    }

/** HU-017 Escenario 1/3: texto del resumen mostrado en el encabezado de mes. Función pura
 * (formateo), separada para poder testear en JVM que el caso de 0% ganadas (Escenario 3) se
 * formatea sin errores de cálculo (sin división por cero -- Room ya garantiza `COUNT(*) >= 1`
 * por grupo, ver [OperationDao.monthlySummaries]). Usa explícitamente `Locale.US` (punto decimal,
 * no coma) para que el resumen sea determinista sin importar el locale del dispositivo -- bug
 * real encontrado durante el TDD de esta historia: `"%.1f".format(value)` sin locale explícito
 * usa el locale por defecto de la JVM/dispositivo, que en es-ES produce coma decimal. */
object MonthSummaryFormatting {
    fun summaryText(summary: MonthSummary): String =
        "${summary.operationCount} op · ${summary.winRatePercent}% ganadas · " +
            "${"%.1f".format(Locale.US, summary.totalResultInR)}R"
}
