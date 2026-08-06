package com.miguel.mentaltrader.core.data

/**
 * HU-027: fila del ranking de emociones o errores más frecuentes -- id + nombre de [CatalogItem]
 * ya resueltos por `JOIN` en [OperationDao.emotionBeforeRanking]/[OperationDao.emotionAfterRanking]/
 * [OperationDao.errorRanking], más la
 * frecuencia de aparición dentro del periodo (todas las operaciones en EP-004-a, ver `design.md`
 * decisión #1). El orden (frecuencia descendente, nombre ascendente como desempate determinístico
 * -- HU-027 Escenario 3) ya lo resuelve la query SQL; este data class no reordena nada.
 */
data class CatalogRankingItem(
    val id: Long,
    val name: String,
    val frequency: Int
)
