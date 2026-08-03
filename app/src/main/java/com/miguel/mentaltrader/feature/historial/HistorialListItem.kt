package com.miguel.mentaltrader.feature.historial

import com.miguel.mentaltrader.core.data.Operation
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** HU-015: elemento del listado de Historial ya "aplanado" para Paging 3 -- una fila real de
 * operación, o un encabezado de agrupación insertado por [HistorialSeparators]. */
sealed interface HistorialListItem {
    /** [month] no nulo solo cuando este punto del listado también inicia una nueva sección de
     * mes (evita insertar dos separadores -- mes y día -- en el mismo hueco entre dos filas). */
    data class GroupHeader(val month: YearMonth?, val day: LocalDate) : HistorialListItem
    data class OperationRow(val operation: Operation) : HistorialListItem
}

/** HU-015: decide qué [HistorialListItem.GroupHeader] insertar entre dos operaciones adyacentes
 * ya ordenadas por `dateTime` descendente. Lógica pura (sin Room/Paging) para poder testearla en
 * JVM sin necesitar la dependencia `androidx.paging:paging-testing` -- se conecta a
 * `PagingData.insertSeparators` desde `HistorialViewModel`. */
object HistorialSeparators {
    fun between(before: Operation?, after: Operation?): HistorialListItem.GroupHeader? {
        if (after == null) return null
        val afterDay = dayOf(after)
        val afterMonth = YearMonth.from(afterDay)
        if (before == null) return HistorialListItem.GroupHeader(afterMonth, afterDay)

        val beforeDay = dayOf(before)
        if (afterDay == beforeDay) return null
        val beforeMonth = YearMonth.from(beforeDay)
        return if (afterMonth != beforeMonth) {
            HistorialListItem.GroupHeader(afterMonth, afterDay)
        } else {
            HistorialListItem.GroupHeader(null, afterDay)
        }
    }

    fun dayOf(operation: Operation): LocalDate =
        Instant.ofEpochMilli(operation.dateTime).atZone(ZoneId.systemDefault()).toLocalDate()
}

/** HU-017: mes real al que pertenece un [HistorialListItem]. Se calcula por item (no por
 * posición en la lista) para que funcione correctamente con la virtualización de LazyColumn --
 * un item puede componerse aislado, sin haber recorrido antes su encabezado de mes. */
fun HistorialListItem.monthOf(): YearMonth = when (this) {
    is HistorialListItem.GroupHeader -> YearMonth.from(day)
    is HistorialListItem.OperationRow -> YearMonth.from(HistorialSeparators.dayOf(operation))
}

/** HU-017 Escenario 2: decide si un item del listado debe renderizarse dado el conjunto de
 * meses colapsados -- estado de UI puro, no afecta ninguna query (design.md decisión #2). El
 * encabezado de mes ([HistorialListItem.GroupHeader] con `month` no nulo) SIEMPRE es visible,
 * incluso colapsado, para que el usuario pueda volver a expandirlo tocándolo. */
object HistorialGroupCollapse {
    fun isVisible(item: HistorialListItem, collapsedMonths: Set<YearMonth>): Boolean {
        val isMonthHeader = item is HistorialListItem.GroupHeader && item.month != null
        if (isMonthHeader) return true
        return item.monthOf() !in collapsedMonths
    }
}
