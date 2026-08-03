package com.miguel.mentaltrader.feature.historial

import com.miguel.mentaltrader.core.data.HistorialFilterSnapshot
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.ResultType
import java.time.LocalDate
import java.time.ZoneId

/** HU-022 Notas técnicas: periodo predefinido para el filtro de fecha. */
enum class PeriodoFiltro { ULTIMA_SEMANA, ULTIMO_MES, ESTE_MES, ULTIMOS_3_MESES, PERSONALIZADO }

/** HU-022 Escenario 2: resuelve el rango [desde, hasta] (millis epoch, día completo) de un
 * [PeriodoFiltro] predefinido, recibiendo "hoy" explícito (no `LocalDate.now()`) para ser puro y
 * determinista/testeable. "Última semana"/"Último mes"/"Últimos 3 meses" son ventanas rodantes de
 * días (nota técnica explícita de HU-022: "'Última semana' es un filtro (últimos 7 días)"), NO
 * meses de calendario -- "Este mes" sí lo es, consistente con su nombre. [PeriodoFiltro.PERSONALIZADO]
 * no tiene rango propio: lo aporta el usuario directamente en [HistorialFilterState.dateFrom]/
 * [HistorialFilterState.dateTo] (selector de fecha personalizado, HistorialScreen). */
object PeriodoFiltroRange {
    fun rangeFor(periodo: PeriodoFiltro, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long>? {
        val desde = when (periodo) {
            PeriodoFiltro.ULTIMA_SEMANA -> today.minusDays(6)
            PeriodoFiltro.ULTIMO_MES -> today.minusDays(29)
            PeriodoFiltro.ESTE_MES -> today.withDayOfMonth(1)
            PeriodoFiltro.ULTIMOS_3_MESES -> today.minusDays(89)
            PeriodoFiltro.PERSONALIZADO -> return null
        }
        val desdeMillis = desde.atStartOfDay(zone).toInstant().toEpochMilli()
        val hastaMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return desdeMillis to hastaMillis
    }
}

/** HU-022/HU-023: estado de filtro+búsqueda de Historial. Todos los campos nulos (estado por
 * defecto / "Limpiar filtros", HU-022 Escenario 4) significan "sin ese criterio". [dateFrom]/
 * [dateTo] ya vienen resueltos (sea desde un [PeriodoFiltro] predefinido vía [PeriodoFiltroRange],
 * sea un rango personalizado elegido por el usuario) -- [selectedPeriodo] es solo el estado de UI
 * de qué chip está seleccionado (para resaltarlo), no afecta el filtrado en sí. */
data class HistorialFilterState(
    val assetId: Long? = null,
    val result: ResultType? = null,
    val errorId: Long? = null,
    val emotionBeforeId: Long? = null,
    val emotionAfterId: Long? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val searchText: String? = null,
    val selectedPeriodo: PeriodoFiltro? = null
) {
    /** HU-022 Escenario 4: "Limpiar filtros" restaura este estado exacto (sin ningún criterio
     * activo, listado completo agrupado por mes). */
    val isEmpty: Boolean get() = this == HistorialFilterState()

    /** HU-024: convierte a [HistorialFilterSnapshot] (tipos neutrales de `core.data`, sin `Long`
     * de periodo ni enum de `feature.historial`) para persistir vía `HistorialFilterRepository`.
     * [customRangeStart]/[customRangeEnd] del snapshot solo se completan cuando el periodo activo
     * es [PeriodoFiltro.PERSONALIZADO] -- un periodo predefinido se recalcula relativo al "hoy"
     * real al restaurar (ver [HistorialFilterState.fromSnapshot]), así que no tiene sentido
     * persistir su rango de fechas ya resuelto (quedaría con una fecha vieja congelada). */
    fun toSnapshot(): HistorialFilterSnapshot = HistorialFilterSnapshot(
        periodName = selectedPeriodo?.name,
        customRangeStart = if (selectedPeriodo == PeriodoFiltro.PERSONALIZADO) dateFrom else null,
        customRangeEnd = if (selectedPeriodo == PeriodoFiltro.PERSONALIZADO) dateTo else null,
        searchText = searchText,
        assetId = assetId,
        result = result,
        errorId = errorId,
        emotionBeforeId = emotionBeforeId,
        emotionAfterId = emotionAfterId
    )

    companion object {
        /** HU-024 Escenario 1/2/3: reconstruye un [HistorialFilterState] real a partir de un
         * [HistorialFilterSnapshot] persistido -- [today] explícito (no `LocalDate.now()` directo)
         * para que la resolución de un periodo predefinido sea determinista/testeable, igual que
         * [PeriodoFiltroRange.rangeFor]. Sin snapshot previo (todos los campos en `null`, Escenario
         * 3) devuelve exactamente `HistorialFilterState()` -- el estado por defecto. */
        fun fromSnapshot(snapshot: HistorialFilterSnapshot, today: LocalDate = LocalDate.now()): HistorialFilterState {
            val periodo = snapshot.periodName?.let { name -> PeriodoFiltro.entries.firstOrNull { it.name == name } }
            var dateFrom: Long? = null
            var dateTo: Long? = null
            when (periodo) {
                null -> Unit
                PeriodoFiltro.PERSONALIZADO -> {
                    dateFrom = snapshot.customRangeStart
                    dateTo = snapshot.customRangeEnd
                }
                else -> {
                    val range = PeriodoFiltroRange.rangeFor(periodo, today)
                    dateFrom = range?.first
                    dateTo = range?.second
                }
            }
            return HistorialFilterState(
                assetId = snapshot.assetId,
                result = snapshot.result,
                errorId = snapshot.errorId,
                emotionBeforeId = snapshot.emotionBeforeId,
                emotionAfterId = snapshot.emotionAfterId,
                dateFrom = dateFrom,
                dateTo = dateTo,
                searchText = snapshot.searchText,
                selectedPeriodo = periodo
            )
        }
    }
}

/** HU-023 Escenario 1: nombres de catálogo ya resueltos (Activo/Emoción antes/Emoción después/
 * Error) de UNA operación concreta -- la búsqueda de texto también coincide contra estos nombres,
 * no solo contra los 4 campos de texto libre de la operación (ver HU-023 Escenario 1: "...o
 * nombres de catálogo seleccionados"). */
data class OperationCatalogNames(
    val assetName: String? = null,
    val emotionBeforeName: String? = null,
    val emotionAfterName: String? = null,
    val errorName: String? = null
)

/** HU-022 Escenarios 1-4 + HU-023 Escenarios 1-3: decide si una [Operation] cumple un
 * [HistorialFilterState] completo, combinando TODOS los criterios activos con lógica AND (HU-022
 * Escenario 3, HU-023 Escenario 2). Lógica PURA (sin Room/SQL), testeada exhaustivamente en JVM --
 * especifica sin ambigüedad el comportamiento que
 * [com.miguel.mentaltrader.core.data.OperationDao.pagingSourceFiltered] debe replicar en SQL
 * (design.md decisión #5: la query real corre en SQL para que la paginación no cargue el
 * histórico completo a memoria, HU-016; la equivalencia entre esta función y esa query SQL queda
 * pendiente de un test instrumentado real, tasks.md 5.6, diferido a la pasada final única sin
 * dispositivo conectado en esta sesión). */
object HistorialFilterMatcher {
    fun matches(operation: Operation, catalogNames: OperationCatalogNames, filter: HistorialFilterState): Boolean {
        if (filter.assetId != null && filter.assetId != operation.assetId) return false
        if (filter.result != null && filter.result != operation.result) return false
        if (filter.errorId != null && filter.errorId != operation.errorId) return false
        if (filter.emotionBeforeId != null && filter.emotionBeforeId != operation.emotionBeforeId) return false
        if (filter.emotionAfterId != null && filter.emotionAfterId != operation.emotionAfterId) return false
        if (filter.dateFrom != null && operation.dateTime < filter.dateFrom) return false
        if (filter.dateTo != null && operation.dateTime > filter.dateTo) return false

        val needle = filter.searchText?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
        if (needle != null) {
            val haystacks = listOfNotNull(
                operation.entryDescription,
                operation.emotionBeforeReason,
                operation.emotionAfterReason,
                operation.errorReason,
                catalogNames.assetName,
                catalogNames.emotionBeforeName,
                catalogNames.emotionAfterName,
                catalogNames.errorName
            )
            if (haystacks.none { it.lowercase().contains(needle) }) return false
        }
        return true
    }
}
