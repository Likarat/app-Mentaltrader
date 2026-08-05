package com.miguel.mentaltrader.core.data

/**
 * HU-026: resumen agregado de las operaciones del periodo (EP-004-a asume "todas las
 * operaciones", ver `design.md` decisión #1 -- el selector real de periodo llega con HU-028 en
 * EP-004-b). Calculado completo a nivel de SQL ([OperationDao.metricsSummary], design.md decisión
 * #2) -- este data class solo transporta lo que Room ya devuelve calculado, sin recalcular nada
 * (mismo criterio que [MonthSummary]). La query ya garantiza valores neutros (0) en vez de
 * división por cero o `NULL` cuando no hay ninguna operación (HU-026 Escenario 4), por lo que
 * [EMPTY] es solo un valor inicial de conveniencia para la UI antes de la primera emisión, no un
 * caso especial adicional.
 */
data class OperationMetricsSummary(
    val operationCount: Int,
    val winPercent: Int,
    val lossPercent: Int,
    val breakEvenPercent: Int,
    val avgQuality: Float,
    val totalResultInR: Float,
    val avgResultInR: Float,
    val avgRiskPercentage: Float
) {
    companion object {
        val EMPTY = OperationMetricsSummary(
            operationCount = 0,
            winPercent = 0,
            lossPercent = 0,
            breakEvenPercent = 0,
            avgQuality = 0f,
            totalResultInR = 0f,
            avgResultInR = 0f,
            avgRiskPercentage = 0f
        )
    }
}
