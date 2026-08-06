package com.miguel.mentaltrader.core.data

/**
 * Fila de vista previa de una operación que referencia un [CatalogItem] en alguno de sus 4 roles
 * posibles -- usada por [OperationDao.getUsageSummaries] para listar, de forma acotada, qué
 * operaciones bloquean el borrado de un elemento de catálogo en uso.
 */
data class OperationUsageSummary(
    val id: Long,
    val dateTime: Long,
    val assetId: Long
)
