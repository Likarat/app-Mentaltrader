package com.miguel.mentaltrader.feature.inicio

import com.miguel.mentaltrader.core.data.InicioFilterSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * HU-028 Notas técnicas: periodo predefinido del selector de Inicio (Día/Semana/Mes/Últimos 3
 * meses/Personalizado). Enum DELIBERADAMENTE SEPARADO de
 * [com.miguel.mentaltrader.feature.historial.PeriodoFiltro] (design.md decisión #7 de este change):
 * los nombres y su semántica de rango difieren de los de Historial (ver KDoc de
 * [PeriodoInicioFiltroRange]). [PERSONALIZADO] se agregó en HU-029/EP-004-d (evolución prevista
 * explícitamente por la decisión #7, punto 3: no existía en el alcance original de HU-028).
 */
enum class PeriodoInicioFiltro { DIA, SEMANA, MES, ULTIMOS_3_MESES, PERSONALIZADO }

/**
 * HU-028 Escenario 1 / HU-029 (PERSONALIZADO): resuelve el rango [desde, hasta] (millis epoch, día
 * completo) de un [PeriodoInicioFiltro], recibiendo "hoy" explícito (no `LocalDate.now()` directo)
 * para ser puro y determinista/testeable -- mismo patrón que
 * `com.miguel.mentaltrader.feature.historial.PeriodoFiltroRange.rangeFor` (EP-003). Devuelve `null`
 * para [PeriodoInicioFiltro.PERSONALIZADO] (HU-029): ese periodo no tiene rango propio, lo aporta el
 * usuario directamente en [InicioFilterState.dateFrom]/[InicioFilterState.dateTo] (calendario/campos
 * de fecha reales de `InicioScreen`, ver `InicioViewModel.onCustomDateRange`) -- mismo criterio
 * exacto que `PeriodoFiltroRange.rangeFor` de Historial para su propio PERSONALIZADO.
 *
 * Semántica de cada periodo predefinido (decisión de producto documentada en design.md decisión #7
 * de este change -- HU-028 no detalla el rango exacto de cada nombre, solo los enumera):
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
 * - [PeriodoInicioFiltro.PERSONALIZADO]: sin rango propio, ver arriba.
 */
object PeriodoInicioFiltroRange {
    fun rangeFor(periodo: PeriodoInicioFiltro, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long>? {
        val desde = when (periodo) {
            PeriodoInicioFiltro.DIA -> today
            PeriodoInicioFiltro.SEMANA -> today.with(DayOfWeek.MONDAY)
            PeriodoInicioFiltro.MES -> today.withDayOfMonth(1)
            PeriodoInicioFiltro.ULTIMOS_3_MESES -> today.minusDays(89)
            PeriodoInicioFiltro.PERSONALIZADO -> return null
        }
        val desdeMillis = desde.atStartOfDay(zone).toInstant().toEpochMilli()
        val hastaMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return desdeMillis to hastaMillis
    }
}

/**
 * HU-028/HU-029/HU-030: estado del selector de periodo de Inicio. [dateFrom]/[dateTo] ya vienen
 * resueltos (vía [PeriodoInicioFiltroRange.rangeFor] para un periodo predefinido, o aportados
 * directamente por el usuario para [PeriodoInicioFiltro.PERSONALIZADO], HU-029) --
 * [selectedPeriodo] es solo el estado de UI de qué chip está resaltado. `InicioFilterState()`
 * (todos `null`) es el estado por defecto: sin ningún periodo seleccionado, Inicio muestra el
 * conjunto completo de operaciones -- mismo comportamiento que tenía EP-004-a antes del selector
 * (design.md decisión #2), y también el periodo por defecto razonable de HU-030 Escenario 3 (ver
 * KDoc de [fromSnapshot]).
 */
data class InicioFilterState(
    val selectedPeriodo: PeriodoInicioFiltro? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null
) {
    /** HU-030: convierte a [InicioFilterSnapshot] (tipos neutrales de `core.data`, sin enum de
     * `feature.inicio`) para persistir vía `InicioFilterRepository`. [customRangeStart]/
     * [customRangeEnd] del snapshot SOLO se completan cuando el periodo activo es
     * [PeriodoInicioFiltro.PERSONALIZADO] -- un periodo predefinido se recalcula relativo al "hoy"
     * real al restaurar (ver [fromSnapshot]), así que no tiene sentido persistir su rango de fechas
     * ya resuelto (quedaría con una fecha vieja congelada), mismo criterio que
     * `HistorialFilterState.toSnapshot`. */
    fun toSnapshot(): InicioFilterSnapshot = InicioFilterSnapshot(
        periodName = selectedPeriodo?.name,
        customRangeStart = if (selectedPeriodo == PeriodoInicioFiltro.PERSONALIZADO) dateFrom else null,
        customRangeEnd = if (selectedPeriodo == PeriodoInicioFiltro.PERSONALIZADO) dateTo else null
    )

    companion object {
        /**
         * HU-030 Escenario 3: reconstruye un [InicioFilterState] real a partir de un
         * [InicioFilterSnapshot] persistido -- [today] explícito (no `LocalDate.now()` directo) para
         * que la resolución de un periodo predefinido sea determinista/testeable, igual que
         * [PeriodoInicioFiltroRange.rangeFor].
         *
         * Decisión de producto documentada (design.md decisión #8 de este change, INVEST de HU-030:
         * "el valor por defecto exacto ... es negociable"): sin ningún filtro guardado previamente
         * ([InicioFilterSnapshot.periodName] `null` -- primera vez real, o el usuario deseleccionó
         * el periodo la última vez que usó la app, ambos casos indistinguibles en disco, mismo
         * criterio de "estado vacío = sin filtro" ya usado por `HistorialFilterSnapshot`), el
         * "periodo por defecto razonable" que exige el Escenario 3 es **"todas las operaciones"**
         * (`InicioFilterState()`, idéntico al comportamiento ya existente desde EP-004-a/b) -- no un
         * periodo predefinido concreto como "Mes": un usuario con datos históricos pero ninguno en
         * el mes/semana actual vería "sin datos para el periodo" de entrada (HU-031
         * `SIN_DATOS_PERIODO`), un resultado engañoso para un primer arranque. "Todas las
         * operaciones" es reasonable porque siempre muestra algo real cuando existe, sin error ni
         * pantalla en blanco (satisface la letra del Escenario 3), y no reintroduce la ambigüedad de
         * distinguir "nunca seleccionó" de "deseleccionó explícitamente".
         */
        fun fromSnapshot(snapshot: InicioFilterSnapshot, today: LocalDate = LocalDate.now()): InicioFilterState {
            val periodo = snapshot.periodName?.let { name -> PeriodoInicioFiltro.entries.firstOrNull { it.name == name } }
            var dateFrom: Long? = null
            var dateTo: Long? = null
            when (periodo) {
                null -> Unit
                PeriodoInicioFiltro.PERSONALIZADO -> {
                    dateFrom = snapshot.customRangeStart
                    dateTo = snapshot.customRangeEnd
                }
                else -> {
                    val range = PeriodoInicioFiltroRange.rangeFor(periodo, today)
                    dateFrom = range?.first
                    dateTo = range?.second
                }
            }
            return InicioFilterState(selectedPeriodo = periodo, dateFrom = dateFrom, dateTo = dateTo)
        }
    }
}
