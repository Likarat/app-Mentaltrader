package com.miguel.mentaltrader.feature.inicio

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * HU-028 Notas técnicas: periodo predefinido del selector de Inicio (Día/Semana/Mes/Últimos 3
 * meses). Enum DELIBERADAMENTE SEPARADO de [com.miguel.mentaltrader.feature.historial.PeriodoFiltro]
 * (design.md decisión #7 de este change): los 4 nombres y su semántica de rango difieren de los 5
 * de Historial (ver KDoc de [PeriodoInicioFiltroRange]), y HU-028 no pide ningún rango
 * "personalizado" todavía -- eso es HU-029/EP-004-d, se agregará como una evolución de este mismo
 * enum cuando corresponda (mismo criterio evolutivo ya usado para
 * [com.miguel.mentaltrader.core.data.OperationDao.pagingSourceFiltered], que tampoco anticipó
 * parámetros de un sub-slice futuro).
 */
enum class PeriodoInicioFiltro { DIA, SEMANA, MES, ULTIMOS_3_MESES }

/**
 * HU-028 Escenario 1: resuelve el rango [desde, hasta] (millis epoch, día completo) de un
 * [PeriodoInicioFiltro], recibiendo "hoy" explícito (no `LocalDate.now()` directo) para ser puro y
 * determinista/testeable -- mismo patrón que
 * `com.miguel.mentaltrader.feature.historial.PeriodoFiltroRange.rangeFor` (EP-003). A diferencia de
 * esa función, ESTA siempre devuelve un rango no-nulo: [PeriodoInicioFiltro] no tiene ningún valor
 * "personalizado" todavía (HU-028 solo pide 4 periodos predefinidos), así que no existe ningún caso
 * "sin rango propio" que resolver aquí.
 *
 * Semántica de cada periodo (decisión de producto documentada en design.md decisión #7 de este
 * change -- HU-028 no detalla el rango exacto de cada nombre, solo los enumera):
 * - [PeriodoInicioFiltro.DIA]: el día de hoy completo (00:00:00.000 a 23:59:59.999).
 * - [PeriodoInicioFiltro.SEMANA]: la semana CALENDARIO actual (lunes a hoy, ISO-8601) -- a
 *   diferencia de `PeriodoFiltro.ULTIMA_SEMANA` de Historial (ventana RODANTE de 7 días), aquí
 *   "Semana" (sin calificar "última") se interpreta como semana de calendario, mismo criterio que
 *   distingue "Este mes" (calendario) de "Últimos 3 meses" (rodante) en Historial.
 * - [PeriodoInicioFiltro.MES]: el mes CALENDARIO actual (día 1 del mes que contiene a `today` hasta
 *   hoy) -- mismo criterio semántico que `PeriodoFiltro.ESTE_MES` de Historial.
 * - [PeriodoInicioFiltro.ULTIMOS_3_MESES]: ventana RODANTE de 90 días hacia atrás desde `today`
 *   (nombre explícito "Últimos", misma convención y mismo valor que
 *   `PeriodoFiltro.ULTIMOS_3_MESES` de Historial).
 */
object PeriodoInicioFiltroRange {
    fun rangeFor(periodo: PeriodoInicioFiltro, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val desde = when (periodo) {
            PeriodoInicioFiltro.DIA -> today
            PeriodoInicioFiltro.SEMANA -> today.with(DayOfWeek.MONDAY)
            PeriodoInicioFiltro.MES -> today.withDayOfMonth(1)
            PeriodoInicioFiltro.ULTIMOS_3_MESES -> today.minusDays(89)
        }
        val desdeMillis = desde.atStartOfDay(zone).toInstant().toEpochMilli()
        val hastaMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return desdeMillis to hastaMillis
    }
}

/**
 * HU-028: estado del selector de periodo de Inicio. [dateFrom]/[dateTo] ya vienen resueltos (vía
 * [PeriodoInicioFiltroRange.rangeFor], en el momento de seleccionar) -- [selectedPeriodo] es solo
 * el estado de UI de qué chip está resaltado; HU-028 no pide persistencia entre sesiones todavía
 * (esa es HU-030/EP-004-d). `InicioFilterState()` (todos `null`) es el estado por defecto: sin
 * ningún periodo seleccionado, Inicio muestra el conjunto completo de operaciones -- mismo
 * comportamiento que tenía EP-004-a antes de este sub-slice (design.md decisión #2).
 */
data class InicioFilterState(
    val selectedPeriodo: PeriodoInicioFiltro? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null
)
