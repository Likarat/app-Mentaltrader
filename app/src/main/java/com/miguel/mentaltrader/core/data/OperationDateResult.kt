package com.miguel.mentaltrader.core.data

/**
 * HU-037 (nuevo slice, post-construccion): proyección de [OperationDao.resultInROrderedByDateAsc]
 * que, además del `resultInR`, carga la fecha de la operación -- necesaria para que
 * `ResultInRCumulativeSeries.Point` pueda exponer la fecha de cada punto (el tooltip de arrastre
 * de la gráfica de R acumulado la necesita, HU-037 Escenario 1). Antes esa query devolvía
 * `List<Float?>` puro; se amplía a esta proyección de 2 columnas sin cambiar el criterio de
 * filtrado/orden ya existente.
 */
data class OperationDateResult(
    val dateTime: Long,
    val resultInR: Float?
)
